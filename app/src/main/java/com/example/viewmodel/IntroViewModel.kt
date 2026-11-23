package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.RpgMissionCatalog
import com.example.model.CommunityMission
import com.example.model.DifficultyTier
import com.example.model.MissionCompletionSummary
import com.example.model.PuzzleCompletionResult
import com.example.model.PuzzleType
import com.example.puzzle.CanopySpotterPuzzle
import com.example.puzzle.FernMosaicPuzzle
import com.example.puzzle.GroveGridPuzzle
import com.example.puzzle.MushroomMelodyPuzzle
import com.example.puzzle.SpiritLockPuzzle
import com.example.puzzle.VineWeaverBoard
import com.example.rpg.IntroStage
import com.example.rpg.IntroTutorialEngine
import com.example.rpg.RpgBattleEngine
import com.example.rpg.RpgBattleState
import com.example.rpg.RpgWorldState
import com.example.rpg.WorldToast
import com.example.stateHolder.FernfableUiState
import com.example.stateHolder.FireflyEchoUiState
import com.example.stateHolder.IntroTutorialState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IntroViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val userDisplayName: String? = null,
    private val userEmail: String? = null
) : ViewModel() {
    private val _introState = MutableStateFlow(IntroTutorialState())
    val introState: StateFlow<IntroTutorialState> = _introState.asStateFlow()

    private val _introCompletedLocally = MutableStateFlow(false)
    val introCompletedLocally: StateFlow<Boolean> = _introCompletedLocally.asStateFlow()

    private val _activeBattle = MutableStateFlow<RpgBattleState?>(null)
    val activeBattle: StateFlow<RpgBattleState?> = _activeBattle.asStateFlow()

    private val _missionSummary = MutableStateFlow<MissionCompletionSummary?>(null)
    val missionSummary: StateFlow<MissionCompletionSummary?> = _missionSummary.asStateFlow()

    private val _worldState = MutableStateFlow(RpgWorldState())
    val worldState: StateFlow<RpgWorldState> = _worldState.asStateFlow()

    private val _fireflyState = MutableStateFlow(FireflyEchoUiState())
    val fireflyState: StateFlow<FireflyEchoUiState> = _fireflyState.asStateFlow()

    private val _completionResult = MutableStateFlow<PuzzleCompletionResult?>(null)
    val completionResult: StateFlow<PuzzleCompletionResult?> = _completionResult.asStateFlow()

    private val _activePuzzleType = MutableStateFlow(PuzzleType.FIREFLY_ECHO)
    val activePuzzleType: StateFlow<PuzzleType> = _activePuzzleType.asStateFlow()

    private val _activePuzzleLevel = MutableStateFlow(1)
    val activePuzzleLevel: StateFlow<Int> = _activePuzzleLevel.asStateFlow()

    private val _isIntroShrinePuzzle = MutableStateFlow(false)
    val isIntroShrinePuzzle: StateFlow<Boolean> = _isIntroShrinePuzzle.asStateFlow()

    private val _shouldPlayFireflyPuzzle = MutableStateFlow(false)
    val shouldPlayFireflyPuzzle: StateFlow<Boolean> = _shouldPlayFireflyPuzzle.asStateFlow()
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private var playbackJob: Job? = null
    private val _selectedDifficulty = MutableStateFlow(DifficultyTier.SPROUT)

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
        started = SharingStarted.Eagerly,
        initialValue = FernfableUiState()
    )

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
                repository.markIntroCompleted(
                    userId = currentUserId,
                    bonusNectar = 0,
                )
            }
        }
    }

    fun completeIntroGraduation() {
        _introCompletedLocally.value = true

        viewModelScope.launch {
            runRepositoryAction {
                repository.markIntroCompleted(
                    userId = currentUserId,
                    bonusNectar = 20,
                )
                _snackbarMessage.value =
                    "Welcome to Solari Canopy! +20 Starter Sunbeam Nectar awarded."
            }
        }
    }

    fun completeIntroShrinePuzzle() {
        _introState.update { current ->
            val nextStage = IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = current.lanternLit,
                shrinePuzzleSolved = true,
                sproutlingDefeated = current.sproutlingDefeated
            )

            val toastSequence = current.worldState.toastSeq + 1
            current.copy(
                stage = nextStage,
                shrinePuzzleSolved = true,
                worldState = current.worldState.copy(
                    toastSeq = toastSequence,
                    worldToast = WorldToast(
                        id = toastSequence,
                        text = "Shrine unlocked! Now head south to spar Mossback Sproutling!",
                        x = current.worldState.heroX,
                        y = current.worldState.heroY
                    )
                )
            )
        }
    }

    fun completeIntroBattle() {
        _introState.update { current ->
            val nextStage = IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = current.lanternLit,
                shrinePuzzleSolved = current.shrinePuzzleSolved,
                sproutlingDefeated = true
            )

            current.copy(
                stage = nextStage,
                sproutlingDefeated = true
            )
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

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissCompletion() {
        _completionResult.value = null
        _isIntroShrinePuzzle.value = false
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

    fun startMission(mission: CommunityMission, skipPrepToCombat: Boolean = false) {
        val profile = uiState.value.profile
        if (!mission.isWildPatrol && profile.communityProsperity < mission.requiredProsperity) {
            _snackbarMessage.value =
                "Build ${mission.requiredProsperity}% Community Prosperity first!"
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

    fun postWorldToast(text: String) {
        _worldState.update { cur ->
            val seq = cur.toastSeq + 1
            cur.copy(
                toastSeq = seq,
                worldToast = WorldToast(id = seq, text = text, x = cur.heroX, y = cur.heroY)
            )
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

    companion object {
        private const val TAG = "IntroVM"
    }
}

class Factory(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val userDisplayName: String? = null,
    private val userEmail: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IntroViewModel(
            repository = repository,
            currentUserId = currentUserId,
            userDisplayName = userDisplayName,
            userEmail = userEmail
        ) as T
    }
}