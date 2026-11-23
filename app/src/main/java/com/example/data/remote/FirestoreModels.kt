package com.example.data.remote

import com.example.data.local.CreatureProgressEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleStatsEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val explorerName: String = "Zuri Vance",
    val displayName: String? = null,
    val email: String? = null,
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
    val introCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): PlayerProfileEntity = PlayerProfileEntity(
        id = 1,
        explorerName = explorerName,
        selectedHeroId = selectedHeroId,
        heroLevel = heroLevel,
        heroExp = heroExp,
        equippedRelicId = equippedRelicId,
        unlockedRelicsCsv = unlockedRelicsCsv,
        completedQuestsCsv = completedQuestsCsv,
        discoveredLoreCsv = discoveredLoreCsv,
        nectar = nectar,
        moonstones = moonstones,
        memoryXp = memoryXp,
        logicXp = logicXp,
        spatialXp = spatialXp,
        deductionXp = deductionXp,
        observationXp = observationXp,
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        totalPuzzlesSolved = totalPuzzlesSolved,
        gentleMode = gentleMode,
        mana = mana,
        magicEssence = magicEssence,
        communityProsperity = communityProsperity,
        completedMissionsCsv = completedMissionsCsv,
        lootBagCsv = lootBagCsv,
        canopyRestored = canopyRestored,
        dailyStreak = dailyStreak,
        longestDailyStreak = longestDailyStreak,
        lastActivityEpochDay = lastActivityEpochDay,
        introCompleted = introCompleted
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun newProfileCreateMap(
            userId: String,
            displayName: String? = null,
            email: String? = null
        ): Map<String, Any> {
            val defaults = PlayerProfileEntity()
            return mapOf(
                "userId" to userId,
                "explorerName" to defaults.explorerName,
                "displayName" to displayName?.takeIf { it.isNotBlank() }?.take(100),
                "email" to email?.takeIf { it.isNotBlank() }?.take(150),
                "selectedHeroId" to defaults.selectedHeroId,
                "heroLevel" to defaults.heroLevel,
                "heroExp" to defaults.heroExp,
                "equippedRelicId" to defaults.equippedRelicId,
                "unlockedRelicsCsv" to defaults.unlockedRelicsCsv,
                "completedQuestsCsv" to defaults.completedQuestsCsv,
                "discoveredLoreCsv" to defaults.discoveredLoreCsv,
                "nectar" to defaults.nectar,
                "moonstones" to defaults.moonstones,
                "memoryXp" to defaults.memoryXp,
                "logicXp" to defaults.logicXp,
                "spatialXp" to defaults.spatialXp,
                "deductionXp" to defaults.deductionXp,
                "observationXp" to defaults.observationXp,
                "currentStreak" to defaults.currentStreak,
                "bestStreak" to defaults.bestStreak,
                "totalPuzzlesSolved" to defaults.totalPuzzlesSolved,
                "gentleMode" to defaults.gentleMode,
                "mana" to defaults.mana,
                "magicEssence" to defaults.magicEssence,
                "communityProsperity" to defaults.communityProsperity,
                "completedMissionsCsv" to defaults.completedMissionsCsv,
                "lootBagCsv" to defaults.lootBagCsv,
                "canopyRestored" to defaults.canopyRestored,
                "dailyStreak" to defaults.dailyStreak,
                "longestDailyStreak" to defaults.longestDailyStreak,
                "lastActivityEpochDay" to defaults.lastActivityEpochDay,
                "introCompleted" to defaults.introCompleted,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
        }
    }
}

@IgnoreExtraProperties
data class FirestoreCreatureProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatureId: String = "",
    val discovered: Boolean = false,
    val bondLevel: Int = 0,
    val bondXp: Int = 0,
    val treatsFed: Int = 0,
    val lastInteractionMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): CreatureProgressEntity = CreatureProgressEntity(
        creatureId = creatureId.ifBlank { id },
        discovered = discovered,
        bondLevel = bondLevel,
        bondXp = bondXp,
        treatsFed = treatsFed,
        lastInteractionMillis = lastInteractionMillis
    )

    companion object {
        fun newCreateMap(
            userId: String,
            creatureId: String,
            discovered: Boolean,
            bondLevel: Int,
            bondXp: Int,
            treatsFed: Int = 0,
            lastInteractionMillis: Long = 0L
        ): Map<String, Any> = mapOf(
            "userId" to userId,
            "creatureId" to creatureId,
            "discovered" to discovered,
            "bondLevel" to bondLevel,
            "bondXp" to bondXp,
            "treatsFed" to treatsFed,
            "lastInteractionMillis" to lastInteractionMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class FirestorePuzzleStats(
    @DocumentId val id: String = "",
    val userId: String = "",
    val puzzleType: String = "",
    val accessed: Boolean = false,
    val currentLevel: Int = 1,
    val totalStars: Int = 0,
    val playedCount: Int = 0,
    val solvedCount: Int = 0,
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val lastSolvedMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): PuzzleStatsEntity = PuzzleStatsEntity(
        puzzleType = puzzleType.ifBlank { id },
        accessed = accessed,
        currentLevel = currentLevel,
        totalStars = totalStars,
        playedCount = playedCount,
        solvedCount = solvedCount,
        bestScore = bestScore,
        bestStreak = bestStreak,
        lastSolvedMillis = lastSolvedMillis
    )

    companion object {
        fun newCreateMap(
            userId: String,
            puzzleType: String,
            accessed: Boolean = false,
            currentLevel: Int = 1,
            totalStars: Int = 0,
            playedCount: Int = 0,
            solvedCount: Int = 0,
            bestScore: Int = 0,
            bestStreak: Int = 0,
            lastSolvedMillis: Long = 0L
        ): Map<String, Any> = mapOf(
            "userId" to userId,
            "puzzleType" to puzzleType,
            "accessed" to accessed,
            "currentLevel" to currentLevel,
            "totalStars" to totalStars,
            "playedCount" to playedCount,
            "solvedCount" to solvedCount,
            "bestScore" to bestScore,
            "bestStreak" to bestStreak,
            "lastSolvedMillis" to lastSolvedMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
