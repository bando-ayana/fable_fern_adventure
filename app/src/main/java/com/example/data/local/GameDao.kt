package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM local_puzzle_progress WHERE userId = :userId")
    fun observeLocalPuzzleProgress(userId: String): Flow<List<LocalPuzzleProgressEntity>>

    @Query("""
    SELECT * FROM local_puzzle_progress
    WHERE userId = :userId AND puzzleType = :puzzleType
""")
    suspend fun getLocalPuzzleProgress(
        userId: String,
        puzzleType: String
    ): LocalPuzzleProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalPuzzleProgress(progress: LocalPuzzleProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingCompletion(event: PendingPuzzleCompletionEntity)

    @Query("""
    SELECT * FROM pending_puzzle_completions
    WHERE userId = :userId ORDER BY completedAtMillis
""")
    suspend fun getPendingCompletions(userId: String): List<PendingPuzzleCompletionEntity>

    @Query("DELETE FROM pending_puzzle_completions WHERE eventId = :eventId")
    suspend fun deletePendingCompletion(eventId: String)



    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM creature_progress ORDER BY creatureId ASC")
    fun observeCreatures(): Flow<List<CreatureProgressEntity>>

    @Query("SELECT * FROM creature_progress WHERE creatureId = :creatureId")
    suspend fun getCreatureOnce(creatureId: String): CreatureProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCreature(creature: CreatureProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCreaturesIfAbsent(creatures: List<CreatureProgressEntity>)

    @Query("SELECT * FROM puzzle_stats")
    fun observePuzzleStats(): Flow<List<PuzzleStatsEntity>>

    @Query("SELECT * FROM puzzle_stats WHERE puzzleType = :puzzleType")
    suspend fun getPuzzleStatsOnce(puzzleType: String): PuzzleStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPuzzleStats(stats: PuzzleStatsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPuzzleStatsIfAbsent(stats: List<PuzzleStatsEntity>)
}
