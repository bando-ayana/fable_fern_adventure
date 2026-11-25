package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.FernfableAccessibilityManager
import com.example.audio.SpeechEngine
import com.example.ui.components.ComfortAccessibilityBar
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.ProvideFernfableAccessibility
import com.example.ui.components.speakOnTouch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccessibilityUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var manager: FernfableAccessibilityManager
    private lateinit var fakeEngine: FakeSpeechEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fernfable_accessibility_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        fakeEngine = FakeSpeechEngine()
        var tick = 50_000L
        manager = FernfableAccessibilityManager(
            context = context,
            speechEngineFactory = { fakeEngine },
            clockMs = {
                tick += 500L
                tick
            }
        )
    }

    @After
    fun tearDown() {
        manager.release()
    }

    @Test
    fun comfortBarTogglesCalmViewAndTouchToSpeakWithoutBlockingClicks() {
        var clickCount = 0

        composeRule.setContent {
            MaterialTheme {
                ProvideFernfableAccessibility(externalManager = manager) {
                    val a11y = LocalAccessibilityUi.current
                    var stateProbe by remember { mutableIntStateOf(0) }
                    Column {
                        ComfortAccessibilityBar()
                        Text(
                            text = "calm=${a11y.calmMode};tts=${a11y.touchToSpeak};clicks=$stateProbe",
                            modifier = Modifier.testTag("a11y_state_probe")
                        )
                        Button(
                            onClick = {
                                clickCount += 1
                                stateProbe = clickCount
                            },
                            modifier = Modifier
                                .speakOnTouch("Explore the Whispering Grove")
                                .testTag("sample_explore_button")
                        ) {
                            Text("Explore")
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comfort_accessibility_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("a11y_state_probe").assertTextContains("calm=false;tts=false;clicks=0")

        // Toggle Calm View on
        composeRule.onNodeWithTag("a11y_calm_mode_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("a11y_state_probe").assertTextContains("calm=true;tts=false;clicks=0")

        // Toggle Touch to Speak on -> confirmation caption appears
        composeRule.onNodeWithTag("a11y_tts_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("a11y_state_probe").assertTextContains("calm=true;tts=true", substring = true)
        composeRule.onNodeWithTag("a11y_spoken_caption")
            .assertIsDisplayed()
            .assertTextContains("Touch to Speak turned on", substring = true)

        // Touching the action button both speaks the label AND invokes normal onClick
        composeRule.onNodeWithTag("sample_explore_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, clickCount)
        assertTrue(fakeEngine.spoken.contains("Explore the Whispering Grove"))
        composeRule.onNodeWithTag("a11y_spoken_caption")
            .assertTextContains("Voice: Explore the Whispering Grove")

        // Turn Touch to Speak off -> caption disappears, button clicks remain functional
        composeRule.onNodeWithTag("a11y_tts_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sample_explore_button").performClick()
        composeRule.waitForIdle()
        assertEquals(2, clickCount)
    }

    private class FakeSpeechEngine : SpeechEngine {
        val spoken = mutableListOf<String>()
        override fun speak(text: String, utteranceId: String) {
            spoken += text
        }
        override fun stop() = Unit
        override fun shutdown() = Unit
    }
}
