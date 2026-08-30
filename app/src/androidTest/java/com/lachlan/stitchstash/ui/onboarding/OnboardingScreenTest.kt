package com.lachlan.stitchstash.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetOnboarding() = runBlocking {
        app.repository.ensureSettingsRow()
        app.repository.updateSettings(AppSettings(onboardingComplete = false))
        app.repository.observeAllMarkets().first().forEach { app.repository.deleteMarket(it.id) }
    }

    @Test
    fun fullFlowThroughAllSteps_completesAndPersists() {
        var completed = false
        composeRule.setContent {
            StitchStashTheme {
                OnboardingScreen(onComplete = { completed = true })
            }
        }
        composeRule.waitForIdle()

        // Step 0: MarketStep — enter a name, pick a date, then Next.
        composeRule.onNodeWithText("When's the next market?").assertExists()
        composeRule.onNodeWithText("Market name (optional)").performTextInput("Autumn Fair")
        composeRule.onNodeWithText("Pick a date").performClick()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()

        // Step 1: HoursStep — move the slider away from its default, then Next.
        composeRule.onNodeWithText("Realistic crochet hours per week?").assertExists()
        composeRule.onNode(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                androidx.compose.ui.semantics.SemanticsActions.SetProgress,
            ),
        ).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(10f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()

        // Step 2: TargetStep — bump target pieces up, then Finish.
        composeRule.onNodeWithText("How many pieces would you love to bring?").assertExists()
        composeRule.onNodeWithContentDescription("Increase target pieces").performClick()
        composeRule.onNodeWithContentDescription("Increase target pieces").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Finish").performClick()
        composeRule.waitForIdle()

        assertTrue(completed)

        val settings = runBlocking { app.repository.observeSettings().first() }
        assertTrue(settings.onboardingComplete)
        assertEquals(10f, settings.weeklyHours, 0.01f)
        assertEquals(2, settings.targetPieces)

        val markets = runBlocking { app.repository.observeAllMarkets().first() }
        assertEquals(1, markets.size)
        assertEquals("Autumn Fair", markets[0].name)
    }

    @Test
    fun skipForNowOnLastStep_alsoCompletesOnboarding() {
        var completed = false
        composeRule.setContent {
            StitchStashTheme {
                OnboardingScreen(onComplete = { completed = true })
            }
        }
        composeRule.waitForIdle()

        // Advance to the last step without filling anything in.
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()

        // "Skip for now" on the TargetStep calls the same completeOnboarding path as "Finish".
        composeRule.onNodeWithText("Skip for now").performClick()
        composeRule.waitForIdle()

        assertTrue(completed)
        val settings = runBlocking { app.repository.observeSettings().first() }
        assertTrue(settings.onboardingComplete)
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescription(
    description: String,
) = this.onNode(androidx.compose.ui.test.hasContentDescription(description))
