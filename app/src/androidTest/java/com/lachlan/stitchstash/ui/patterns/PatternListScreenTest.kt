package com.lachlan.stitchstash.ui.patterns

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.ui.components.TopLevelDestination
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() = runBlocking {
        val repo = app.repository
        repo.observePatterns().first().forEach { repo.deletePattern(it.id) }
    }

    @Test
    fun seededPatterns_areDisplayed() {
        runBlocking {
            app.repository.addPattern(Pattern(name = "Rusty Fox"))
            app.repository.addPattern(Pattern(name = "Cream Bunny"))
        }

        composeRule.setContent {
            StitchStashTheme {
                PatternListScreen(
                    onAddPattern = {},
                    onNavigate = { _: TopLevelDestination -> },
                    onEditEstimate = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Rusty Fox").assertIsDisplayed()
        composeRule.onNodeWithText("Cream Bunny").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun longPress_thenConfirm_deletesPattern() {
        runBlocking {
            app.repository.addPattern(Pattern(name = "Delete Me"))
        }

        composeRule.setContent {
            StitchStashTheme {
                PatternListScreen(
                    onAddPattern = {},
                    onNavigate = { _: TopLevelDestination -> },
                    onEditEstimate = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete Me").performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        val remaining = runBlocking { app.repository.observePatterns().first() }
        assert(remaining.none { it.name == "Delete Me" }) { "Pattern was not deleted" }
    }
}
