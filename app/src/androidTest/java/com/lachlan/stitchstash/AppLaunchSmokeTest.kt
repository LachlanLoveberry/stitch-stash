package com.lachlan.stitchstash

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end smoke test: the real app launches on a device/emulator, through whichever start
 * destination onboarding-completion routes to (Onboarding or Home), without crashing.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndRendersAStableScreen() {
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
