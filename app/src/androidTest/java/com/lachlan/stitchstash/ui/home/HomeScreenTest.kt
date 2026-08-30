package com.lachlan.stitchstash.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
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
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() = runBlocking {
        val repo = app.repository
        repo.observeAllCompletions().first().forEach { repo.deleteCompletion(it.id) }
        repo.observePatterns().first().forEach { repo.deletePattern(it.id) }
    }

    @Test
    fun emptyState_showsFallbackRecentWinsMessage() {
        composeRule.setContent {
            StitchStashTheme {
                HomeScreen(onLogFinish = {}, onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            "Your finished pieces will land here — every one's worth celebrating. ",
        ).assertIsDisplayed()
    }

    @Test
    fun completedPiece_showsInRecentWinsAndCelebrateButtonNavigatesToLog() {
        runBlocking {
            val patternId = app.repository.addPattern(Pattern(name = "Home Fox"))
            val colourwayId = app.repository.addColourway(
                Colourway(patternId = patternId, name = "Rust", targetCount = 3),
            )
            app.repository.addCompletionAndEarnStickers(
                Completion(colourwayId = colourwayId, completedAtEpochDay = 19000L),
            )
        }

        var loggedFinishClicked = false
        composeRule.setContent {
            StitchStashTheme {
                HomeScreen(onLogFinish = { loggedFinishClicked = true }, onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Home Fox").assertIsDisplayed()

        composeRule.onNodeWithText("I finished one ").performClick()
        composeRule.waitForIdle()
        assert(loggedFinishClicked) { "onLogFinish was not invoked" }
    }
}
