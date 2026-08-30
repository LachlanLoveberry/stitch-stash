package com.lachlan.stitchstash.ui.finishcard

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinishCardScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()
    private var completionId: Long = -1

    @Before
    fun resetData() = runBlocking {
        val repo = app.repository
        repo.observeFinishCards().first().forEach { repo.deleteFinishCard(it.id) }
        repo.observeAllCompletions().first().forEach { repo.deleteCompletion(it.id) }
        repo.observePatterns().first().forEach { repo.deletePattern(it.id) }

        val patternId = repo.addPattern(Pattern(name = "Card Fox"))
        val colourwayId = repo.addColourway(Colourway(patternId = patternId, name = "Rust", targetCount = 3))
        val result = repo.addCompletionAndEarnStickers(
            Completion(colourwayId = colourwayId, completedAtEpochDay = 19000L),
        )
        completionId = result.completion.id
    }

    /** Rendering is a real bitmap-to-file operation; wait for the Save button to become enabled. */
    private fun waitForRenderToFinish() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText("Save").assertIsEnabled() }.isSuccess
        }
    }

    @Test
    fun rendersCard_selectsBorderStyle_andSaves() {
        composeRule.setContent {
            StitchStashTheme {
                FinishCardScreen(completionId = completionId, onBack = {})
            }
        }
        waitForRenderToFinish()

        composeRule.onNodeWithText("Border style").assertIsDisplayed()

        // Switch to a non-default border style and let it re-render.
        composeRule.onNodeWithText("Simple").performClick()
        waitForRenderToFinish()

        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        val cards = runBlocking { app.repository.observeFinishCards().first() }
        assert(cards.any { it.completionId == completionId }) {
            "Finish card was not persisted for completion $completionId"
        }
    }
}
