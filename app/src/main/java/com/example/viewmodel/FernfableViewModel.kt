package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.GameRepository
import com.example.model.BiomeNode
import com.example.model.CommunityMission
import com.example.model.DifficultyTier
import com.example.model.LootItemType
import com.example.model.PuzzleType
import com.example.model.StoryChoice
import com.example.puzzle.LeafToken
import com.example.puzzle.SpiritRune
import com.example.rpg.BattleActionType
import com.example.rpg.InteractableKind

class FernfableViewModel(
    repository: GameRepository,
    currentUserId: String,
    userDisplayName: String? = null,
    userEmail: String? = null
) : ViewModel() {

    private val sanctuary =
        SanctuaryViewModel(
            repository,
            currentUserId,
            userDisplayName,
            userEmail
        )

    private val puzzles =
        PuzzleViewModel(
            repository,
            currentUserId,
            sanctuary.uiState
        )

    private val missions =
        CommunityMissionViewModel(
            repository,
            currentUserId,
            sanctuary.uiState
        )

    val uiState = sanctuary.uiState
    val pendingDatabaseRetry =
        sanctuary.pendingDatabaseRetry
    val activeEncounter =
        sanctuary.activeEncounter
    val encounterFeedback =
        sanctuary.encounterFeedback
    val isProfileLoaded =
        sanctuary.isProfileLoaded
    val introState =
        sanctuary.introState
    val introCompletedLocally =
        sanctuary.introCompletedLocally

    val activePuzzleType =
        puzzles.activePuzzleType
    val activePuzzleLevel =
        puzzles.activePuzzleLevel
    val fireflyState =
        puzzles.fireflyState
    val mushroomState =
        puzzles.mushroomState
    val mushroomSelectedSlot =
        puzzles.mushroomSelectedSlot
    val fernMosaicState =
        puzzles.fernMosaicState
    val mosaicStatus =
        puzzles.mosaicStatus
    val canopySpotterState =
        puzzles.canopySpotterState
    val spotterStatus =
        puzzles.spotterStatus
    val groveGridState =
        puzzles.groveGridState
    val selectedGridCell =
        puzzles.selectedGridCell
    val vineWeaverState =
        puzzles.vineWeaverState
    val spiritLockState =
        puzzles.spiritLockState
    val completionResult =
        puzzles.completionResult
    val isIntroShrinePuzzle =
        puzzles.isIntroShrinePuzzle

    val activeBattle =
        missions.activeBattle
    val missionSummary =
        missions.missionSummary
    val worldState =
        missions.worldState
    val isRecordingMissionRewards =
        missions.isRecordingMissionRewards
    val activeZoneState = missions.activeZoneState

    //Sancuary functions
    fun setDifficulty(tier: DifficultyTier) =
        sanctuary.setDifficulty(tier)

    fun setGentleMode(enabled: Boolean) =
        sanctuary.setGentleMode(enabled)

    fun selectHero(heroId: String) =
        sanctuary.selectHero(heroId)

    fun craftOrEquipRelic(relicId: String) =
        sanctuary.craftOrEquipRelic(relicId)

    fun openBiomeEncounter(biome: BiomeNode) =
        sanctuary.openBiomeEncounter(biome)

    fun dismissEncounter() =
        sanctuary.dismissEncounter()

    fun chooseStoryOption(
        biome: BiomeNode,
        choice: StoryChoice
    ) = sanctuary.chooseStoryOption(
        biome,
        choice
    )

    fun feedCreature(creatureId: String) =
        sanctuary.feedCreature(creatureId)

    fun retryPendingDatabaseOperation() =
        sanctuary.retryPendingDatabaseOperation()

    fun cancelPendingDatabaseRetry() =
        sanctuary.cancelPendingDatabaseRetry()

    fun clearSnackbar() {
        sanctuary.clearSnackbar()
        missions.clearSnackbar()
    }

    // Puzzle functions
    fun startPuzzle(
        type: PuzzleType,
        levelOverride: Int? = null,
        accessedFromRpg: Boolean = true
    ) = puzzles.startPuzzle(
        type,
        levelOverride,
        accessedFromRpg
    )

    fun dismissCompletion() =
        puzzles.dismissCompletion()

    fun replayFireflyHint() =
        puzzles.replayFireflyHint()

    fun tapFireflyBlossom(index: Int) =
        puzzles.tapFireflyBlossom(index)

    fun tapMushroomSlot(slotIdx: Int) =
        puzzles.tapMushroomSlot(slotIdx)

    fun useMushroomHint() =
        puzzles.useMushroomHint()

    fun selectMosaicSlot(slotIdx: Int) =
        puzzles.selectMosaicSlot(slotIdx)

    fun chooseMosaicTile(token: LeafToken) =
        puzzles.chooseMosaicTile(token)

    fun useMosaicHint() =
        puzzles.useMosaicHint()

    fun tapSpotterCharm(charmId: Int) =
        puzzles.tapSpotterCharm(charmId)

    fun useSpotterHint() =
        puzzles.useSpotterHint()

    fun selectGridCell(index: Int) =
        puzzles.selectGridCell(index)

    fun placeGridRune(runeId: Int) =
        puzzles.placeGridRune(runeId)

    fun useGridHint() =
        puzzles.useGridHint()

    fun rotateVineTile(row: Int, col: Int) =
        puzzles.rotateVineTile(row, col)

    fun useVineHint() =
        puzzles.useVineHint()

    fun appendDraftRune(rune: SpiritRune) =
        puzzles.appendDraftRune(rune)

    fun removeLastDraftRune() =
        puzzles.removeLastDraftRune()

    fun submitSpiritDraft() =
        puzzles.submitSpiritDraft()

    fun useSpiritHint() =
        puzzles.useSpiritHint()

    // Missions functions

    fun stepWorldFrame(
        joyX: Float,
        joyY: Float,
        deltaSec: Float
    ) = missions.stepWorldFrame(
        joyX,
        joyY,
        deltaSec
    )

    fun setWorldTapTarget(x: Float, y: Float) =
        missions.setWorldTapTarget(x, y)

    fun showWorldDialogue(
        speaker: String,
        body: String
    ) = missions.showWorldDialogue(
        speaker,
        body
    )

    fun dismissWorldDialogue() =
        missions.dismissWorldDialogue()

    fun postWorldToast(text: String) =
        missions.postWorldToast(text)

    fun interactWithWorldLantern(
        lanternId: String,
        title: String
    ) = missions.interactWithWorldLantern(
        lanternId,
        title
    )

    fun forageWorldObject(
        objectId: String,
        kind: InteractableKind,
        title: String
    ) = missions.forageWorldObject(
        objectId,
        kind,
        title
    )

    fun startMission(
        mission: CommunityMission,
        skipPrepToCombat: Boolean = false
    ) = missions.startMission(
        mission,
        skipPrepToCombat
    )

    fun advanceMissionCommunityStep() =
        missions.advanceMissionCommunityStep()

    fun performBattleAction(
        action: BattleActionType
    ) = missions.performBattleAction(action)

    fun useBattleLoot(item: LootItemType) =
        missions.useBattleLoot(item)

    fun retreatFromBattle() =
        missions.retreatFromBattle()

    fun dismissMissionSheet() =
        missions.dismissMissionSheet()

    fun restManaAtSanctuary() =
        missions.restManaAtSanctuary()

    fun brewBattleLoot(item: LootItemType) =
        missions.brewBattleLoot(item)

    override fun onCleared() {
        puzzles.cancelPlayback()
        super.onCleared()
    }

    class Factory(
        private val repository: GameRepository,
        private val currentUserId: String,
        private val userDisplayName: String? = null,
        private val userEmail: String? = null
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            return FernfableViewModel(
                repository = repository,
                currentUserId = currentUserId,
                userDisplayName = userDisplayName,
                userEmail = userEmail
            ) as T
        }
    }
}