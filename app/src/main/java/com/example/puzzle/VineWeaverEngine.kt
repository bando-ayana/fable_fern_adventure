package com.example.puzzle

import com.example.model.DifficultyTier
import kotlin.random.Random

enum class PipeDir(val dr: Int, val dc: Int) {
    NORTH(-1, 0),
    EAST(0, 1),
    SOUTH(1, 0),
    WEST(0, -1);

    fun rotateCW(steps: Int = 1): PipeDir {
        val idx = (ordinal + steps).mod(4)
        return entries[idx]
    }

    fun opposite(): PipeDir = rotateCW(2)
}

enum class VineShape(val baseConnections: Set<PipeDir>) {
    STRAIGHT(setOf(PipeDir.NORTH, PipeDir.SOUTH)),
    CORNER(setOf(PipeDir.NORTH, PipeDir.EAST)),
    TEE(setOf(PipeDir.NORTH, PipeDir.EAST, PipeDir.WEST));

    fun connectionsAt(rotation: Int): Set<PipeDir> =
        baseConnections.map { it.rotateCW(rotation) }.toSet()
}

data class VineTile(
    val row: Int,
    val col: Int,
    val shape: VineShape,
    val rotation: Int, // 0..3
    val solvedRotation: Int,
    val isPowered: Boolean = false
) {
    val activeConnections: Set<PipeDir>
        get() = shape.connectionsAt(rotation)
}

data class VineWeaverBoard(
    val size: Int,
    val tiles: List<VineTile>,
    val moonflowerBloomed: Boolean,
    val difficulty: DifficultyTier
)

object VineWeaverEngine {

    fun generate(
        difficulty: DifficultyTier,
        level: Int = 1,
        random: Random = Random.Default
    ): VineWeaverBoard {
        val size = when {
            difficulty == DifficultyTier.SPROUT && level == 1 -> 3
            difficulty == DifficultyTier.ELDER || (difficulty == DifficultyTier.EXPLORER && level >= 4) -> 5
            else -> 4
        }
        val path = buildRandomPath(size, random)
        val pathIndex = path.withIndex().associate { (i, cell) -> cell to i }

        val rawTiles = MutableList(size * size) { idx ->
            val r = idx / size
            val c = idx % size
            val pos = r to c
            val pIdx = pathIndex[pos]
            if (pIdx != null) {
                val prevDir = if (pIdx == 0) {
                    PipeDir.WEST // Spring Source feeds into (0,0) from the West
                } else {
                    directionBetween(pos, path[pIdx - 1])
                }
                val nextDir = if (pIdx == path.lastIndex) {
                    PipeDir.EAST // Moonflower drinks from (size-1, size-1) on the East
                } else {
                    directionBetween(pos, path[pIdx + 1])
                }
                val needed = setOf(prevDir, nextDir)
                val (shape, solvedRot) = matchShapeForDirections(needed, difficulty, random)
                val scrambledRot = (solvedRot + random.nextInt(1, 4)) % 4
                VineTile(
                    row = r,
                    col = c,
                    shape = shape,
                    rotation = scrambledRot,
                    solvedRotation = solvedRot
                )
            } else {
                val shape = when (random.nextInt(10)) {
                    in 0..4 -> VineShape.CORNER
                    in 5..7 -> VineShape.STRAIGHT
                    else -> VineShape.TEE
                }
                val rot = random.nextInt(4)
                VineTile(
                    row = r,
                    col = c,
                    shape = shape,
                    rotation = rot,
                    solvedRotation = rot
                )
            }
        }

        var board = evaluateFlow(VineWeaverBoard(size, rawTiles, false, difficulty))
        // Guarantee board doesn't start already solved
        if (board.moonflowerBloomed) {
            val rotated = board.tiles.mapIndexed { idx, tile ->
                if (idx == 0) tile.copy(rotation = (tile.rotation + 1) % 4) else tile
            }
            board = evaluateFlow(board.copy(tiles = rotated))
        }
        return board
    }

    fun rotateTile(board: VineWeaverBoard, row: Int, col: Int): VineWeaverBoard {
        val updated = board.tiles.map { tile ->
            if (tile.row == row && tile.col == col) {
                tile.copy(rotation = (tile.rotation + 1) % 4)
            } else {
                tile
            }
        }
        return evaluateFlow(board.copy(tiles = updated))
    }

    fun applyHint(board: VineWeaverBoard): VineWeaverBoard {
        // Snap the first unaligned path tile to its solved rotation
        val target = board.tiles.firstOrNull { it.rotation != it.solvedRotation } ?: return board
        val updated = board.tiles.map { tile ->
            if (tile.row == target.row && tile.col == target.col) {
                tile.copy(rotation = tile.solvedRotation)
            } else {
                tile
            }
        }
        return evaluateFlow(board.copy(tiles = updated))
    }

    fun evaluateFlow(board: VineWeaverBoard): VineWeaverBoard {
        val n = board.size
        val powered = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()

        val startTile = board.tiles[0]
        if (PipeDir.WEST in startTile.activeConnections) {
            powered.add(0)
            queue.add(0)
        }

        while (queue.isNotEmpty()) {
            val currentIdx = queue.removeFirst()
            val current = board.tiles[currentIdx]
            for (dir in current.activeConnections) {
                val nr = current.row + dir.dr
                val nc = current.col + dir.dc
                if (nr in 0 until n && nc in 0 until n) {
                    val neighborIdx = nr * n + nc
                    if (neighborIdx !in powered) {
                        val neighbor = board.tiles[neighborIdx]
                        if (dir.opposite() in neighbor.activeConnections) {
                            powered.add(neighborIdx)
                            queue.add(neighborIdx)
                        }
                    }
                }
            }
        }

        val exitIdx = n * n - 1
        val bloomed = exitIdx in powered && PipeDir.EAST in board.tiles[exitIdx].activeConnections

        val refreshedTiles = board.tiles.mapIndexed { idx, tile ->
            tile.copy(isPowered = idx in powered)
        }
        return board.copy(tiles = refreshedTiles, moonflowerBloomed = bloomed)
    }

    private fun buildRandomPath(size: Int, random: Random): List<Pair<Int, Int>> {
        val target = (size - 1) to (size - 1)
        val visited = mutableSetOf(0 to 0)
        val path = mutableListOf(0 to 0)

        fun dfs(r: Int, c: Int): Boolean {
            if (r to c == target) return true
            val dirs = PipeDir.entries.shuffled(random)
            for (d in dirs) {
                val nr = r + d.dr
                val nc = c + d.dc
                if (nr in 0 until size && nc in 0 until size && (nr to nc) !in visited) {
                    visited.add(nr to nc)
                    path.add(nr to nc)
                    if (dfs(nr, nc)) return true
                    path.removeAt(path.lastIndex)
                }
            }
            return false
        }

        dfs(0, 0)
        return path
    }

    private fun directionBetween(from: Pair<Int, Int>, to: Pair<Int, Int>): PipeDir {
        val dr = to.first - from.first
        val dc = to.second - from.second
        return PipeDir.entries.first { it.dr == dr && it.dc == dc }
    }

    private fun matchShapeForDirections(
        needed: Set<PipeDir>,
        difficulty: DifficultyTier,
        random: Random
    ): Pair<VineShape, Int> {
        val dirs = needed.toList()
        val isOpposite = dirs.size == 2 && dirs[0].opposite() == dirs[1]
        val preferTee = difficulty != DifficultyTier.SPROUT && random.nextInt(5) == 0

        if (!preferTee) {
            val baseShape = if (isOpposite) VineShape.STRAIGHT else VineShape.CORNER
            for (rot in 0..3) {
                if (needed.all { it in baseShape.connectionsAt(rot) }) {
                    return baseShape to rot
                }
            }
        }

        for (rot in 0..3) {
            if (needed.all { it in VineShape.TEE.connectionsAt(rot) }) {
                return VineShape.TEE to rot
            }
        }
        return VineShape.CORNER to 0
    }

    fun computeScore(
        difficulty: DifficultyTier,
        taps: Int,
        hintsUsed: Int
    ): Int {
        val base = when (difficulty) {
            DifficultyTier.SPROUT -> 130
            DifficultyTier.EXPLORER -> 190
            DifficultyTier.ELDER -> 260
        }
        val extraTaps = (taps - 12).coerceAtLeast(0)
        return (base - extraTaps * 2 - hintsUsed * 15).coerceAtLeast(65)
    }
}
