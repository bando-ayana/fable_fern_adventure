package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        CreatureProgressEntity::class,
        PuzzleStatsEntity::class,
        LocalPuzzleProgressEntity::class,
        PendingPuzzleCompletionEntity::class,
        PendingMissionOutcomeEntity::class,
        PendingProfileRewardEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class FernfableDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: FernfableDatabase? = null

        fun getInstance(context: Context): FernfableDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FernfableDatabase::class.java,
                    "fernfable_sanctuary.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_mission_outcomes (
                        eventId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        missionId TEXT NOT NULL,
                        victory INTEGER NOT NULL,
                        calmedSpirit INTEGER NOT NULL,
                        remainingMana INTEGER NOT NULL,
                        usedLootCsv TEXT NOT NULL,
                        completedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(eventId)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
