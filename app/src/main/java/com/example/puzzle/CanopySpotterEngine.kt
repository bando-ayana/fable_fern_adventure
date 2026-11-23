package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.random.Random

enum class CharmBase(val label: String) {
    BLOSSOM("Blossom"),
    MUSHROOM("Mushroom"),
    FERN("Fern"),
    LANTERN("Lantern")
}

enum class SpotterMode {
    FIND_TWINS,
    ODD_ONE_OUT
}

data class CanopyCharm(
    val id: Int,
    val base: CharmBase,
    val detailCount: Int, // 2..5 petals/spots/fronds
    val tintIndex: Int,   // 0..3 palette colors
    val tilted: Boolean = false
) {
    fun visuallyMatches(other: CanopyCharm): Boolean =
        base == other.base &&
            detailCount == other.detailCount &&
            tintIndex == other.tintIndex &&
            tilted == other.tilted
}

data class SpotterRound(
    val roundNumber: Int,
    val totalRounds: Int,
    val mode: SpotterMode,
    val prompt: String,
    val charms: List<CanopyCharm>,
    val targetIds: Set<Int>
)

data class CanopySpotterPuzzle(
    val rounds: List<SpotterRound>,
    val currentRoundIdx: Int = 0,
    val selectedIds: Set<Int> = emptySet(),
    val hintedId: Int? = null,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val difficulty: DifficultyTier
) {
    val currentRound: SpotterRound? get() = rounds.getOrNull(currentRoundIdx)
    val isCompleted: Boolean get() = currentRoundIdx >= rounds.size
}

object CanopySpotterEngine {

    fun generate(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): CanopySpotterPuzzle {
        val totalRounds = 3
        val rounds = (0 until totalRounds).map { idx ->
            val mode = if (idx % 2 == 0) SpotterMode.FIND_TWINS else SpotterMode.ODD_ONE_OUT
            buildRound(
                roundNumber = idx + 1,
                totalRounds = totalRounds,
                mode = mode,
                difficulty = difficulty,
                level = level,
                random = random
            )
        }
        return CanopySpotterPuzzle(
            rounds = rounds,
            difficulty = difficulty
        )
    }

    private fun buildRound(
        roundNumber: Int,
        totalRounds: Int,
        mode: SpotterMode,
        difficulty: DifficultyTier,
        level: Int,
        random: Random
    ): SpotterRound {
        val baseCount = when {
            difficulty == DifficultyTier.SPROUT && level == 1 -> 4
            difficulty == DifficultyTier.SPROUT -> 6
            difficulty == DifficultyTier.EXPLORER -> 6
            else -> 9
        }

        return if (mode == SpotterMode.ODD_ONE_OUT) {
            val sharedBase = CharmBase.entries[random.nextInt(CharmBase.entries.size)]
            val commonDetails = 2 + random.nextInt(3)
            val oddDetails = if (commonDetails < 5) commonDetails + 1 else commonDetails - 1
            val commonTint = random.nextInt(4)
            val oddTint = if (difficulty == DifficultyTier.SPROUT) (commonTint + 1) % 4 else commonTint
            val oddTilted = difficulty == DifficultyTier.ELDER

            val oddIndex = random.nextInt(baseCount)
            val charms = (0 until baseCount).map { idx ->
                if (idx == oddIndex) {
                    CanopyCharm(
                        id = idx,
                        base = sharedBase,
                        detailCount = oddDetails,
                        tintIndex = oddTint,
                        tilted = oddTilted
                    )
                } else {
                    CanopyCharm(
                        id = idx,
                        base = sharedBase,
                        detailCount = commonDetails,
                        tintIndex = commonTint,
                        tilted = false
                    )
                }
            }
            SpotterRound(
                roundNumber = roundNumber,
                totalRounds = totalRounds,
                mode = mode,
                prompt = "Spot the 1 unique ${sharedBase.label.lowercase()} charm!",
                charms = charms,
                targetIds = setOf(oddIndex)
            )
        } else {
            val twinCount = if (baseCount <= 4) 6 else baseCount
            val focusBase = CharmBase.entries[random.nextInt(CharmBase.entries.size)]
            val twinDetails = 2 + random.nextInt(3)
            val twinTint = random.nextInt(4)

            val twinTemplate = CanopyCharm(
                id = 0,
                base = focusBase,
                detailCount = twinDetails,
                tintIndex = twinTint,
                tilted = false
            )

            // Build unique distractors that never match the twin or each other
            val allPossible = buildList {
                for (b in listOf(focusBase, CharmBase.entries[(focusBase.ordinal + 1) % CharmBase.entries.size])) {
                    for (d in 2..5) {
                        for (t in 0..3) {
                            val c = CanopyCharm(0, b, d, t, tilted = difficulty == DifficultyTier.ELDER && d % 2 == 0)
                            if (!c.visuallyMatches(twinTemplate)) add(c)
                        }
                    }
                }
            }.distinctBy { listOf(it.base, it.detailCount, it.tintIndex, it.tilted) }.shuffled(random)

            val rawList = mutableListOf(twinTemplate, twinTemplate)
            rawList.addAll(allPossible.take(twinCount - 2))
            val shuffled = rawList.shuffled(random).mapIndexed { idx, charm ->
                charm.copy(id = idx)
            }
            val twinIds = shuffled.filter { it.visuallyMatches(twinTemplate) }.map { it.id }.toSet()

            SpotterRound(
                roundNumber = roundNumber,
                totalRounds = totalRounds,
                mode = mode,
                prompt = "Tap the 2 identical twin charms!",
                charms = shuffled,
                targetIds = twinIds
            )
        }
    }

    fun tapCharm(puzzle: CanopySpotterPuzzle, charmId: Int): Pair<CanopySpotterPuzzle, Boolean> {
        val round = puzzle.currentRound ?: return puzzle to false
        if (charmId in round.targetIds) {
            val updatedSelected = puzzle.selectedIds + charmId
            return if (updatedSelected.containsAll(round.targetIds)) {
                puzzle.copy(
                    currentRoundIdx = puzzle.currentRoundIdx + 1,
                    selectedIds = emptySet(),
                    hintedId = null
                ) to true
            } else {
                puzzle.copy(selectedIds = updatedSelected) to true
            }
        } else {
            return puzzle.copy(mistakes = puzzle.mistakes + 1) to false
        }
    }

    fun applyHint(puzzle: CanopySpotterPuzzle): CanopySpotterPuzzle {
        val round = puzzle.currentRound ?: return puzzle
        val unfound = round.targetIds.firstOrNull { it !in puzzle.selectedIds } ?: return puzzle
        return puzzle.copy(hintedId = unfound, hintsUsed = puzzle.hintsUsed + 1)
    }

    fun computeScore(
        difficulty: DifficultyTier,
        mistakes: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 120
            DifficultyTier.EXPLORER -> 180
            DifficultyTier.ELDER -> 245
        }
        return (base - mistakes * 12 - hintsUsed * 15).coerceAtLeast(65)
    }
}
