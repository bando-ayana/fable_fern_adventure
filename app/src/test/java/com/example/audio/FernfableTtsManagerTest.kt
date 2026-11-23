package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FernfableTtsManagerTest {

    private lateinit var context: Context
    private var nowMs: Long = 10_000L
    private lateinit var fakeEngine: RecordingSpeechEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("fernfable_accessibility_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        nowMs = 10_000L
        fakeEngine = RecordingSpeechEngine()
    }

    @Test
    fun togglesPersistAcrossInstancesAndAnnounceWhenTtsEnabled() {
        val manager1 = FernfableAccessibilityManager(
            context = context,
            speechEngineFactory = { fakeEngine },
            clockMs = { nowMs }
        )

        assertFalse(manager1.calmMode.value)
        assertFalse(manager1.touchToSpeak.value)

        manager1.toggleCalmMode()
        assertTrue(manager1.calmMode.value)
        assertTrue(fakeEngine.spokenPhrases.isEmpty())

        manager1.toggleTouchToSpeak()
        assertTrue(manager1.touchToSpeak.value)
        assertEquals(1, fakeEngine.spokenPhrases.size)
        assertTrue(fakeEngine.spokenPhrases.last().contains("Touch to Speak turned on"))

        nowMs += 500L
        manager1.toggleCalmMode()
        assertFalse(manager1.calmMode.value)
        assertEquals(2, fakeEngine.spokenPhrases.size)
        assertTrue(fakeEngine.spokenPhrases.last().contains("Calm View turned off"))

        manager1.release()

        // Re-create from SharedPreferences and confirm persisted state
        val restoredEngine = RecordingSpeechEngine()
        val manager2 = FernfableAccessibilityManager(
            context = context,
            speechEngineFactory = { restoredEngine },
            clockMs = { nowMs }
        )
        assertFalse(manager2.calmMode.value)
        assertTrue(manager2.touchToSpeak.value)
        manager2.release()
    }

    @Test
    fun speakItemRespectsEnabledForegroundAndDebounce() {
        val manager = FernfableAccessibilityManager(
            context = context,
            speechEngineFactory = { fakeEngine },
            clockMs = { nowMs }
        )

        // Disabled -> ignored
        manager.speakItem("Moss Lantern")
        assertTrue(fakeEngine.spokenPhrases.isEmpty())
        assertNull(manager.lastSpokenText.value)

        // Enable Touch to Speak
        manager.setTouchToSpeak(true)
        fakeEngine.spokenPhrases.clear()

        nowMs += 400L
        manager.speakItem("  Sunleaf   Blossom  ")
        assertEquals(listOf("Sunleaf Blossom"), fakeEngine.spokenPhrases)
        assertEquals("Sunleaf Blossom", manager.lastSpokenText.value)

        // Immediate repeat within debounce window (< 280ms) is suppressed
        nowMs += 150L
        manager.speakItem("Sunleaf Blossom")
        assertEquals(1, fakeEngine.spokenPhrases.size)

        // Past debounce window (>= 280ms) speaks again
        nowMs += 200L
        manager.speakItem("Sunleaf Blossom")
        assertEquals(2, fakeEngine.spokenPhrases.size)

        // Backgrounded -> stops speech and ignores new speak requests
        manager.onForegroundChanged(false)
        assertEquals(1, fakeEngine.stopCount)
        nowMs += 500L
        manager.speakItem("Should not speak while paused")
        assertEquals(2, fakeEngine.spokenPhrases.size)

        // Disable stops engine and clears caption; release shuts down
        manager.onForegroundChanged(true)
        manager.setTouchToSpeak(false)
        assertEquals(2, fakeEngine.stopCount)
        assertNull(manager.lastSpokenText.value)

        manager.release()
        assertEquals(1, fakeEngine.shutdownCount)
    }

    private class RecordingSpeechEngine : SpeechEngine {
        val spokenPhrases = mutableListOf<String>()
        var stopCount = 0
        var shutdownCount = 0

        override fun speak(text: String, utteranceId: String) {
            spokenPhrases += text
        }

        override fun stop() {
            stopCount += 1
        }

        override fun shutdown() {
            shutdownCount += 1
        }
    }
}
