package com.example.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Acoustic chamber synthesizer producing a warm, lush 44.1 kHz stereo medley of
 * soft concert flutes, ensemble violins (with warm cello bed), and wooden clarinet.
 */
object ChamberMedleySynth {
    const val SAMPLE_RATE = 44_100
    const val DURATION_SEC = 38.4 // 12 bars @ 75 BPM (3.2s per bar)
    const val NUM_FRAMES = (SAMPLE_RATE * DURATION_SEC).toInt()

    private const val TWO_PI = 2.0 * PI

    enum class Instrument {
        FLUTE,
        CLARINET,
        VIOLIN,
        CELLO,
        HARP_SOFT
    }

    data class NoteEvent(
        val instrument: Instrument,
        val startSec: Double,
        val durationSec: Double,
        val midiNote: Int,
        val velocity: Double,
        val pan: Double // -1.0 left .. +1.0 right
    )

    private fun midiToFreq(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

    // Midi helpers (C4 = 60, F4 = 65, A4 = 69)
    private const val G2 = 43
    private const val A2 = 45
    private const val Bb2 = 46
    private const val C3 = 48
    private const val D3 = 50
    private const val E3 = 52
    private const val F3 = 53
    private const val G3 = 55
    private const val A3 = 57
    private const val Bb3 = 58
    private const val C4 = 60
    private const val D4 = 62
    private const val E4 = 64
    private const val F4 = 65
    private const val G4 = 67
    private const val A4 = 69
    private const val Bb4 = 70
    private const val C5 = 72
    private const val D5 = 74
    private const val E5 = 76
    private const val F5 = 77
    private const val G5 = 79
    private const val A5 = 81

    fun buildMedleyScore(): List<NoteEvent> {
        val b = 0.8 // 75 BPM beat duration in seconds
        val events = mutableListOf<NoteEvent>()

        fun addChord(barIdx: Int, bass: Int, inner: List<Int>, arp: List<Int>) {
            val t0 = barIdx * 4 * b
            // Warm sustained cello root
            events += NoteEvent(Instrument.CELLO, t0, 4 * b + 0.15, bass, 0.36, -0.08)
            // Soft sustained lower strings pad
            inner.forEachIndexed { idx, note ->
                val pan = if (idx % 2 == 0) -0.35 else 0.35
                events += NoteEvent(Instrument.VIOLIN, t0, 4 * b + 0.12, note, 0.18, pan)
            }
            // Gentle muted harp / acoustic pulse on beats 1..4
            arp.forEachIndexed { idx, note ->
                events += NoteEvent(
                    Instrument.HARP_SOFT,
                    t0 + idx * b,
                    b * 1.6,
                    note,
                    0.20,
                    if (idx % 2 == 0) -0.25 else 0.25
                )
            }
        }

        // 12-Bar Pastoral Harmony in F Major / D Minor
        addChord(0, F3, listOf(A3, C4, E4), listOf(F3, C4, A4, E4))
        addChord(1, D3, listOf(A3, F4, C4), listOf(D3, A3, F4, C4))
        addChord(2, Bb2, listOf(F3, D4, A4), listOf(Bb2, F3, D4, A4))
        addChord(3, C3, listOf(G3, Bb3, E4), listOf(C3, G3, D4, E4))

        addChord(4, F3, listOf(C4, G4, A4), listOf(F3, C4, G4, A4))
        addChord(5, A2, listOf(E3, C4, G4), listOf(A2, E3, C4, G4))
        addChord(6, D3, listOf(A3, E4, F4), listOf(D3, A3, E4, F4))
        addChord(7, G2, listOf(D3, Bb3, F4), listOf(G2, D3, C4, E4))

        addChord(8, Bb2, listOf(F3, D4, A4), listOf(Bb2, F3, D4, A4))
        addChord(9, A2, listOf(E3, C4, G4), listOf(A2, E3, D4, F4))
        addChord(10, G2, listOf(D3, Bb3, F4), listOf(G2, D3, Bb3, F4))
        addChord(11, C3, listOf(G3, C4, E4), listOf(C3, G3, E4, C4))

        // --- SECTION A (Bars 0..3): Soft Woody Clarinet Theme + Warm Violin Harmonies ---
        val secA = listOf(
            // Bar 0 (0..4b)
            NoteEvent(Instrument.CLARINET, 0.0 * b, 1.5 * b, A4, 0.56, -0.22),
            NoteEvent(Instrument.CLARINET, 1.5 * b, 0.5 * b, Bb4, 0.50, -0.22),
            NoteEvent(Instrument.CLARINET, 2.0 * b, 1.0 * b, C5, 0.58, -0.22),
            NoteEvent(Instrument.CLARINET, 3.0 * b, 1.0 * b, A4, 0.52, -0.22),
            // Bar 1
            NoteEvent(Instrument.CLARINET, 4.0 * b, 1.5 * b, F4, 0.54, -0.22),
            NoteEvent(Instrument.CLARINET, 5.5 * b, 0.5 * b, G4, 0.50, -0.22),
            NoteEvent(Instrument.CLARINET, 6.0 * b, 2.0 * b, A4, 0.56, -0.22),
            // Violin soft answer
            NoteEvent(Instrument.VIOLIN, 6.0 * b, 2.0 * b, C5, 0.34, 0.38),
            // Bar 2
            NoteEvent(Instrument.CLARINET, 8.0 * b, 1.5 * b, D4, 0.52, -0.22),
            NoteEvent(Instrument.CLARINET, 9.5 * b, 0.5 * b, F4, 0.50, -0.22),
            NoteEvent(Instrument.CLARINET, 10.0 * b, 1.0 * b, Bb4, 0.56, -0.22),
            NoteEvent(Instrument.CLARINET, 11.0 * b, 1.0 * b, A4, 0.52, -0.22),
            // Bar 3
            NoteEvent(Instrument.CLARINET, 12.0 * b, 2.0 * b, G4, 0.54, -0.22),
            NoteEvent(Instrument.FLUTE, 14.0 * b, 1.0 * b, C5, 0.44, 0.25),
            NoteEvent(Instrument.FLUTE, 15.0 * b, 1.0 * b, D5, 0.46, 0.25)
        )
        events += secA

        // --- SECTION B (Bars 4..7): Airy Soft Flute Lead + Singing Violin Countermelody ---
        val secB = listOf(
            // Bar 4
            NoteEvent(Instrument.FLUTE, 16.0 * b, 1.5 * b, F5, 0.58, 0.25),
            NoteEvent(Instrument.FLUTE, 17.5 * b, 0.5 * b, E5, 0.52, 0.25),
            NoteEvent(Instrument.FLUTE, 18.0 * b, 1.0 * b, C5, 0.55, 0.25),
            NoteEvent(Instrument.FLUTE, 19.0 * b, 1.0 * b, A4, 0.50, 0.25),
            NoteEvent(Instrument.VIOLIN, 16.0 * b, 2.0 * b, A4, 0.36, -0.35),
            NoteEvent(Instrument.VIOLIN, 18.0 * b, 2.0 * b, G4, 0.36, -0.35),
            // Bar 5
            NoteEvent(Instrument.FLUTE, 20.0 * b, 1.5 * b, C5, 0.55, 0.25),
            NoteEvent(Instrument.FLUTE, 21.5 * b, 0.5 * b, D5, 0.52, 0.25),
            NoteEvent(Instrument.FLUTE, 22.0 * b, 2.0 * b, E5, 0.56, 0.25),
            NoteEvent(Instrument.CLARINET, 20.0 * b, 2.0 * b, E4, 0.36, -0.28),
            NoteEvent(Instrument.CLARINET, 22.0 * b, 2.0 * b, G4, 0.38, -0.28),
            // Bar 6
            NoteEvent(Instrument.FLUTE, 24.0 * b, 1.0 * b, F5, 0.58, 0.25),
            NoteEvent(Instrument.FLUTE, 25.0 * b, 1.0 * b, G5, 0.58, 0.25),
            NoteEvent(Instrument.FLUTE, 26.0 * b, 1.0 * b, E5, 0.54, 0.25),
            NoteEvent(Instrument.FLUTE, 27.0 * b, 1.0 * b, D5, 0.52, 0.25),
            NoteEvent(Instrument.VIOLIN, 24.0 * b, 2.0 * b, A4, 0.38, -0.35),
            NoteEvent(Instrument.VIOLIN, 26.0 * b, 2.0 * b, C5, 0.38, -0.35),
            // Bar 7
            NoteEvent(Instrument.FLUTE, 28.0 * b, 2.0 * b, D5, 0.52, 0.25),
            NoteEvent(Instrument.FLUTE, 30.0 * b, 2.0 * b, C5, 0.48, 0.25),
            NoteEvent(Instrument.CLARINET, 28.0 * b, 2.0 * b, Bb4, 0.40, -0.25),
            NoteEvent(Instrument.CLARINET, 30.0 * b, 2.0 * b, G4, 0.40, -0.25)
        )
        events += secB

        // --- SECTION C (Bars 8..11): Lush Flute, Violin & Clarinet Chamber Medley Trio ---
        val secC = listOf(
            // Bar 8: Violin sings soaring melody while Flute & Clarinet weave softly
            NoteEvent(Instrument.VIOLIN, 32.0 * b, 1.5 * b, D5, 0.56, -0.30),
            NoteEvent(Instrument.VIOLIN, 33.5 * b, 0.5 * b, C5, 0.52, -0.30),
            NoteEvent(Instrument.VIOLIN, 34.0 * b, 2.0 * b, Bb4, 0.54, -0.30),
            NoteEvent(Instrument.FLUTE, 32.0 * b, 2.0 * b, F5, 0.42, 0.32),
            NoteEvent(Instrument.FLUTE, 34.0 * b, 2.0 * b, D5, 0.40, 0.32),
            NoteEvent(Instrument.CLARINET, 32.0 * b, 4.0 * b, F4, 0.36, -0.10),

            // Bar 9
            NoteEvent(Instrument.VIOLIN, 36.0 * b, 1.5 * b, C5, 0.54, -0.30),
            NoteEvent(Instrument.VIOLIN, 37.5 * b, 0.5 * b, Bb4, 0.50, -0.30),
            NoteEvent(Instrument.VIOLIN, 38.0 * b, 2.0 * b, A4, 0.52, -0.30),
            NoteEvent(Instrument.FLUTE, 36.0 * b, 2.0 * b, E5, 0.40, 0.32),
            NoteEvent(Instrument.FLUTE, 38.0 * b, 2.0 * b, C5, 0.40, 0.32),
            NoteEvent(Instrument.CLARINET, 36.0 * b, 2.0 * b, G4, 0.38, -0.15),
            NoteEvent(Instrument.CLARINET, 38.0 * b, 2.0 * b, F4, 0.40, -0.15),

            // Bar 10
            NoteEvent(Instrument.CLARINET, 40.0 * b, 1.5 * b, G4, 0.52, -0.20),
            NoteEvent(Instrument.CLARINET, 41.5 * b, 0.5 * b, A4, 0.50, -0.20),
            NoteEvent(Instrument.CLARINET, 42.0 * b, 2.0 * b, Bb4, 0.52, -0.20),
            NoteEvent(Instrument.FLUTE, 40.0 * b, 2.0 * b, D5, 0.42, 0.28),
            NoteEvent(Instrument.FLUTE, 42.0 * b, 2.0 * b, F5, 0.42, 0.28),
            NoteEvent(Instrument.VIOLIN, 40.0 * b, 4.0 * b, Bb4, 0.36, 0.0),

            // Bar 11: Gentle cadence back to Fmaj7
            NoteEvent(Instrument.FLUTE, 44.0 * b, 2.0 * b, E5, 0.44, 0.25),
            NoteEvent(Instrument.FLUTE, 46.0 * b, 2.0 * b, C5, 0.38, 0.25),
            NoteEvent(Instrument.CLARINET, 44.0 * b, 2.0 * b, G4, 0.46, -0.22),
            NoteEvent(Instrument.CLARINET, 46.0 * b, 2.0 * b, E4, 0.40, -0.22),
            NoteEvent(Instrument.VIOLIN, 44.0 * b, 4.0 * b, A4, 0.36, 0.10)
        )
        events += secC

        return events
    }

    fun renderPcmStereo(): Pair<DoubleArray, DoubleArray> {
        // Render extra 1.8s tail for seamless loop overlap-add
        val tailFrames = (SAMPLE_RATE * 1.8).toInt()
        val totalFrames = NUM_FRAMES + tailFrames
        val left = DoubleArray(totalFrames)
        val right = DoubleArray(totalFrames)
        val rng = Random(20261115L)

        val notes = buildMedleyScore()
        for (note in notes) {
            synthesizeNoteInto(note, left, right, totalFrames, rng)
        }

        // Apply warm stereo wooden-hall reverb + master low-pass warmth filter
        applyWarmHallReverb(left, right)
        applyWarmLowPass(left, cutoffHz = 2600.0)
        applyWarmLowPass(right, cutoffHz = 2600.0)

        // Seamless loop fold-back: add tail beyond NUM_FRAMES smoothly into start of track
        val loopL = DoubleArray(NUM_FRAMES)
        val loopR = DoubleArray(NUM_FRAMES)
        for (i in 0 until NUM_FRAMES) {
            loopL[i] = left[i]
            loopR[i] = right[i]
        }
        for (i in 0 until tailFrames) {
            val fade = 1.0 - (i.toDouble() / tailFrames)
            loopL[i] += left[NUM_FRAMES + i] * fade
            loopR[i] += right[NUM_FRAMES + i] * fade
        }

        // Soft-knee warm normalization (target peak ~0.78 so it feels plush & gentle, never harsh)
        var maxAbs = 0.0
        for (i in 0 until NUM_FRAMES) {
            val a = kotlin.math.abs(loopL[i])
            val b = kotlin.math.abs(loopR[i])
            if (a > maxAbs) maxAbs = a
            if (b > maxAbs) maxAbs = b
        }
        val gain = if (maxAbs > 1e-6) 0.78 / maxAbs else 1.0
        for (i in 0 until NUM_FRAMES) {
            loopL[i] = tanh(loopL[i] * gain * 1.05) * 0.85
            loopR[i] = tanh(loopR[i] * gain * 1.05) * 0.85
        }

        return loopL to loopR
    }

    private fun synthesizeNoteInto(
        note: NoteEvent,
        left: DoubleArray,
        right: DoubleArray,
        maxFrames: Int,
        rng: Random
    ) {
        val baseFreq = midiToFreq(note.midiNote)
        val startIdx = (note.startSec * SAMPLE_RATE).toInt().coerceAtLeast(0)
        val releaseSec = when (note.instrument) {
            Instrument.VIOLIN -> 0.28
            Instrument.CELLO -> 0.32
            Instrument.FLUTE -> 0.18
            Instrument.CLARINET -> 0.16
            Instrument.HARP_SOFT -> 0.34
        }
        val attackSec = when (note.instrument) {
            Instrument.VIOLIN -> 0.18
            Instrument.CELLO -> 0.20
            Instrument.FLUTE -> 0.095
            Instrument.CLARINET -> 0.080
            Instrument.HARP_SOFT -> 0.015
        }

        val totalNoteSec = note.durationSec + releaseSec
        val noteFrames = (totalNoteSec * SAMPLE_RATE).toInt()
        val endIdx = (startIdx + noteFrames).coerceAtMost(maxFrames)

        // Stereo panning (equal-power)
        val panAngle = (note.pan.coerceIn(-1.0, 1.0) + 1.0) * (PI / 4.0)
        val gainL = cos(panAngle) * note.velocity
        val gainR = sin(panAngle) * note.velocity

        // Per-voice phase accumulators
        var phase1 = rng.nextDouble() * TWO_PI
        var phase2 = rng.nextDouble() * TWO_PI
        var phase3 = rng.nextDouble() * TWO_PI
        var breathFilter = 0.0
        var voiceLp1 = 0.0
        var voiceLp2 = 0.0

        val lpCutoff = when (note.instrument) {
            Instrument.FLUTE -> 1650.0
            Instrument.CLARINET -> 1380.0
            Instrument.VIOLIN -> 1750.0
            Instrument.CELLO -> 850.0
            Instrument.HARP_SOFT -> 1350.0
        }
        val lpAlpha = 1.0 - exp(-TWO_PI * lpCutoff / SAMPLE_RATE)

        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val env = when {
                t < attackSec -> {
                    val u = t / attackSec
                    0.5 * (1.0 - cos(PI * u)) // silky S-curve onset, zero click
                }
                t <= note.durationSec -> {
                    if (note.instrument == Instrument.HARP_SOFT) {
                        exp(-(t - attackSec) * 2.2)
                    } else {
                        // Gentle musical phrasing swell
                        val progress = (t - attackSec) / (note.durationSec - attackSec).coerceAtLeast(0.01)
                        0.86 + 0.14 * sin(PI * progress)
                    }
                }
                else -> {
                    val u = (t - note.durationSec) / releaseSec
                    val hold = if (note.instrument == Instrument.HARP_SOFT) exp(-(note.durationSec - attackSec) * 2.2) else 0.86
                    hold * 0.5 * (1.0 + cos(PI * u.coerceIn(0.0, 1.0)))
                }
            }

            // Expressive natural vibrato after onset
            val vibRamp = ((t - 0.16) / 0.26).coerceIn(0.0, 1.0)
            val vibDepth = when (note.instrument) {
                Instrument.FLUTE -> 0.0040 * vibRamp
                Instrument.CLARINET -> 0.0030 * vibRamp
                Instrument.VIOLIN -> 0.0050 * vibRamp
                Instrument.CELLO -> 0.0024 * vibRamp
                Instrument.HARP_SOFT -> 0.0
            }
            val vibRate = when (note.instrument) {
                Instrument.FLUTE -> 4.8
                Instrument.CLARINET -> 4.3
                Instrument.VIOLIN -> 5.1
                else -> 4.0
            }
            val fNow = baseFreq * (1.0 + vibDepth * sin(TWO_PI * vibRate * t))

            val rawSample = when (note.instrument) {
                Instrument.FLUTE -> {
                    phase1 += TWO_PI * fNow / SAMPLE_RATE
                    if (phase1 > TWO_PI) phase1 -= TWO_PI
                    val body = sin(phase1) + 0.18 * sin(2.0 * phase1) + 0.025 * sin(3.0 * phase1)
                    // Velvety filtered air-stream breath
                    val white = rng.nextDouble() * 2.0 - 1.0
                    breathFilter += 0.075 * (white - breathFilter)
                    val breathAmt = if (t < 0.14) 0.028 else 0.010
                    (body * 0.85 + breathFilter * breathAmt) * 0.72
                }

                Instrument.CLARINET -> {
                    // Warm cylindrical wooden bore: rich odd harmonics 1, 3, 5, gently rounded
                    phase1 += TWO_PI * fNow / SAMPLE_RATE
                    if (phase1 > TWO_PI) phase1 -= TWO_PI
                    val body = sin(phase1) +
                        0.05 * sin(2.0 * phase1) +
                        0.30 * sin(3.0 * phase1) +
                        0.02 * sin(4.0 * phase1) +
                        0.08 * sin(5.0 * phase1)
                    body * 0.64
                }

                Instrument.VIOLIN -> {
                    // 3 slightly detuned bowed voices for warm ensemble chorus without nasal/tinny edge
                    phase1 += TWO_PI * (fNow * 0.9965) / SAMPLE_RATE
                    phase2 += TWO_PI * fNow / SAMPLE_RATE
                    phase3 += TWO_PI * (fNow * 1.0040) / SAMPLE_RATE
                    if (phase1 > TWO_PI) phase1 -= TWO_PI
                    if (phase2 > TWO_PI) phase2 -= TWO_PI
                    if (phase3 > TWO_PI) phase3 -= TWO_PI

                    fun bowed(p: Double): Double =
                        sin(p) + 0.42 * sin(2.0 * p) + 0.17 * sin(3.0 * p) +
                            0.06 * sin(4.0 * p) + 0.02 * sin(5.0 * p)

                    (bowed(phase1) + bowed(phase2) + bowed(phase3)) * 0.25
                }

                Instrument.CELLO -> {
                    phase1 += TWO_PI * fNow / SAMPLE_RATE
                    phase2 += TWO_PI * (fNow * 1.002) / SAMPLE_RATE
                    if (phase1 > TWO_PI) phase1 -= TWO_PI
                    if (phase2 > TWO_PI) phase2 -= TWO_PI
                    val s1 = sin(phase1) + 0.45 * sin(2.0 * phase1) + 0.18 * sin(3.0 * phase1) + 0.06 * sin(4.0 * phase1)
                    val s2 = sin(phase2) + 0.45 * sin(2.0 * phase2) + 0.18 * sin(3.0 * phase2)
                    (s1 + s2) * 0.36
                }

                Instrument.HARP_SOFT -> {
                    phase1 += TWO_PI * fNow / SAMPLE_RATE
                    if (phase1 > TWO_PI) phase1 -= TWO_PI
                    val decayHarm = exp(-t * 4.5)
                    (sin(phase1) + 0.24 * decayHarm * sin(2.0 * phase1) + 0.05 * decayHarm * sin(3.0 * phase1)) * 0.64
                }
            }

            // Cascaded 2-stage per-voice warm low-pass body filter
            voiceLp1 += lpAlpha * (rawSample - voiceLp1)
            voiceLp2 += lpAlpha * (voiceLp1 - voiceLp2)
            val shaped = voiceLp2 * env
            left[i] += shaped * gainL
            right[i] += shaped * gainR
        }
    }

    private fun applyWarmHallReverb(left: DoubleArray, right: DoubleArray) {
        val len = left.size
        val delays = intArrayOf(
            (0.029 * SAMPLE_RATE).toInt(),
            (0.047 * SAMPLE_RATE).toInt(),
            (0.073 * SAMPLE_RATE).toInt(),
            (0.113 * SAMPLE_RATE).toInt()
        )
        val gains = doubleArrayOf(0.20, 0.16, 0.12, 0.09)
        val wetL = DoubleArray(len)
        val wetR = DoubleArray(len)

        for (tap in delays.indices) {
            val d = delays[tap]
            val g = gains[tap]
            var dampL = 0.0
            var dampR = 0.0
            val dampAlpha = 0.22 // heavy warm absorption on reflections (wood hall feel)
            for (i in d until len) {
                // Cross-coupled stereo taps for spacious acoustic ensemble bloom
                val srcL = if (tap % 2 == 0) left[i - d] else right[i - d]
                val srcR = if (tap % 2 == 0) right[i - d] else left[i - d]
                dampL += dampAlpha * (srcL - dampL)
                dampR += dampAlpha * (srcR - dampR)
                wetL[i] += dampL * g
                wetR[i] += dampR * g
            }
        }

        for (i in 0 until len) {
            left[i] = left[i] * 0.80 + wetL[i] * 0.42
            right[i] = right[i] * 0.80 + wetR[i] * 0.42
        }
    }

    private fun applyWarmLowPass(signal: DoubleArray, cutoffHz: Double) {
        // Cascaded 2-stage RC low-pass filter for smooth high-frequency warmth
        val alpha = 1.0 - exp(-TWO_PI * cutoffHz / SAMPLE_RATE)
        var s1 = 0.0
        var s2 = 0.0
        for (i in signal.indices) {
            s1 += alpha * (signal[i] - s1)
            s2 += alpha * (s1 - s2)
            signal[i] = s2
        }
    }

    fun writeWavFile(outputFile: File) {
        val (left, right) = renderPcmStereo()
        outputFile.parentFile?.mkdirs()
        val dataSize = NUM_FRAMES * 4 // 2 channels * 2 bytes per sample
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(2) // Stereo
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 4) // Byte rate
            putShort(4) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }

        BufferedOutputStream(FileOutputStream(outputFile)).use { out ->
            out.write(header.array())
            val buf = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until NUM_FRAMES) {
                val sL = (left[i].coerceIn(-1.0, 1.0) * 32767.0).roundToInt().toShort()
                val sR = (right[i].coerceIn(-1.0, 1.0) * 32767.0).roundToInt().toShort()
                buf.putShort(sL)
                buf.putShort(sR)
                if (!buf.hasRemaining()) {
                    out.write(buf.array(), 0, buf.position())
                    buf.clear()
                }
            }
            if (buf.position() > 0) {
                out.write(buf.array(), 0, buf.position())
            }
        }
    }
}
