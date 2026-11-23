package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.random.Random

enum class LeafShape(val label: String) {
    HEARTLEAF("Heartleaf"),
    CURLFERN("Curlfern"),
    TRIFOIL("Trifoil"),
    MOONPETAL("Moonpetal")
}

enum class LeafTint(val label: String) {
    EMERALD("Emerald"),
    GOLD("Gold"),
    TEAL("Teal"),
    CORAL("Coral")
}

data class LeafToken(
    val shape: LeafShape,
    val tint: LeafTint,
    val tilted: Boolean = false
)

data class FernMosaicRound(
    val roundNumber: Int,
    val totalRounds: Int,
    val patternHint: String,
    val sequence: List<LeafToken>,
    val missingIndices: Set<Int>,
    val placedAnswers: Map<Int, LeafToken> = emptyMap(),
    val choices: List<LeafToken>
) {
    val isRoundSolved: Boolean
        get() = missingIndices.all { idx -> placedAnswers[idx] == sequence[idx] }
}

data class FernMosaicPuzzle(
    val rounds: List<FernMosaicRound>,
    val currentRoundIdx: Int = 0,
    val selectedSlotIdx: Int,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val difficulty: DifficultyTier
) {
    val currentRound: FernMosaicRound? get() = rounds.getOrNull(currentRoundIdx)
    val isCompleted: Boolean get() = currentRoundIdx >= rounds.size
}

object FernMosaicEngine {

    fun generate(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): FernMosaicPuzzle {
        val totalRounds = 3
        val rounds = (0 until totalRounds).map { roundIdx ->
            buildRound(
                roundNumber = roundIdx + 1,
                totalRounds = totalRounds,
                difficulty = difficulty,
                level = level,
                random = random
            )
        }
        val firstSlot = rounds.first().missingIndices.min()
        return FernMosaicPuzzle(
            rounds = rounds,
            currentRoundIdx = 0,
            selectedSlotIdx = firstSlot,
            difficulty = difficulty
        )
    }

    private fun buildRound(
        roundNumber: Int,
        totalRounds: Int,
        difficulty: DifficultyTier,
        level: Int,
        random: Random
    ): FernMosaicRound {
        val shapes = LeafShape.entries.shuffled(random)
        val tints = LeafTint.entries.shuffled(random)
        val length = if (difficulty == DifficultyTier.SPROUT && level == 1) 6 else 8

        val patternStyle = when {
            difficulty == DifficultyTier.SPROUT && roundNumber == 1 -> 0
            difficulty == DifficultyTier.SPROUT -> random.nextInt(2)
            difficulty == DifficultyTier.EXPLORER -> random.nextInt(3)
            else -> 1 + random.nextInt(3)
        }

        val (hint, seq) = when (patternStyle) {
            0 -> {
                // Rhythmic ABAB alternating leaves
                val a = LeafToken(shapes[0], tints[0])
                val b = LeafToken(shapes[1], tints[1])
                "Two jungle leaves trade places in steady rhythm." to List(length) { i ->
                    if (i % 2 == 0) a else b
                }
            }
            1 -> {
                // Trio ABCABC cycle
                val a = LeafToken(shapes[0], tints[0])
                val b = LeafToken(shapes[1], tints[1])
                val c = LeafToken(shapes[2], tints[2])
                "Three canopy leaves repeat in a rolling trio." to List(length) { i ->
                    listOf(a, b, c)[i % 3]
                }
            }
            2 -> {
                // Mirrored AABB / ABBA cadence
                val a = LeafToken(shapes[0], tints[0])
                val b = LeafToken(shapes[1], tints[1])
                "Twin pairs echo each other along the branch." to List(length) { i ->
                    if ((i / 2) % 2 == 0) a else b
                }
            }
            else -> {
                // Dual-rule: 3 shapes cycling while 2 tints + tilt alternate!
                "Watch both the leaf shape trio and the alternating color & tilt!" to List(length) { i ->
                    LeafToken(
                        shape = shapes[i % 3],
                        tint = tints[i % 2],
                        tilted = i % 2 == 1
                    )
                }
            }
        }

        val missingCount = if (difficulty == DifficultyTier.SPROUT || roundNumber == 1) 1 else 2
        val candidateSlots = (2 until length).shuffled(random).take(missingCount).toSet()

        val requiredTokens = candidateSlots.map { seq[it] }.distinct()
        val distractors = buildList {
            for (s in shapes) {
                for (t in tints) {
                    val tok = LeafToken(s, t, tilted = patternStyle >= 3 && random.nextBoolean())
                    if (tok !in requiredTokens) add(tok)
                }
            }
        }.shuffled(random)

        val choiceCount = 4
        val choices = (requiredTokens + distractors.take((choiceCount - requiredTokens.size).coerceAtLeast(0)))
            .shuffled(random)

        return FernMosaicRound(
            roundNumber = roundNumber,
            totalRounds = totalRounds,
            patternHint = hint,
            sequence = seq,
            missingIndices = candidateSlots,
            choices = choices
        )
    }

    fun selectChoice(puzzle: FernMosaicPuzzle, token: LeafToken): Pair<FernMosaicPuzzle, Boolean> {
        val round = puzzle.currentRound ?: return puzzle to false
        val slot = puzzle.selectedSlotIdx
        if (slot !in round.missingIndices) return puzzle to false

        val isCorrect = round.sequence[slot] == token
        return if (isCorrect) {
            val updatedPlaced = round.placedAnswers + (slot to token)
            val updatedRound = round.copy(placedAnswers = updatedPlaced)
            val updatedRounds = puzzle.rounds.toMutableList().apply {
                this[puzzle.currentRoundIdx] = updatedRound
            }

            if (updatedRound.isRoundSolved) {
                val nextIdx = puzzle.currentRoundIdx + 1
                val nextSlot = updatedRounds.getOrNull(nextIdx)?.missingIndices?.minOrNull() ?: 0
                puzzle.copy(
                    rounds = updatedRounds,
                    currentRoundIdx = nextIdx,
                    selectedSlotIdx = nextSlot
                ) to true
            } else {
                val nextMissing = updatedRound.missingIndices.first { it !in updatedPlaced }
                puzzle.copy(
                    rounds = updatedRounds,
                    selectedSlotIdx = nextMissing
                ) to true
            }
        } else {
            puzzle.copy(mistakes = puzzle.mistakes + 1) to false
        }
    }

    fun applyHint(puzzle: FernMosaicPuzzle): FernMosaicPuzzle {
        val round = puzzle.currentRound ?: return puzzle
        val slot = puzzle.selectedSlotIdx.takeIf { it in round.missingIndices && it !in round.placedAnswers }
            ?: round.missingIndices.firstOrNull { it !in round.placedAnswers }
            ?: return puzzle
        val correctToken = round.sequence[slot]
        val (updatedPuzzle, _) = selectChoice(
            puzzle.copy(selectedSlotIdx = slot, hintsUsed = puzzle.hintsUsed + 1),
            correctToken
        )
        return updatedPuzzle
    }

    fun computeScore(
        difficulty: DifficultyTier,
        mistakes: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 125
            DifficultyTier.EXPLORER -> 185
            DifficultyTier.ELDER -> 250
        }
        return (base - mistakes * 12 - hintsUsed * 15).coerceAtLeast(65)
    }
}
