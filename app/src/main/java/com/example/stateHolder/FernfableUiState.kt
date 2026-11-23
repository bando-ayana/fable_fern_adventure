package com.example.stateHolder

import com.example.data.local.CreatureProgressEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleStatsEntity
import com.example.model.DifficultyTier
import com.example.model.PuzzleType

data class FernfableUiState(
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val creatureProgress: Map<String, CreatureProgressEntity> = emptyMap(),
    val puzzleStats: Map<PuzzleType, PuzzleStatsEntity> = emptyMap(),
    val selectedDifficulty: DifficultyTier = DifficultyTier.SPROUT,
    val snackbarMessage: String? = null,
    val syncError: String? = null
)