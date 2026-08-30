package com.lachlan.stitchstash.ui.stickers

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.Sticker
import com.lachlan.stitchstash.domain.stickers.StickerCatalog
import com.lachlan.stitchstash.ui.theme.StitchStashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StickerBookScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<StitchStashApp>()

    @Before
    fun resetData() = runBlocking {
        app.repository.observeStickers().first().forEach { app.database.stickerDao().delete(it.id) }
    }

    @Test
    fun noStickersEarned_allTilesLocked() {
        composeRule.setContent {
            StitchStashTheme {
                StickerBookScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("0 earned · ${StickerCatalog.all().size} still to find").assertIsDisplayed()
    }

    @Test
    fun earnedSticker_showsItsTitleInsteadOfLocked() {
        runBlocking {
            app.database.stickerDao().insert(Sticker(type = StickerCatalog.FIRST_EVER))
        }

        composeRule.setContent {
            StitchStashTheme {
                StickerBookScreen(onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        val def = StickerCatalog.get(StickerCatalog.FIRST_EVER)
        composeRule.onNodeWithText(def.title).assertIsDisplayed()
        composeRule.onNodeWithText(
            "1 earned · ${StickerCatalog.all().size - 1} still to find",
        ).assertIsDisplayed()
    }
}
