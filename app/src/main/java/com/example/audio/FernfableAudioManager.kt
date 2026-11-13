package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import com.example.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AudioScene {
    NONE,
    RPG_MUSIC,
    PUZZLE_AMBIENT
}

interface SceneAudioTrack {
    fun setVolume(volume: Float)
    fun start()
    fun pause()
    fun isPlaying(): Boolean
    fun release()
}

fun interface SceneTrackFactory {
    fun create(context: Context, @RawRes resId: Int): SceneAudioTrack?
}

internal class AndroidSceneAudioTrack(
    private val player: MediaPlayer
) : SceneAudioTrack {
    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        runCatching { player.setVolume(clamped, clamped) }
    }

    override fun start() {
        runCatching {
            if (!player.isPlaying) {
                player.start()
            }
        }
    }

    override fun pause() {
        runCatching {
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    override fun isPlaying(): Boolean = runCatching { player.isPlaying }.getOrDefault(false)

    override fun release() {
        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        runCatching { player.release() }
    }
}

private object DefaultSceneTrackFactory : SceneTrackFactory {
    override fun create(context: Context, @RawRes resId: Int): SceneAudioTrack? {
        return runCatching {
            val afd = context.resources.openRawResourceFd(resId) ?: return null
            val player = MediaPlayer()
            try {
                afd.use { descriptor ->
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    player.setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length
                    )
                    player.isLooping = true
                    player.prepare()
                }
                AndroidSceneAudioTrack(player)
            } catch (t: Throwable) {
                runCatching { player.release() }
                throw t
            }
        }.onFailure { error ->
            Log.w("FernfableAudio", "Unable to initialize audio track for resource $resId", error)
        }.getOrNull()
    }
}

class FernfableAudioManager(
    context: Context,
    private val trackFactory: SceneTrackFactory = DefaultSceneTrackFactory,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _currentScene = MutableStateFlow(AudioScene.NONE)
    val currentScene: StateFlow<AudioScene> = _currentScene.asStateFlow()

    private var isForeground: Boolean = true
    private var rpgTrack: SceneAudioTrack? = null
    private var ambientTrack: SceneAudioTrack? = null
    private var transitionJob: Job? = null

    fun setScene(scene: AudioScene) {
        if (_currentScene.value == scene && isDesiredTrackActive(scene)) {
            return
        }
        _currentScene.value = scene
        applySceneTransition(immediate = false)
    }

    fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    fun setMuted(muted: Boolean) {
        if (_isMuted.value == muted) return
        _isMuted.value = muted
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
        if (muted) {
            transitionJob?.cancel()
            pauseAllTracks()
        } else {
            applySceneTransition(immediate = true)
        }
    }

    fun onForegroundChanged(foreground: Boolean) {
        if (isForeground == foreground) return
        isForeground = foreground
        if (!foreground) {
            transitionJob?.cancel()
            pauseAllTracks()
        } else {
            applySceneTransition(immediate = true)
        }
    }

    fun release() {
        transitionJob?.cancel()
        scope.cancel()
        rpgTrack?.release()
        ambientTrack?.release()
        rpgTrack = null
        ambientTrack = null
    }

    private fun isDesiredTrackActive(scene: AudioScene): Boolean {
        if (_isMuted.value || !isForeground) {
            return rpgTrack?.isPlaying() != true && ambientTrack?.isPlaying() != true
        }
        return when (scene) {
            AudioScene.NONE -> rpgTrack?.isPlaying() != true && ambientTrack?.isPlaying() != true
            AudioScene.RPG_MUSIC -> rpgTrack?.isPlaying() == true && ambientTrack?.isPlaying() != true
            AudioScene.PUZZLE_AMBIENT -> ambientTrack?.isPlaying() == true && rpgTrack?.isPlaying() != true
        }
    }

    private fun applySceneTransition(immediate: Boolean) {
        transitionJob?.cancel()
        val scene = _currentScene.value
        if (_isMuted.value || !isForeground || scene == AudioScene.NONE) {
            pauseAllTracks()
            return
        }

        val targetTrack = getOrCreateTrack(scene)
        val outgoingTrack = when (scene) {
            AudioScene.RPG_MUSIC -> ambientTrack
            AudioScene.PUZZLE_AMBIENT -> rpgTrack
            AudioScene.NONE -> null
        }
        val targetVolume = when (scene) {
            AudioScene.RPG_MUSIC -> RPG_TARGET_VOLUME
            AudioScene.PUZZLE_AMBIENT -> AMBIENT_TARGET_VOLUME
            AudioScene.NONE -> 0f
        }
        val outgoingStartVolume = when (scene) {
            AudioScene.RPG_MUSIC -> AMBIENT_TARGET_VOLUME
            AudioScene.PUZZLE_AMBIENT -> RPG_TARGET_VOLUME
            AudioScene.NONE -> 0f
        }

        if (immediate || outgoingTrack == null || !outgoingTrack.isPlaying()) {
            outgoingTrack?.setVolume(0f)
            outgoingTrack?.pause()
            targetTrack?.setVolume(targetVolume)
            targetTrack?.start()
            return
        }

        transitionJob = scope.launch {
            targetTrack?.setVolume(0f)
            targetTrack?.start()
            for (step in 1..CROSSFADE_STEPS) {
                val progress = step.toFloat() / CROSSFADE_STEPS.toFloat()
                targetTrack?.setVolume(targetVolume * progress)
                outgoingTrack.setVolume(outgoingStartVolume * (1f - progress))
                delay(CROSSFADE_STEP_MS)
            }
            outgoingTrack.setVolume(0f)
            outgoingTrack.pause()
            targetTrack?.setVolume(targetVolume)
        }
    }

    private fun getOrCreateTrack(scene: AudioScene): SceneAudioTrack? {
        return when (scene) {
            AudioScene.RPG_MUSIC -> {
                if (rpgTrack == null) {
                    rpgTrack = trackFactory.create(appContext, R.raw.rpg_canopy_music)
                }
                rpgTrack
            }

            AudioScene.PUZZLE_AMBIENT -> {
                if (ambientTrack == null) {
                    ambientTrack = trackFactory.create(appContext, R.raw.jungle_puzzle_ambient)
                }
                ambientTrack
            }

            AudioScene.NONE -> null
        }
    }

    private fun pauseAllTracks() {
        rpgTrack?.setVolume(0f)
        rpgTrack?.pause()
        ambientTrack?.setVolume(0f)
        ambientTrack?.pause()
    }

    companion object {
        private const val PREFS_NAME = "fernfable_audio_prefs"
        private const val KEY_MUTED = "audio_muted"
        internal const val RPG_TARGET_VOLUME = 0.55f
        internal const val AMBIENT_TARGET_VOLUME = 0.65f
        private const val CROSSFADE_STEPS = 10
        private const val CROSSFADE_STEP_MS = 45L
    }
}