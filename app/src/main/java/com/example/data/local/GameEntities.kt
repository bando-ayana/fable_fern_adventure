package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val explorerName: String = "Zuri Vance",
    val selectedHeroId: String = "zuri",
    val heroLevel: Int = 1,
    val heroExp: Int = 0,
    val equippedRelicId: String = "solar_visor",
    val unlockedRelicsCsv: String = "solar_visor",
    val completedQuestsCsv: String = "",
    val discoveredLoreCsv: String = "solari_spires,moss_hollow_chronicle",
    val nectar: Int = 15,
    val moonstones: Int = 2,
    val memoryXp: Int = 0,
    val logicXp: Int = 0,
    val spatialXp: Int = 0,
    val deductionXp: Int = 0,
    val observationXp: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalPuzzlesSolved: Int = 0,
    val gentleMode: Boolean = true,
    val mana: Int = 40,
    val magicEssence: Int = 0,
    val communityProsperity: Int = 0,
    val completedMissionsCsv: String = "",
    val lootBagCsv: String = "sunberry_tonic:2,mana_vial:1",
    val canopyRestored: Boolean = false,
    val dailyStreak: Int = 0,
    val longestDailyStreak: Int = 0,
    val lastActivityEpochDay: Long = -1L,
) {
    val totalXp: Int
        get() = memoryXp + logicXp + spatialXp + deductionXp + observationXp

    val expForNextLevel: Int
        get() = heroLevel * 100

    val maxMana: Int
        get() = 40 + (heroLevel - 1) * 8 + (communityProsperity / 20) * 6 + (magicEssence / 25) * 4

    val arcanePowerBonus: Int
        get() = magicEssence / 20

    val unlockedRelicIds: Set<String>
        get() = unlockedRelicsCsv.split(",").filter { it.isNotBlank() }.toSet()

    val completedQuestIds: Set<String>
        get() = completedQuestsCsv.split(",").filter { it.isNotBlank() }.toSet()

    val completedMissionIds: Set<String>
        get() = completedMissionsCsv.split(",").filter { it.isNotBlank() }.toSet()

    val discoveredLoreIds: Set<String>
        get() = discoveredLoreCsv.split(",").filter { it.isNotBlank() }.toSet()

    val explorerRank: String
        get() = when {
            canopyRestored -> "Crowned Canopy Champion"
            heroLevel >= 8 || totalXp >= 700 -> "Mythic Canopy Guardian"
            heroLevel >= 5 || totalXp >= 420 -> "Solari Magitech Sage"
            heroLevel >= 3 || totalXp >= 200 -> "Dewdrop Pathfinder"
            heroLevel >= 2 || totalXp >= 70 -> "Fern Whisperer"
            else -> "Sprout Seeker"
        }
}

@Entity(tableName = "creature_progress")
data class CreatureProgressEntity(
    @PrimaryKey val creatureId: String,
    val discovered: Boolean = false,
    val bondLevel: Int = 0,
    val bondXp: Int = 0,
    val treatsFed: Int = 0,
    val lastInteractionMillis: Long = 0L
)

@Entity(tableName = "puzzle_stats")
data class PuzzleStatsEntity(
    @PrimaryKey val puzzleType: String,
    val accessed: Boolean = false,
    val currentLevel: Int = 1,
    val totalStars: Int = 0,
    val playedCount: Int = 0,
    val solvedCount: Int = 0,
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val lastSolvedMillis: Long = 0L
)
