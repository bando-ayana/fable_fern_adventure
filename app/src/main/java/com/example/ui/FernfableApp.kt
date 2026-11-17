package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.audio.AudioScene
import com.example.audio.FernfableAudioManager
import com.example.model.PuzzleType
import com.example.navigation.FernfableRoutes
import com.example.navigation.TopDestination
import com.example.rpg.IntroStage
import com.example.stateHolder.FernfableUiState
import com.example.ui.components.ComfortAccessibilityBar
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.ProvideFernfableAccessibility
import com.example.ui.components.speakOnTouch
import com.example.ui.screens.BestiaryScreen
import com.example.ui.screens.BrainSkillsScreen
import com.example.ui.screens.GroveScreen
import com.example.ui.screens.IntroTutorialScreen
import com.example.ui.screens.MissionBattleSheet
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.OpeningCutsceneScreen
import com.example.ui.screens.PuzzleHubScreen
import com.example.ui.screens.PuzzlePlayScreen

private val topDestinations = listOf(
    TopDestination(
        route = FernfableRoutes.GROVE,
        labelRes = R.string.nav_grove,
        selectedIcon = Icons.Rounded.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_grove",
        spokenDescription = "Grove tab. Explore the jungle world and story chapters."
    ),
    TopDestination(
        route = FernfableRoutes.MISSIONS,
        labelRes = R.string.nav_missions,
        selectedIcon = Icons.Rounded.Flag,
        unselectedIcon = Icons.Outlined.Flag,
        testTag = "nav_missions",
        spokenDescription = "Missions tab. Build community projects and battle wild creatures."
    ),
    TopDestination(
        route = FernfableRoutes.PUZZLES,
        labelRes = R.string.nav_puzzles,
        selectedIcon = Icons.Rounded.Extension,
        unselectedIcon = Icons.Outlined.Extension,
        testTag = "nav_puzzles",
        spokenDescription = "Puzzles tab. Replay discovered brain teaser puzzles for higher scores."
    ),
    TopDestination(
        route = FernfableRoutes.BESTIARY,
        labelRes = R.string.nav_bestiary,
        selectedIcon = Icons.Rounded.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_bestiary",
        spokenDescription = "Creatures tab. Visit and feed mythical jungle friends."
    ),
    TopDestination(
        route = FernfableRoutes.SKILLS,
        labelRes = R.string.nav_skills,
        selectedIcon = Icons.Rounded.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        testTag = "nav_skills",
        spokenDescription = "Brain Skills tab. View your cognitive progress and badges."
    )
)

@Composable
fun FernfableApp(
    viewModel: FernfableViewModel,
    userEmail: String? = null,
    onSignOut: () -> Unit = {}
) {
    ProvideFernfableAccessibility {
        FernfableAppShell(
            viewModel = viewModel,
            userEmail = userEmail,
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun FernfableAppShell(
    viewModel: FernfableViewModel,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPlayingPuzzle = currentRoute == FernfableRoutes.PLAY_PUZZLE
    val audioManager = remember(context.applicationContext) {
        FernfableAudioManager(context.applicationContext)
    }

    DisposableEffect(lifecycleOwner, audioManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> audioManager.onForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> audioManager.onForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioManager.release()
        }
    }

    LaunchedEffect(isPlayingPuzzle) {
        audioManager.setScene(
            if (isPlayingPuzzle) AudioScene.PUZZLE_AMBIENT else AudioScene.RPG_MUSIC
        )
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun launchPuzzle(
        type: PuzzleType,
        levelOverride: Int? = null,
        accessedFromRpg: Boolean = true
    ) {
        viewModel.startPuzzle(type, levelOverride, accessedFromRpg = accessedFromRpg)
        navController.navigate(FernfableRoutes.PLAY_PUZZLE) {
            launchSingleTop = true
        }
    }

    JungleAtmosphereBackground(
        viewModel,
        isPlayingPuzzle,
        snackbarHostState,
        ::navigateTop,
        ::launchPuzzle,
        onSignOut,
        userEmail,
        backStackEntry,
        navController,
        uiState,
        audioManager
    )
}

@Composable
fun JungleAtmosphereBackground(
    viewModel: FernfableViewModel = viewModel(),
    isPlayingPuzzle: Boolean,
    snackbarHostState: SnackbarHostState,
    navigateTop: (String) -> Unit,
    launchPuzzle: (PuzzleType, Int?, Boolean) -> Unit,
    onSignOut: () -> Unit,
    userEmail: String?,
    backStackEntry: NavBackStackEntry?,
    navController: NavHostController,
    uiState: FernfableUiState,
    audioManager: FernfableAudioManager
) {
    val activeEncounter by viewModel.activeEncounter.collectAsStateWithLifecycle()
    val encounterFeedback by viewModel.encounterFeedback.collectAsStateWithLifecycle()
    val activePuzzleType by viewModel.activePuzzleType.collectAsStateWithLifecycle()
    val activePuzzleLevel by viewModel.activePuzzleLevel.collectAsStateWithLifecycle()
    val fireflyState by viewModel.fireflyState.collectAsStateWithLifecycle()
    val mushroomState by viewModel.mushroomState.collectAsStateWithLifecycle()
    val mushroomSelectedSlot by viewModel.mushroomSelectedSlot.collectAsStateWithLifecycle()
    val fernMosaicState by viewModel.fernMosaicState.collectAsStateWithLifecycle()
    val mosaicStatus by viewModel.mosaicStatus.collectAsStateWithLifecycle()
    val canopySpotterState by viewModel.canopySpotterState.collectAsStateWithLifecycle()
    val spotterStatus by viewModel.spotterStatus.collectAsStateWithLifecycle()
    val groveGridState by viewModel.groveGridState.collectAsStateWithLifecycle()
    val selectedGridCell by viewModel.selectedGridCell.collectAsStateWithLifecycle()
    val vineWeaverState by viewModel.vineWeaverState.collectAsStateWithLifecycle()
    val spiritLockState by viewModel.spiritLockState.collectAsStateWithLifecycle()
    val completionResult by viewModel.completionResult.collectAsStateWithLifecycle()
    val activeBattle by viewModel.activeBattle.collectAsStateWithLifecycle()
    val missionSummary by viewModel.missionSummary.collectAsStateWithLifecycle()
    val worldState by viewModel.worldState.collectAsStateWithLifecycle()
    val calmMode = LocalAccessibilityUi.current.calmMode
    val isProfileLoaded by viewModel.isProfileLoaded.collectAsStateWithLifecycle()
    val isRecordingMissionReward by viewModel.isRecordingMissionRewards.collectAsStateWithLifecycle()
    val isAudioMuted by audioManager.isMuted.collectAsStateWithLifecycle()
    val isInOnboarding = !isProfileLoaded || !uiState.profile.introCompleted
    val introState by viewModel.introState.collectAsStateWithLifecycle()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val useRail = screenWidthDp >= 600
    val pendingDatabaseRetry by
    viewModel.pendingDatabaseRetry.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!useRail && !isPlayingPuzzle && !isInOnboarding) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (calmMode) 1f else 0.94f)
                ) {
                    topDestinations.forEach { dest ->
                        val selected = backStackEntry?.destination?.hierarchy?.any {
                            it.route == dest.route
                        } == true
                        val label = stringResource(dest.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTop(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            modifier = Modifier
                                .speakOnTouch(dest.spokenDescription)
                                .testTag(dest.testTag)
                        )
                    }
                }
            }
        }


    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ComfortAccessibilityBar()

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (useRail && !isPlayingPuzzle && !isInOnboarding) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (calmMode) 1f else 0.92f)
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = backStackEntry?.destination?.hierarchy?.any {
                                it.route == dest.route
                            } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier
                                    .speakOnTouch(dest.spokenDescription)
                                    .testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = FernfableRoutes.GROVE,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(FernfableRoutes.GROVE) {
                        if (!isProfileLoaded) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (!uiState.profile.introCompleted) {
                            if (introState.stage == IntroStage.CUTSCENE) {
                                OpeningCutsceneScreen(
                                    page = introState.cutscenePage,
                                    heroId = uiState.profile.selectedHeroId,
                                    isAudioMuted = isAudioMuted,
                                    onToggleAudioMute = audioManager::toggleMute,
                                    onPageChange = viewModel::setCutscenePage,
                                    onBeginTraining = viewModel::beginIntroTrainingLevel,
                                    onSkipIntro = viewModel::skipIntroTutorial
                                )
                            } else {
                                IntroTutorialScreen(
                                    introState = introState,
                                    heroId = uiState.profile.selectedHeroId,
                                    heroLevel = uiState.profile.heroLevel,
                                    mana = uiState.profile.mana,
                                    maxMana = uiState.profile.maxMana,
                                    nectar = uiState.profile.nectar,
                                    isAudioMuted = isAudioMuted,
                                    hasActiveBattle = activeBattle != null,
                                    onToggleAudioMute = audioManager::toggleMute,
                                    onSkipIntro = viewModel::skipIntroTutorial,
                                    onGraduateToWorld = viewModel::completeIntroGraduation,
                                    onStepFrame = viewModel::stepIntroWorldFrame,
                                    onSetTapTarget = viewModel::setIntroTapTarget,
                                    onInteractLantern = viewModel::interactWithIntroLantern,
                                    onLaunchIntroPuzzle = { type -> launchPuzzle(type, 1, true) },
                                    onStartIntroBattle = viewModel::startMission,
                                    onRestMana = viewModel::restManaAtSanctuary,
                                    onShowDialogue = viewModel::showIntroDialogue,
                                    onDismissDialogue = viewModel::dismissIntroDialogue
                                )
                            }
                        } else {
                            GroveScreen(
                                uiState = uiState,
                                worldState = worldState,
                                hasActiveBattle = activeBattle != null,
                                activeEncounter = activeEncounter,
                                encounterFeedback = encounterFeedback,
                                userEmail = userEmail,
                                isAudioMuted = isAudioMuted,
                                onToggleAudioMute = audioManager::toggleMute,
                                onSignOut = onSignOut,
                                onOpenBiome = viewModel::openBiomeEncounter,
                                onDismissEncounter = viewModel::dismissEncounter,
                                onChooseStoryOption = viewModel::chooseStoryOption,
                                onSelectHero = viewModel::selectHero,
                                onCraftOrEquipRelic = viewModel::craftOrEquipRelic,
                                onLaunchPuzzle = { type -> launchPuzzle(type, null, true) },
                                onOpenPuzzleHub = { navigateTop(FernfableRoutes.PUZZLES) },
                                onOpenMissions = { navigateTop(FernfableRoutes.MISSIONS) },
                                onStepWorldFrame = viewModel::stepWorldFrame,
                                onSetWorldTapTarget = viewModel::setWorldTapTarget,
                                onShowWorldDialogue = viewModel::showWorldDialogue,
                                onDismissWorldDialogue = viewModel::dismissWorldDialogue,
                                onPostWorldToast = viewModel::postWorldToast,
                                onInteractLantern = viewModel::interactWithWorldLantern,
                                onForageObject = viewModel::forageWorldObject,
                                onRestMana = viewModel::restManaAtSanctuary,
                                onStartWorldMission = viewModel::startMission,
                                onReplayIntro = viewModel::replayIntroTutorial
                            )
                        }
                    }

                    composable(FernfableRoutes.MISSIONS) {
                        MissionsScreen(
                            uiState = uiState,
                            onStartMission = viewModel::startMission,
                            onRestMana = viewModel::restManaAtSanctuary,
                            onBrewLoot = viewModel::brewBattleLoot,
                            onLaunchPuzzle = { type -> launchPuzzle(type, null, true) }
                        )
                    }

                    composable(FernfableRoutes.PUZZLES) {
                        PuzzleHubScreen(
                            uiState = uiState,
                            onSelectDifficulty = viewModel::setDifficulty,
                            onToggleGentleMode = viewModel::setGentleMode,
                            onStartPuzzle = { type, level -> launchPuzzle(type, level, false) },
                            onNavigateToGrove = { navigateTop(FernfableRoutes.GROVE) },
                            onNavigateToMissions = { navigateTop(FernfableRoutes.MISSIONS) }
                        )
                    }

                    composable(FernfableRoutes.BESTIARY) {
                        BestiaryScreen(
                            uiState = uiState,
                            onFeedCreature = viewModel::feedCreature,
                            onPlaySignaturePuzzle = { type -> launchPuzzle(type, null, true) }
                        )
                    }

                    composable(FernfableRoutes.SKILLS) {
                        BrainSkillsScreen(
                            uiState = uiState,
                            onTrainSkill = { type -> launchPuzzle(type, null, false) },
                            onExploreRpg = { navigateTop(FernfableRoutes.GROVE) }
                        )
                    }

                    composable(FernfableRoutes.PLAY_PUZZLE) {
                        PuzzlePlayScreen(
                            puzzleType = activePuzzleType,
                            level = activePuzzleLevel,
                            difficulty = uiState.selectedDifficulty,
                            gentleMode = uiState.profile.gentleMode,
                            isAudioMuted = isAudioMuted,
                            onToggleAudioMute = audioManager::toggleMute,
                            fireflyState = fireflyState,
                            mushroomState = mushroomState,
                            mushroomSelectedSlot = mushroomSelectedSlot,
                            fernMosaicState = fernMosaicState,
                            mosaicStatus = mosaicStatus,
                            canopySpotterState = canopySpotterState,
                            spotterStatus = spotterStatus,
                            groveGridState = groveGridState,
                            selectedGridCell = selectedGridCell,
                            vineWeaverState = vineWeaverState,
                            spiritLockState = spiritLockState,
                            completionResult = completionResult,
                            onBack = {
                                viewModel.dismissCompletion()
                                navController.popBackStack()
                            },
                            onRestartLevel = { level ->
                                viewModel.startPuzzle(
                                    activePuzzleType,
                                    level
                                )
                            },
                            onTapBlossom = viewModel::tapFireflyBlossom,
                            onReplayFireflyHint = viewModel::replayFireflyHint,
                            onTapMushroomSlot = viewModel::tapMushroomSlot,
                            onUseMushroomHint = viewModel::useMushroomHint,
                            onSelectMosaicSlot = viewModel::selectMosaicSlot,
                            onChooseMosaicTile = viewModel::chooseMosaicTile,
                            onUseMosaicHint = viewModel::useMosaicHint,
                            onTapSpotterCharm = viewModel::tapSpotterCharm,
                            onUseSpotterHint = viewModel::useSpotterHint,
                            onSelectGridCell = viewModel::selectGridCell,
                            onPlaceGridRune = viewModel::placeGridRune,
                            onUseGridHint = viewModel::useGridHint,
                            onRotateVineTile = viewModel::rotateVineTile,
                            onUseVineHint = viewModel::useVineHint,
                            onAppendDraftRune = viewModel::appendDraftRune,
                            onRemoveLastDraftRune = viewModel::removeLastDraftRune,
                            onSubmitSpiritDraft = viewModel::submitSpiritDraft,
                            onUseSpiritHint = viewModel::useSpiritHint,
                            onDismissCompletion = viewModel::dismissCompletion
                        )
                    }
                }
            }
        }
    }

    pendingDatabaseRetry?.let { prompt ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingDatabaseRetry,
            title = {
                Text("Couldn’t save to the cloud")
            },
            text = {
                Text(
                    "${prompt.operation} failed.\n\n" +
                            "${prompt.error}\n\n" +
                            "Retry the save, or choose Don’t save to cancel it."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::retryPendingDatabaseOperation) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingDatabaseRetry) {
                    Text("Don’t save")
                }
            }
        )
    }

    val currentBattle = activeBattle
    if (currentBattle != null) {
        MissionBattleSheet(
            battleState = currentBattle,
            heroId = uiState.profile.selectedHeroId,
            missionSummary = missionSummary,
            onAdvanceCommunityStep = viewModel::advanceMissionCommunityStep,
            onPerformAction = viewModel::performBattleAction,
            onUseLoot = viewModel::useBattleLoot,
            onRetreat = viewModel::retreatFromBattle,
            onDismiss = viewModel::dismissMissionSheet,
            isRecordingMissionReward = isRecordingMissionReward,
            onLaunchPuzzle = { type, _, _ -> launchPuzzle(type, null, true) }
        )
    }
}