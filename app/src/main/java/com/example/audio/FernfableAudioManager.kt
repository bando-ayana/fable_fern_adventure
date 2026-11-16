package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import androidx.core.content.edit
import com.example.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    fun seekTo(positionMs: Int) {}
    fun durationMs(): Int = 0
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

    override fun seekTo(positionMs: Int) {
        val dur = durationMs()
        val safePos = if (dur > 0) positionMs.coerceIn(0, dur - 1) else positionMs.coerceAtLeast(0)
        runCatching { player.seekTo(safePos) }
    }

    override fun durationMs(): Int = runCatching { player.duration }.getOrDefault(0)

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
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val layerRandom: Random = Random.Default
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val layerSupervisor = SupervisorJob(scope.coroutineContext[Job])
    private val layerScope = CoroutineScope(layerSupervisor + dispatcher)

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _currentScene = MutableStateFlow(AudioScene.NONE)
    val currentScene: StateFlow<AudioScene> = _currentScene.asStateFlow()

    private var isForeground: Boolean = true
    private var rpgTrack: SceneAudioTrack? = null
    private var guitarLayerTrack: SceneAudioTrack? = null
    private var steelDrumLayerTrack: SceneAudioTrack? = null
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
        prefs.edit { putBoolean(KEY_MUTED, muted) }
        if (muted) {
            transitionJob?.cancel()
            stopAccentLayers()
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
            stopAccentLayers()
            pauseAllTracks()
        } else {
            applySceneTransition(immediate = true)
        }
    }

    fun release() {
        transitionJob?.cancel()
        stopAccentLayers()
        scope.cancel()
        rpgTrack?.release()
        guitarLayerTrack?.release()
        steelDrumLayerTrack?.release()
        ambientTrack?.release()
        rpgTrack = null
        guitarLayerTrack = null
        steelDrumLayerTrack = null
        ambientTrack = null
    }

    private fun isDesiredTrackActive(scene: AudioScene): Boolean {
        if (_isMuted.value || !isForeground) {
            return rpgTrack?.isPlaying() != true &&
                ambientTrack?.isPlaying() != true &&
                guitarLayerTrack?.isPlaying() != true &&
                steelDrumLayerTrack?.isPlaying() != true
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
            stopAccentLayers()
            pauseAllTracks()
            return
        }

        if (scene != AudioScene.RPG_MUSIC) {
            stopAccentLayers()
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
            if (scene == AudioScene.RPG_MUSIC) {
                startAccentLayersIfNeeded()
            }
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
            if (scene == AudioScene.RPG_MUSIC) {
                startAccentLayersIfNeeded()
            }
        }
    }

    private fun startAccentLayersIfNeeded() {
        if (_isMuted.value || !isForeground || _currentScene.value != AudioScene.RPG_MUSIC) return
        if (layerSupervisor.children.any { it.isActive }) return

        // Acoustic nylon guitar layer enters after 2.4..6.2s, swells smoothly, and exits irregularly
        layerScope.launch {
            runIrregularAccentLoop(
                resId = R.raw.rpg_guitar_layer,
                assignTrack = { guitarLayerTrack = it },
                getTrack = { guitarLayerTrack },
                initialRestRangeMs = 2_400L..6_200L,
                restRangeMs = 4_200L..10_400L,
                activeHoldRangeMs = 7_500L..13_800L,
                peakVolumeRange = 0.21f..0.29f
            )
        }

        // Warm steel drum layer enters on a distinct offset (5.0..9.8s) with gentler volume & independent rhythm
        layerScope.launch {
            runIrregularAccentLoop(
                resId = R.raw.rpg_steel_drum_layer,
                assignTrack = { steelDrumLayerTrack = it },
                getTrack = { steelDrumLayerTrack },
                initialRestRangeMs = 5_000L..9_800L,
                restRangeMs = 5_800L..13_200L,
                activeHoldRangeMs = 6_200L..11_400L,
                peakVolumeRange = 0.16f..0.24f
            )
        }
    }

    private suspend fun kotlinx.coroutines.CoroutineScope.runIrregularAccentLoop(
        @RawRes resId: Int,
        assignTrack: (SceneAudioTrack?) -> Unit,
        getTrack: () -> SceneAudioTrack?,
        initialRestRangeMs: LongRange,
        restRangeMs: LongRange,
        activeHoldRangeMs: LongRange,
        peakVolumeRange: ClosedFloatingPointRange<Float>
    ) {
        var firstEntry = true
        var lastBucket = -1
        while (isActive && !_isMuted.value && isForeground && _currentScene.value == AudioScene.RPG_MUSIC) {
            val restMs = if (firstEntry) {
                layerRandom.nextLong(initialRestRangeMs.first, initialRestRangeMs.last + 1)
            } else {
                layerRandom.nextLong(restRangeMs.first, restRangeMs.last + 1)
            }
            firstEntry = false
            delay(restMs)

            if (!isActive || _isMuted.value || !isForeground || _currentScene.value != AudioScene.RPG_MUSIC) break

            val track = getTrack() ?: trackFactory.create(appContext, resId)?.also(assignTrack)
            if (track == null) {
                // Resource unavailable on current runtime; retry after long interval or exit gracefully
                delay(restRangeMs.last)
                continue
            }

            // Pick a different phrase offset across the stem so consecutive entries sound fresh
            val durMs = track.durationMs()
            if (durMs > 2_000) {
                var bucket = layerRandom.nextInt(PHRASE_OFFSET_FRACTIONS.size)
                if (bucket == lastBucket) {
                    bucket = (bucket + 1 + layerRandom.nextInt(PHRASE_OFFSET_FRACTIONS.size - 1)) %
                        PHRASE_OFFSET_FRACTIONS.size
                }
                lastBucket = bucket
                val offsetMs = (durMs * PHRASE_OFFSET_FRACTIONS[bucket]).toInt()
                track.seekTo(offsetMs)
            }

            val peakVol = peakVolumeRange.start +
                layerRandom.nextFloat() * (peakVolumeRange.endInclusive - peakVolumeRange.start)

            track.setVolume(0f)
            track.start()

            // Gentle swell-in
            for (step in 1..LAYER_FADE_STEPS) {
                if (!isActive || _isMuted.value || !isForeground || _currentScene.value != AudioScene.RPG_MUSIC) break
                val progress = step.toFloat() / LAYER_FADE_STEPS.toFloat()
                track.setVolume(peakVol * progress)
                delay(LAYER_FADE_STEP_MS)
            }

            val holdMs = layerRandom.nextLong(activeHoldRangeMs.first, activeHoldRangeMs.last + 1)
            delay(holdMs)

            // Gentle fade-out
            for (step in LAYER_FADE_STEPS - 1 downTo 0) {
                if (!isActive || _isMuted.value || !isForeground || _currentScene.value != AudioScene.RPG_MUSIC) break
                val progress = step.toFloat() / LAYER_FADE_STEPS.toFloat()
                track.setVolume(peakVol * progress)
                delay(LAYER_FADE_STEP_MS)
            }
            track.setVolume(0f)
            track.pause()
        }
    }

    private fun stopAccentLayers() {
        layerSupervisor.cancelChildren()
        guitarLayerTrack?.setVolume(0f)
        guitarLayerTrack?.pause()
        steelDrumLayerTrack?.setVolume(0f)
        steelDrumLayerTrack?.pause()
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
        guitarLayerTrack?.setVolume(0f)
        guitarLayerTrack?.pause()
        steelDrumLayerTrack?.setVolume(0f)
        steelDrumLayerTrack?.pause()
        ambientTrack?.setVolume(0f)
        ambientTrack?.pause()
    }

    companion object {
        private const val PREFS_NAME = "fernfable_audio_prefs"
        private const val KEY_MUTED = "audio_muted"
        internal const val RPG_TARGET_VOLUME = 0.42f
        internal const val AMBIENT_TARGET_VOLUME = 0.35f
        private const val CROSSFADE_STEPS = 10
        private const val CROSSFADE_STEP_MS = 45L
        private const val LAYER_FADE_STEPS = 12
        private const val LAYER_FADE_STEP_MS = 180L
        private val PHRASE_OFFSET_FRACTIONS = floatArrayOf(0.0f, 0.22f, 0.43f, 0.64f, 0.81f)
    }
}
