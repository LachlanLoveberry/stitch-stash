package com.lachlan.stitchstash.ui.markets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MarketsScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() = runBlocking {
        allMarkets().forEach { app.repository.deleteMarket(it.id) }
    }

    private suspend fun allMarkets(): List<Market> = app.repository.observeAllMarkets().first()

    /**
     * Taps today's day cell in the currently-open Material3 DatePicker calendar grid, then
     * confirms with "OK". Each day cell's accessible label is a full sentence like
     * "Saturday 30 August 2026", so the day number must be matched as a substring surrounded
     * by spaces (an exact match against just "30" never matches).
     */
    private fun pickTodayInDatePicker() {
        val day = LocalDate.now().dayOfMonth.toString()
        composeRule.onAllNodes(hasText(" $day ", substring = true) and hasClickAction())
            .onFirst()
            .performClick()
        composeRule.onNodeWithText("OK").performClick()
    }

    /** The dialog's own confirm button shares its "Add" label with the top app bar action. */
    private fun clickDialogAddButton() {
        composeRule.onAllNodesWithText("Add").onLast().performClick()
    }

    @Test
    fun addMarketWithNameAndDate_persists() {
        composeRule.setContent {
            StitchStashTheme {
                MarketsScreen(onNavigate = {})
            }
        }

        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithText("Name (optional)").performTextInput("Spring Fair")
        composeRule.onNodeWithText("Pick date").performClick()
        pickTodayInDatePicker()
        composeRule.waitForIdle()
        clickDialogAddButton()
        composeRule.waitForIdle()

        val saved = runBlocking { allMarkets() }
        assertEquals(1, saved.size)
        assertEquals("Spring Fair", saved[0].name)
    }

    @Test
    fun addMarketWithBlankName_storesNullName() {
        composeRule.setContent {
            StitchStashTheme {
                MarketsScreen(onNavigate = {})
            }
        }

        composeRule.onNodeWithText("Add").performClick()
        // Leave name blank, just pick a date.
        composeRule.onNodeWithText("Pick date").performClick()
        pickTodayInDatePicker()
        composeRule.waitForIdle()
        clickDialogAddButton()
        composeRule.waitForIdle()

        val saved = runBlocking { allMarkets() }
        assertEquals(1, saved.size)
        assertNull(saved[0].name)
        // Row falls back to a default label when name is null.
        composeRule.onNodeWithText(" Your market").assertExists()
    }

    @Test
    fun rescheduleAndSkipAndDelete_flow() {
        runBlocking { app.repository.addMarket("Winter Market", LocalDate.now().plusDays(10)) }

        composeRule.setContent {
            StitchStashTheme {
                MarketsScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        // Skip toggles isSkipped.
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()
        var saved = runBlocking { allMarkets() }
        assertTrue(saved.single().isSkipped)

        // Unskip toggles it back.
        composeRule.onNodeWithText("Unskip").performClick()
        composeRule.waitForIdle()
        saved = runBlocking { allMarkets() }
        assertTrue(!saved.single().isSkipped)

        // Reschedule via the date picker's OK button exercises the reschedule call path.
        composeRule.onNodeWithText("Reschedule").performClick()
        pickTodayInDatePicker()
        composeRule.waitForIdle()
        saved = runBlocking { allMarkets() }
        assertEquals(1, saved.size)

        // Delete requires confirming the dialog. The row's own "Delete" button stays composed
        // (just visually obscured) behind the ConfirmDeleteDialog, so once the dialog is open
        // there are two "Delete" nodes — the dialog's confirm button is the last one added.
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Delete").onLast().performClick()
        composeRule.waitForIdle()
        saved = runBlocking { allMarkets() }
        assertTrue(saved.isEmpty())
    }

    /**
     * [MarketReflectionDialog] is not wired into [MarketsScreen]'s composition (it's shown
     * elsewhere, e.g. from Home, based on [com.lachlan.stitchstash.data.repository.StitchRepository.observeNextMarket]
     * / reflection-prompt logic), so it's exercised directly here.
     */
    @Test
    fun marketReflectionDialog_savesEnteredFields() {
        val market = runBlocking {
            val id = app.repository.addMarket("Reflect Market", LocalDate.now().minusDays(1))
            app.database.marketDao().getById(id)!!
        }

        var saved: Triple<String?, String?, String?>? = null
        composeRule.setContent {
            StitchStashTheme {
                MarketReflectionDialog(
                    market = market,
                    onDismissDidNotGo = {},
                    onSaveReflection = { a, b, c -> saved = Triple(a, b, c) },
                    onSkipReflection = {},
                )
            }
        }

        composeRule.onNodeWithText("I went!").performClick()
        composeRule.onNodeWithText("How did it go?").assertExists()

        composeRule.onNode(hasText("Sales, conversations, anything that stood out..."))
            .performTextInput("Sold ten hats")
        composeRule.onNode(hasText("Proud, tired, inspired — however it landed"))
            .performTextInput("Proud")
        composeRule.onNode(hasText("Something to try again, or differently, next time"))
            .performTextInput("Bring more stock")

        composeRule.onNodeWithText("Save reflection").performClick()
        composeRule.waitForIdle()

        assertEquals("Sold ten hats", saved?.first)
        assertEquals("Proud", saved?.second)
        assertEquals("Bring more stock", saved?.third)
    }
}
