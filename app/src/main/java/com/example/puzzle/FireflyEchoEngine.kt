package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.random.Random

data class FireflyRound(
    val roundNumber: Int,
    val totalRounds: Int,
    val sequence: List<Int>,
    val blossomCount: Int = 6
)

object FireflyEchoEngine {

    val blossomNames = listOf(
        "Sunlily",
        "Dewbell",
        "Orchid",
        "Fernbud",
        "Moonpetal",
        "Starberry"
    )

    fun createRounds(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): List<FireflyRound> {
        val levelBonus = ((level - 1) / 2).coerceAtMost(2)
        val startLen = when (difficulty) {
            DifficultyTier.SPROUT -> 2 + levelBonus // Level 1 Sprout starts at super approachable 2->3->4!
            DifficultyTier.EXPLORER -> 3 + levelBonus
            DifficultyTier.ELDER -> 4 + levelBonus
        }
        val totalRounds = 3
        return (0 until totalRounds).map { roundIdx ->
            val len = (startLen + roundIdx).coerceAtMost(8)
            val seq = buildList<Int> {
                repeat(len) {
                    var next = random.nextInt(6)
                    // Keep sequences musical and readable by avoiding 3 identical consecutive taps
                    if (size >= 2 && this[size - 1] == next && this[size - 2] == next) {
                        next = (next + 1 + random.nextInt(5)) % 6
                    }
                    add(next)
                }
            }
            FireflyRound(
                roundNumber = roundIdx + 1,
                totalRounds = totalRounds,
                sequence = seq
            )
        }
    }

    fun computeScore(
        difficulty: DifficultyTier,
        mistakes: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 120
            DifficultyTier.EXPLORER -> 180
            DifficultyTier.ELDER -> 240
        }
        return (base - mistakes * 15 - hintsUsed * 10).coerceAtLeast(60)
    }
}
