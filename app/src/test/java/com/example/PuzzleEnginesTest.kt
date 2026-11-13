package com.example

import com.example.data.JungleLoreCatalog
import com.example.model.DifficultyTier
import com.example.puzzle.CanopySpotterEngine
import com.example.puzzle.FernMosaicEngine
import com.example.puzzle.FireflyEchoEngine
import com.example.puzzle.GroveGridEngine
import com.example.puzzle.MushroomMelodyEngine
import com.example.puzzle.SpiritLockEngine
import com.example.puzzle.SpiritRune
import com.example.puzzle.VineWeaverEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleEnginesTest {

    @Test
    fun `grove grid generated solutions are valid 4x4 latin squares across difficulties`() {
        DifficultyTier.entries.forEach { tier ->
            val puzzle = GroveGridEngine.generate(tier, level = 2, random = Random(42))
            assertTrue("Solution for $tier should be valid", GroveGridEngine.isSolved(puzzle.solution))
            assertFalse("Initial puzzle should have blanks", GroveGridEngine.isSolved(puzzle.board))
        }
    }

    @Test
    fun `vine weaver generated boards solve cleanly when rotated to solvedRotation`() {
        DifficultyTier.entries.forEach { tier ->
            val initial = VineWeaverEngine.generate(tier, level = 1, random = Random(7))
            assertFalse("Initial scrambled board should not already be solved", initial.moonflowerBloomed)

            val solvedTiles = initial.tiles.map { it.copy(rotation = it.solvedRotation) }
            val evaluated = VineWeaverEngine.evaluateFlow(initial.copy(tiles = solvedTiles))
            assertTrue("Board with solved rotations must bloom Moonflower for $tier", evaluated.moonflowerBloomed)
        }
    }

    @Test
    fun `spirit lock evaluates exact and misplaced runes accurately`() {
        val secret = listOf(SpiritRune.SUN, SpiritRune.MOON, SpiritRune.FERN, SpiritRune.SUN)
        val guess = listOf(SpiritRune.SUN, SpiritRune.FERN, SpiritRune.DEW, SpiritRune.ORCHID)
        val record = SpiritLockEngine.evaluateGuess(secret, guess)
        assertEquals(1, record.exactMatches)
        assertEquals(1, record.misplacedMatches)
    }

    @Test
    fun `firefly echo creates progressive rounds scaled by level and difficulty`() {
        val sproutLv1 = FireflyEchoEngine.createRounds(DifficultyTier.SPROUT, level = 1, random = Random(11))
        val elderLv3 = FireflyEchoEngine.createRounds(DifficultyTier.ELDER, level = 3, random = Random(11))

        assertEquals(3, sproutLv1.size)
        assertEquals(2, sproutLv1.first().sequence.size)
        assertTrue(elderLv3.last().sequence.size > sproutLv1.last().sequence.size)
    }

    @Test
    fun `mushroom melody clues are uniquely satisfied and hints solve stepwise`() {
        DifficultyTier.entries.forEach { tier ->
            var puzzle = MushroomMelodyEngine.generate(tier, level = 2, random = Random(19))
            assertFalse("Initial mushroom order must not already satisfy all clues", puzzle.isSolved)
            assertTrue(puzzle.clues.all { it.isSatisfied(puzzle.solution) })

            repeat(puzzle.solution.size) {
                if (!puzzle.isSolved) {
                    puzzle = MushroomMelodyEngine.applyHint(puzzle)
                }
            }
            assertTrue("Repeated hints should guide Mushroom Melody to solved state", puzzle.isSolved)
        }
    }

    @Test
    fun `fern mosaic completes across 3 rounds via correct choices and hints`() {
        DifficultyTier.entries.forEach { tier ->
            var puzzle = FernMosaicEngine.generate(tier, level = 2, random = Random(23))
            assertEquals(3, puzzle.rounds.size)
            assertFalse(puzzle.isCompleted)

            var steps = 0
            while (!puzzle.isCompleted && steps < 10) {
                puzzle = FernMosaicEngine.applyHint(puzzle)
                steps++
            }
            assertTrue("Hints should solve all 3 rounds of Fern Mosaic for $tier", puzzle.isCompleted)
        }
    }

    @Test
    fun `canopy spotter finds twins and odd one out across 3 rounds`() {
        DifficultyTier.entries.forEach { tier ->
            var puzzle = CanopySpotterEngine.generate(tier, level = 2, random = Random(31))
            assertEquals(3, puzzle.rounds.size)

            while (!puzzle.isCompleted) {
                val round = puzzle.currentRound!!
                assertTrue(round.targetIds.isNotEmpty())
                val nextTarget = round.targetIds.first { it !in puzzle.selectedIds }
                val (updated, matched) = CanopySpotterEngine.tapCharm(puzzle, nextTarget)
                assertTrue("Target charm tap should register as a match", matched)
                puzzle = updated
            }
            assertTrue("Tapping all round targets completes Canopy Spotter", puzzle.isCompleted)
        }
    }

    @Test
    fun `quest log lore unlocks progressively with starter, puzzles, chapters, and nectar`() {
        val starterCount = JungleLoreCatalog.loreEntries.count { entry ->
            JungleLoreCatalog.isLoreDiscovered(
                entry = entry,
                nectar = 15,
                totalPuzzlesSolved = 0,
                completedQuests = emptySet(),
                discoveredCreatures = setOf("lumafern"),
                savedLoreIds = emptySet()
            )
        }
        assertEquals(2, starterCount)

        val forgeLore = JungleLoreCatalog.loreEntries.first { it.id == "solari_forge_craft" }
        assertFalse(
            JungleLoreCatalog.isLoreDiscovered(
                entry = forgeLore,
                nectar = 15,
                totalPuzzlesSolved = 0,
                completedQuests = emptySet(),
                discoveredCreatures = setOf("lumafern"),
                savedLoreIds = emptySet()
            )
        )
        assertTrue(
            JungleLoreCatalog.isLoreDiscovered(
                entry = forgeLore,
                nectar = 15,
                totalPuzzlesSolved = 1,
                completedQuests = emptySet(),
                discoveredCreatures = setOf("lumafern"),
                savedLoreIds = emptySet()
            )
        )

        val allUnlockedCount = JungleLoreCatalog.loreEntries.count { entry ->
            JungleLoreCatalog.isLoreDiscovered(
                entry = entry,
                nectar = 220,
                totalPuzzlesSolved = 6,
                completedQuests = setOf("dew_lagoon", "crystal_cavern", "heartwood_shrine"),
                discoveredCreatures = setOf("lumafern", "aureowl"),
                savedLoreIds = emptySet()
            )
        }
        assertEquals(JungleLoreCatalog.loreEntries.size, allUnlockedCount)
    }
}
