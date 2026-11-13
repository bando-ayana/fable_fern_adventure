package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.math.abs
import kotlin.random.Random

enum class MushroomKind(val displayName: String, val spotCount: Int) {
    AMBER_CAP("Amber", 3),
    DEW_BELL("Dewbell", 2),
    CORAL_PUFF("Coral", 4),
    MOSS_SPIRE("Spire", 1),
    STAR_MOREL("Star", 5)
}

sealed interface MushroomClue {
    val text: String
    fun isSatisfied(order: List<MushroomKind>): Boolean

    data class FixedPosition(val kind: MushroomKind, val oneBasedSlot: Int) : MushroomClue {
        override val text: String = "${kind.displayName} glows in spot #$oneBasedSlot."
        override fun isSatisfied(order: List<MushroomKind>): Boolean =
            order.getOrNull(oneBasedSlot - 1) == kind
    }

    data class LeftOf(val left: MushroomKind, val right: MushroomKind) : MushroomClue {
        override val text: String = "${left.displayName} sits to the left of ${right.displayName}."
        override fun isSatisfied(order: List<MushroomKind>): Boolean {
            val lIdx = order.indexOf(left)
            val rIdx = order.indexOf(right)
            return lIdx >= 0 && rIdx >= 0 && lIdx < rIdx
        }
    }

    data class Adjacent(val first: MushroomKind, val second: MushroomKind) : MushroomClue {
        override val text: String = "${first.displayName} sits right beside ${second.displayName}."
        override fun isSatisfied(order: List<MushroomKind>): Boolean {
            val i1 = order.indexOf(first)
            val i2 = order.indexOf(second)
            return i1 >= 0 && i2 >= 0 && abs(i1 - i2) == 1
        }
    }

    data class NotOnEnds(val kind: MushroomKind) : MushroomClue {
        override val text: String = "${kind.displayName} rests in a middle spot (not on an end)."
        override fun isSatisfied(order: List<MushroomKind>): Boolean {
            val idx = order.indexOf(kind)
            return idx > 0 && idx < order.lastIndex
        }
    }
}

data class MushroomMelodyPuzzle(
    val solution: List<MushroomKind>,
    val currentOrder: List<MushroomKind>,
    val clues: List<MushroomClue>,
    val difficulty: DifficultyTier
) {
    val isSolved: Boolean
        get() = clues.all { it.isSatisfied(currentOrder) }
}

object MushroomMelodyEngine {

    fun generate(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): MushroomMelodyPuzzle {
        val count = when {
            difficulty == DifficultyTier.SPROUT && level == 1 -> 3
            difficulty == DifficultyTier.ELDER || (difficulty == DifficultyTier.EXPLORER && level >= 3) -> 5
            else -> 4
        }

        val chosenKinds = MushroomKind.entries.shuffled(random).take(count)
        val solution = chosenKinds.shuffled(random)
        val clues = buildUniqueClues(solution, difficulty, random)

        var startOrder = solution.shuffled(random)
        while (clues.all { it.isSatisfied(startOrder) }) {
            startOrder = solution.shuffled(random)
        }

        return MushroomMelodyPuzzle(
            solution = solution,
            currentOrder = startOrder,
            clues = clues,
            difficulty = difficulty
        )
    }

    fun swap(puzzle: MushroomMelodyPuzzle, idxA: Int, idxB: Int): MushroomMelodyPuzzle {
        if (idxA == idxB || idxA !in puzzle.currentOrder.indices || idxB !in puzzle.currentOrder.indices) {
            return puzzle
        }
        val mutable = puzzle.currentOrder.toMutableList()
        val temp = mutable[idxA]
        mutable[idxA] = mutable[idxB]
        mutable[idxB] = temp
        return puzzle.copy(currentOrder = mutable)
    }

    fun applyHint(puzzle: MushroomMelodyPuzzle): MushroomMelodyPuzzle {
        val wrongIdx = puzzle.currentOrder.indices.firstOrNull {
            puzzle.currentOrder[it] != puzzle.solution[it]
        } ?: return puzzle
        val targetKind = puzzle.solution[wrongIdx]
        val currentLoc = puzzle.currentOrder.indexOf(targetKind)
        return swap(puzzle, wrongIdx, currentLoc)
    }

    private fun buildUniqueClues(
        solution: List<MushroomKind>,
        difficulty: DifficultyTier,
        random: Random
    ): List<MushroomClue> {
        val allPerms = permutations(solution)
        val candidates = mutableListOf<MushroomClue>()

        // Relational clues first (more engaging than fixed positions)
        for (i in solution.indices) {
            if (i in 1 until solution.lastIndex) {
                candidates.add(MushroomClue.NotOnEnds(solution[i]))
            }
            if (i + 1 < solution.size) {
                candidates.add(MushroomClue.Adjacent(solution[i], solution[i + 1]))
            }
            for (j in i + 1 until solution.size) {
                candidates.add(MushroomClue.LeftOf(solution[i], solution[j]))
            }
        }
        candidates.shuffle(random)

        // Add fixed-position clues as fallback (or starter anchor on Sprout)
        val fixedClues = solution.mapIndexed { idx, kind ->
            MushroomClue.FixedPosition(kind, idx + 1)
        }.shuffled(random)

        val pool = if (difficulty == DifficultyTier.SPROUT) {
            (fixedClues.take(1) + candidates + fixedClues.drop(1)).distinct()
        } else {
            (candidates + fixedClues).distinct()
        }

        val selected = mutableListOf<MushroomClue>()
        var remaining = allPerms
        for (clue in pool) {
            if (remaining.size <= 1) break
            val narrowed = remaining.filter { clue.isSatisfied(it) }
            if (narrowed.size < remaining.size) {
                selected.add(clue)
                remaining = narrowed
            }
        }

        // Remove redundant clues if any earlier clue became redundant after later clues
        val minimized = selected.toMutableList()
        for (clue in selected) {
            if (minimized.size <= 2) break
            val without = minimized - clue
            if (allPerms.count { p -> without.all { it.isSatisfied(p) } } == 1) {
                minimized.remove(clue)
            }
        }
        return minimized
    }

    private fun <T> permutations(items: List<T>): List<List<T>> {
        if (items.size <= 1) return listOf(items)
        val result = mutableListOf<List<T>>()
        for (i in items.indices) {
            val rest = items.toMutableList().apply { removeAt(i) }
            for (sub in permutations(rest)) {
                result.add(listOf(items[i]) + sub)
            }
        }
        return result
    }

    fun computeScore(
        difficulty: DifficultyTier,
        swaps: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 130
            DifficultyTier.EXPLORER -> 195
            DifficultyTier.ELDER -> 265
        }
        val extraSwaps = (swaps - 5).coerceAtLeast(0)
        return (base - extraSwaps * 4 - hintsUsed * 15).coerceAtLeast(65)
    }
}
