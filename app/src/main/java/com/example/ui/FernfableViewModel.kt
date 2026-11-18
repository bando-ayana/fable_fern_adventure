package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.model.BiomeNode
import com.example.model.CommunityMission
import com.example.model.DifficultyTier
import com.example.model.LootItemType
import com.example.model.MissionCompletionSummary
import com.example.model.PuzzleCompletionResult
import com.example.model.PuzzleType
import com.example.model.StoryChoice
import com.example.puzzle.CanopySpotterEngine
import com.example.puzzle.CanopySpotterPuzzle
import com.example.puzzle.FernMosaicEngine
import com.example.puzzle.FernMosaicPuzzle
import com.example.puzzle.FireflyEchoEngine
import com.example.puzzle.GroveGridEngine
import com.example.puzzle.GroveGridPuzzle
import com.example.puzzle.LeafToken
import com.example.puzzle.MushroomMelodyEngine
import com.example.puzzle.MushroomMelodyPuzzle
import com.example.puzzle.SpiritLockEngine
import com.example.puzzle.SpiritLockPuzzle
import com.example.puzzle.SpiritRune
import com.example.puzzle.VineWeaverBoard
import com.example.puzzle.VineWeaverEngine
import com.example.rpg.BattleActionType
import com.example.rpg.BattlePhase
import com.example.rpg.InteractableKind
import com.example.rpg.IntroStage
import com.example.rpg.IntroTutorialEngine
import com.example.rpg.IntroTutorialState
import com.example.rpg.RpgBattleEngine
import com.example.rpg.RpgBattleState
import com.example.rpg.RpgWorldEngine
import com.example.rpg.RpgWorldState
import com.example.rpg.WorldToast
import com.example.stateHolder.FernfableUiState
import com.example.stateHolder.FireflyEchoUiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

class FernfableViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val userDisplayName: String? = null,
    private val userEmail: String? = null
) : ViewModel() {
    private val _pendingDatabaseRetry = MutableStateFlow<DatabaseRetryPrompt?>(null)
    val pendingDatabaseRetry: StateFlow<DatabaseRetryPrompt?> =
        _pendingDatabaseRetry.asStateFlow()

    private val retryPromptMutex = Mutex()
    private var retryDecision: CompletableDeferred<Boolean>? = null
    private val _selectedDifficulty = MutableStateFlow(DifficultyTier.SPROUT)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FernfableUiState> = combine(
        repository.observeProfile(currentUserId),
        repository.observeCreatures(currentUserId),
        repository.observePuzzleStats(currentUserId),
        _selectedDifficulty,
        _snackbarMessage
    ) { profile, creatures, stats, diff, snackbar ->
        FernfableUiState(
            profile = profile,
            creatureProgress = creatures.associateBy { it.creatureId },
            puzzleStats = stats.associateBy { PuzzleType.fromId(it.puzzleType) },
            selectedDifficulty = diff,
            snackbarMessage = snackbar
        )
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing sanctuary data", rootError)
        emit(FernfableUiState(syncError = rootError.message ?: "Failed to sync sanctuary data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FernfableUiState()
    )

    // Story Encounter state
    private val _activeEncounter = MutableStateFlow<BiomeNode?>(null)
    val activeEncounter: StateFlow<BiomeNode?> = _activeEncounter.asStateFlow()

    private val _encounterFeedback = MutableStateFlow<StoryChoice?>(null)
    val encounterFeedback: StateFlow<StoryChoice?> = _encounterFeedback.asStateFlow()

    // Active Puzzle session states
    private val _activePuzzleType = MutableStateFlow(PuzzleType.FIREFLY_ECHO)
    val activePuzzleType: StateFlow<PuzzleType> = _activePuzzleType.asStateFlow()

    private val _activePuzzleLevel = MutableStateFlow(1)
    val activePuzzleLevel: StateFlow<Int> = _activePuzzleLevel.asStateFlow()

    private val _fireflyState = MutableStateFlow(FireflyEchoUiState())
    val fireflyState: StateFlow<FireflyEchoUiState> = _fireflyState.asStateFlow()

    private val _mushroomState = MutableStateFlow<MushroomMelodyPuzzle?>(null)
    val mushroomState: StateFlow<MushroomMelodyPuzzle?> = _mushroomState.asStateFlow()
    private val _mushroomSelectedSlot = MutableStateFlow<Int?>(null)
    val mushroomSelectedSlot: StateFlow<Int?> = _mushroomSelectedSlot.asStateFlow()
    private var mushroomSwaps = 0
    private var mushroomHints = 0

    private val _fernMosaicState = MutableStateFlow<FernMosaicPuzzle?>(null)
    val fernMosaicState: StateFlow<FernMosaicPuzzle?> = _fernMosaicState.asStateFlow()
    private val _mosaicStatus = MutableStateFlow<String?>(null)
    val mosaicStatus: StateFlow<String?> = _mosaicStatus.asStateFlow()

    private val _canopySpotterState = MutableStateFlow<CanopySpotterPuzzle?>(null)
    val canopySpotterState: StateFlow<CanopySpotterPuzzle?> = _canopySpotterState.asStateFlow()
    private val _spotterStatus = MutableStateFlow<String?>(null)
    val spotterStatus: StateFlow<String?> = _spotterStatus.asStateFlow()

    private val _groveGridState = MutableStateFlow<GroveGridPuzzle?>(null)
    val groveGridState: StateFlow<GroveGridPuzzle?> = _groveGridState.asStateFlow()
    private val _selectedGridCell = MutableStateFlow<Int?>(null)
    val selectedGridCell: StateFlow<Int?> = _selectedGridCell.asStateFlow()
    private var gridMoves = 0
    private var gridHints = 0

    private val _vineWeaverState = MutableStateFlow<VineWeaverBoard?>(null)
    val vineWeaverState: StateFlow<VineWeaverBoard?> = _vineWeaverState.asStateFlow()
    private var vineTaps = 0
    private var vineHints = 0

    private val _spiritLockState = MutableStateFlow<SpiritLockPuzzle?>(null)
    val spiritLockState: StateFlow<SpiritLockPuzzle?> = _spiritLockState.asStateFlow()
    private var lockHints = 0

    private val _completionResult = MutableStateFlow<PuzzleCompletionResult?>(null)
    val completionResult: StateFlow<PuzzleCompletionResult?> = _completionResult.asStateFlow()

    private val _isIntroShrinePuzzle = MutableStateFlow(false)
    val isIntroShrinePuzzle: StateFlow<Boolean> = _isIntroShrinePuzzle.asStateFlow()

    private val _introCompletedLocally = MutableStateFlow(false)
    val introCompletedLocally: StateFlow<Boolean> = _introCompletedLocally.asStateFlow()

    // RPG Community Missions & Wild Creature Battle state
    private val _activeBattle = MutableStateFlow<RpgBattleState?>(null)
    val activeBattle: StateFlow<RpgBattleState?> = _activeBattle.asStateFlow()

    private val _missionSummary = MutableStateFlow<MissionCompletionSummary?>(null)
    val missionSummary: StateFlow<MissionCompletionSummary?> = _missionSummary.asStateFlow()

    private val _worldState = MutableStateFlow(RpgWorldState())
    val worldState: StateFlow<RpgWorldState> = _worldState.asStateFlow()

    private val _isRecordingMissionRewards = MutableStateFlow(false)
    val isRecordingMissionRewards = _isRecordingMissionRewards.asStateFlow()

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded: StateFlow<Boolean> = _isProfileLoaded.asStateFlow()

    private val _introState = MutableStateFlow(IntroTutorialState())
    val introState: StateFlow<IntroTutorialState> = _introState.asStateFlow()
    private var playbackJob: kotlinx.coroutines.Job? = null

    init {
        repository.requestManualRetry = ::requestDatabaseRetry

        viewModelScope.launch {
            try {
                repository.ensureSeeded(currentUserId, userDisplayName, userEmail)
                val streakEval = repository.recordDailyActivity(currentUserId)
                if (streakEval.didAdvanceStreak && streakEval.rewardMessage != null) {
                    _snackbarMessage.value = streakEval.rewardMessage
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Initial sanctuary seed failed", e)
                _snackbarMessage.value = e.localizedMessage ?: "Couldn't initialize cloud sanctuary."
            } finally {
                _isProfileLoaded.value = true
            }
        }
    }

    fun setDifficulty(tier: DifficultyTier) {
        _selectedDifficulty.value = tier
    }

    fun setGentleMode(enabled: Boolean) {
        viewModelScope.launch {
            runRepositoryAction {
                repository.setGentleMode(currentUserId, enabled)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun selectHero(heroId: String) {
        viewModelScope.launch {
            val hero = JungleLoreCatalog.heroById(heroId)
            if (uiState.value.profile.nectar >= hero.unlockNectar) {
                runRepositoryAction {
                    repository.selectHero(currentUserId, heroId)
                    _snackbarMessage.value = "Now adventuring as ${hero.name} (${hero.title})!"
                }
            } else {
                _snackbarMessage.value = "Gather ${hero.unlockNectar} Sunbeam Nectar to welcome ${hero.name}!"
            }
        }
    }

    fun craftOrEquipRelic(relicId: String) {
        viewModelScope.launch {
            runRepositoryAction {
                _snackbarMessage.value = repository.craftOrEquipRelic(currentUserId, relicId)
            }
        }
    }

    fun openBiomeEncounter(biome: BiomeNode) {
        _encounterFeedback.value = null
        _activeEncounter.value = biome
    }

    fun dismissEncounter() {
        _activeEncounter.value = null
        _encounterFeedback.value = null
    }

    fun chooseStoryOption(biome: BiomeNode, choice: StoryChoice) {
        viewModelScope.launch {
            runRepositoryAction {
                repository.recordStoryChoice(
                    userId = currentUserId,
                    biomeId = biome.id,
                    creatureId = biome.creatureId,
                    rewardNectar = choice.rewardNectar,
                    bondGain = choice.bondGain,
                    rewardExp = choice.rewardExp
                )
                _encounterFeedback.value = choice
            }
        }
    }

    fun feedCreature(creatureId: String) {
        viewModelScope.launch {
            val creature = JungleLoreCatalog.creatureById(creatureId)
            runRepositoryAction {
                val fed = repository.feedCreatureTreat(currentUserId, creatureId, nectarCost = 10)
                _snackbarMessage.value = if (fed) {
                    "${creature.name} happily nibbles ${creature.favoriteTreat}! Bond deepened & +15 EXP."
                } else {
                    "Gather 10 Sunbeam Nectar from puzzles or story quests to share a treat."
                }
            }
        }
    }

    private suspend fun runRepositoryAction(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Sanctuary update failed", e)
            _snackbarMessage.value = e.localizedMessage ?: "Cloud sync failed. Please try again."
        }
    }

    fun startPuzzle(
        type: PuzzleType,
        levelOverride: Int? = null,
        accessedFromRpg: Boolean = true
    ) {
        playbackJob?.cancel()
        _activePuzzleType.value = type
        _completionResult.value = null
        val diff = _selectedDifficulty.value
        val existingStats = uiState.value.puzzleStats[type]
        val currentSavedLevel = existingStats?.currentLevel ?: 1
        val level = (levelOverride ?: currentSavedLevel).coerceAtLeast(1)
        _activePuzzleLevel.value = level

        _isIntroShrinePuzzle.value =
            type == PuzzleType.FIREFLY_ECHO &&
                    !uiState.value.profile.introCompleted &&
                    _introState.value.stage == IntroStage.LESSON_PUZZLE_SHRINE

        if (accessedFromRpg && existingStats?.accessed != true) {
            viewModelScope.launch {
                runRepositoryAction {
                    repository.markPuzzleAccessed(currentUserId, type)
                }
            }
        }

        when (type) {
            PuzzleType.FIREFLY_ECHO -> {
                val rounds = FireflyEchoEngine.createRounds(diff, level)
                _fireflyState.value = FireflyEchoUiState(
                    rounds = rounds,
                    currentRoundIdx = 0,
                    statusBanner = "Level $level — Watch the lantern blossoms glow..."
                )
                playFireflySequence()
            }

            PuzzleType.MUSHROOM_MELODY -> {
                mushroomSwaps = 0
                mushroomHints = 0
                _mushroomSelectedSlot.value = null
                _mushroomState.value = MushroomMelodyEngine.generate(diff, level)
            }

            PuzzleType.FERN_MOSAIC -> {
                _mosaicStatus.value = null
                _fernMosaicState.value = FernMosaicEngine.generate(diff, level)
            }

            PuzzleType.CANOPY_SPOTTER -> {
                _spotterStatus.value = null
                _canopySpotterState.value = CanopySpotterEngine.generate(diff, level)
            }

            PuzzleType.GROVE_GRID -> {
                gridMoves = 0
                gridHints = 0
                val puzzle = GroveGridEngine.generate(diff, level)
                _groveGridState.value = puzzle
                _selectedGridCell.value = puzzle.board.indexOfFirst { it == 0 }.takeIf { it >= 0 }
            }

            PuzzleType.VINE_WEAVER -> {
                vineTaps = 0
                vineHints = 0
                _vineWeaverState.value = VineWeaverEngine.generate(diff, level)
            }

            PuzzleType.SPIRIT_LOCK -> {
                lockHints = 0
                _spiritLockState.value = SpiritLockEngine.generate(diff, level)
            }
        }
    }

    fun dismissCompletion() {
        _completionResult.value = null
        _isIntroShrinePuzzle.value = false
    }

    // --- 1. Firefly Echo Actions ---

    private fun playFireflySequence() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val round = _fireflyState.value.currentRound ?: return@launch
            _fireflyState.update {
                it.copy(
                    isPlayingSequence = true,
                    userInput = emptyList(),
                    activeBlossomIdx = null,
                    errorFlashIdx = null,
                    statusBanner = "Round ${round.roundNumber} of ${round.totalRounds} — Watch closely!"
                )
            }
            delay(450L.milliseconds)
            val pulseMs = when (_selectedDifficulty.value) {
                DifficultyTier.SPROUT -> 460L
                DifficultyTier.EXPLORER -> 380L
                DifficultyTier.ELDER -> 310L
            }
            for (blossomIdx in round.sequence) {
                _fireflyState.update { it.copy(activeBlossomIdx = blossomIdx) }
                delay(pulseMs.milliseconds)
                _fireflyState.update { it.copy(activeBlossomIdx = null) }
                delay(180L.milliseconds)
            }
            _fireflyState.update {
                it.copy(
                    isPlayingSequence = false,
                    statusBanner = "Your turn! Tap ${round.sequence.size} blossoms in order."
                )
            }
        }
    }

    fun replayFireflyHint() {
        if (_fireflyState.value.isPlayingSequence || _completionResult.value != null) return
        _fireflyState.update { it.copy(hintsUsed = it.hintsUsed + 1) }
        playFireflySequence()
    }

    fun tapFireflyBlossom(index: Int) {
        val state = _fireflyState.value
        if (state.isPlayingSequence || _completionResult.value != null) return
        val round = state.currentRound ?: return

        val expectedIdx = round.sequence.getOrNull(state.userInput.size) ?: return
        if (index == expectedIdx) {
            val nextInput = state.userInput + index
            viewModelScope.launch {
                _fireflyState.update { it.copy(activeBlossomIdx = index) }
                delay(160L.milliseconds)
                _fireflyState.update { it.copy(activeBlossomIdx = null) }
            }

            if (nextInput.size == round.sequence.size) {
                if (state.currentRoundIdx + 1 >= state.rounds.size) {
                    val finalScore = FireflyEchoEngine.computeScore(
                        difficulty = _selectedDifficulty.value,
                        mistakes = state.mistakes,
                        hintsUsed = state.hintsUsed
                    )
                    _fireflyState.update {
                        it.copy(
                            userInput = nextInput,
                            statusBanner = "Harmony achieved! The fireflies shimmer!"
                        )
                    }
                    val stars = when {
                        state.mistakes == 0 && state.hintsUsed == 0 -> 3
                        state.mistakes <= 2 && state.hintsUsed <= 1 -> 2
                        else -> 1
                    }
                    completeActivePuzzle(PuzzleType.FIREFLY_ECHO, finalScore, stars)
                } else {
                    _fireflyState.update {
                        it.copy(
                            currentRoundIdx = it.currentRoundIdx + 1,
                            userInput = emptyList(),
                            statusBanner = "Lovely! Next lantern song..."
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
                        statusBanner = "${nextInput.size} / ${round.sequence.size} echoed"
                    )
                }
            }
        } else {
            val isGentle = uiState.value.profile.gentleMode
            val newMistakes = state.mistakes + 1
            viewModelScope.launch {
                _fireflyState.update {
                    it.copy(
                        mistakes = newMistakes,
                        errorFlashIdx = index,
                        userInput = emptyList(),
                        statusBanner = if (isGentle) {
                            "Oops, gentle breeze! Watch the blossoms again."
                        } else {
                            "Missed a petal! Replaying sequence..."
                        }
                    )
                }
                delay(550L.milliseconds)
                _fireflyState.update { it.copy(errorFlashIdx = null) }
                playFireflySequence()
            }
        }
    }

    // --- 2. Mushroom Melody Actions ---

    fun tapMushroomSlot(slotIdx: Int) {
        val puzzle = _mushroomState.value ?: return
        if (puzzle.isSolved || _completionResult.value != null) return
        val selected = _mushroomSelectedSlot.value
        if (selected == null) {
            _mushroomSelectedSlot.value = slotIdx
        } else if (selected == slotIdx) {
            _mushroomSelectedSlot.value = null
        } else {
            mushroomSwaps++
            val updated = MushroomMelodyEngine.swap(puzzle, selected, slotIdx)
            _mushroomState.value = updated
            _mushroomSelectedSlot.value = null
            if (updated.isSolved) {
                val score = MushroomMelodyEngine.computeScore(
                    difficulty = _selectedDifficulty.value,
                    swaps = mushroomSwaps,
                    hintsUsed = mushroomHints
                )
                val stars = when {
                    mushroomHints == 0 -> 3
                    mushroomHints <= 1 -> 2
                    else -> 1
                }
                completeActivePuzzle(PuzzleType.MUSHROOM_MELODY, score, stars)
            }
        }
    }

    fun useMushroomHint() {
        val puzzle = _mushroomState.value ?: return
        if (puzzle.isSolved || _completionResult.value != null) return
        mushroomHints++
        val updated = MushroomMelodyEngine.applyHint(puzzle)
        _mushroomState.value = updated
        _mushroomSelectedSlot.value = null
        if (updated.isSolved) {
            val score = MushroomMelodyEngine.computeScore(
                difficulty = _selectedDifficulty.value,
                swaps = mushroomSwaps,
                hintsUsed = mushroomHints
            )
            val stars = if (mushroomHints <= 1) 2 else 1
            completeActivePuzzle(PuzzleType.MUSHROOM_MELODY, score, stars)
        }
    }

    // --- 3. Fern Mosaic Actions ---

    fun selectMosaicSlot(slotIdx: Int) {
        val puzzle = _fernMosaicState.value ?: return
        val round = puzzle.currentRound ?: return
        if (slotIdx !in round.missingIndices || slotIdx in round.placedAnswers || _completionResult.value != null) return
        _fernMosaicState.value = puzzle.copy(selectedSlotIdx = slotIdx)
    }

    fun chooseMosaicTile(token: LeafToken) {
        val puzzle = _fernMosaicState.value ?: return
        if (puzzle.isCompleted || _completionResult.value != null) return
        val (updated, matched) = FernMosaicEngine.selectChoice(puzzle, token)
        _fernMosaicState.value = updated
        if (matched) {
            _mosaicStatus.value = "Lovely botanical match!"
            if (updated.isCompleted) {
                val score = FernMosaicEngine.computeScore(
                    difficulty = _selectedDifficulty.value,
                    mistakes = updated.mistakes,
                    hintsUsed = updated.hintsUsed
                )
                val stars = when {
                    updated.mistakes == 0 && updated.hintsUsed == 0 -> 3
                    updated.mistakes <= 2 && updated.hintsUsed <= 1 -> 2
                    else -> 1
                }
                completeActivePuzzle(PuzzleType.FERN_MOSAIC, score, stars)
            }
        } else {
            val gentle = uiState.value.profile.gentleMode
            _mosaicStatus.value = if (gentle) {
                "Study the repeating shape and color rhythm along the branch!"
            } else {
                "Not quite — check the next leaf pattern."
            }
        }
    }

    fun useMosaicHint() {
        val puzzle = _fernMosaicState.value ?: return
        if (puzzle.isCompleted || _completionResult.value != null) return
        val updated = FernMosaicEngine.applyHint(puzzle)
        _fernMosaicState.value = updated
        _mosaicStatus.value = "A glowing firefly placed a matching leaf!"
        if (updated.isCompleted) {
            val score = FernMosaicEngine.computeScore(
                difficulty = _selectedDifficulty.value,
                mistakes = updated.mistakes,
                hintsUsed = updated.hintsUsed
            )
            val stars = if (updated.hintsUsed <= 1 && updated.mistakes <= 2) 2 else 1
            completeActivePuzzle(PuzzleType.FERN_MOSAIC, score, stars)
        }
    }

    // --- 4. Canopy Spotter Actions ---

    fun tapSpotterCharm(charmId: Int) {
        val puzzle = _canopySpotterState.value ?: return
        if (puzzle.isCompleted || _completionResult.value != null) return
        val (updated, matched) = CanopySpotterEngine.tapCharm(puzzle, charmId)
        _canopySpotterState.value = updated
        if (matched) {
            _spotterStatus.value = "Sharp canopy eyes!"
            if (updated.isCompleted) {
                val score = CanopySpotterEngine.computeScore(
                    difficulty = _selectedDifficulty.value,
                    mistakes = updated.mistakes,
                    hintsUsed = updated.hintsUsed
                )
                val stars = when {
                    updated.mistakes == 0 && updated.hintsUsed == 0 -> 3
                    updated.mistakes <= 2 && updated.hintsUsed <= 1 -> 2
                    else -> 1
                }
                completeActivePuzzle(PuzzleType.CANOPY_SPOTTER, score, stars)
            }
        } else {
            val gentle = uiState.value.profile.gentleMode
            _spotterStatus.value = if (gentle) {
                "Count petals/spots, check colors & tilt carefully!"
            } else {
                "Not that charm — keep scanning the foliage!"
            }
        }
    }

    fun useSpotterHint() {
        val puzzle = _canopySpotterState.value ?: return
        if (puzzle.isCompleted || _completionResult.value != null) return
        _canopySpotterState.value = CanopySpotterEngine.applyHint(puzzle)
        _spotterStatus.value = "Golden spore-light highlights a matching charm!"
    }

    // --- 5. Grove Grid Actions ---

    fun selectGridCell(index: Int) {
        val puzzle = _groveGridState.value ?: return
        if (puzzle.givenMask[index] || _completionResult.value != null) return
        _selectedGridCell.value = index
    }

    fun placeGridRune(runeId: Int) {
        val puzzle = _groveGridState.value ?: return
        val cell = _selectedGridCell.value ?: return
        if (puzzle.givenMask[cell] || _completionResult.value != null) return

        gridMoves++
        val newBoard = puzzle.board.toMutableList().apply { this[cell] = runeId }
        val updated = puzzle.copy(board = newBoard)
        _groveGridState.value = updated

        if (GroveGridEngine.isSolved(newBoard)) {
            val score = GroveGridEngine.computeScore(_selectedDifficulty.value, gridMoves, gridHints)
            val stars = when {
                gridHints == 0 -> 3
                gridHints <= 2 -> 2
                else -> 1
            }
            completeActivePuzzle(PuzzleType.GROVE_GRID, score, stars)
        } else if (runeId != 0) {
            val nextEmpty = newBoard.indexOfFirst { it == 0 }
            if (nextEmpty >= 0) _selectedGridCell.value = nextEmpty
        }
    }

    fun useGridHint() {
        val puzzle = _groveGridState.value ?: return
        if (_completionResult.value != null) return
        val targetCell = _selectedGridCell.value
            ?.takeIf { !puzzle.givenMask[it] && puzzle.board[it] != puzzle.solution[it] }
            ?: puzzle.board.indices.firstOrNull { !puzzle.givenMask[it] && puzzle.board[it] != puzzle.solution[it] }
            ?: return

        gridHints++
        val newBoard = puzzle.board.toMutableList().apply { this[targetCell] = puzzle.solution[targetCell] }
        _groveGridState.value = puzzle.copy(board = newBoard)
        _selectedGridCell.value = newBoard.indexOfFirst { it == 0 }.takeIf { it >= 0 }

        if (GroveGridEngine.isSolved(newBoard)) {
            val score = GroveGridEngine.computeScore(_selectedDifficulty.value, gridMoves, gridHints)
            val stars = when {
                gridHints == 0 -> 3
                gridHints <= 2 -> 2
                else -> 1
            }
            completeActivePuzzle(PuzzleType.GROVE_GRID, score, stars)
        }
    }

    // --- 6. Vine Weaver Actions ---

    fun rotateVineTile(row: Int, col: Int) {
        val board = _vineWeaverState.value ?: return
        if (board.moonflowerBloomed || _completionResult.value != null) return

        vineTaps++
        val updated = VineWeaverEngine.rotateTile(board, row, col)
        _vineWeaverState.value = updated

        if (updated.moonflowerBloomed) {
            val score = VineWeaverEngine.computeScore(_selectedDifficulty.value, vineTaps, vineHints)
            val stars = when {
                vineHints == 0 -> 3
                vineHints <= 2 -> 2
                else -> 1
            }
            completeActivePuzzle(PuzzleType.VINE_WEAVER, score, stars)
        }
    }

    fun useVineHint() {
        val board = _vineWeaverState.value ?: return
        if (board.moonflowerBloomed || _completionResult.value != null) return

        vineHints++
        val updated = VineWeaverEngine.applyHint(board)
        _vineWeaverState.value = updated

        if (updated.moonflowerBloomed) {
            val score = VineWeaverEngine.computeScore(_selectedDifficulty.value, vineTaps, vineHints)
            val stars = when {
                vineHints == 0 -> 3
                vineHints <= 2 -> 2
                else -> 1
            }
            completeActivePuzzle(PuzzleType.VINE_WEAVER, score, stars)
        }
    }

    // --- 7. Spirit Lock Actions ---

    fun appendDraftRune(rune: SpiritRune) {
        val puzzle = _spiritLockState.value ?: return
        if (puzzle.isSolved || puzzle.isOutOfAttempts || _completionResult.value != null) return
        if (puzzle.currentDraft.size >= puzzle.secretCode.size) return
        _spiritLockState.value = puzzle.copy(currentDraft = puzzle.currentDraft + rune)
    }

    fun removeLastDraftRune() {
        val puzzle = _spiritLockState.value ?: return
        if (puzzle.currentDraft.isEmpty() || puzzle.isSolved) return
        _spiritLockState.value = puzzle.copy(currentDraft = puzzle.currentDraft.dropLast(1))
    }

    fun submitSpiritDraft() {
        val puzzle = _spiritLockState.value ?: return
        if (puzzle.currentDraft.size != puzzle.secretCode.size) return

        val updated = SpiritLockEngine.submitDraft(puzzle)
        _spiritLockState.value = updated

        if (updated.isSolved) {
            val score = SpiritLockEngine.computeScore(
                _selectedDifficulty.value,
                updated.attempts.size,
                lockHints
            )
            val stars = when {
                lockHints == 0 && updated.attempts.size <= 5 -> 3
                lockHints <= 1 -> 2
                else -> 1
            }
            completeActivePuzzle(PuzzleType.SPIRIT_LOCK, score, stars)
        } else if (updated.isOutOfAttempts && uiState.value.profile.gentleMode) {
            _spiritLockState.value = updated.copy(
                maxAttempts = updated.maxAttempts + 2,
                isOutOfAttempts = false
            )
            _snackbarMessage.value = "Gentle Breeze granted 2 extra guesses!"
        }
    }

    fun useSpiritHint() {
        val puzzle = _spiritLockState.value ?: return
        if (puzzle.isSolved || _completionResult.value != null) return
        lockHints++
        _spiritLockState.value = SpiritLockEngine.revealHint(puzzle)
    }

    private fun completeActivePuzzle(type: PuzzleType, rawScore: Int, starsEarned: Int) {
        viewModelScope.launch {
            runRepositoryAction {
                val result = repository.recordPuzzleSolvedLocally(
                    userId = currentUserId,
                    puzzleType = type,
                    difficulty = _selectedDifficulty.value,
                    levelPlayed = _activePuzzleLevel.value,
                    starsEarned = starsEarned,
                    rawScore = rawScore,
                    cloudStats = uiState.value.puzzleStats[type]
                )
                _completionResult.value = result
                if (!uiState.value.profile.introCompleted &&
                    _introState.value.stage == IntroStage.LESSON_PUZZLE_SHRINE
                ) {
                    _introState.update { cur ->
                        val nextStage = IntroTutorialEngine.computeStage(
                            inCutscene = false,
                            lanternLit = cur.lanternLit,
                            shrinePuzzleSolved = true,
                            sproutlingDefeated = cur.sproutlingDefeated
                        )
                        val seq = cur.worldState.toastSeq + 1
                        cur.copy(
                            stage = nextStage,
                            shrinePuzzleSolved = true,
                            worldState = cur.worldState.copy(
                                toastSeq = seq,
                                worldToast = WorldToast(
                                    id = seq,
                                    text = "Shrine unlocked! Now head south to spar Mossback Sproutling!",
                                    x = cur.worldState.heroX,
                                    y = cur.worldState.heroY
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    // --- Opening Cutscene & Introductory Training Level ---

    fun setCutscenePage(page: Int) {
        _introState.update { it.copy(cutscenePage = page.coerceIn(0, 3)) }
    }

    fun beginIntroTrainingLevel() {
        _introState.value = IntroTutorialState(
            stage = IntroStage.LESSON_MOVE_AND_LANTERN,
            cutscenePage = 3,
            worldState = IntroTutorialEngine.initialWorldState(),
            lanternLit = false,
            shrinePuzzleSolved = false,
            sproutlingDefeated = false
        )
    }

    fun skipIntroTutorial() {
        _introCompletedLocally.value = true

        viewModelScope.launch {
            runRepositoryAction {
                repository.markIntroCompleted(currentUserId, bonusNectar = 0)
            }
        }
    }

    fun completeIntroGraduation() {
        _introCompletedLocally.value = true

        viewModelScope.launch {
            runRepositoryAction {
                repository.markIntroCompleted(currentUserId, bonusNectar = 20)
                _snackbarMessage.value = "Welcome to Solari Canopy! +20 Starter Sunbeam Nectar awarded."
            }
        }
    }

    fun replayIntroTutorial() {
        _introState.value = IntroTutorialState(
            stage = IntroStage.CUTSCENE,
            cutscenePage = 0,
            worldState = IntroTutorialEngine.initialWorldState()
        )
        viewModelScope.launch {
            runRepositoryAction {
                repository.resetIntroTutorial(currentUserId)
            }
        }
    }

    fun stepIntroWorldFrame(joyX: Float, joyY: Float, deltaSec: Float) {
        if (_activeBattle.value != null) return
        val current = _introState.value
        val (nextWorld, engaged) = IntroTutorialEngine.stepWorld(current, joyX, joyY, deltaSec)
        _introState.update { it.copy(worldState = nextWorld) }
        if (engaged != null) {
            startMission(RpgMissionCatalog.introMission, skipPrepToCombat = true)
        }
    }

    fun setIntroTapTarget(x: Float, y: Float) {
        val clampedX = x.coerceIn(0.6f, IntroTutorialEngine.MAP_WIDTH - 0.6f)
        val clampedY = y.coerceIn(0.6f, IntroTutorialEngine.MAP_HEIGHT - 0.6f)
        _introState.update { cur ->
            cur.copy(
                worldState = cur.worldState.copy(
                    tapTarget = clampedX to clampedY,
                    dialogueSpeaker = null,
                    dialogueBody = null
                )
            )
        }
    }

    fun showIntroDialogue(speaker: String, body: String) {
        _introState.update { cur ->
            cur.copy(
                worldState = cur.worldState.copy(
                    tapTarget = null,
                    dialogueSpeaker = speaker,
                    dialogueBody = body
                )
            )
        }
    }

    fun dismissIntroDialogue() {
        _introState.update { cur ->
            cur.copy(
                worldState = cur.worldState.copy(
                    dialogueSpeaker = null,
                    dialogueBody = null
                )
            )
        }
    }

    fun postIntroToast(text: String) {
        _introState.update { cur ->
            val seq = cur.worldState.toastSeq + 1
            cur.copy(
                worldState = cur.worldState.copy(
                    toastSeq = seq,
                    worldToast = WorldToast(
                        id = seq,
                        text = text,
                        x = cur.worldState.heroX,
                        y = cur.worldState.heroY
                    )
                )
            )
        }
    }

    fun interactWithIntroLantern(lanternId: String, title: String) {
        val current = _introState.value
        val alreadyLit = lanternId in current.worldState.litLanternIds
        _introState.update { cur ->
            val nextStage = IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = true,
                shrinePuzzleSolved = cur.shrinePuzzleSolved,
                sproutlingDefeated = cur.sproutlingDefeated
            )
            cur.copy(
                stage = nextStage,
                lanternLit = true,
                worldState = cur.worldState.copy(
                    litLanternIds = cur.worldState.litLanternIds + lanternId
                )
            )
        }
        if (!alreadyLit) {
            postIntroToast("$title lit! +5 Nectar — Next: Firefly Shrine east!")
            viewModelScope.launch {
                runRepositoryAction {
                    repository.forageWorldResource(currentUserId, nectarGain = 5)
                }
            }
        } else {
            postIntroToast("$title shines brightly!")
        }
    }

    // --- RPG World Overworld Simulation & Interactions ---

    fun stepWorldFrame(joyX: Float, joyY: Float, deltaSec: Float) {
        if (_activeBattle.value != null || _activeEncounter.value != null) return
        val profile = uiState.value.profile
        val (nextState, engagedCreature) = RpgWorldEngine.stepWorld(
            state = _worldState.value,
            joyX = joyX,
            joyY = joyY,
            deltaSec = deltaSec,
            communityProsperity = profile.communityProsperity,
            completedMissions = profile.completedMissionIds
        )
        _worldState.value = nextState
        if (engagedCreature != null) {
            val mission = engagedCreature.missionId?.let { RpgMissionCatalog.missionById(it) }
            if (mission != null) {
                startMission(mission, skipPrepToCombat = true)
            }
        }
    }

    fun setWorldTapTarget(x: Float, y: Float) {
        val clampedX = x.coerceIn(0.6f, RpgWorldEngine.MAP_WIDTH - 0.6f)
        val clampedY = y.coerceIn(0.6f, RpgWorldEngine.MAP_HEIGHT - 0.6f)
        _worldState.update {
            it.copy(
                tapTarget = clampedX to clampedY,
                dialogueSpeaker = null,
                dialogueBody = null
            )
        }
    }

    fun showWorldDialogue(speaker: String, body: String) {
        _worldState.update {
            it.copy(
                tapTarget = null,
                dialogueSpeaker = speaker,
                dialogueBody = body
            )
        }
    }

    fun dismissWorldDialogue() {
        _worldState.update { it.copy(dialogueSpeaker = null, dialogueBody = null) }
    }

    fun postWorldToast(text: String) {
        _worldState.update { cur ->
            val seq = cur.toastSeq + 1
            cur.copy(
                toastSeq = seq,
                worldToast = WorldToast(id = seq, text = text, x = cur.heroX, y = cur.heroY)
            )
        }
    }

    fun interactWithWorldLantern(lanternId: String, title: String) {
        val alreadyLit = lanternId in _worldState.value.litLanternIds
        _worldState.update { it.copy(litLanternIds = it.litLanternIds + lanternId) }
        if (!alreadyLit) {
            postWorldToast("$title lit! +2 Sunbeam Nectar")
            viewModelScope.launch {
                runRepositoryAction {
                    repository.forageWorldResource(currentUserId, nectarGain = 2)
                }
            }
        } else {
            postWorldToast("$title hums softly with golden light.")
        }
    }

    fun forageWorldObject(objectId: String, kind: InteractableKind, title: String) {
        if (objectId in _worldState.value.harvestedIds) {
            postWorldToast("$title has already been harvested today.")
            return
        }
        _worldState.update { it.copy(harvestedIds = it.harvestedIds + objectId) }
        when (kind) {
            InteractableKind.HARVEST_TONIC -> {
                postWorldToast("Foraged 1 Sunberry Tonic & +3 Nectar!")
                viewModelScope.launch {
                    runRepositoryAction {
                        repository.forageWorldResource(
                            userId = currentUserId,
                            nectarGain = 3,
                            lootItem = LootItemType.SUNBERRY_TONIC
                        )
                    }
                }
            }
            else -> {
                postWorldToast("Harvested +5 Sunbeam Nectar from $title!")
                viewModelScope.launch {
                    runRepositoryAction {
                        repository.forageWorldResource(userId = currentUserId, nectarGain = 5)
                    }
                }
            }
        }
    }

    // --- RPG Community Missions & Wild Creature Battles ---

    fun startMission(mission: CommunityMission, skipPrepToCombat: Boolean = false) {
        val profile = uiState.value.profile
        if (!mission.isWildPatrol && profile.communityProsperity < mission.requiredProsperity) {
            _snackbarMessage.value = "Build ${mission.requiredProsperity}% Community Prosperity first!"
            postWorldToast("Need ${mission.requiredProsperity}% Prosperity to face ${mission.opponent.name}")
            return
        }
        val loot = RpgMissionCatalog.parseLootBag(profile.lootBagCsv)
        _missionSummary.value = null
        val initial = RpgBattleEngine.createBattleState(
            mission = mission,
            profile = profile,
            lootAvailable = loot
        )
        _activeBattle.value = if (skipPrepToCombat) {
            RpgBattleEngine.beginWildEncounter(initial)
        } else {
            initial
        }
    }

    fun advanceMissionCommunityStep() {
        val current = _activeBattle.value ?: return
        if (current.phase != BattlePhase.COMMUNITY_PREP) return
        _activeBattle.value = RpgBattleEngine.beginWildEncounter(current)
    }

    fun performBattleAction(action: BattleActionType) {
        val current = _activeBattle.value ?: return
        if (current.phase != BattlePhase.ACTIVE_COMBAT) return
        val updated = RpgBattleEngine.performAction(current, action)
        _activeBattle.value = updated
        if (updated.phase == BattlePhase.VICTORY || updated.phase == BattlePhase.RETREATED) {
            finalizeMissionBattle(updated)
        }
    }

    fun useBattleLoot(item: LootItemType) {
        val current = _activeBattle.value ?: return
        if (current.phase != BattlePhase.ACTIVE_COMBAT) return
        val updated = RpgBattleEngine.useLootItem(current, item)
        _activeBattle.value = updated
        if (updated.phase == BattlePhase.VICTORY) {
            finalizeMissionBattle(updated)
        }
    }

    fun retreatFromBattle() {
        val current = _activeBattle.value ?: return
        _worldState.update { RpgWorldEngine.applyBattleCooldownAll(it) }
        if (current.phase == BattlePhase.COMMUNITY_PREP) {
            _activeBattle.value = null
            return
        }
        val retreated = current.copy(
            phase = BattlePhase.RETREATED,
            combatLog = (listOf("${current.heroName} withdrew back to Solari Canopy!") + current.combatLog).take(6)
        )
        _activeBattle.value = retreated
        finalizeMissionBattle(retreated)
    }

    fun dismissMissionSheet() {
        _activeBattle.value = null
        _missionSummary.value = null
        _worldState.update { RpgWorldEngine.applyBattleCooldownAll(it) }
        _introState.update { cur ->
            cur.copy(worldState = RpgWorldEngine.applyBattleCooldownAll(cur.worldState))
        }
    }

    fun restManaAtSanctuary() {
        viewModelScope.launch {
            runRepositoryAction {
                val maxMana = repository.restAtSanctuary(currentUserId)
                _snackbarMessage.value = "Mana restored to $maxMana/$maxMana at the Sanctuary!"
                postWorldToast("Mana restored to $maxMana/$maxMana!")
                postIntroToast("Mana restored to $maxMana/$maxMana!")
            }
        }
    }

    fun brewBattleLoot(item: LootItemType) {
        viewModelScope.launch {
            runRepositoryAction {
                val success = repository.brewBattleLoot(currentUserId, item)
                _snackbarMessage.value = if (success) {
                    "Crafted 1 ${item.displayName} for your battle satchel!"
                } else {
                    "Need 12 Sunbeam Nectar to brew ${item.displayName}."
                }
            }
        }
    }

    private suspend fun requestDatabaseRetry(
        operation: String,
        error: String
    ): Boolean = retryPromptMutex.withLock {
        val decision = CompletableDeferred<Boolean>()
        retryDecision = decision
        _pendingDatabaseRetry.value = DatabaseRetryPrompt(operation, error)

        try {
            decision.await()
        } finally {
            retryDecision = null
            _pendingDatabaseRetry.value = null
        }
    }

    fun retryPendingDatabaseOperation() {
        retryDecision?.complete(true)
    }

    fun cancelPendingDatabaseRetry() {
        retryDecision?.complete(false)
    }

    private fun finalizeMissionBattle(finalState: RpgBattleState) {
        val victory = finalState.phase == BattlePhase.VICTORY
        val isIntroBattle = finalState.mission.id == RpgMissionCatalog.introMission.id

        if (victory && isIntroBattle) {
            _introState.update { cur ->
                val nextStage = IntroTutorialEngine.computeStage(
                    inCutscene = false,
                    lanternLit = cur.lanternLit,
                    shrinePuzzleSolved = cur.shrinePuzzleSolved,
                    sproutlingDefeated = true
                )
                cur.copy(
                    stage = nextStage,
                    sproutlingDefeated = true,
                    worldState = RpgWorldEngine.applyBattleCooldownAll(cur.worldState)
                )
            }

            _missionSummary.value = MissionCompletionSummary(
                mission = finalState.mission,
                victory = true,
                calmedSpirit = finalState.calmedSpirit,
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
        updateRepository(finalState, victory)
    }

    private fun updateRepository(finalState: RpgBattleState, victory: Boolean) {
        viewModelScope.launch {
            _isRecordingMissionRewards.value = true
            try {
                val summary = repository.recordMissionOutcome(
                    userId = currentUserId,
                    mission = finalState.mission,
                    victory = victory,
                    calmedSpirit = finalState.calmedSpirit,
                    remainingMana = finalState.heroMana,
                    usedLootInBattle = finalState.usedLootInBattle
                )
                _missionSummary.value = summary
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Mission outcome cloud save failed", e)
                _snackbarMessage.value =
                    "Victory counted for this intro. Cloud rewards were not saved; you can continue playing."
            } finally {
                _isRecordingMissionRewards.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }

    companion object {
        private const val TAG = "FernfableVM"
    }

    data class DatabaseRetryPrompt(
        val operation: String,
        val error: String
    )

    class Factory(
        private val repository: GameRepository,
        private val currentUserId: String,
        private val userDisplayName: String? = null,
        private val userEmail: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FernfableViewModel(
                repository = repository,
                currentUserId = currentUserId,
                userDisplayName = userDisplayName,
                userEmail = userEmail
            ) as T
        }
    }
}

