package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.JungleLoreCatalog
import com.example.model.BiomeNode
import com.example.model.DifficultyTier
import com.example.model.PuzzleType
import com.example.model.StoryChoice
import com.example.stateHolder.FernfableUiState
import com.example.stateHolder.IntroTutorialState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SanctuaryViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val userDisplayName: String? = null,
    private val userEmail: String? = null
) : ViewModel() {

    private val _selectedDifficulty =
        MutableStateFlow(DifficultyTier.SPROUT)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FernfableUiState> = combine(
        repository.observeProfile(currentUserId),
        repository.observeCreatures(currentUserId),
        repository.observePuzzleStats(currentUserId),
        _selectedDifficulty,
        _snackbarMessage
    ) { profile, creatures, stats, difficulty, snackbar ->
        FernfableUiState(
            profile = profile,
            creatureProgress = creatures.associateBy { it.creatureId },
            puzzleStats = stats.associateBy {
                PuzzleType.fromId(it.puzzleType)
            },
            selectedDifficulty = difficulty,
            snackbarMessage = snackbar
        )
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

        if (firestoreError == null && error is CancellationException) {
            throw error
        }

        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing sanctuary data", rootError)
        emit(
            FernfableUiState(
                syncError = rootError.message
                    ?: "Failed to sync sanctuary data"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FernfableUiState()
    )

    private val _activeEncounter = MutableStateFlow<BiomeNode?>(null)
    val activeEncounter: StateFlow<BiomeNode?> =
        _activeEncounter.asStateFlow()

    private val _encounterFeedback =
        MutableStateFlow<StoryChoice?>(null)
    val encounterFeedback: StateFlow<StoryChoice?> =
        _encounterFeedback.asStateFlow()

    private val _pendingDatabaseRetry =
        MutableStateFlow<DatabaseRetryPrompt?>(null)
    val pendingDatabaseRetry: StateFlow<DatabaseRetryPrompt?> =
        _pendingDatabaseRetry.asStateFlow()

    private val retryPromptMutex = Mutex()
    private var retryDecision: CompletableDeferred<Boolean>? = null

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded: StateFlow<Boolean> =
        _isProfileLoaded.asStateFlow()

    private val _introCompletedLocally = MutableStateFlow(false)
    val introCompletedLocally: StateFlow<Boolean> =
        _introCompletedLocally.asStateFlow()

    private val _introState = MutableStateFlow(IntroTutorialState())
    val introState: StateFlow<IntroTutorialState> =
        _introState.asStateFlow()

    init {
        repository.requestManualRetry = ::requestDatabaseRetry
        _isProfileLoaded.value = true

        viewModelScope.launch {
            try {
                repository.ensureSeeded(
                    currentUserId,
                    userDisplayName,
                    userEmail
                )

                val streakEval =
                    repository.recordDailyActivity(currentUserId)

                if (
                    streakEval.didAdvanceStreak &&
                    streakEval.rewardMessage != null
                ) {
                    _snackbarMessage.value = streakEval.rewardMessage
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Initial sanctuary seed failed", e)
                _snackbarMessage.value =
                    e.localizedMessage
                        ?: "Couldn't initialize cloud sanctuary."
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

    fun selectHero(heroId: String) {
        viewModelScope.launch {
            val hero = JungleLoreCatalog.heroById(heroId)

            if (uiState.value.profile.nectar >= hero.unlockNectar) {
                runRepositoryAction {
                    repository.selectHero(currentUserId, heroId)
                    _snackbarMessage.value =
                        "Now adventuring as ${hero.name} (${hero.title})!"
                }
            } else {
                _snackbarMessage.value =
                    "Gather ${hero.unlockNectar} Sunbeam Nectar " +
                            "to welcome ${hero.name}!"
            }
        }
    }

    fun craftOrEquipRelic(relicId: String) {
        viewModelScope.launch {
            runRepositoryAction {
                _snackbarMessage.value =
                    repository.craftOrEquipRelic(
                        currentUserId,
                        relicId
                    )
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

    fun chooseStoryOption(
        biome: BiomeNode,
        choice: StoryChoice
    ) {
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
            val creature =
                JungleLoreCatalog.creatureById(creatureId)

            runRepositoryAction {
                val fed = repository.feedCreatureTreat(
                    currentUserId,
                    creatureId,
                    nectarCost = 10
                )

                _snackbarMessage.value = if (fed) {
                    "${creature.name} happily nibbles " +
                            "${creature.favoriteTreat}! " +
                            "Bond deepened & +15 EXP."
                } else {
                    "Gather 10 Sunbeam Nectar from puzzles " +
                            "or story quests to share a treat."
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private suspend fun runRepositoryAction(
        block: suspend () -> Unit
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Sanctuary update failed", e)
            _snackbarMessage.value =
                e.localizedMessage
                    ?: "Cloud sync failed. Please try again."
        }
    }

    private suspend fun requestDatabaseRetry(
        operation: String,
        error: String
    ): Boolean = retryPromptMutex.withLock {
        val decision = CompletableDeferred<Boolean>()
        retryDecision = decision
        _pendingDatabaseRetry.value =
            DatabaseRetryPrompt(operation, error)

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

    data class DatabaseRetryPrompt(
        val operation: String,
        val error: String
    )

    companion object {
        private const val TAG = "SanctuaryVM"
    }
}