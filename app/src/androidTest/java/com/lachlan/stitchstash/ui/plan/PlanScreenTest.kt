package com.lachlan.stitchstash.ui.plan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() = runBlocking {
        app.repository.observeScenarios().first().forEach { app.repository.deleteScenario(it.id) }
    }

    @Test
    fun switchingQuestionTabs_showsDifferentQuestionTitle() {
        composeRule.setContent {
            StitchStashTheme {
                PlanScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        // Defaults to "How many?" tab.
        composeRule.onNodeWithText("How many pieces by market day?").assertIsDisplayed()

        composeRule.onNodeWithText("How long?").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("How many hours per week do I need?").assertIsDisplayed()

        composeRule.onNodeWithText("When?").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("When will my stash be ready?").assertIsDisplayed()
    }

    @Test
    fun saveScenario_persistsAndActivatesIt() {
        composeRule.setContent {
            StitchStashTheme {
                PlanScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        // Top app-bar action and the dialog's own confirm button both say "Save".
        composeRule.onAllNodesWithText("Save").onLast().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Name").performTextClearance()
        composeRule.onNodeWithText("Name").performTextInput("My Autumn Plan")
        composeRule.onAllNodesWithText("Save").onLast().performClick()
        composeRule.waitForIdle()

        val scenarios = runBlocking { app.repository.observeScenarios().first() }
        assert(scenarios.any { it.name == "My Autumn Plan" }) {
            "Scenario was not persisted: ${scenarios.map { it.name }}"
        }
        val active = runBlocking { app.repository.observeActiveScenario().first() }
        assert(active?.name == "My Autumn Plan") { "Newly-saved scenario was not activated" }
    }
}
