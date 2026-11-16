package com.example.rpg

import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.data.local.PlayerProfileEntity
import com.example.model.LootItemType
import com.example.model.PuzzleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RpgBattleEngineTest {

    @Test
    fun `solar strike recovers mana and arcane spell spends mana for heavy damage`() {
        val mission = RpgMissionCatalog.communityMissions.first()
        val profile = PlayerProfileEntity(heroLevel = 3, mana = 30, magicEssence = 40)
        val prep = RpgBattleEngine.createBattleState(
            mission = mission,
            profile = profile,
            lootAvailable = mapOf(LootItemType.SUNBERRY_TONIC to 2),
            random = Random(12)
        )
        assertEquals(BattlePhase.COMMUNITY_PREP, prep.phase)

        val combat = RpgBattleEngine.beginWildEncounter(prep)
        assertEquals(BattlePhase.ACTIVE_COMBAT, combat.phase)

        val afterStrike = RpgBattleEngine.performAction(combat, BattleActionType.SOLAR_STRIKE, Random(12))
        assertTrue(afterStrike.enemyHp < combat.enemyHp)
        assertEquals(30 + RpgBattleEngine.STRIKE_MANA_GAIN, afterStrike.heroMana)

        val manaBeforeSpell = afterStrike.heroMana
        val enemyBeforeSpell = afterStrike.enemyHp
        val afterSpell = RpgBattleEngine.performAction(afterStrike, BattleActionType.ARCANE_SPELL, Random(12))
        assertEquals(manaBeforeSpell - RpgBattleEngine.ARCANE_SPELL_COST, afterSpell.heroMana)
        assertTrue(afterSpell.enemyHp < enemyBeforeSpell)
    }

    @Test
    fun `loot items heal hp restore mana and deal burst damage`() {
        val mission = RpgMissionCatalog.communityMissions[1]
        val profile = PlayerProfileEntity(heroLevel = 2, mana = 10)
        val combat = RpgBattleEngine.beginWildEncounter(
            RpgBattleEngine.createBattleState(
                mission = mission,
                profile = profile,
                lootAvailable = mapOf(
                    LootItemType.SUNBERRY_TONIC to 1,
                    LootItemType.MANA_VIAL to 1,
                    LootItemType.SUNFIRE_CHARM to 1
                ),
                random = Random(7)
            )
        ).copy(heroHp = 40)

        val afterTonic = RpgBattleEngine.useLootItem(combat, LootItemType.SUNBERRY_TONIC)
        assertEquals(78, afterTonic.heroHp)
        assertFalse(afterTonic.lootAvailable.containsKey(LootItemType.SUNBERRY_TONIC))

        val afterVial = RpgBattleEngine.useLootItem(afterTonic, LootItemType.MANA_VIAL)
        assertEquals(42, afterVial.heroMana)

        val afterCharm = RpgBattleEngine.useLootItem(afterVial, LootItemType.SUNFIRE_CHARM)
        assertEquals(afterVial.enemyHp - 34, afterCharm.enemyHp)
    }

    @Test
    fun `weakened beautiful creature can be harmonized via soothe spirit`() {
        val mission = RpgMissionCatalog.communityMissions[1] // Prism-Winged Stag
        val profile = PlayerProfileEntity(heroLevel = 3, mana = 35)
        val combat = RpgBattleEngine.beginWildEncounter(
            RpgBattleEngine.createBattleState(
                mission = mission,
                profile = profile,
                lootAvailable = emptyMap(),
                random = Random(5)
            )
        ).copy(enemyHp = 20)

        assertTrue(combat.canSootheSpirit)
        val harmonized = RpgBattleEngine.performAction(combat, BattleActionType.SOOTHE_SPIRIT, Random(5))
        assertEquals(BattlePhase.VICTORY, harmonized.phase)
        assertTrue(harmonized.calmedSpirit)
        assertEquals(0, harmonized.enemyHp)
    }

    @Test
    fun `every puzzle type can be accessed during rpg missions and story chapters`() {
        val missionPuzzles = (RpgMissionCatalog.communityMissions + RpgMissionCatalog.wildPatrols)
            .mapNotNull { it.puzzleChallenge }
            .toSet()
        val chapterPuzzles = JungleLoreCatalog.biomes.flatMap { biome ->
            listOfNotNull(biome.signaturePuzzle) + biome.storyChoices.mapNotNull { it.opensPuzzle }
        }.toSet()

        PuzzleType.entries.forEach { type ->
            assertTrue("Puzzle ${type.id} missing from RPG missions", type in missionPuzzles)
            assertTrue("Puzzle ${type.id} missing from RPG story chapters", type in chapterPuzzles)
        }
    }
}
