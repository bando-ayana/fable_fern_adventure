package com.example.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RpgMusicQualityTest {

    private fun resolveRpgMusicFile(): File {
        val candidates = listOf(
            File("src/main/res/raw/rpg_canopy_music.wav"),
            File("app/src/main/res/raw/rpg_canopy_music.wav")
        )
        return candidates.firstOrNull { it.exists() || it.parentFile?.exists() == true }
            ?: candidates.first()
    }

    @Test
    fun `medley score features flutes violins and clarinet with warm accompaniment`() {
        val score = ChamberMedleySynth.buildMedleyScore()
        val instruments = score.map { it.instrument }.toSet()
        assertTrue(ChamberMedleySynth.Instrument.FLUTE in instruments)
        assertTrue(ChamberMedleySynth.Instrument.VIOLIN in instruments)
        assertTrue(ChamberMedleySynth.Instrument.CLARINET in instruments)
        assertTrue(ChamberMedleySynth.Instrument.CELLO in instruments)
        assertTrue(score.count { it.instrument == ChamberMedleySynth.Instrument.FLUTE } >= 10)
        assertTrue(score.count { it.instrument == ChamberMedleySynth.Instrument.CLARINET } >= 10)
        assertTrue(score.count { it.instrument == ChamberMedleySynth.Instrument.VIOLIN } >= 10)
    }

    @Test
    fun `rpg_canopy_music wav is warm 44100Hz stereo soft non-clipping and seamlessly loops`() {
        val wavFile = resolveRpgMusicFile()
        assertTrue("Expected WAV file to exist at ${wavFile.absolutePath}", wavFile.exists())

        val bytes = wavFile.readBytes()
        assertTrue("WAV file too small: ${bytes.size}", bytes.size > 44)

        val header = ByteBuffer.wrap(bytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
        val riff = ByteArray(4).apply { header.get(this) }.decodeToString()
        header.int // file size - 8
        val wave = ByteArray(4).apply { header.get(this) }.decodeToString()
        val fmt = ByteArray(4).apply { header.get(this) }.decodeToString()
        val fmtChunkSize = header.int
        val audioFormat = header.short.toInt()
        val channels = header.short.toInt()
        val sampleRate = header.int
        header.int // byte rate
        header.short // block align
        val bitsPerSample = header.short.toInt()
        val dataId = ByteArray(4).apply { header.get(this) }.decodeToString()
        val dataSize = header.int

        assertEquals("RIFF", riff)
        assertEquals("WAVE", wave)
        assertEquals("fmt ", fmt)
        assertEquals(16, fmtChunkSize)
        assertEquals(1, audioFormat)
        assertEquals(2, channels)
        assertEquals(44_100, sampleRate)
        assertEquals(16, bitsPerSample)
        assertEquals("data", dataId)

        val expectedDataSize = ChamberMedleySynth.NUM_FRAMES * 4
        assertEquals(expectedDataSize, dataSize)
        assertEquals(44 + expectedDataSize, bytes.size)

        // Read PCM samples and analyze dynamics, loop seam, and spectral warmth
        val pcm = ByteBuffer.wrap(bytes, 44, dataSize).order(ByteOrder.LITTLE_ENDIAN)
        val numFrames = ChamberMedleySynth.NUM_FRAMES
        val left = DoubleArray(numFrames)
        val right = DoubleArray(numFrames)
        var maxAbs = 0.0
        var sumSq = 0.0
        for (i in 0 until numFrames) {
            val l = pcm.short.toDouble() / 32768.0
            val r = pcm.short.toDouble() / 32768.0
            left[i] = l
            right[i] = r
            val a = maxOf(abs(l), abs(r))
            if (a > maxAbs) maxAbs = a
            sumSq += 0.5 * (l * l + r * r)
        }
        val rms = sqrt(sumSq / numFrames)
        assertTrue("Track should not clip (peak=$maxAbs)", maxAbs < 0.85)
        assertTrue("Track should have audible soft level (rms=$rms)", rms in 0.05..0.32)

        // Seamless loop boundary check (end frame to start frame delta)
        val seamJumpL = abs(left[0] - left[numFrames - 1])
        val seamJumpR = abs(right[0] - right[numFrames - 1])
        assertTrue("Left loop seam jump too large: $seamJumpL", seamJumpL < 0.025)
        assertTrue("Right loop seam jump too large: $seamJumpR", seamJumpR < 0.025)

        // Verify warm spectral balance using Goertzel power across frequency bands
        val warmFreqs = doubleArrayOf(175.0, 262.0, 349.0, 440.0, 587.0, 784.0, 1046.0, 1397.0)
        val tinnyFreqs = doubleArrayOf(3500.0, 4200.0, 5000.0, 6500.0, 8000.0)
        val startSample = (sampleRate * 8.0).toInt()
        val windowLen = 16384

        fun bandPower(freqs: DoubleArray): Double {
            var total = 0.0
            for (f in freqs) {
                val omega = 2.0 * PI * f / sampleRate
                val coeff = 2.0 * cos(omega)
                var q1 = 0.0
                var q2 = 0.0
                for (n in 0 until windowLen) {
                    val hann = 0.5 * (1.0 - cos(2.0 * PI * n / (windowLen - 1)))
                    val sample = 0.5 * (left[startSample + n] + right[startSample + n]) * hann
                    val q0 = coeff * q1 - q2 + sample
                    q2 = q1
                    q1 = q0
                }
                val real = q1 - q2 * cos(omega)
                val imag = q2 * sin(omega)
                total += real * real + imag * imag
            }
            return total / freqs.size
        }

        val warmPower = bandPower(warmFreqs)
        val highPower = bandPower(tinnyFreqs)
        assertTrue("Warm musical body power should dominate high frequencies ($warmPower vs $highPower)", warmPower > highPower * 100.0)
    }
}
