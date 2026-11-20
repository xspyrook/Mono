package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferenceManager
import com.example.engine.IconProcessor
import com.example.model.AppLanguage
import com.example.stability.StabilityMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MonoGlassStabilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `launcher survives extended idle and full screen navigation without crashing`() {
        composeTestRule.waitForIdle()

        // Verify Home Screen elements exist
        composeTestRule.onNodeWithTag("home_status_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_drawer_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_search_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_control_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_settings_button").assertIsDisplayed()

        // Simulate 35 seconds of runtime idle (no 5-second crash or timer failure)
        ShadowLooper.idleMainLooper(35_000L, java.util.concurrent.TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_status_pill").assertIsDisplayed()

        // Open & close App Drawer repeatedly
        repeat(3) {
            composeTestRule.onNodeWithTag("home_drawer_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("drawer_search_input").assertIsDisplayed()
            composeTestRule.onNodeWithTag("drawer_close_btn").performClick()
            composeTestRule.waitForIdle()
        }

        // Open Search and type query
        composeTestRule.onNodeWithTag("home_search_pill").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("search_screen_input").performTextInput("mono")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("search_web_fallback_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_close_btn").performClick()
        composeTestRule.waitForIdle()

        // Open Control Panel
        composeTestRule.onNodeWithTag("home_control_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("control_wifi_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("control_close_btn").performClick()
        composeTestRule.waitForIdle()

        // Open Settings & toggle language English -> Arabic
        composeTestRule.onNodeWithTag("home_settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lang_english_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lang_arabic_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_close_btn").performClick()
        composeTestRule.waitForIdle()

        // Verify activity recreation (rotation / config change)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_status_pill").assertIsDisplayed()
    }

    @Test
    fun `icon processor handles missing packages safely without crashing`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val processor = IconProcessor(context)
        val result = processor.getOrLoadMonochromeIcon("com.nonexistent.fake.pkg", "MainActivity")
        assertNull(result)
    }

    @Test
    fun `preference manager persists language and settings safely`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)
        prefs.updateSettings { it.copy(language = AppLanguage.ENGLISH, iconSizeDp = 64f) }
        val updated = prefs.settingsFlow.first()
        assertEquals(AppLanguage.ENGLISH, updated.language)
        assertEquals(64f, updated.iconSizeDp)

        // Restore Arabic default
        prefs.updateSettings { it.copy(language = AppLanguage.ARABIC) }
        val restored = prefs.settingsFlow.first()
        assertEquals(AppLanguage.ARABIC, restored.language)
        assertNotNull(StabilityMonitor.snapshot.value)
        assertNotNull(ColorDrawable(Color.WHITE))
    }
}
