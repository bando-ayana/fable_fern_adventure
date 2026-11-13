package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.random.Random

enum class GroveRune(val id: Int, val label: String) {
    SUN(1, "Sun"),
    MOON(2, "Moon"),
    FERN(3, "Fern"),
    DEWDROP(4, "Dew");

    companion object {
        fun fromId(id: Int): GroveRune? = entries.firstOrNull { it.id == id }
    }
}

data class GroveGridPuzzle(
    val solution: List<Int>,      // 16 values in 1..4
    val givenMask: List<Boolean>, // 16 booleans; true = prefilled clue
    val board: List<Int>,         // 16 values in 0..4 (0 = empty)
    val difficulty: DifficultyTier
)

object GroveGridEngine {

    private val baseGrid = listOf(
        1, 2, 3, 4,
        3, 4, 1, 2,
        2, 1, 4, 3,
        4, 3, 2, 1
    )

    fun generate(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): GroveGridPuzzle {
        val symbolPerm = listOf(1, 2, 3, 4).shuffled(random)
        val rowOrder = buildBandOrder(random)
        val colOrder = buildBandOrder(random)

        val solution = MutableList(16) { 0 }
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val srcR = rowOrder[r]
                val srcC = colOrder[c]
                val baseVal = baseGrid[srcR * 4 + srcC]
                solution[r * 4 + c] = symbolPerm[baseVal - 1]
            }
        }

        val levelExtra = (level - 1).coerceAtMost(2)
        val blankCount = when (difficulty) {
            DifficultyTier.SPROUT -> 4 + levelExtra
            DifficultyTier.EXPLORER -> 7 + levelExtra
            DifficultyTier.ELDER -> (9 + levelExtra).coerceAtMost(11)
        }

        val indicesToClear = (0 until 16).shuffled(random).take(blankCount).toSet()
        val givenMask = (0 until 16).map { it !in indicesToClear }
        val board = (0 until 16).map { idx ->
            if (givenMask[idx]) solution[idx] else 0
        }

        return GroveGridPuzzle(
            solution = solution,
            givenMask = givenMask,
            board = board,
            difficulty = difficulty
        )
    }

    private fun buildBandOrder(random: Random): List<Int> {
        val band0 = if (random.nextBoolean()) listOf(0, 1) else listOf(1, 0)
        val band1 = if (random.nextBoolean()) listOf(2, 3) else listOf(3, 2)
        return if (random.nextBoolean()) band0 + band1 else band1 + band0
    }

    fun conflictingIndices(board: List<Int>): Set<Int> {
        val conflicts = mutableSetOf<Int>()
        // Rows
        for (r in 0 until 4) {
            markDuplicates(board, (0 until 4).map { c -> r * 4 + c }, conflicts)
        }
        // Columns
        for (c in 0 until 4) {
            markDuplicates(board, (0 until 4).map { r -> r * 4 + c }, conflicts)
        }
        // 2x2 sub-groves
        for (br in 0 until 2) {
            for (bc in 0 until 2) {
                val boxIndices = listOf(
                    (br * 2) * 4 + (bc * 2),
                    (br * 2) * 4 + (bc * 2 + 1),
                    (br * 2 + 1) * 4 + (bc * 2),
                    (br * 2 + 1) * 4 + (bc * 2 + 1)
                )
                markDuplicates(board, boxIndices, conflicts)
            }
        }
        return conflicts
    }

    private fun markDuplicates(
        board: List<Int>,
        indices: List<Int>,
        conflicts: MutableSet<Int>
    ) {
        val seen = mutableMapOf<Int, MutableList<Int>>()
        for (idx in indices) {
            val v = board[idx]
            if (v != 0) {
                seen.getOrPut(v) { mutableListOf() }.add(idx)
            }
        }
        seen.values.filter { it.size > 1 }.forEach { conflicts.addAll(it) }
    }

    fun isSolved(board: List<Int>): Boolean {
        if (board.size != 16 || board.any { it !in 1..4 }) return false
        return conflictingIndices(board).isEmpty()
    }

    fun computeScore(
        difficulty: DifficultyTier,
        moves: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 140
            DifficultyTier.EXPLORER -> 200
            DifficultyTier.ELDER -> 270
        }
        val extraMoves = (moves - 10).coerceAtLeast(0)
        return (base - extraMoves * 3 - hintsUsed * 15).coerceAtLeast(70)
    }
}
