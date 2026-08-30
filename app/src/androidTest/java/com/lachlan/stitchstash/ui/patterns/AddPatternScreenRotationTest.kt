package com.lachlan.stitchstash.ui.patterns

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.MainActivity
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Config-change (rotation) survival check driven through the real app (MainActivity + nav),
 * since a bare host Activity does not re-invoke setContent on ActivityScenario.recreate() and so
 * cannot exercise this scenario at all (confirmed empirically — see git history for the earlier,
 * non-working bare-host version of this test).
 *
 * AddPatternScreen holds its typed fields in plain `remember { mutableStateOf(...) }`, not
 * `rememberSaveable`, so this documents whether in-progress typed input survives an activity
 * recreation (e.g. a real device rotation) rather than being silently lost.
 */
@RunWith(AndroidJUnit4::class)
class AddPatternScreenRotationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() {
        runBlocking {
            app.repository.observePatterns().first().forEach { app.repository.deletePattern(it.id) }
            // Ensure MainActivity's start destination is Home, not Onboarding.
            app.repository.updateSettings(
                app.repository.observeSettings().first().copy(onboardingComplete = true),
            )
        }
    }

    @Test
    fun typedPatternName_survivesActivityRecreation() {
        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Patterns").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Add pattern").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Pattern name").performTextInput("Amigurumi Fox")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Amigurumi Fox").assertExists()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // If this fails, it confirms typed input is lost on rotation (see class doc comment).
        composeRule.onNodeWithText("Amigurumi Fox").assertExists()
    }
}
