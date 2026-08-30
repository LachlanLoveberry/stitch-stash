package com.lachlan.stitchstash.ui.log

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Photo attachment is intentionally not exercised here: PickVisualMedia launches the system
 * photo picker UI, which is outside the app's process and not something Espresso/Compose test
 * APIs can drive without a real gallery + Instrumentation.registerIntent-style stubbing. The
 * "log without a photo" path below covers persistence end-to-end regardless.
 */
@RunWith(AndroidJUnit4::class)
class LogCompletionScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()
    private var patternId: Long = -1
    private var colourwayId: Long = -1

    @Before
    fun resetData() = runBlocking {
        val repo = app.repository
        repo.observeAllCompletions().first().forEach { repo.deleteCompletion(it.id) }
        repo.observePatterns().first().forEach { repo.deletePattern(it.id) }

        patternId = repo.addPattern(Pattern(name = "Seeded Owl"))
        colourwayId = repo.addColourway(
            Colourway(patternId = patternId, name = "Grey", targetCount = 2),
        )
    }

    @Test
    fun saveButton_disabledUntilColourwaySelected() {
        composeRule.setContent {
            StitchStashTheme {
                LogCompletionScreen(onDone = {}, onBack = {}, onAddPattern = {}, onCreateCard = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Celebrate this one ").assertIsNotEnabled()

        composeRule.onNodeWithText("Seeded Owl").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Grey").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Celebrate this one ").assertIsEnabled()
    }

    @Test
    fun selectPatternAndColourway_logsCompletion_andPersistsIt() {
        var doneCalled = false
        composeRule.setContent {
            StitchStashTheme {
                LogCompletionScreen(
                    onDone = { doneCalled = true },
                    onBack = {},
                    onAddPattern = {},
                    onCreateCard = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Seeded Owl").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Grey").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Celebrate this one ").performClick()
        composeRule.waitForIdle()

        // The celebration dialog should appear (logCompletion is fire-and-forget through a
        // coroutine, so give composition a beat to settle after the DB write completes).
        composeRule.waitForIdle()

        val completions = runBlocking { app.repository.observeAllCompletions().first() }
        assert(completions.any { it.colourwayId == colourwayId }) {
            "Completion was not persisted for the selected colourway"
        }

        // Dismiss the celebration dialog via its "Done" button, which triggers onDone.
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()
        assert(doneCalled) { "onDone was not invoked after dismissing the celebration dialog" }
    }

    @Test
    fun emptyPatternState_showsAddPatternPrompt() {
        runBlocking {
            app.repository.observePatterns().first().forEach { app.repository.deletePattern(it.id) }
        }

        composeRule.setContent {
            StitchStashTheme {
                LogCompletionScreen(onDone = {}, onBack = {}, onAddPattern = {}, onCreateCard = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Add a pattern first").assertIsDisplayed()
    }
}
