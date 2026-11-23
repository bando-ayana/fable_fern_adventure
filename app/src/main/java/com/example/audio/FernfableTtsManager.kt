package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface SpeechEngine {
    fun speak(text: String, utteranceId: String)
    fun stop()
    fun shutdown()
}

fun interface SpeechEngineFactory {
    fun create(context: Context): SpeechEngine?
}

internal class AndroidSpeechEngine(
    context: Context
) : SpeechEngine {
    private val appContext = context.applicationContext
    @Volatile
    private var isReady: Boolean = false
    @Volatile
    private var isShutdown: Boolean = false
    @Volatile
    private var pendingPhrase: Pair<String, String>? = null

    private var tts: TextToSpeech? = null

    init {
        runCatching {
            var createdEngine: TextToSpeech? = null
            createdEngine = TextToSpeech(appContext) { status ->
                if (isShutdown) {
                    runCatching { createdEngine?.shutdown() }
                    return@TextToSpeech
                }
                if (status == TextToSpeech.SUCCESS) {
                    isReady = true
                    runCatching {
                        val engine = createdEngine ?: tts
                        engine?.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        val localeResult = engine?.setLanguage(Locale.getDefault())
                        if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                            localeResult == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            engine?.setLanguage(Locale.US)
                        }
                        engine?.setSpeechRate(0.95f)
                        engine?.setPitch(1.0f)
                        pendingPhrase?.let { (text, id) ->
                            pendingPhrase = null
                            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                        }
                    }
                } else {
                    Log.w("FernfableTts", "TextToSpeech init failed with status=$status")
                }
            }
            tts = createdEngine
        }.onFailure { error ->
            Log.w("FernfableTts", "Unable to construct TextToSpeech engine", error)
        }
    }

    override fun speak(text: String, utteranceId: String) {
        if (isShutdown || text.isBlank()) return
        if (!isReady) {
            pendingPhrase = text to utteranceId
            return
        }
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    override fun stop() {
        pendingPhrase = null
        runCatching { tts?.stop() }
    }

    override fun shutdown() {
        isShutdown = true
        pendingPhrase = null
        isReady = false
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }
}

private object DefaultSpeechEngineFactory : SpeechEngineFactory {
    override fun create(context: Context): SpeechEngine? = AndroidSpeechEngine(context)
}

class FernfableAccessibilityManager(
    context: Context,
    private val speechEngineFactory: SpeechEngineFactory = DefaultSpeechEngineFactory,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _calmMode = MutableStateFlow(prefs.getBoolean(KEY_CALM_MODE, false))
    val calmMode: StateFlow<Boolean> = _calmMode.asStateFlow()

    private val _touchToSpeak = MutableStateFlow(prefs.getBoolean(KEY_TOUCH_TO_SPEAK, false))
    val touchToSpeak: StateFlow<Boolean> = _touchToSpeak.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String?>(null)
    val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

    private var speechEngine: SpeechEngine? = null
    private var isForeground: Boolean = true
    private var utteranceSeq: Long = 0L
    private var lastText: String = ""
    private var lastSpokenAtMs: Long = Long.MIN_VALUE

    init {
        if (_touchToSpeak.value) {
            ensureEngine()
        }
    }

    fun toggleCalmMode() {
        setCalmMode(!_calmMode.value)
    }

    fun setCalmMode(enabled: Boolean) {
        if (_calmMode.value == enabled) return
        _calmMode.value = enabled
        prefs.edit().putBoolean(KEY_CALM_MODE, enabled).apply()
        if (_touchToSpeak.value) {
            speakItem(
                if (enabled) "Calm View turned on. Screens are simplified with larger, calmer controls."
                else "Calm View turned off. Full scenery restored."
            )
        }
    }

    fun toggleTouchToSpeak() {
        setTouchToSpeak(!_touchToSpeak.value)
    }

    fun setTouchToSpeak(enabled: Boolean) {
        if (_touchToSpeak.value == enabled) return
        _touchToSpeak.value = enabled
        prefs.edit().putBoolean(KEY_TOUCH_TO_SPEAK, enabled).apply()
        if (enabled) {
            ensureEngine()
            speakItem("Touch to Speak turned on. Touch any item on screen to hear it read aloud.")
        } else {
            speechEngine?.stop()
            _lastSpokenText.value = null
            lastText = ""
        }
    }

    fun speakItem(text: String) {
        if (!_touchToSpeak.value || !isForeground) return
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return

        val now = clockMs()
        if (normalized == lastText && (now - lastSpokenAtMs) < REPEAT_DEBOUNCE_MS) {
            return
        }
        lastText = normalized
        lastSpokenAtMs = now
        _lastSpokenText.value = normalized

        val engine = ensureEngine() ?: return
        utteranceSeq += 1L
        engine.speak(normalized, "ff_tts_$utteranceSeq")
    }

    fun onForegroundChanged(foreground: Boolean) {
        if (isForeground == foreground) return
        isForeground = foreground
        if (!foreground) {
            speechEngine?.stop()
        }
    }

    fun release() {
        speechEngine?.shutdown()
        speechEngine = null
    }

    private fun ensureEngine(): SpeechEngine? {
        if (speechEngine == null) {
            speechEngine = speechEngineFactory.create(appContext)
        }
        return speechEngine
    }

    companion object {
        private const val PREFS_NAME = "fernfable_accessibility_prefs"
        private const val KEY_CALM_MODE = "calm_mode"
        private const val KEY_TOUCH_TO_SPEAK = "touch_to_speak"
        internal const val REPEAT_DEBOUNCE_MS = 280L
    }
}
