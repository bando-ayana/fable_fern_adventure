package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.CreatureProgressEntity
import com.example.data.local.FernfableDatabase
import com.example.data.local.LocalPuzzleProgressEntity
import com.example.data.local.PendingMissionOutcomeEntity
import com.example.data.local.PendingPuzzleCompletionEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleStatsEntity
import com.example.data.remote.FirestoreCreatureProgress
import com.example.data.remote.FirestorePuzzleStats
import com.example.data.remote.FirestoreUserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.model.BrainSkill
import com.example.model.CommunityMission
import com.example.model.DifficultyTier
import com.example.model.LootItemType
import com.example.model.MissionCompletionSummary
import com.example.model.PuzzleCompletionResult
import com.example.model.PuzzleType
import com.example.rpg.DailyStreakEvaluation
import com.example.rpg.DailyStreakTracker
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class GameRepository(
    private val db: FirebaseFirestore,
    private val localDatabase: FernfableDatabase,
    private val context: Context
) {
    var requestManualRetry: (suspend (operation: String, error: String) -> Boolean)? = null

    suspend fun recordPuzzleSolvedLocally(
        userId: String,
        puzzleType: PuzzleType,
        difficulty: DifficultyTier,
        levelPlayed: Int,
        starsEarned: Int,
        rawScore: Int,
        cloudStats: PuzzleStatsEntity?
    ): PuzzleCompletionResult {
        val now = System.currentTimeMillis()

        val updated = localDatabase.withTransaction {
            val dao = localDatabase.gameDao()
            val previous = dao.getLocalPuzzleProgress(userId, puzzleType.id)

            val priorBestScore = maxOf(
                previous?.bestScore ?: 0,
                cloudStats?.bestScore ?: 0
            )
            val nextLevel = maxOf(
                previous?.currentLevel ?: 1,
                cloudStats?.currentLevel ?: 1,
                levelPlayed + 1
            )

            val progress = LocalPuzzleProgressEntity(
                userId = userId,
                puzzleType = puzzleType.id,
                accessed = true,
                currentLevel = nextLevel,
                totalStars = maxOf(previous?.totalStars ?: 0, cloudStats?.totalStars ?: 0) +
                        starsEarned,
                playedCount = maxOf(previous?.playedCount ?: 0, cloudStats?.playedCount ?: 0) +
                        1,
                solvedCount = maxOf(previous?.solvedCount ?: 0, cloudStats?.solvedCount ?: 0) +
                        1,
                bestScore = maxOf(priorBestScore, rawScore),
                bestStreak = maxOf(previous?.bestStreak ?: 0, cloudStats?.bestStreak ?: 0),
                lastSolvedMillis = now
            )

            dao.upsertLocalPuzzleProgress(progress)
            dao.insertPendingCompletion(
                PendingPuzzleCompletionEntity(
                    eventId = UUID.randomUUID().toString(),
                    userId = userId,
                    puzzleType = puzzleType.id,
                    difficulty = difficulty.name,
                    levelPlayed = levelPlayed,
                    starsEarned = starsEarned,
                    rawScore = rawScore,
                    completedAtMillis = now
                )
            )
            progress to priorBestScore
        }

        enqueuePuzzleCompletionSync(userId)

        return PuzzleCompletionResult(
            puzzleType = puzzleType,
            difficulty = difficulty,
            completedLevel = levelPlayed,
            nextLevelUnlocked = updated.first.currentLevel,
            starsEarned = starsEarned,
            score = rawScore,
            nectarEarned = 0,
            moonstonesEarned = 0,
            skillXpEarned = 0,
            newBestScore = rawScore > updated.second
        )
    }

    private fun enqueuePuzzleCompletionSync(userId: String) {
        PuzzleCompletionSyncWorker.enqueue(context, userId)
    }

    suspend fun queueMissionOutcomeLocally(
        userId: String,
        mission: CommunityMission,
        victory: Boolean,
        calmedSpirit: Boolean,
        remainingMana: Int,
        usedLootInBattle: Map<LootItemType, Int>,
        profile: PlayerProfileEntity
    ): MissionCompletionSummary {
        val event = PendingMissionOutcomeEntity(
            eventId = UUID.randomUUID().toString(),
            userId = userId,
            missionId = mission.id,
            victory = victory,
            calmedSpirit = calmedSpirit,
            remainingMana = remainingMana,
            usedLootCsv = RpgMissionCatalog.serializeLootBag(usedLootInBattle),
            completedAtMillis = System.currentTimeMillis()
        )

        saveRoomEventAndScheduleSync(
            event = event,
            saveToRoom = { localDatabase.gameDao().insertPendingMissionOutcome(it) },
            scheduleSync = { PuzzleCompletionSyncWorker.enqueue(context, userId) }
        )

        if (!victory) {
            return MissionCompletionSummary(
                mission = mission,
                victory = false,
                calmedSpirit = false,
                expGained = 0,
                leveledUpTo = null,
                magicGained = 0,
                manaGained = 0,
                nectarGained = 0,
                moonstonesGained = 0,
                prosperityGained = 0,
                lootEarned = emptyList(),
                gameBeatenJustNow = false
            )
        }

        val firstClear = mission.id !in profile.completedMissionIds
        val opponent = mission.opponent
        val prosperityGained = when {
            mission.isWildPatrol ->
                if (profile.communityProsperity < 100) mission.prosperityReward else 0
            firstClear -> mission.prosperityReward
            else -> 0
        }
        val expGained = opponent.rewardExp +
                if (firstClear && !mission.isWildPatrol) 25 else 0
        val earnedLoot = mutableListOf(opponent.guaranteedLoot)
        if (calmedSpirit && opponent.guaranteedLoot != LootItemType.MANA_VIAL) {
            earnedLoot += LootItemType.MANA_VIAL
        }
        val (newLevel, _) = applyHeroExp(profile.heroLevel, profile.heroExp, expGained)

        return MissionCompletionSummary(
            mission = mission,
            victory = true,
            calmedSpirit = calmedSpirit,
            expGained = expGained,
            leveledUpTo = newLevel.takeIf { it > profile.heroLevel },
            magicGained = opponent.rewardMagic + if (calmedSpirit) 8 else 0,
            manaGained = opponent.rewardMana + if (calmedSpirit) 10 else 0,
            nectarGained = opponent.rewardNectar,
            moonstonesGained = opponent.rewardMoonstones,
            prosperityGained = prosperityGained,
            lootEarned = earnedLoot,
            gameBeatenJustNow = mission.isFinalExpedition && !profile.canopyRestored
        )
    }

    suspend fun syncPendingMissionOutcomes(userId: String) {
        ensureSeeded(userId)

        val dao = localDatabase.gameDao()
        syncRoomEventsAndRemove(
            events = dao.getPendingMissionOutcomes(userId),
            saveToCloud = { event -> saveMissionOutcomeToCloud(event) },
            removeFromRoom = { event ->
                dao.deletePendingMissionOutcome(event.eventId)
            }
        )
    }

    private suspend fun saveMissionOutcomeToCloud(event: PendingMissionOutcomeEntity) {
        val mission = RpgMissionCatalog.missionById(event.missionId)
            ?: throw IllegalStateException("Unknown mission: ${event.missionId}")
        val profileRef = userDocRef(event.userId)
        val receiptRef = profileRef
            .collection("mission_outcomes")
            .document(event.eventId)

        db.runTransaction { transaction ->
            if (transaction.get(receiptRef).exists()) {
                return@runTransaction false
            }

            val profile = transaction.get(profileRef)
                .toObject(FirestoreUserProfile::class.java)
                ?.toEntity()
                ?: throw IllegalStateException("Missing profile for mission outcome")

            var lootCsv = profile.lootBagCsv
            RpgMissionCatalog.parseLootBag(event.usedLootCsv).forEach { (item, count) ->
                lootCsv = RpgMissionCatalog.consumeLoot(lootCsv, item, count) ?: lootCsv
            }

            val updates = mutableMapOf<String, Any>(
                "mana" to event.remainingMana.coerceIn(4, profile.maxMana),
                "lootBagCsv" to lootCsv,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (event.victory) {
                val firstClear = mission.id !in profile.completedMissionIds
                val opponent = mission.opponent
                val expGained = opponent.rewardExp +
                        if (firstClear && !mission.isWildPatrol) 25 else 0
                val magicGained = opponent.rewardMagic + if (event.calmedSpirit) 8 else 0
                val manaGained = opponent.rewardMana + if (event.calmedSpirit) 10 else 0
                val prosperityGained = when {
                    mission.isWildPatrol ->
                        if (profile.communityProsperity < 100) mission.prosperityReward else 0
                    firstClear -> mission.prosperityReward
                    else -> 0
                }
                val earnedLoot = mutableListOf(opponent.guaranteedLoot)
                if (event.calmedSpirit &&
                    opponent.guaranteedLoot != LootItemType.MANA_VIAL
                ) {
                    earnedLoot += LootItemType.MANA_VIAL
                }
                lootCsv = RpgMissionCatalog.addLoot(lootCsv, earnedLoot)

                val (newLevel, newExp) = applyHeroExp(
                    profile.heroLevel,
                    profile.heroExp,
                    expGained
                )
                val newProsperity =
                    (profile.communityProsperity + prosperityGained).coerceAtMost(100)
                val newNectar = profile.nectar + opponent.rewardNectar
                val updatedProfile = profile.copy(
                    heroLevel = newLevel,
                    communityProsperity = newProsperity,
                    magicEssence = profile.magicEssence + magicGained
                )
                val newMana = (event.remainingMana + manaGained)
                    .coerceIn(0, updatedProfile.maxMana)

                updates["heroLevel"] = newLevel
                updates["heroExp"] = newExp
                updates["mana"] = newMana
                updates["magicEssence"] = profile.magicEssence + magicGained
                updates["communityProsperity"] = newProsperity
                updates["completedMissionsCsv"] =
                    (profile.completedMissionIds + mission.id).joinToString(",")
                updates["lootBagCsv"] = lootCsv
                updates["nectar"] = newNectar
                updates["moonstones"] = profile.moonstones + opponent.rewardMoonstones
                updates["canopyRestored"] = profile.canopyRestored || mission.isFinalExpedition
            }

            transaction.update(profileRef, updates)
            transaction.set(
                receiptRef,
                mapOf(
                    "eventId" to event.eventId,
                    "userId" to event.userId,
                    "missionId" to event.missionId,
                    "victory" to event.victory,
                    "completedAtMillis" to event.completedAtMillis,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            true
        }.await()

        val updatedProfile = getProfileOnce(event.userId) ?: return
        if (event.victory) {
            unlockBiomesForNectar(event.userId, updatedProfile.nectar)
            syncDiscoveredLore(event.userId)
        }
        if (updatedProfile.lastActivityEpochDay != DailyStreakTracker.localEpochDay()) {
            recordDailyActivity(event.userId)
        }
    }

    suspend fun syncPendingPuzzleCompletions(userId: String) {
        ensureSeeded(userId)

        val dao = localDatabase.gameDao()
        val pending = dao.getPendingCompletions(userId)

        for (event in pending) {
            val puzzleType = PuzzleType.fromId(event.puzzleType)
            val difficulty = DifficultyTier.entries.firstOrNull {
                it.name == event.difficulty
            } ?: throw IllegalStateException(
                "Unknown puzzle difficulty: ${event.difficulty}"
            )

            val profileRef = userDocRef(userId)
            val statsRef = puzzleStatsCollection(userId).document(event.puzzleType)
            val receiptRef = profileRef
                .collection("puzzle_completions")
                .document(event.eventId)

            syncRoomEventsAndRemove(
                events = listOf(event),
                saveToCloud = {
                    db.runTransaction { transaction ->
                        if (transaction.get(receiptRef).exists()) {
                            return@runTransaction false
                        }

                        val profile = transaction.get(profileRef)
                            .toObject(FirestoreUserProfile::class.java)
                            ?.toEntity()
                            ?: PlayerProfileEntity()

                        val stats = transaction.get(statsRef)
                            .toObject(FirestorePuzzleStats::class.java)
                            ?.toEntity()
                            ?: PuzzleStatsEntity(puzzleType = event.puzzleType)

                        val hero = JungleLoreCatalog.heroById(profile.selectedHeroId)
                        val relic = JungleLoreCatalog.relicById(profile.equippedRelicId)
                        val nectar = (
                                (puzzleType.baseNectarReward + event.levelPlayed * 2) *
                                        difficulty.rewardMultiplier
                                ).roundToInt() + hero.bonusNectarPerPuzzle + relic.bonusNectar
                        val moonstones =
                            if (event.starsEarned == 3 || difficulty == DifficultyTier.ELDER) 2 else 1
                        val skillXp = difficulty.xpBonus +
                                event.starsEarned * 5 +
                                (event.rawScore / 25).coerceAtMost(20) +
                                hero.bonusSkillXp
                        val (heroLevel, heroExp) = applyHeroExp(
                            profile.heroLevel,
                            profile.heroExp,
                            35 + event.starsEarned * 10
                        )
                        val streak = profile.currentStreak + 1
                        val nextLevel = maxOf(stats.currentLevel, event.levelPlayed + 1)

                        val profileUpdates = mutableMapOf<String, Any>(
                            "heroLevel" to heroLevel,
                            "heroExp" to heroExp,
                            "nectar" to profile.nectar + nectar,
                            "moonstones" to profile.moonstones + moonstones,
                            "currentStreak" to streak,
                            "bestStreak" to maxOf(profile.bestStreak, streak),
                            "totalPuzzlesSolved" to profile.totalPuzzlesSolved + 1,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )

                        when (puzzleType.skill) {
                            BrainSkill.MEMORY ->
                                profileUpdates["memoryXp"] = profile.memoryXp + skillXp
                            BrainSkill.LOGIC ->
                                profileUpdates["logicXp"] = profile.logicXp + skillXp
                            BrainSkill.SPATIAL ->
                                profileUpdates["spatialXp"] = profile.spatialXp + skillXp
                            BrainSkill.DEDUCTION ->
                                profileUpdates["deductionXp"] = profile.deductionXp + skillXp
                            BrainSkill.OBSERVATION ->
                                profileUpdates["observationXp"] = profile.observationXp + skillXp
                        }

                        transaction.update(profileRef, profileUpdates)
                        transaction.update(
                            statsRef,
                            mapOf(
                                "accessed" to true,
                                "currentLevel" to nextLevel,
                                "totalStars" to stats.totalStars + event.starsEarned,
                                "playedCount" to stats.playedCount + 1,
                                "solvedCount" to stats.solvedCount + 1,
                                "bestScore" to maxOf(stats.bestScore, event.rawScore),
                                "bestStreak" to maxOf(stats.bestStreak, streak),
                                "lastSolvedMillis" to event.completedAtMillis,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        transaction.set(
                            receiptRef,
                            mapOf(
                                "userId" to userId,
                                "eventId" to event.eventId,
                                "puzzleType" to event.puzzleType,
                                "levelPlayed" to event.levelPlayed,
                                "starsEarned" to event.starsEarned,
                                "rawScore" to event.rawScore,
                                "completedAtMillis" to event.completedAtMillis,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                        true
                    }.await()
                },
                removeFromRoom = {
                    dao.deletePendingCompletion(event.eventId)
                }
            )
        }
    }

    private fun userDocRef(userId: String) = db.collection("users").document(userId)

    private fun creaturesCollection(userId: String) =
        userDocRef(userId).collection("creatures")

    private fun puzzleStatsCollection(userId: String) =
        userDocRef(userId).collection("puzzle_stats")

    fun observeProfile(userId: String): Flow<PlayerProfileEntity> =
        userDocRef(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toEntity() ?: PlayerProfileEntity()
            }

    fun observeCreatures(userId: String): Flow<List<CreatureProgressEntity>> =
        creaturesCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreCreatureProgress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.toEntity() }.sortedBy { it.creatureId }
            }

    fun observePuzzleStats(userId: String): Flow<List<PuzzleStatsEntity>> =
        puzzleStatsCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestorePuzzleStats::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.toEntity() }
            }

    suspend fun getProfileOnce(userId: String): PlayerProfileEntity? {
        val ref = userDocRef(userId)
        return try {
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                null
            } else {
                snapshot.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toEntity()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            throw e
        }
    }

    suspend fun ensureSeeded(
        userId: String,
        displayName: String? = null,
        email: String? = null
    ) {
        val profileRef = userDocRef(userId)
        val profileSnap = try {
            profileRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, profileRef.path)
            throw e
        }

        val existingCreatures = try {
            creaturesCollection(userId).get().await().documents.map { it.id }.toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, creaturesCollection(userId).path)
            throw e
        }

        val existingStatsDocs = try {
            puzzleStatsCollection(userId).get().await().documents
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, puzzleStatsCollection(userId).path)
            throw e
        }
        val existingStatsById = existingStatsDocs.associateBy { it.id }

        val batch = db.batch()
        var hasWrites = false

        if (!profileSnap.exists()) {
            batch.set(
                profileRef,
                FirestoreUserProfile.newProfileCreateMap(
                    userId = userId,
                    displayName = displayName,
                    email = email
                )
            )
            hasWrites = true
        } else if (!profileSnap.contains("mana") ||
            !profileSnap.contains("magicEssence") ||
            !profileSnap.contains("communityProsperity") ||
            !profileSnap.contains("completedMissionsCsv") ||
            !profileSnap.contains("lootBagCsv") ||
            !profileSnap.contains("canopyRestored") ||
            !profileSnap.contains("dailyStreak") ||
            !profileSnap.contains("longestDailyStreak") ||
            !profileSnap.contains("lastActivityEpochDay") ||
            !profileSnap.contains("introCompleted")
        ) {
            val defaults = PlayerProfileEntity()
            val migrationPatch = mutableMapOf<String, Any>()
            if (!profileSnap.contains("mana")) migrationPatch["mana"] = defaults.mana
            if (!profileSnap.contains("magicEssence")) migrationPatch["magicEssence"] =
                defaults.magicEssence
            if (!profileSnap.contains("communityProsperity")) migrationPatch["communityProsperity"] =
                defaults.communityProsperity
            if (!profileSnap.contains("completedMissionsCsv")) migrationPatch["completedMissionsCsv"] =
                defaults.completedMissionsCsv
            if (!profileSnap.contains("lootBagCsv")) migrationPatch["lootBagCsv"] =
                defaults.lootBagCsv
            if (!profileSnap.contains("canopyRestored")) migrationPatch["canopyRestored"] =
                defaults.canopyRestored
            if (!profileSnap.contains("dailyStreak")) migrationPatch["dailyStreak"] =
                defaults.dailyStreak
            if (!profileSnap.contains("longestDailyStreak")) migrationPatch["longestDailyStreak"] =
                defaults.longestDailyStreak
            if (!profileSnap.contains("lastActivityEpochDay")) migrationPatch["lastActivityEpochDay"] =
                defaults.lastActivityEpochDay
            if (!profileSnap.contains("introCompleted")) migrationPatch["introCompleted"] =
                defaults.introCompleted
            migrationPatch["updatedAt"] = FieldValue.serverTimestamp()
            batch.update(profileRef, migrationPatch)
            hasWrites = true
        }

        JungleLoreCatalog.creatures.forEachIndexed { idx, creature ->
            if (creature.id !in existingCreatures) {
                batch.set(
                    creaturesCollection(userId).document(creature.id),
                    FirestoreCreatureProgress.newCreateMap(
                        userId = userId,
                        creatureId = creature.id,
                        discovered = idx == 0,
                        bondLevel = if (idx == 0) 1 else 0,
                        bondXp = if (idx == 0) 15 else 0
                    )
                )
                hasWrites = true
            }
        }

        PuzzleType.entries.forEach { type ->
            val existingDoc = existingStatsById[type.id]
            if (existingDoc == null) {
                batch.set(
                    puzzleStatsCollection(userId).document(type.id),
                    FirestorePuzzleStats.newCreateMap(
                        userId = userId,
                        puzzleType = type.id,
                        accessed = false
                    )
                )
                hasWrites = true
            } else if (!existingDoc.contains("accessed")) {
                val playedCount = (existingDoc.getLong("playedCount") ?: 0L).toInt()
                val solvedCount = (existingDoc.getLong("solvedCount") ?: 0L).toInt()
                val currentLevel = (existingDoc.getLong("currentLevel") ?: 1L).toInt()
                val inferredAccessed = playedCount > 0 || solvedCount > 0 || currentLevel > 1
                batch.update(
                    existingDoc.reference,
                    mapOf(
                        "accessed" to inferredAccessed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                hasWrites = true
            }
        }

        if (hasWrites) {
            try {
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, profileRef.path)
                throw e
            }
        }
    }

    suspend fun selectHero(userId: String, heroId: String) {
        ensureSeeded(userId)
        val currentProfile = getProfileOnce(userId) ?: PlayerProfileEntity()
        val hero = JungleLoreCatalog.heroById(heroId)
        if (currentProfile.nectar >= hero.unlockNectar) {
            updateProfileFields(
                userId = userId,
                fields = mapOf(
                    "selectedHeroId" to hero.id,
                    "explorerName" to hero.name
                )
            )
        }
    }

    suspend fun craftOrEquipRelic(userId: String, relicId: String): String {
        ensureSeeded(userId)
        val current = getProfileOnce(userId) ?: PlayerProfileEntity()
        val relic = JungleLoreCatalog.relicById(relicId)
        val unlocked = current.unlockedRelicIds
        return if (relic.id in unlocked) {
            updateProfileFields(userId, mapOf("equippedRelicId" to relic.id))
            "Equipped ${relic.name}!"
        } else if (current.moonstones >= relic.shardCost) {
            val newUnlocked = (unlocked + relic.id).joinToString(",")
            updateProfileFields(
                userId = userId,
                fields = mapOf(
                    "moonstones" to current.moonstones - relic.shardCost,
                    "unlockedRelicsCsv" to newUnlocked,
                    "equippedRelicId" to relic.id
                )
            )
            "Forged & equipped ${relic.name}!"
        } else {
            "Need ${relic.shardCost} Moonstone Shards to forge ${relic.name}."
        }
    }

    suspend fun markIntroCompleted(userId: String, bonusNectar: Int = 0) {
        ensureSeeded(userId)
        val current = getProfileOnce(userId) ?: PlayerProfileEntity()
        val fields = mutableMapOf<String, Any>("introCompleted" to true)
        val extra = bonusNectar.coerceAtLeast(0)
        if (!current.introCompleted && extra > 0) {
            fields["nectar"] = current.nectar + extra
        }
        updateProfileFields(userId = userId, fields = fields)
        if (!current.introCompleted && extra > 0) {
            unlockBiomesForNectar(userId, current.nectar + extra)
            syncDiscoveredLore(userId)
        }
    }

    suspend fun resetIntroTutorial(userId: String) {
        ensureSeeded(userId)
        updateProfileFields(userId = userId, fields = mapOf("introCompleted" to false))
    }

    suspend fun markPuzzleAccessed(userId: String, puzzleType: PuzzleType) {
        ensureSeeded(userId)
        val existing = getPuzzleStatsOnce(userId, puzzleType.id)
        if (existing?.accessed == true) return
        val statsRef = puzzleStatsCollection(userId).document(puzzleType.id)

        awaitWithRetry(
            operation = "Puzzle accessed",
            path = statsRef.path
        ) {
            statsRef.update(
                mapOf(
                    "accessed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun recordDailyActivity(
        userId: String,
        todayEpochDay: Long = DailyStreakTracker.localEpochDay()
    ): DailyStreakEvaluation {
        ensureSeeded(userId)
        val currentProfile = getProfileOnce(userId) ?: PlayerProfileEntity()
        val evaluation = DailyStreakTracker.evaluate(
            currentStreak = currentProfile.dailyStreak,
            longestStreak = currentProfile.longestDailyStreak,
            lastActivityEpochDay = currentProfile.lastActivityEpochDay,
            todayEpochDay = todayEpochDay
        )
        if (!evaluation.didAdvanceStreak) return evaluation

        val (newLevel, newExp) = applyHeroExp(
            currentProfile.heroLevel,
            currentProfile.heroExp,
            evaluation.bonusHeroExp
        )
        val projectedMaxMana = currentProfile.copy(heroLevel = newLevel).maxMana
        val newMana = maxOf(
            currentProfile.mana,
            (currentProfile.mana + evaluation.bonusMana).coerceAtMost(projectedMaxMana)
        )
        updateProfileFields(
            userId = userId,
            fields = mapOf(
                "dailyStreak" to evaluation.newStreak,
                "longestDailyStreak" to evaluation.newLongestStreak,
                "lastActivityEpochDay" to evaluation.newLastActivityEpochDay,
                "heroLevel" to newLevel,
                "heroExp" to newExp,
                "mana" to newMana
            )
        )
        return evaluation
    }

    suspend fun recordPuzzleSolved(
        userId: String,
        puzzleType: PuzzleType,
        difficulty: DifficultyTier,
        levelPlayed: Int,
        starsEarned: Int,
        rawScore: Int
    ): PuzzleCompletionResult {
        ensureSeeded(userId)
        val currentProfile = getProfileOnce(userId) ?: PlayerProfileEntity()
        val hero = JungleLoreCatalog.heroById(currentProfile.selectedHeroId)
        val relic = JungleLoreCatalog.relicById(currentProfile.equippedRelicId)
        val existingStats = getPuzzleStatsOnce(userId, puzzleType.id)
            ?: PuzzleStatsEntity(puzzleType = puzzleType.id)

        val nectarEarned =
            ((puzzleType.baseNectarReward + levelPlayed * 2) * difficulty.rewardMultiplier).roundToInt() +
                    hero.bonusNectarPerPuzzle + relic.bonusNectar
        val moonstonesEarned = if (starsEarned == 3 || difficulty == DifficultyTier.ELDER) 2 else 1
        val skillXpEarned =
            difficulty.xpBonus + starsEarned * 5 + (rawScore / 25).coerceAtMost(20) +
                    hero.bonusSkillXp
        val heroExpGain = 35 + starsEarned * 10
        val (newLevel, newExp) = applyHeroExp(
            currentProfile.heroLevel,
            currentProfile.heroExp,
            heroExpGain
        )
        val newStreak = currentProfile.currentStreak + 1
        val updatedBestStreak = maxOf(currentProfile.bestStreak, newStreak)
        val totalNectarAfter = currentProfile.nectar + nectarEarned

        updateProfileFields(
            userId = userId,
            fields = mapOf(
                "heroLevel" to newLevel,
                "heroExp" to newExp,
                "nectar" to totalNectarAfter,
                "moonstones" to currentProfile.moonstones + moonstonesEarned,
                "memoryXp" to currentProfile.memoryXp + if (puzzleType.skill == BrainSkill.MEMORY) skillXpEarned else 0,
                "logicXp" to currentProfile.logicXp + if (puzzleType.skill == BrainSkill.LOGIC) skillXpEarned else 0,
                "spatialXp" to currentProfile.spatialXp + if (puzzleType.skill == BrainSkill.SPATIAL) skillXpEarned else 0,
                "deductionXp" to currentProfile.deductionXp + if (puzzleType.skill == BrainSkill.DEDUCTION) skillXpEarned else 0,
                "observationXp" to currentProfile.observationXp + if (puzzleType.skill == BrainSkill.OBSERVATION) skillXpEarned else 0,
                "currentStreak" to newStreak,
                "bestStreak" to updatedBestStreak,
                "totalPuzzlesSolved" to currentProfile.totalPuzzlesSolved + 1
            )
        )

        val nextLevel = maxOf(existingStats.currentLevel, levelPlayed + 1)
        val isNewBest = rawScore > existingStats.bestScore
        val statsRef = puzzleStatsCollection(userId).document(puzzleType.id)
        awaitWithRetry(
            operation = "Save puzzle completion",
            path = statsRef.path
        ) {
            statsRef.update(
                mapOf(
                    "accessed" to true,
                    "currentLevel" to nextLevel,
                    "totalStars" to existingStats.totalStars + starsEarned,
                    "playedCount" to existingStats.playedCount + 1,
                    "solvedCount" to existingStats.solvedCount + 1,
                    "bestScore" to maxOf(existingStats.bestScore, rawScore),
                    "bestStreak" to maxOf(existingStats.bestStreak, newStreak),
                    "lastSolvedMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        unlockBiomesForNectar(userId, totalNectarAfter)
        syncDiscoveredLore(userId)
        if (currentProfile.lastActivityEpochDay != DailyStreakTracker.localEpochDay()) {
            recordDailyActivity(userId)
        }

        return PuzzleCompletionResult(
            puzzleType = puzzleType,
            difficulty = difficulty,
            completedLevel = levelPlayed,
            nextLevelUnlocked = nextLevel,
            starsEarned = starsEarned,
            score = rawScore,
            nectarEarned = nectarEarned,
            moonstonesEarned = moonstonesEarned,
            skillXpEarned = skillXpEarned,
            newBestScore = isNewBest
        )
    }

    suspend fun recordStoryChoice(
        userId: String,
        biomeId: String,
        creatureId: String,
        rewardNectar: Int,
        bondGain: Int,
        rewardExp: Int
    ) {
        ensureSeeded(userId)
        val currentProfile = getProfileOnce(userId) ?: PlayerProfileEntity()
        val hero = JungleLoreCatalog.heroById(currentProfile.selectedHeroId)
        val newNectar = currentProfile.nectar + rewardNectar
        val (newLevel, newExp) = applyHeroExp(
            currentProfile.heroLevel,
            currentProfile.heroExp,
            rewardExp
        )
        val updatedQuests = (currentProfile.completedQuestIds + biomeId).joinToString(",")

        updateProfileFields(
            userId = userId,
            fields = mapOf(
                "heroLevel" to newLevel,
                "heroExp" to newExp,
                "completedQuestsCsv" to updatedQuests,
                "nectar" to newNectar,
                "moonstones" to currentProfile.moonstones + 1
            )
        )
        addCreatureBond(userId, creatureId, bondGain + hero.bonusBondXp)
        unlockBiomesForNectar(userId, newNectar)
        syncDiscoveredLore(userId)
        if (currentProfile.lastActivityEpochDay != DailyStreakTracker.localEpochDay()) {
            recordDailyActivity(userId)
        }
    }

    suspend fun feedCreatureTreat(
        userId: String,
        creatureId: String,
        nectarCost: Int = 10
    ): Boolean {
        ensureSeeded(userId)
        val currentProfile = getProfileOnce(userId) ?: PlayerProfileEntity()
        if (currentProfile.nectar < nectarCost) return false
        val (newLevel, newExp) = applyHeroExp(currentProfile.heroLevel, currentProfile.heroExp, 15)
        updateProfileFields(
            userId = userId,
            fields = mapOf(
                "heroLevel" to newLevel,
                "heroExp" to newExp,
                "nectar" to currentProfile.nectar - nectarCost
            )
        )
        addCreatureBond(userId, creatureId, bondGain = 30, incrementTreats = true)
        return true
    }

    suspend fun setGentleMode(userId: String, enabled: Boolean) {
        ensureSeeded(userId)
        updateProfileFields(userId, mapOf("gentleMode" to enabled))
    }

    suspend fun recordMissionOutcome(
        userId: String,
        mission: CommunityMission,
        victory: Boolean,
        calmedSpirit: Boolean,
        remainingMana: Int,
        usedLootInBattle: Map<LootItemType, Int>
    ): MissionCompletionSummary {
        ensureSeeded(userId)
        val currentProfile = getProfileOnce(userId) ?: PlayerProfileEntity()

        var updatedLootCsv = currentProfile.lootBagCsv
        usedLootInBattle.forEach { (item, qty) ->
            updatedLootCsv =
                RpgMissionCatalog.consumeLoot(updatedLootCsv, item, qty) ?: updatedLootCsv
        }

        if (!victory) {
            val clampedMana = remainingMana.coerceIn(4, currentProfile.maxMana)
            updateProfileFields(
                userId = userId,
                fields = mapOf(
                    "mana" to clampedMana,
                    "lootBagCsv" to updatedLootCsv
                )
            )
            return MissionCompletionSummary(
                mission = mission,
                victory = false,
                calmedSpirit = false,
                expGained = 0,
                leveledUpTo = null,
                magicGained = 0,
                manaGained = 0,
                nectarGained = 0,
                moonstonesGained = 0,
                prosperityGained = 0,
                lootEarned = emptyList(),
                gameBeatenJustNow = false
            )
        }

        val firstClear = mission.id !in currentProfile.completedMissionIds
        val opponent = mission.opponent
        val sootheBonusMagic = if (calmedSpirit) 8 else 0
        val sootheBonusMana = if (calmedSpirit) 10 else 0

        val expGained = opponent.rewardExp + if (firstClear && !mission.isWildPatrol) 25 else 0
        val magicGained = opponent.rewardMagic + sootheBonusMagic
        val manaGained = opponent.rewardMana + sootheBonusMana
        val nectarGained = opponent.rewardNectar
        val moonstonesGained = opponent.rewardMoonstones
        val prosperityGained = when {
            mission.isWildPatrol -> if (currentProfile.communityProsperity < 100) mission.prosperityReward else 0
            firstClear -> mission.prosperityReward
            else -> 0
        }

        val earnedLoot = mutableListOf(opponent.guaranteedLoot)
        if (calmedSpirit && opponent.guaranteedLoot != LootItemType.MANA_VIAL) {
            earnedLoot += LootItemType.MANA_VIAL
        }
        updatedLootCsv = RpgMissionCatalog.addLoot(updatedLootCsv, earnedLoot)

        val (newLevel, newExp) = applyHeroExp(
            currentProfile.heroLevel,
            currentProfile.heroExp,
            expGained
        )
        val leveledUpTo = if (newLevel > currentProfile.heroLevel) newLevel else null
        val newProsperity =
            (currentProfile.communityProsperity + prosperityGained).coerceAtMost(100)
        val newMissionsCsv = (currentProfile.completedMissionIds + mission.id).joinToString(",")
        val newNectar = currentProfile.nectar + nectarGained
        val gameBeatenNow = mission.isFinalExpedition && !currentProfile.canopyRestored
        val updatedCanopyRestored = currentProfile.canopyRestored || mission.isFinalExpedition

        val projectedProfile = currentProfile.copy(
            heroLevel = newLevel,
            communityProsperity = newProsperity,
            magicEssence = currentProfile.magicEssence + magicGained
        )
        val newMana = (remainingMana + manaGained).coerceIn(0, projectedProfile.maxMana)

        updateProfileFields(
            userId = userId,
            fields = mapOf(
                "heroLevel" to newLevel,
                "heroExp" to newExp,
                "mana" to newMana,
                "magicEssence" to (currentProfile.magicEssence + magicGained),
                "communityProsperity" to newProsperity,
                "completedMissionsCsv" to newMissionsCsv,
                "lootBagCsv" to updatedLootCsv,
                "nectar" to newNectar,
                "moonstones" to (currentProfile.moonstones + moonstonesGained),
                "canopyRestored" to updatedCanopyRestored
            )
        )

        unlockBiomesForNectar(userId, newNectar)
        syncDiscoveredLore(userId)
        if (currentProfile.lastActivityEpochDay != DailyStreakTracker.localEpochDay()) {
            recordDailyActivity(userId)
        }

        return MissionCompletionSummary(
            mission = mission,
            victory = true,
            calmedSpirit = calmedSpirit,
            expGained = expGained,
            leveledUpTo = leveledUpTo,
            magicGained = magicGained,
            manaGained = manaGained,
            nectarGained = nectarGained,
            moonstonesGained = moonstonesGained,
            prosperityGained = prosperityGained,
            lootEarned = earnedLoot,
            gameBeatenJustNow = gameBeatenNow
        )
    }

    suspend fun restAtSanctuary(userId: String): Int {
        ensureSeeded(userId)
        val current = getProfileOnce(userId) ?: PlayerProfileEntity()
        val maxMana = current.maxMana
        updateProfileFields(userId, mapOf("mana" to maxMana))
        return maxMana
    }

    suspend fun brewBattleLoot(userId: String, item: LootItemType, nectarCost: Int = 12): Boolean {
        ensureSeeded(userId)
        val current = getProfileOnce(userId) ?: PlayerProfileEntity()
        if (current.nectar < nectarCost) return false
        val updatedLoot = RpgMissionCatalog.addLoot(current.lootBagCsv, listOf(item))
        updateProfileFields(
            userId = userId,
            fields = mapOf(
                "nectar" to (current.nectar - nectarCost),
                "lootBagCsv" to updatedLoot
            )
        )
        return true
    }

    suspend fun forageWorldResource(
        userId: String,
        nectarGain: Int,
        lootItem: LootItemType? = null
    ) {
        ensureSeeded(userId)
        val current = getProfileOnce(userId) ?: PlayerProfileEntity()
        val newNectar = current.nectar + nectarGain.coerceAtLeast(0)
        val fields = mutableMapOf<String, Any>("nectar" to newNectar)
        if (lootItem != null) {
            fields["lootBagCsv"] = RpgMissionCatalog.addLoot(current.lootBagCsv, listOf(lootItem))
        }
        updateProfileFields(userId = userId, fields = fields)
        if (nectarGain > 0) {
            unlockBiomesForNectar(userId, newNectar)
            syncDiscoveredLore(userId)
        }
    }

    suspend fun getCreatureOnce(userId: String, creatureId: String): CreatureProgressEntity? {
        val ref = creaturesCollection(userId).document(creatureId)
        return try {
            val snap = ref.get().await()
            if (!snap.exists()) null
            else snap.toObject(
                FirestoreCreatureProgress::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toEntity()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            throw e
        }
    }

    suspend fun getPuzzleStatsOnce(userId: String, puzzleType: String): PuzzleStatsEntity? {
        val ref = puzzleStatsCollection(userId).document(puzzleType)
        return try {
            val snap = ref.get().await()
            if (!snap.exists()) null
            else snap.toObject(
                FirestorePuzzleStats::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toEntity()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            throw e
        }
    }

    private suspend fun <T> awaitWithRetry(
        operation: String,
        path: String,
        request: suspend () -> T
    ): T {
        var automaticRetries = 0

        while (true) {
            try {
                return request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (automaticRetries < 3) {
                    automaticRetries++
                    delay((500L * automaticRetries).milliseconds)
                    continue
                }

                val retry = requestManualRetry?.invoke(
                    operation,
                    e.localizedMessage ?: e.javaClass.simpleName
                ) ?: false

                if (!retry) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                    throw CancellationException("Player chose not to retry $operation")
                }

                automaticRetries = 0
            }
        }
    }

    private suspend fun updateProfileFields(userId: String, fields: Map<String, Any>) {
        val ref = userDocRef(userId)
        awaitWithRetry(
            operation = "Update user profile",
            path = ref.path
        ) {
            ref.update(fields + ("updatedAt" to FieldValue.serverTimestamp())).await()
        }

    }

    private fun applyHeroExp(level: Int, currentExp: Int, gained: Int): Pair<Int, Int> {
        var lvl = level
        var exp = currentExp + gained
        while (exp >= lvl * 100) {
            exp -= lvl * 100
            lvl++
        }
        return lvl to exp
    }

    private suspend fun addCreatureBond(
        userId: String,
        creatureId: String,
        bondGain: Int,
        incrementTreats: Boolean = false
    ) {
        val ref = creaturesCollection(userId).document(creatureId)
        val current = getCreatureOnce(userId, creatureId)
        val currentXp = current?.bondXp ?: 0
        val newXp = currentXp + bondGain
        val newLevel = when {
            newXp >= 120 -> 3
            newXp >= 50 -> 2
            else -> maxOf(1, current?.bondLevel ?: 1)
        }
        if (current == null) {
            ref.set(
                FirestoreCreatureProgress.newCreateMap(
                    userId = userId,
                    creatureId = creatureId,
                    discovered = true,
                    bondLevel = newLevel,
                    bondXp = newXp,
                    treatsFed = if (incrementTreats) 1 else 0,
                    lastInteractionMillis = System.currentTimeMillis()
                )
            ).await()
        } else {
            awaitWithRetry(
                operation = "Bond creature with user",
                path = ref.path
            ) {
                ref.update(
                    mapOf(
                        "discovered" to true,
                        "bondXp" to newXp,
                        "bondLevel" to newLevel,
                        "treatsFed" to current.treatsFed + if (incrementTreats) 1 else 0,
                        "lastInteractionMillis" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    private suspend fun unlockBiomesForNectar(userId: String, totalNectar: Int) {
        JungleLoreCatalog.biomes.forEach { biome ->
            if (totalNectar >= biome.unlockNectar) {
                val existing = getCreatureOnce(userId, biome.creatureId)
                val ref = creaturesCollection(userId).document(biome.creatureId)
                if (existing == null) {
                    ref.set(
                        FirestoreCreatureProgress.newCreateMap(
                            userId = userId,
                            creatureId = biome.creatureId,
                            discovered = true,
                            bondLevel = 1,
                            bondXp = 0
                        )
                    ).await()
                } else if (!existing.discovered) {
                    awaitWithRetry(
                        operation = "Biomes discovered and unlocked",
                        path = ref.path
                    ) {
                        ref.update(
                            mapOf(
                                "discovered" to true,
                                "bondLevel" to maxOf(1, existing.bondLevel),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
        }
    }

    private suspend fun syncDiscoveredLore(userId: String) {
        val current = getProfileOnce(userId) ?: return
        val creaturesSnap = try {
            creaturesCollection(userId).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, creaturesCollection(userId).path)
            throw e
        }

        val discoveredCreatureIds = creaturesSnap.toObjects(
            FirestoreCreatureProgress::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
            .filter { it.discovered }
            .map { it.creatureId }
            .toSet()

        val updatedLoreIds = JungleLoreCatalog.loreEntries
            .filter { entry ->
                JungleLoreCatalog.isLoreDiscovered(
                    entry = entry,
                    nectar = current.nectar,
                    totalPuzzlesSolved = current.totalPuzzlesSolved,
                    completedQuests = current.completedQuestIds,
                    discoveredCreatures = discoveredCreatureIds,
                    savedLoreIds = current.discoveredLoreIds
                )
            }
            .map { it.id }
            .toSet()

        if (updatedLoreIds != current.discoveredLoreIds) {
            updateProfileFields(
                userId = userId,
                fields = mapOf("discoveredLoreCsv" to updatedLoreIds.joinToString(","))
            )
        }
    }
}
