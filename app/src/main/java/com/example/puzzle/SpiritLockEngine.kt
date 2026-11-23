package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.random.Random

enum class SpiritRune(val label: String) {
    SUN("Sun"),
    MOON("Moon"),
    FERN("Fern"),
    DEW("Dew"),
    ORCHID("Orchid"),
    STAR("Star")
}

data class SpiritGuessRecord(
    val guess: List<SpiritRune>,
    val exactMatches: Int,     // Right rune & right position (Gold pip)
    val misplacedMatches: Int  // Right rune, wrong position (Silver pip)
)

data class SpiritLockPuzzle(
    val secretCode: List<SpiritRune>,
    val availableRunes: List<SpiritRune>,
    val maxAttempts: Int,
    val attempts: List<SpiritGuessRecord> = emptyList(),
    val currentDraft: List<SpiritRune> = emptyList(),
    val revealedHintIndices: Map<Int, SpiritRune> = emptyMap(),
    val isSolved: Boolean = false,
    val isOutOfAttempts: Boolean = false,
    val difficulty: DifficultyTier
)

object SpiritLockEngine {

    fun generate(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): SpiritLockPuzzle {
        val codeLength = if (difficulty == DifficultyTier.SPROUT) 3 else 4
        val poolCount = when {
            difficulty == DifficultyTier.SPROUT && level == 1 -> 4
            difficulty == DifficultyTier.SPROUT -> 5
            else -> 6
        }
        val availableRunes = SpiritRune.entries.take(poolCount)
        val allowDuplicates = difficulty == DifficultyTier.ELDER || (difficulty == DifficultyTier.EXPLORER && level >= 4)

        val secretCode = if (allowDuplicates) {
            List(codeLength) { availableRunes[random.nextInt(availableRunes.size)] }
        } else {
            availableRunes.shuffled(random).take(codeLength)
        }

        val maxAttempts = when (difficulty) {
            DifficultyTier.SPROUT -> 7
            DifficultyTier.EXPLORER -> 8
            DifficultyTier.ELDER -> 8
        }

        return SpiritLockPuzzle(
            secretCode = secretCode,
            availableRunes = availableRunes,
            maxAttempts = maxAttempts,
            difficulty = difficulty
        )
    }

    fun evaluateGuess(
        secret: List<SpiritRune>,
        guess: List<SpiritRune>
    ): SpiritGuessRecord {
        var exact = 0
        val unmatchedSecret = mutableMapOf<SpiritRune, Int>()
        val unmatchedGuess = mutableMapOf<SpiritRune, Int>()

        for (i in secret.indices) {
            if (secret[i] == guess[i]) {
                exact++
            } else {
                unmatchedSecret[secret[i]] = (unmatchedSecret[secret[i]] ?: 0) + 1
                unmatchedGuess[guess[i]] = (unmatchedGuess[guess[i]] ?: 0) + 1
            }
        }

        var misplaced = 0
        for ((rune, count) in unmatchedGuess) {
            misplaced += minOf(count, unmatchedSecret[rune] ?: 0)
        }

        return SpiritGuessRecord(
            guess = guess,
            exactMatches = exact,
            misplacedMatches = misplaced
        )
    }

    fun submitDraft(puzzle: SpiritLockPuzzle): SpiritLockPuzzle {
        if (puzzle.currentDraft.size != puzzle.secretCode.size || puzzle.isSolved) return puzzle
        val record = evaluateGuess(puzzle.secretCode, puzzle.currentDraft)
        val newAttempts = puzzle.attempts + record
        val solved = record.exactMatches == puzzle.secretCode.size
        val outOfAttempts = !solved && newAttempts.size >= puzzle.maxAttempts

        return puzzle.copy(
            attempts = newAttempts,
            currentDraft = emptyList(),
            isSolved = solved,
            isOutOfAttempts = outOfAttempts
        )
    }

    fun revealHint(puzzle: SpiritLockPuzzle): SpiritLockPuzzle {
        val unrevealed = puzzle.secretCode.indices.firstOrNull { it !in puzzle.revealedHintIndices }
            ?: return puzzle
        val updatedHints = puzzle.revealedHintIndices + (unrevealed to puzzle.secretCode[unrevealed])
        return puzzle.copy(revealedHintIndices = updatedHints)
    }

    fun computeScore(
        difficulty: DifficultyTier,
        attemptsUsed: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 150
            DifficultyTier.EXPLORER -> 210
            DifficultyTier.ELDER -> 280
        }
        return (base - (attemptsUsed - 1) * 16 - hintsUsed * 20).coerceAtLeast(75)
    }
}
