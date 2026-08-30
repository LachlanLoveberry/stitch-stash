package com.lachlan.stitchstash.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

/**
 * Drive sign-in / "Back up now" flows are skipped here: they need real Google auth and
 * WorkManager execution which aren't available in this test environment.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetSettings() = runBlocking {
        app.repository.ensureSettingsRow()
        app.repository.updateSettings(AppSettings())
    }

    @Test
    fun movingWeeklyHoursSlider_persists() {
        composeRule.setContent {
            StitchStashTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        val before = runBlocking { app.repository.observeSettings().first() }.weeklyHours

        composeRule.onNodeWithText("Weekly hours target").assertExists()
        // Drive the Slider directly through its SetProgress semantics action so we land on a
        // deterministic value rather than depending on drag-gesture pixel math.
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        composeRule.waitForIdle()

        val after = runBlocking { app.repository.observeSettings().first() }.weeklyHours
        assertEquals(20f, after, 0.01f)
        assertTrue(after != before)
    }

    @Test
    fun toggleForecastVisible_persists() {
        composeRule.setContent {
            StitchStashTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        val before = runBlocking { app.repository.observeSettings().first() }.forecastVisible
        assertTrue(before) // default is true

        composeRule.onNodeWithText("Show forecast on home").assertExists()
        // "Show forecast on home" is the first of the three Display-section Switches in
        // composition order (forecastVisible, stickerBookEnabled, weeklyRecapEnabled).
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))[0]
            .performClick()
        composeRule.waitForIdle()

        val after = runBlocking { app.repository.observeSettings().first() }.forecastVisible
        assertTrue(!after)
    }
}
