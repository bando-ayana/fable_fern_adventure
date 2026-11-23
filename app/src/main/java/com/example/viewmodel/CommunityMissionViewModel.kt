package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.RpgMissionCatalog
import com.example.model.CommunityMission
import com.example.model.LootItemType
import com.example.model.MissionCompletionSummary
import com.example.rpg.BattleActionType
import com.example.rpg.BattlePhase
import com.example.rpg.InteractableKind
import com.example.rpg.RpgBattleEngine
import com.example.rpg.RpgBattleState
import com.example.rpg.RpgWorldEngine
import com.example.rpg.RpgWorldState
import com.example.rpg.WorldToast
import com.example.stateHolder.FernfableUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityMissionViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val sanctuaryUiState: StateFlow<FernfableUiState>
) : ViewModel() {

    private val _activeBattle =
        MutableStateFlow<RpgBattleState?>(null)
    val activeBattle: StateFlow<RpgBattleState?> =
        _activeBattle.asStateFlow()

    private val _missionSummary =
        MutableStateFlow<MissionCompletionSummary?>(null)
    val missionSummary:
        StateFlow<MissionCompletionSummary?> =
        _missionSummary.asStateFlow()

    private val _worldState =
        MutableStateFlow(RpgWorldState())
    val worldState: StateFlow<RpgWorldState> =
        _worldState.asStateFlow()

    private val _isRecordingMissionRewards =
        MutableStateFlow(false)
    val isRecordingMissionRewards: StateFlow<Boolean> =
        _isRecordingMissionRewards.asStateFlow()

    private val _snackbarMessage =
        MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> =
        _snackbarMessage.asStateFlow()

    fun stepWorldFrame(
        joyX: Float,
        joyY: Float,
        deltaSec: Float
    ) {
        if (
            _activeBattle.value != null ||
            sanctuaryUiState.value.profile != null
        ) {
            // Encounter blocking is handled by the facade
        }

        val profile = sanctuaryUiState.value.profile
        val (nextState, engagedCreature) =
            RpgWorldEngine.stepWorld(
                state = _worldState.value,
                joyX = joyX,
                joyY = joyY,
                deltaSec = deltaSec,
                communityProsperity =
                    profile.communityProsperity,
                completedMissions =
                    profile.completedMissionIds
            )

        _worldState.value = nextState

        if (engagedCreature != null) {
            val mission =
                engagedCreature.missionId?.let {
                    RpgMissionCatalog.missionById(it)
                }

            if (mission != null) {
                startMission(
                    mission,
                    skipPrepToCombat = true
                )
            }
        }
    }

    fun setWorldTapTarget(x: Float, y: Float) {
        val clampedX =
            x.coerceIn(
                0.6f,
                RpgWorldEngine.MAP_WIDTH - 0.6f
            )
        val clampedY =
            y.coerceIn(
                0.6f,
                RpgWorldEngine.MAP_HEIGHT - 0.6f
            )

        _worldState.update {
            it.copy(
                tapTarget = clampedX to clampedY,
                dialogueSpeaker = null,
                dialogueBody = null
            )
        }
    }

    fun showWorldDialogue(
        speaker: String,
        body: String
    ) {
        _worldState.update {
            it.copy(
                tapTarget = null,
                dialogueSpeaker = speaker,
                dialogueBody = body
            )
        }
    }

    fun dismissWorldDialogue() {
        _worldState.update {
            it.copy(
                dialogueSpeaker = null,
                dialogueBody = null
            )
        }
    }

    fun postWorldToast(text: String) {
        _worldState.update { current ->
            val sequence = current.toastSeq + 1
            current.copy(
                toastSeq = sequence,
                worldToast = WorldToast(
                    id = sequence,
                    text = text,
                    x = current.heroX,
                    y = current.heroY
                )
            )
        }
    }

    fun interactWithWorldLantern(
        lanternId: String,
        title: String
    ) {
        val alreadyLit =
            lanternId in
                _worldState.value.litLanternIds

        _worldState.update {
            it.copy(
                litLanternIds =
                    it.litLanternIds + lanternId
            )
        }

        if (!alreadyLit) {
            postWorldToast(
                "$title lit! +2 Sunbeam Nectar"
            )
            viewModelScope.launch {
                runRepositoryAction {
                    repository.forageWorldResource(
                        currentUserId,
                        nectarGain = 2
                    )
                }
            }
        } else {
            postWorldToast(
                "$title hums softly with golden light."
            )
        }
    }

    fun forageWorldObject(
        objectId: String,
        kind: InteractableKind,
        title: String
    ) {
        if (
            objectId in
            _worldState.value.harvestedIds
        ) {
            postWorldToast(
                "$title has already been harvested today."
            )
            return
        }

        _worldState.update {
            it.copy(
                harvestedIds =
                    it.harvestedIds + objectId
            )
        }

        when (kind) {
            InteractableKind.HARVEST_TONIC -> {
                postWorldToast(
                    "Foraged 1 Sunberry Tonic & +3 Nectar!"
                )
                viewModelScope.launch {
                    runRepositoryAction {
                        repository.forageWorldResource(
                            userId = currentUserId,
                            nectarGain = 3,
                            lootItem =
                                LootItemType.SUNBERRY_TONIC
                        )
                    }
                }
            }

            else -> {
                postWorldToast(
                    "Harvested +5 Sunbeam Nectar " +
                        "from $title!"
                )
                viewModelScope.launch {
                    runRepositoryAction {
                        repository.forageWorldResource(
                            userId = currentUserId,
                            nectarGain = 5
                        )
                    }
                }
            }
        }
    }

    fun startMission(
        mission: CommunityMission,
        skipPrepToCombat: Boolean = false
    ) {
        val profile = sanctuaryUiState.value.profile

        if (
            !mission.isWildPatrol &&
            profile.communityProsperity <
            mission.requiredProsperity
        ) {
            _snackbarMessage.value =
                "Build ${mission.requiredProsperity}% " +
                "Community Prosperity first!"

            postWorldToast(
                "Need ${mission.requiredProsperity}% " +
                "Prosperity to face " +
                mission.opponent.name
            )
            return
        }

        val loot =
            RpgMissionCatalog.parseLootBag(
                profile.lootBagCsv
            )

        _missionSummary.value = null

        val initial =
            RpgBattleEngine.createBattleState(
                mission = mission,
                profile = profile,
                lootAvailable = loot
            )

        _activeBattle.value =
            if (skipPrepToCombat) {
                RpgBattleEngine.beginWildEncounter(
                    initial
                )
            } else {
                initial
            }
    }

    fun advanceMissionCommunityStep() {
        val current = _activeBattle.value ?: return
        if (
            current.phase !=
            BattlePhase.COMMUNITY_PREP
        ) return

        _activeBattle.value =
            RpgBattleEngine.beginWildEncounter(
                current
            )
    }

    fun performBattleAction(
        action: BattleActionType
    ) {
        val current = _activeBattle.value ?: return
        if (
            current.phase !=
            BattlePhase.ACTIVE_COMBAT
        ) return

        val updated =
            RpgBattleEngine.performAction(
                current,
                action
            )

        _activeBattle.value = updated

        if (
            updated.phase == BattlePhase.VICTORY ||
            updated.phase == BattlePhase.RETREATED
        ) {
            finalizeMissionBattle(updated)
        }
    }

    fun useBattleLoot(item: LootItemType) {
        val current = _activeBattle.value ?: return
        if (
            current.phase !=
            BattlePhase.ACTIVE_COMBAT
        ) return

        val updated =
            RpgBattleEngine.useLootItem(
                current,
                item
            )

        _activeBattle.value = updated

        if (updated.phase == BattlePhase.VICTORY) {
            finalizeMissionBattle(updated)
        }
    }

    fun retreatFromBattle() {
        val current = _activeBattle.value ?: return

        _worldState.update {
            RpgWorldEngine.applyBattleCooldownAll(it)
        }

        if (
            current.phase ==
            BattlePhase.COMMUNITY_PREP
        ) {
            _activeBattle.value = null
            return
        }

        val retreated = current.copy(
            phase = BattlePhase.RETREATED,
            combatLog = (
                listOf(
                    "${current.heroName} withdrew back " +
                        "to Solari Canopy!"
                ) + current.combatLog
            ).take(6)
        )

        _activeBattle.value = retreated
        finalizeMissionBattle(retreated)
    }

    fun dismissMissionSheet() {
        _activeBattle.value = null
        _missionSummary.value = null
        _worldState.update {
            RpgWorldEngine.applyBattleCooldownAll(it)
        }
    }

    fun restManaAtSanctuary() {
        viewModelScope.launch {
            runRepositoryAction {
                val maxMana =
                    repository.restAtSanctuary(
                        currentUserId
                    )
                _snackbarMessage.value =
                    "Mana restored to $maxMana/$maxMana " +
                    "at the Sanctuary!"
            }
        }
    }

    fun brewBattleLoot(item: LootItemType) {
        viewModelScope.launch {
            runRepositoryAction {
                val success =
                    repository.brewBattleLoot(
                        currentUserId,
                        item
                    )

                _snackbarMessage.value =
                    if (success) {
                        "Crafted 1 ${item.displayName} " +
                            "for your battle satchel!"
                    } else {
                        "Need 12 Sunbeam Nectar to brew " +
                            item.displayName
                    }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun finalizeMissionBattle(
        finalState: RpgBattleState
    ) {
        val victory =
            finalState.phase == BattlePhase.VICTORY

        viewModelScope.launch {
            _isRecordingMissionRewards.value = true
            try {
                val summary =
                    repository.recordMissionOutcome(
                        userId = currentUserId,
                        mission = finalState.mission,
                        victory = victory,
                        calmedSpirit =
                            finalState.calmedSpirit,
                        remainingMana =
                            finalState.heroMana,
                        usedLootInBattle =
                            finalState.usedLootInBattle
                    )
                _missionSummary.value = summary
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Mission outcome cloud save failed",
                    e
                )
                _snackbarMessage.value =
                    "Victory counted for this intro. " +
                    "Cloud rewards were not saved; " +
                    "you can continue playing."
            } finally {
                _isRecordingMissionRewards.value =
                    false
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
            Log.w(
                TAG,
                "Community mission update failed",
                e
            )
            _snackbarMessage.value =
                e.localizedMessage
                    ?: "Cloud sync failed."
        }
    }

    companion object {
        private const val TAG = "CommunityMissionVM"
    }
}