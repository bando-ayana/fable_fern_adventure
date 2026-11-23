package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FernfableAudioManagerTest {

    private class FakeSceneAudioTrack : SceneAudioTrack {
        var playing = false
        var currentVolume = 0f
        var released = false

        override fun setVolume(volume: Float) {
            currentVolume = volume
        }

        override fun start() {
            playing = true
        }

        override fun pause() {
            playing = false
        }

        override fun isPlaying(): Boolean = playing

        override fun release() {
            playing = false
            released = true
        }
    }

    @Test
    fun `switches between rpg music and puzzle jungle ambience cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fernfable_audio_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val tracks = mutableMapOf<Int, FakeSceneAudioTrack>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FernfableAudioManager(
            context = context,
            trackFactory = { _, resId -> tracks.getOrPut(resId) { FakeSceneAudioTrack() } },
            dispatcher = dispatcher
        )

        manager.setScene(AudioScene.RPG_MUSIC)
        advanceUntilIdle()

        val rpgTrack = tracks[R.raw.rpg_canopy_music]!!
        assertTrue(rpgTrack.playing)
        assertEquals(FernfableAudioManager.RPG_TARGET_VOLUME, rpgTrack.currentVolume, 0.001f)

        manager.setScene(AudioScene.PUZZLE_AMBIENT)
        advanceUntilIdle()

        val ambientTrack = tracks[R.raw.jungle_puzzle_ambient]!!
        assertFalse(rpgTrack.playing)
        assertTrue(ambientTrack.playing)
        assertEquals(FernfableAudioManager.AMBIENT_TARGET_VOLUME, ambientTrack.currentVolume, 0.001f)

        manager.setScene(AudioScene.RPG_MUSIC)
        advanceUntilIdle()

        assertTrue(rpgTrack.playing)
        assertFalse(ambientTrack.playing)

        manager.release()
        assertTrue(rpgTrack.released)
        assertTrue(ambientTrack.released)
    }

    @Test
    fun `mute toggle persists and pauses or resumes active scene`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fernfable_audio_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val tracks = mutableMapOf<Int, FakeSceneAudioTrack>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FernfableAudioManager(
            context = context,
            trackFactory = { _, resId -> tracks.getOrPut(resId) { FakeSceneAudioTrack() } },
            dispatcher = dispatcher
        )

        manager.setScene(AudioScene.PUZZLE_AMBIENT)
        advanceUntilIdle()

        val ambientTrack = tracks[R.raw.jungle_puzzle_ambient]!!
        assertTrue(ambientTrack.playing)

        manager.toggleMute()
        advanceUntilIdle()
        assertTrue(manager.isMuted.value)
        assertFalse(ambientTrack.playing)

        // Foreground resume while muted must stay muted
        manager.onForegroundChanged(false)
        manager.onForegroundChanged(true)
        advanceUntilIdle()
        assertFalse(ambientTrack.playing)

        // Unmute resumes active puzzle ambience
        manager.toggleMute()
        advanceUntilIdle()
        assertFalse(manager.isMuted.value)
        assertTrue(ambientTrack.playing)

        // Recreated manager reads persisted mute state
        manager.setMuted(true)
        val reloaded = FernfableAudioManager(
            context = context,
            trackFactory = { _, resId -> tracks.getOrPut(resId) { FakeSceneAudioTrack() } },
            dispatcher = dispatcher
        )
        assertTrue(reloaded.isMuted.value)

        manager.release()
        reloaded.release()
    }

    @Test
    fun `lifecycle pause and resume controls playback when unmuted`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fernfable_audio_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val tracks = mutableMapOf<Int, FakeSceneAudioTrack>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FernfableAudioManager(
            context = context,
            trackFactory = { _, resId -> tracks.getOrPut(resId) { FakeSceneAudioTrack() } },
            dispatcher = dispatcher
        )

        manager.setScene(AudioScene.RPG_MUSIC)
        advanceUntilIdle()
        val rpgTrack = tracks[R.raw.rpg_canopy_music]!!
        assertTrue(rpgTrack.playing)

        manager.onForegroundChanged(false)
        advanceUntilIdle()
        assertFalse(rpgTrack.playing)

        manager.onForegroundChanged(true)
        advanceUntilIdle()
        assertTrue(rpgTrack.playing)

        manager.release()
    }
}
