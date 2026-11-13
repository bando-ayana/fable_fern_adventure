package com.example.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Psychology
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.model.PuzzleType
import com.example.navigation.FernfableRoutes
import com.example.navigation.TopDestination
import com.example.ui.screens.BestiaryScreen
import com.example.ui.screens.BrainSkillsScreen
import com.example.ui.screens.GroveScreen
import com.example.ui.screens.PuzzleHubScreen
import com.example.ui.screens.PuzzlePlayScreen

private val topDestinations = listOf(
    TopDestination(
        route = FernfableRoutes.GROVE,
        labelRes = R.string.nav_grove,
        selectedIcon = Icons.Rounded.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_grove"
    ),
    TopDestination(
        route = FernfableRoutes.PUZZLES,
        labelRes = R.string.nav_puzzles,
        selectedIcon = Icons.Rounded.Extension,
        unselectedIcon = Icons.Outlined.Extension,
        testTag = "nav_puzzles"
    ),
    TopDestination(
        route = FernfableRoutes.BESTIARY,
        labelRes = R.string.nav_bestiary,
        selectedIcon = Icons.Rounded.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_bestiary"
    ),
    TopDestination(
        route = FernfableRoutes.SKILLS,
        labelRes = R.string.nav_skills,
        selectedIcon = Icons.Rounded.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        testTag = "nav_skills"
    )
)

@Composable
fun FernfableApp(
    viewModel: FernfableViewModel,
    userEmail: String? = null,
    onSignOut: () -> Unit = {}
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPlayingPuzzle = currentRoute == FernfableRoutes.PLAY_PUZZLE


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

    fun launchPuzzle(type: PuzzleType, levelOverride: Int? = null) {
        viewModel.startPuzzle(type, levelOverride)
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
        uiState
    )
}

@Composable
fun JungleAtmosphereBackground(
    viewModel: FernfableViewModel = viewModel(),
    isPlayingPuzzle: Boolean,
    snackbarHostState: SnackbarHostState,
    navigateTop: (String) -> Unit,
    launchPuzzle: (PuzzleType, Int?) -> Unit,
    onSignOut: () -> Unit,
    userEmail: String?,
    backStackEntry: NavBackStackEntry?,
    navController: NavHostController,
    uiState: FernfableUiState
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
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val useRail = screenWidthDp >= 600

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!useRail && !isPlayingPuzzle) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
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
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useRail && !isPlayingPuzzle) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
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
                            modifier = Modifier.testTag(dest.testTag)
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
                    GroveScreen(
                        uiState = uiState,
                        activeEncounter = activeEncounter,
                        encounterFeedback = encounterFeedback,
                        userEmail = userEmail,
                        onSignOut = onSignOut,
                        onOpenBiome = viewModel::openBiomeEncounter,
                        onDismissEncounter = viewModel::dismissEncounter,
                        onChooseStoryOption = viewModel::chooseStoryOption,
                        onSelectHero = viewModel::selectHero,
                        onCraftOrEquipRelic = viewModel::craftOrEquipRelic,
                        onLaunchPuzzle = { puzzleType -> launchPuzzle(puzzleType, null) },
                        onOpenPuzzleHub = { navigateTop(FernfableRoutes.PUZZLES) }
                    )
                }

                composable(FernfableRoutes.PUZZLES) {
                    PuzzleHubScreen(
                        uiState = uiState,
                        onSelectDifficulty = viewModel::setDifficulty,
                        onToggleGentleMode = viewModel::setGentleMode,
                        onStartPuzzle = { puzzleType, level -> launchPuzzle(puzzleType, level) }
                    )
                }

                composable(FernfableRoutes.BESTIARY) {
                    BestiaryScreen(
                        uiState = uiState,
                        onFeedCreature = viewModel::feedCreature,
                        onPlaySignaturePuzzle = { puzzleType -> launchPuzzle(puzzleType, null) }
                    )
                }

                composable(FernfableRoutes.SKILLS) {
                    BrainSkillsScreen(
                        uiState = uiState,
                        onTrainSkill = { puzzleType -> launchPuzzle(puzzleType, null) }
                    )
                }

                composable(FernfableRoutes.PLAY_PUZZLE) {
                    PuzzlePlayScreen(
                        puzzleType = activePuzzleType,
                        level = activePuzzleLevel,
                        difficulty = uiState.selectedDifficulty,
                        gentleMode = uiState.profile.gentleMode,
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
                        onRestartLevel = { level -> viewModel.startPuzzle(activePuzzleType, level) },
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