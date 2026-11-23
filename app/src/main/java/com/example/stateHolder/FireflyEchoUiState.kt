package com.example.stateHolder

import com.example.puzzle.FireflyRound

data class FireflyEchoUiState(
    val rounds: List<FireflyRound> = emptyList(),
    val currentRoundIdx: Int = 0,
    val isPlayingSequence: Boolean = false,
    val activeBlossomIdx: Int? = null,
    val userInput: List<Int> = emptyList(),
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val statusBanner: String = "Watch the lantern blossoms glow...",
    val errorFlashIdx: Int? = null
) {
    val currentRound: FireflyRound? get() = rounds.getOrNull(currentRoundIdx)
}

