package com.lachlan.stitchstash.ui.log

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.MainActivity
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Config-change (rotation) survival check driven through the real app (MainActivity + nav) —
 * see AddPatternScreenRotationTest's doc comment for why a bare host Activity can't exercise
 * this scenario at all.
 *
 * The pattern/colourway selection while logging a completion lives in plain `remember`, not
 * `rememberSaveable` or the ViewModel, so this documents whether that selection survives an
 * activity recreation rather than being silently lost.
 */
@RunWith(AndroidJUnit4::class)
class LogCompletionScreenRotationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() {
        runBlocking {
            val repo = app.repository
            repo.observeAllCompletions().first().forEach { repo.deleteCompletion(it.id) }
            repo.observePatterns().first().forEach { repo.deletePattern(it.id) }
            repo.updateSettings(repo.observeSettings().first().copy(onboardingComplete = true))

            val patternId = repo.addPattern(Pattern(name = "Rotation Owl"))
            repo.addColourway(Colourway(patternId = patternId, name = "Grey", targetCount = 2))
        }
    }

    @Test
    fun selectedPatternAndColourway_surviveActivityRecreation() {
        composeRule.onNodeWithText("I finished one ").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Rotation Owl").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Grey").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Celebrate this one ").assertIsEnabled()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // If this fails, the pattern/colourway selection was lost on rotation (see doc comment).
        composeRule.onNodeWithText("Celebrate this one ").assertIsEnabled()
    }
}
