package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.model.DifficultyTier
import com.example.model.PuzzleCompletionResult
import com.example.model.PuzzleType
import com.example.puzzle.CanopySpotterEngine
import com.example.puzzle.CanopySpotterPuzzle
import com.example.puzzle.FernMosaicEngine
import com.example.puzzle.FernMosaicPuzzle
import com.example.puzzle.FireflyEchoEngine
import com.example.puzzle.GroveGridEngine
import com.example.puzzle.GroveGridPuzzle
import com.example.puzzle.MushroomMelodyEngine
import com.example.puzzle.MushroomMelodyPuzzle
import com.example.puzzle.SpiritLockEngine
import com.example.puzzle.SpiritLockPuzzle
import com.example.puzzle.SpiritRune
import com.example.puzzle.VineWeaverBoard
import com.example.puzzle.VineWeaverEngine
import com.example.stateHolder.FernfableUiState
import com.example.stateHolder.FireflyEchoUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PuzzleViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val sanctuaryUiState: StateFlow<FernfableUiState>
) : ViewModel() {

    private val _activePuzzleType =
        MutableStateFlow(PuzzleType.FIREFLY_ECHO)
    val activePuzzleType: StateFlow<PuzzleType> =
        _activePuzzleType.asStateFlow()

    private val _activePuzzleLevel = MutableStateFlow(1)
    val activePuzzleLevel: StateFlow<Int> =
        _activePuzzleLevel.asStateFlow()

    private val _fireflyState =
        MutableStateFlow(FireflyEchoUiState())
    val fireflyState: StateFlow<FireflyEchoUiState> =
        _fireflyState.asStateFlow()

    private val _mushroomState =
        MutableStateFlow<MushroomMelodyPuzzle?>(null)
    val mushroomState: StateFlow<MushroomMelodyPuzzle?> =
        _mushroomState.asStateFlow()

    private val _mushroomSelectedSlot =
        MutableStateFlow<Int?>(null)
    val mushroomSelectedSlot: StateFlow<Int?> =
        _mushroomSelectedSlot.asStateFlow()

    private val _fernMosaicState =
        MutableStateFlow<FernMosaicPuzzle?>(null)
    val fernMosaicState: StateFlow<FernMosaicPuzzle?> =
        _fernMosaicState.asStateFlow()

    private val _mosaicStatus = MutableStateFlow<String?>(null)
    val mosaicStatus: StateFlow<String?> =
        _mosaicStatus.asStateFlow()

    private val _canopySpotterState =
        MutableStateFlow<CanopySpotterPuzzle?>(null)
    val canopySpotterState: StateFlow<CanopySpotterPuzzle?> =
        _canopySpotterState.asStateFlow()

    private val _spotterStatus = MutableStateFlow<String?>(null)
    val spotterStatus: StateFlow<String?> =
        _spotterStatus.asStateFlow()

    private val _groveGridState =
        MutableStateFlow<GroveGridPuzzle?>(null)
    val groveGridState: StateFlow<GroveGridPuzzle?> =
        _groveGridState.asStateFlow()

    private val _selectedGridCell =
        MutableStateFlow<Int?>(null)
    val selectedGridCell: StateFlow<Int?> =
        _selectedGridCell.asStateFlow()

    private val _vineWeaverState =
        MutableStateFlow<VineWeaverBoard?>(null)
    val vineWeaverState: StateFlow<VineWeaverBoard?> =
        _vineWeaverState.asStateFlow()

    private val _spiritLockState =
        MutableStateFlow<SpiritLockPuzzle?>(null)
    val spiritLockState: StateFlow<SpiritLockPuzzle?> =
        _spiritLockState.asStateFlow()

    private val _completionResult =
        MutableStateFlow<PuzzleCompletionResult?>(null)
    val completionResult: StateFlow<PuzzleCompletionResult?> =
        _completionResult.asStateFlow()

    private val _isIntroShrinePuzzle =
        MutableStateFlow(false)
    val isIntroShrinePuzzle: StateFlow<Boolean> =
        _isIntroShrinePuzzle.asStateFlow()

    private var mushroomSwaps = 0
    private var mushroomHints = 0
    private var gridMoves = 0
    private var gridHints = 0
    private var vineTaps = 0
    private var vineHints = 0
    private var lockHints = 0
    private var playbackJob: Job? = null

    private val difficulty: DifficultyTier
        get() = sanctuaryUiState.value.selectedDifficulty

    fun startPuzzle(
        type: PuzzleType,
        levelOverride: Int? = null,
        accessedFromRpg: Boolean = true
    ) {
        playbackJob?.cancel()
        _activePuzzleType.value = type
        _completionResult.value = null

        val existingStats =
            sanctuaryUiState.value.puzzleStats[type]
        val level = (
            levelOverride
                ?: existingStats?.currentLevel
                ?: 1
            ).coerceAtLeast(1)

        _activePuzzleLevel.value = level
        _isIntroShrinePuzzle.value =
            type == PuzzleType.FIREFLY_ECHO &&
            !sanctuaryUiState.value.profile.introCompleted &&
            levelOverride != null &&
            accessedFromRpg

        if (accessedFromRpg && existingStats?.accessed != true) {
            viewModelScope.launch {
                runRepositoryAction {
                    repository.markPuzzleAccessed(
                        currentUserId,
                        type
                    )
                }
            }
        }

        when (type) {
            PuzzleType.FIREFLY_ECHO -> {
                val rounds =
                    FireflyEchoEngine.createRounds(difficulty, level)
                _fireflyState.value = FireflyEchoUiState(
                    rounds = rounds,
                    currentRoundIdx = 0,
                    statusBanner =
                        "Level $level — Watch the lantern blossoms glow..."
                )
                playFireflySequence()
            }

            PuzzleType.MUSHROOM_MELODY -> {
                mushroomSwaps = 0
                mushroomHints = 0
                _mushroomSelectedSlot.value = null
                _mushroomState.value =
                    MushroomMelodyEngine.generate(difficulty, level)
            }

            PuzzleType.FERN_MOSAIC -> {
                _mosaicStatus.value = null
                _fernMosaicState.value =
                    FernMosaicEngine.generate(difficulty, level)
            }

            PuzzleType.CANOPY_SPOTTER -> {
                _spotterStatus.value = null
                _canopySpotterState.value =
                    CanopySpotterEngine.generate(difficulty, level)
            }

            PuzzleType.GROVE_GRID -> {
                gridMoves = 0
                gridHints = 0
                val puzzle =
                    GroveGridEngine.generate(difficulty, level)
                _groveGridState.value = puzzle
                _selectedGridCell.value =
                    puzzle.board.indexOfFirst { it == 0 }
                        .takeIf { it >= 0 }
            }

            PuzzleType.VINE_WEAVER -> {
                vineTaps = 0
                vineHints = 0
                _vineWeaverState.value =
                    VineWeaverEngine.generate(difficulty, level)
            }

            PuzzleType.SPIRIT_LOCK -> {
                lockHints = 0
                _spiritLockState.value =
                    SpiritLockEngine.generate(difficulty, level)
            }
        }
    }

    fun dismissCompletion() {
        _completionResult.value = null
        _isIntroShrinePuzzle.value = false
    }

    fun playFireflySequence() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val round =
                _fireflyState.value.currentRound
                    ?: return@launch

            _fireflyState.update {
                it.copy(
                    isPlayingSequence = true,
                    userInput = emptyList(),
                    activeBlossomIdx = null,
                    errorFlashIdx = null,
                    statusBanner =
                        "Round ${round.roundNumber} of " +
                        "${round.totalRounds} — Watch closely!"
                )
            }

            delay(450L.milliseconds)

            val pulseMs = when (difficulty) {
                DifficultyTier.SPROUT -> 460L
                DifficultyTier.EXPLORER -> 380L
                DifficultyTier.ELDER -> 310L
            }

            for (blossomIdx in round.sequence) {
                _fireflyState.update {
                    it.copy(activeBlossomIdx = blossomIdx)
                }
                delay(pulseMs.milliseconds)
                _fireflyState.update {
                    it.copy(activeBlossomIdx = null)
                }
                delay(180L.milliseconds)
            }

            _fireflyState.update {
                it.copy(
                    isPlayingSequence = false,
                    statusBanner =
                        "Your turn! Tap ${round.sequence.size} " +
                        "blossoms in order."
                )
            }
        }
    }

    fun replayFireflyHint() {
        if (
            _fireflyState.value.isPlayingSequence ||
            _completionResult.value != null
        ) return

        _fireflyState.update {
            it.copy(hintsUsed = it.hintsUsed + 1)
        }
        playFireflySequence()
    }

    fun tapFireflyBlossom(index: Int) {
        val state = _fireflyState.value
        if (
            state.isPlayingSequence ||
            _completionResult.value != null
        ) return

        val round = state.currentRound ?: return
        val expected =
            round.sequence.getOrNull(state.userInput.size)
                ?: return

        if (index == expected) {
            val nextInput = state.userInput + index

            viewModelScope.launch {
                _fireflyState.update {
                    it.copy(activeBlossomIdx = index)
                }
                delay(160L.milliseconds)
                _fireflyState.update {
                    it.copy(activeBlossomIdx = null)
                }
            }

            if (nextInput.size == round.sequence.size) {
                if (
                    state.currentRoundIdx + 1 >=
                    state.rounds.size
                ) {
                    val score =
                        FireflyEchoEngine.computeScore(
                            difficulty,
                            state.mistakes,
                            state.hintsUsed
                        )

                    val stars = when {
                        state.mistakes == 0 &&
                            state.hintsUsed == 0 -> 3
                        state.mistakes <= 2 &&
                            state.hintsUsed <= 1 -> 2
                        else -> 1
                    }

                    _fireflyState.update {
                        it.copy(
                            userInput = nextInput,
                            statusBanner =
                                "Harmony achieved! The fireflies shimmer!"
                        )
                    }

                    completePuzzle(
                        PuzzleType.FIREFLY_ECHO,
                        score,
                        stars
                    )
                } else {
                    _fireflyState.update {
                        it.copy(
                            currentRoundIdx =
                                it.currentRoundIdx + 1,
                            userInput = emptyList(),
                            statusBanner =
                                "Lovely! Next lantern song..."
                        )
                    }

                    viewModelScope.launch {
                        delay(550L.milliseconds)
                        playFireflySequence()
                    }
                }
            } else {
                _fireflyState.update {
                    it.copy(
                        userInput = nextInput,
                        statusBanner =
                            "${nextInput.size} / " +
                            "${round.sequence.size} echoed"
                    )
                }
            }
        } else {
            val gentle =
                sanctuaryUiState.value.profile.gentleMode
            val mistakes = state.mistakes + 1

            viewModelScope.launch {
                _fireflyState.update {
                    it.copy(
                        mistakes = mistakes,
                        errorFlashIdx = index,
                        userInput = emptyList(),
                        statusBanner = if (gentle) {
                            "Oops, gentle breeze! " +
                                "Watch the blossoms again."
                        } else {
                            "Missed a petal! " +
                                "Replaying sequence..."
                        }
                    )
                }

                delay(550L.milliseconds)
                _fireflyState.update {
                    it.copy(errorFlashIdx = null)
                }
                playFireflySequence()
            }
        }
    }

    fun tapMushroomSlot(slotIdx: Int) {
        val puzzle = _mushroomState.value ?: return
        if (
            puzzle.isSolved ||
            _completionResult.value != null
        ) return

        val selected = _mushroomSelectedSlot.value
        if (selected == null) {
            _mushroomSelectedSlot.value = slotIdx
        } else if (selected == slotIdx) {
            _mushroomSelectedSlot.value = null
        } else {
            mushroomSwaps++
            val updated =
                MushroomMelodyEngine.swap(
                    puzzle,
                    selected,
                    slotIdx
                )
            _mushroomState.value = updated
            _mushroomSelectedSlot.value = null

            if (updated.isSolved) {
                val score =
                    MushroomMelodyEngine.computeScore(
                        difficulty,
                        mushroomSwaps,
                        mushroomHints
                    )
                val stars = when {
                    mushroomHints == 0 -> 3
                    mushroomHints <= 1 -> 2
                    else -> 1
                }
                completePuzzle(
                    PuzzleType.MUSHROOM_MELODY,
                    score,
                    stars
                )
            }
        }
    }

    fun useMushroomHint() {
        val puzzle = _mushroomState.value ?: return
        if (
            puzzle.isSolved ||
            _completionResult.value != null
        ) return

        mushroomHints++
        val updated =
            MushroomMelodyEngine.applyHint(puzzle)
        _mushroomState.value = updated
        _mushroomSelectedSlot.value = null

        if (updated.isSolved) {
            val score =
                MushroomMelodyEngine.computeScore(
                    difficulty,
                    mushroomSwaps,
                    mushroomHints
                )
            completePuzzle(
                PuzzleType.MUSHROOM_MELODY,
                score,
                if (mushroomHints <= 1) 2 else 1
            )
        }
    }

    fun selectMosaicSlot(slotIdx: Int) {
        val puzzle = _fernMosaicState.value ?: return
        val round = puzzle.currentRound ?: return
        if (
            slotIdx !in round.missingIndices ||
            slotIdx in round.placedAnswers ||
            _completionResult.value != null
        ) return

        _fernMosaicState.value =
            puzzle.copy(selectedSlotIdx = slotIdx)
    }

    fun chooseMosaicTile(token: com.example.puzzle.LeafToken) {
        val puzzle = _fernMosaicState.value ?: return
        if (
            puzzle.isCompleted ||
            _completionResult.value != null
        ) return

        val (updated, matched) =
            FernMosaicEngine.selectChoice(puzzle, token)
        _fernMosaicState.value = updated

        if (matched) {
            _mosaicStatus.value =
                "Lovely botanical match!"

            if (updated.isCompleted) {
                val score =
                    FernMosaicEngine.computeScore(
                        difficulty,
                        updated.mistakes,
                        updated.hintsUsed
                    )
                val stars = when {
                    updated.mistakes == 0 &&
                        updated.hintsUsed == 0 -> 3
                    updated.mistakes <= 2 &&
                        updated.hintsUsed <= 1 -> 2
                    else -> 1
                }
                completePuzzle(
                    PuzzleType.FERN_MOSAIC,
                    score,
                    stars
                )
            }
        } else {
            val gentle =
                sanctuaryUiState.value.profile.gentleMode
            _mosaicStatus.value = if (gentle) {
                "Study the repeating shape and color rhythm!"
            } else {
                "Not quite — check the next leaf pattern."
            }
        }
    }

    fun useMosaicHint() {
        val puzzle = _fernMosaicState.value ?: return
        if (
            puzzle.isCompleted ||
            _completionResult.value != null
        ) return

        val updated =
            FernMosaicEngine.applyHint(puzzle)
        _fernMosaicState.value = updated
        _mosaicStatus.value =
            "A glowing firefly placed a matching leaf!"

        if (updated.isCompleted) {
            val score =
                FernMosaicEngine.computeScore(
                    difficulty,
                    updated.mistakes,
                    updated.hintsUsed
                )
            val stars =
                if (
                    updated.hintsUsed <= 1 &&
                    updated.mistakes <= 2
                ) 2 else 1

            completePuzzle(
                PuzzleType.FERN_MOSAIC,
                score,
                stars
            )
        }
    }

    fun tapSpotterCharm(charmId: Int) {
        val puzzle =
            _canopySpotterState.value ?: return
        if (
            puzzle.isCompleted ||
            _completionResult.value != null
        ) return

        val (updated, matched) =
            CanopySpotterEngine.tapCharm(puzzle, charmId)
        _canopySpotterState.value = updated

        if (matched) {
            _spotterStatus.value =
                "Sharp canopy eyes!"

            if (updated.isCompleted) {
                val score =
                    CanopySpotterEngine.computeScore(
                        difficulty,
                        updated.mistakes,
                        updated.hintsUsed
                    )
                val stars = when {
                    updated.mistakes == 0 &&
                        updated.hintsUsed == 0 -> 3
                    updated.mistakes <= 2 &&
                        updated.hintsUsed <= 1 -> 2
                    else -> 1
                }
                completePuzzle(
                    PuzzleType.CANOPY_SPOTTER,
                    score,
                    stars
                )
            }
        } else {
            val gentle =
                sanctuaryUiState.value.profile.gentleMode
            _spotterStatus.value = if (gentle) {
                "Count petals/spots, check colors & tilt carefully!"
            } else {
                "Not that charm — keep scanning the foliage!"
            }
        }
    }

    fun useSpotterHint() {
        val puzzle =
            _canopySpotterState.value ?: return
        if (
            puzzle.isCompleted ||
            _completionResult.value != null
        ) return

        _canopySpotterState.value =
            CanopySpotterEngine.applyHint(puzzle)
        _spotterStatus.value =
            "Golden spore-light highlights a matching charm!"
    }

    fun selectGridCell(index: Int) {
        val puzzle = _groveGridState.value ?: return
        if (
            puzzle.givenMask[index] ||
            _completionResult.value != null
        ) return
        _selectedGridCell.value = index
    }

    fun placeGridRune(runeId: Int) {
        val puzzle = _groveGridState.value ?: return
        val cell = _selectedGridCell.value ?: return
        if (
            puzzle.givenMask[cell] ||
            _completionResult.value != null
        ) return

        gridMoves++
        val board =
            puzzle.board.toMutableList().apply {
                this[cell] = runeId
            }

        val updated = puzzle.copy(board = board)
        _groveGridState.value = updated

        if (GroveGridEngine.isSolved(board)) {
            val score =
                GroveGridEngine.computeScore(
                    difficulty,
                    gridMoves,
                    gridHints
                )
            val stars = when {
                gridHints == 0 -> 3
                gridHints <= 2 -> 2
                else -> 1
            }
            completePuzzle(
                PuzzleType.GROVE_GRID,
                score,
                stars
            )
        } else if (runeId != 0) {
            val nextEmpty =
                board.indexOfFirst { it == 0 }
            if (nextEmpty >= 0) {
                _selectedGridCell.value = nextEmpty
            }
        }
    }

    fun useGridHint() {
        val puzzle = _groveGridState.value ?: return
        if (_completionResult.value != null) return

        val targetCell =
            _selectedGridCell.value
                ?.takeIf {
                    !puzzle.givenMask[it] &&
                    puzzle.board[it] != puzzle.solution[it]
                }
                ?: puzzle.board.indices.firstOrNull {
                    !puzzle.givenMask[it] &&
                    puzzle.board[it] != puzzle.solution[it]
                }
                ?: return

        gridHints++
        val board = puzzle.board.toMutableList().apply {
            this[targetCell] =
                puzzle.solution[targetCell]
        }

        _groveGridState.value =
            puzzle.copy(board = board)
        _selectedGridCell.value =
            board.indexOfFirst { it == 0 }
                .takeIf { it >= 0 }

        if (GroveGridEngine.isSolved(board)) {
            val score =
                GroveGridEngine.computeScore(
                    difficulty,
                    gridMoves,
                    gridHints
                )
            val stars = when {
                gridHints == 0 -> 3
                gridHints <= 2 -> 2
                else -> 1
            }
            completePuzzle(
                PuzzleType.GROVE_GRID,
                score,
                stars
            )
        }
    }

    fun rotateVineTile(row: Int, col: Int) {
        val board = _vineWeaverState.value ?: return
        if (
            board.moonflowerBloomed ||
            _completionResult.value != null
        ) return

        vineTaps++
        val updated =
            VineWeaverEngine.rotateTile(board, row, col)
        _vineWeaverState.value = updated

        if (updated.moonflowerBloomed) {
            val score =
                VineWeaverEngine.computeScore(
                    difficulty,
                    vineTaps,
                    vineHints
                )
            val stars = when {
                vineHints == 0 -> 3
                vineHints <= 2 -> 2
                else -> 1
            }
            completePuzzle(
                PuzzleType.VINE_WEAVER,
                score,
                stars
            )
        }
    }

    fun useVineHint() {
        val board = _vineWeaverState.value ?: return
        if (
            board.moonflowerBloomed ||
            _completionResult.value != null
        ) return

        vineHints++
        val updated =
            VineWeaverEngine.applyHint(board)
        _vineWeaverState.value = updated

        if (updated.moonflowerBloomed) {
            val score =
                VineWeaverEngine.computeScore(
                    difficulty,
                    vineTaps,
                    vineHints
                )
            val stars = when {
                vineHints == 0 -> 3
                vineHints <= 2 -> 2
                else -> 1
            }
            completePuzzle(
                PuzzleType.VINE_WEAVER,
                score,
                stars
            )
        }
    }

    fun appendDraftRune(rune: SpiritRune) {
        val puzzle = _spiritLockState.value ?: return
        if (
            puzzle.isSolved ||
            puzzle.isOutOfAttempts ||
            _completionResult.value != null
        ) return
        if (
            puzzle.currentDraft.size >=
            puzzle.secretCode.size
        ) return

        _spiritLockState.value =
            puzzle.copy(
                currentDraft =
                    puzzle.currentDraft + rune
            )
    }

    fun removeLastDraftRune() {
        val puzzle = _spiritLockState.value ?: return
        if (
            puzzle.currentDraft.isEmpty() ||
            puzzle.isSolved
        ) return

        _spiritLockState.value =
            puzzle.copy(
                currentDraft =
                    puzzle.currentDraft.dropLast(1)
            )
    }

    fun submitSpiritDraft() {
        val puzzle = _spiritLockState.value ?: return
        if (
            puzzle.currentDraft.size !=
            puzzle.secretCode.size
        ) return

        val updated =
            SpiritLockEngine.submitDraft(puzzle)
        _spiritLockState.value = updated

        if (updated.isSolved) {
            val score =
                SpiritLockEngine.computeScore(
                    difficulty,
                    updated.attempts.size,
                    lockHints
                )
            val stars = when {
                lockHints == 0 &&
                    updated.attempts.size <= 5 -> 3
                lockHints <= 1 -> 2
                else -> 1
            }
            completePuzzle(
                PuzzleType.SPIRIT_LOCK,
                score,
                stars
            )
        } else if (
            updated.isOutOfAttempts &&
            sanctuaryUiState.value.profile.gentleMode
        ) {
            _spiritLockState.value =
                updated.copy(
                    maxAttempts = updated.maxAttempts + 2,
                    isOutOfAttempts = false
                )
        }
    }

    fun useSpiritHint() {
        val puzzle = _spiritLockState.value ?: return
        if (
            puzzle.isSolved ||
            _completionResult.value != null
        ) return

        lockHints++
        _spiritLockState.value =
            SpiritLockEngine.revealHint(puzzle)
    }

    private fun completePuzzle(
        type: PuzzleType,
        rawScore: Int,
        starsEarned: Int
    ) {
        viewModelScope.launch {
            runRepositoryAction {
                val result =
                    repository.recordPuzzleSolvedLocally(
                        userId = currentUserId,
                        puzzleType = type,
                        difficulty = difficulty,
                        levelPlayed =
                            _activePuzzleLevel.value,
                        starsEarned = starsEarned,
                        rawScore = rawScore,
                        cloudStats =
                            sanctuaryUiState.value
                                .puzzleStats[type]
                    )
                _completionResult.value = result
            }
        }
    }

    private suspend fun runRepositoryAction(
        block: suspend () -> Unit
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Puzzle update failed", e)
        }
    }

    fun cancelPlayback() {
        playbackJob?.cancel()
    }

    companion object {
        private const val TAG = "PuzzleVM"
    }
}