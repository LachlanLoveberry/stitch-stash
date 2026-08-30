package com.lachlan.stitchstash.ui.marketprep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MarketPrepScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    private lateinit var market: Market

    @Before
    fun seedMarket() = runBlocking {
        // Clear existing markets (and their todos cascade away with them via marketId FK
        // semantics in this schema — todos are cleaned up per-market below regardless).
        app.repository.observeAllMarkets().first().forEach { app.repository.deleteMarket(it.id) }

        // observeNextMarket() requires an upcoming, non-skipped market to produce non-null state.
        val id = app.repository.addMarket("Prep Market", LocalDate.now().plusDays(5))
        market = app.database.marketDao().getById(id)!!

        // Clear any pre-existing todos for this market id (fresh id each run, but be defensive).
        app.repository.observeMarketTodos(market.id).first().forEach {
            app.repository.deleteMarketTodo(it.id)
        }
    }

    @Test
    fun addTodo_trimsAndPersists() {
        composeRule.setContent {
            StitchStashTheme {
                MarketPrepScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Add something to prep — signage, float, packaging...")
            .performTextInput("  Bring float cash  ")
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.waitForIdle()

        val todos = runBlocking { app.repository.observeMarketTodos(market.id).first() }
        assertEquals(1, todos.size)
        assertEquals("Bring float cash", todos[0].text)
    }

    @Test
    fun addBlankTodo_doesNothing() {
        composeRule.setContent {
            StitchStashTheme {
                MarketPrepScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        // Whitespace-only text trims to empty in the ViewModel and is a no-op.
        composeRule.onNodeWithText("Add something to prep — signage, float, packaging...")
            .performTextInput("   ")
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.waitForIdle()

        val todos = runBlocking { app.repository.observeMarketTodos(market.id).first() }
        assertTrue(todos.isEmpty())
    }

    @Test
    fun toggleTodo_flipsDoneState() {
        runBlocking { app.repository.addMarketTodo(market.id, "Signage") }

        composeRule.setContent {
            StitchStashTheme {
                MarketPrepScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Signage").assertExists()
        // Checkbox has no content description, so locate it by its toggleable semantics role.
        composeRule.onNode(androidx.compose.ui.test.isToggleable()).performClick()
        composeRule.waitForIdle()

        val todos = runBlocking { app.repository.observeMarketTodos(market.id).first() }
        assertTrue(todos.single().isDone)
    }

    @Test
    fun deleteTodo_removesIt() {
        runBlocking { app.repository.addMarketTodo(market.id, "Packaging") }

        composeRule.setContent {
            StitchStashTheme {
                MarketPrepScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Packaging").assertExists()
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()

        val todos = runBlocking { app.repository.observeMarketTodos(market.id).first() }
        assertTrue(todos.isEmpty())
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescription(
    description: String,
) = this.onNode(androidx.compose.ui.test.hasContentDescription(description))
