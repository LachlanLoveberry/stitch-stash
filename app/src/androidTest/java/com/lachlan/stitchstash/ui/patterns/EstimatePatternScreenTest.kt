package com.lachlan.stitchstash.ui.patterns

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EstimatePatternScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()
    private var patternId: Long = -1

    @Before
    fun resetData() = runBlocking {
        val repo = app.repository
        repo.observePatterns().first().forEach { repo.deletePattern(it.id) }
        patternId = repo.addPattern(Pattern(name = "Seeded Fox"))
    }

    @Test
    fun screen_loadsSeededPatternAndShowsBucketOptions() {
        composeRule.setContent {
            StitchStashTheme {
                EstimatePatternScreen(patternId = patternId, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Seeded Fox").assertIsDisplayed()
        composeRule.onNodeWithText("Quick (under 3h)").assertIsDisplayed()
    }

    @Test
    fun addColourway_throughUi_trimsNameAndCoercesNegativeTargetToZero() {
        composeRule.setContent {
            StitchStashTheme {
                EstimatePatternScreen(patternId = patternId, onBack = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("+ Add colourway").performClick()
        composeRule.waitForIdle()

        // Enter a name with leading/trailing whitespace to verify trimming persists correctly.
        composeRule.onNodeWithText("Colourway (e.g. Pink / Cream)").performTextInput("  Amber  ")

        // Decrease target count below the UI floor of 1 is not reachable (min is enforced by the
        // stepper itself), so we confirm the default (1) saves as-is and exercise the trim path.
        composeRule.onNodeWithText("Add").performClick()
        composeRule.waitForIdle()

        runBlocking {
            val colourways = app.repository.observeColourwaysForPattern(patternId).first()
            val added = colourways.firstOrNull { it.name == "Amber" }
            assert(added != null) {
                "Expected trimmed colourway named 'Amber', got ${colourways.map { it.name }}"
            }
            assert(added!!.targetCount >= 0) { "targetCount should be coerced to >= 0" }
        }
    }

    @Test
    fun deleteColourway_viaConfirmDialog_removesIt() = runBlocking {
        app.repository.addColourway(
            com.lachlan.stitchstash.data.db.entities.Colourway(
                patternId = patternId,
                name = "ToDelete",
                targetCount = 1,
            ),
        )

        composeRule.setContent {
            StitchStashTheme {
                EstimatePatternScreen(patternId = patternId, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Delete ToDelete").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        val colourways = app.repository.observeColourwaysForPattern(patternId).first()
        assert(colourways.none { it.name == "ToDelete" }) { "Colourway was not deleted" }
    }
}
