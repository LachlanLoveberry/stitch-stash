package com.lachlan.stitchstash.ui.patterns

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
class AddPatternScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() = runBlocking {
        val repo = app.repository
        repo.observePatterns().first().forEach { repo.deletePattern(it.id) }
    }

    @Test
    fun blankName_keepsSaveDisabled() {
        composeRule.setContent {
            StitchStashTheme {
                AddPatternScreen(onDone = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun fillNameAndColourways_savesPatternAndColourways() {
        var done = false
        composeRule.setContent {
            StitchStashTheme {
                AddPatternScreen(onDone = { done = true }, onBack = {})
            }
        }

        composeRule.onNodeWithText("Pattern name").performTextInput("Test Fox")

        // Two colourway rows exist by default.
        composeRule.onAllNodesWithText("Colourway (e.g. Pink / Cream)")[0]
            .performTextInput("Rust")
        composeRule.onAllNodesWithText("Colourway (e.g. Pink / Cream)")[1]
            .performTextInput("Cream")

        composeRule.onNodeWithText("Save").assertIsEnabled()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        runBlocking {
            val patterns = app.repository.observePatterns().first()
            val pattern = patterns.firstOrNull { it.name == "Test Fox" }
            assert(pattern != null) { "Pattern was not persisted" }
            val colourways = app.repository.observeColourwaysForPattern(pattern!!.id).first()
            assert(colourways.map { it.name }.toSet() == setOf("Rust", "Cream")) {
                "Expected colourways Rust/Cream, got ${colourways.map { it.name }}"
            }
        }
        assert(done)
    }

    @Test
    fun addAndRemoveColourwayRows_changesRowCount() {
        composeRule.setContent {
            StitchStashTheme {
                AddPatternScreen(onDone = {}, onBack = {})
            }
        }

        // Starts with 2 rows.
        composeRule.onAllNodesWithText("Colourway (e.g. Pink / Cream)").assertCountEquals(2)

        composeRule.onNodeWithText("+ Another colourway").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Colourway (e.g. Pink / Cream)").assertCountEquals(3)

        // Remove buttons only show when there's more than 1 row; click the first "Remove".
        composeRule.onAllNodesWithText("Remove")[0].performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Colourway (e.g. Pink / Cream)").assertCountEquals(2)
    }

    @Test
    fun veryLongPatternName_stillSaves() {
        val longName = "A".repeat(220)
        composeRule.setContent {
            StitchStashTheme {
                AddPatternScreen(onDone = {}, onBack = {})
            }
        }

        composeRule.onNodeWithText("Pattern name").performTextInput(longName)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        runBlocking {
            val patterns = app.repository.observePatterns().first()
            assert(patterns.any { it.name == longName }) {
                "Long-named pattern was not persisted"
            }
        }
    }
}
