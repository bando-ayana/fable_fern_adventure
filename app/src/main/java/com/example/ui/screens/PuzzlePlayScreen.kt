package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DifficultyTier
import com.example.model.PuzzleCompletionResult
import com.example.model.PuzzleType
import com.example.puzzle.CanopySpotterPuzzle
import com.example.puzzle.FernMosaicPuzzle
import com.example.puzzle.GroveGridPuzzle
import com.example.puzzle.LeafToken
import com.example.puzzle.MushroomMelodyPuzzle
import com.example.puzzle.SpiritLockPuzzle
import com.example.puzzle.SpiritRune
import com.example.puzzle.VineWeaverBoard
import com.example.ui.FireflyEchoUiState
import com.example.ui.puzzles.CanopySpotterView
import com.example.ui.puzzles.FernMosaicView
import com.example.ui.puzzles.FireflyEchoView
import com.example.ui.puzzles.GroveGridView
import com.example.ui.puzzles.MushroomMelodyView
import com.example.ui.puzzles.SpiritLockView
import com.example.ui.puzzles.VineWeaverView
import com.example.ui.theme.FireflyGoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzlePlayScreen(
    puzzleType: PuzzleType,
    level: Int,
    difficulty: DifficultyTier,
    gentleMode: Boolean,
    fireflyState: FireflyEchoUiState,
    mushroomState: MushroomMelodyPuzzle?,
    mushroomSelectedSlot: Int?,
    fernMosaicState: FernMosaicPuzzle?,
    mosaicStatus: String?,
    canopySpotterState: CanopySpotterPuzzle?,
    spotterStatus: String?,
    groveGridState: GroveGridPuzzle?,
    selectedGridCell: Int?,
    vineWeaverState: VineWeaverBoard?,
    spiritLockState: SpiritLockPuzzle?,
    completionResult: PuzzleCompletionResult?,
    onBack: () -> Unit,
    onRestartLevel: (Int) -> Unit,
    onTapBlossom: (Int) -> Unit,
    onReplayFireflyHint: () -> Unit,
    onTapMushroomSlot: (Int) -> Unit,
    onUseMushroomHint: () -> Unit,
    onSelectMosaicSlot: (Int) -> Unit,
    onChooseMosaicTile: (LeafToken) -> Unit,
    onUseMosaicHint: () -> Unit,
    onTapSpotterCharm: (Int) -> Unit,
    onUseSpotterHint: () -> Unit,
    onSelectGridCell: (Int) -> Unit,
    onPlaceGridRune: (Int) -> Unit,
    onUseGridHint: () -> Unit,
    onRotateVineTile: (Int, Int) -> Unit,
    onUseVineHint: () -> Unit,
    onAppendDraftRune: (SpiritRune) -> Unit,
    onRemoveLastDraftRune: () -> Unit,
    onSubmitSpiritDraft: () -> Unit,
    onUseSpiritHint: () -> Unit,
    onDismissCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showGuide by remember(puzzleType) { mutableStateOf(level == 1) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("puzzle_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = puzzleType.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Level $level · ${difficulty.label} · ${puzzleType.skill.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGuide = !showGuide },
                        modifier = Modifier.testTag("puzzle_guide_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = "Toggle how to play guide"
                        )
                    }
                    IconButton(
                        onClick = {
                            when (puzzleType) {
                                PuzzleType.FIREFLY_ECHO -> onReplayFireflyHint()
                                PuzzleType.MUSHROOM_MELODY -> onUseMushroomHint()
                                PuzzleType.FERN_MOSAIC -> onUseMosaicHint()
                                PuzzleType.CANOPY_SPOTTER -> onUseSpotterHint()
                                PuzzleType.GROVE_GRID -> onUseGridHint()
                                PuzzleType.VINE_WEAVER -> onUseVineHint()
                                PuzzleType.SPIRIT_LOCK -> onUseSpiritHint()
                            }
                        },
                        modifier = Modifier.testTag("puzzle_hint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = stringResource(R.string.cd_hint),
                            tint = FireflyGoldAccent
                        )
                    }
                    IconButton(
                        onClick = { onRestartLevel(level) },
                        modifier = Modifier.testTag("puzzle_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_reset_puzzle)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = showGuide) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = puzzleType.howToPlay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                when (puzzleType) {
                    PuzzleType.FIREFLY_ECHO -> {
                        FireflyEchoView(
                            state = fireflyState,
                            onTapBlossom = onTapBlossom,
                            onReplayHint = onReplayFireflyHint
                        )
                    }

                    PuzzleType.MUSHROOM_MELODY -> {
                        if (mushroomState != null) {
                            MushroomMelodyView(
                                puzzle = mushroomState,
                                selectedSlotIdx = mushroomSelectedSlot,
                                gentleMode = gentleMode,
                                onTapSlot = onTapMushroomSlot
                            )
                        }
                    }

                    PuzzleType.FERN_MOSAIC -> {
                        if (fernMosaicState != null) {
                            FernMosaicView(
                                puzzle = fernMosaicState,
                                gentleMode = gentleMode,
                                statusMessage = mosaicStatus,
                                onSelectSlot = onSelectMosaicSlot,
                                onSelectChoice = onChooseMosaicTile
                            )
                        }
                    }

                    PuzzleType.CANOPY_SPOTTER -> {
                        if (canopySpotterState != null) {
                            CanopySpotterView(
                                puzzle = canopySpotterState,
                                statusMessage = spotterStatus,
                                onTapCharm = onTapSpotterCharm
                            )
                        }
                    }

                    PuzzleType.GROVE_GRID -> {
                        if (groveGridState != null) {
                            GroveGridView(
                                puzzle = groveGridState,
                                selectedCell = selectedGridCell,
                                gentleMode = gentleMode,
                                onSelectCell = onSelectGridCell,
                                onPlaceRune = onPlaceGridRune
                            )
                        }
                    }

                    PuzzleType.VINE_WEAVER -> {
                        if (vineWeaverState != null) {
                            VineWeaverView(
                                board = vineWeaverState,
                                onRotateTile = onRotateVineTile
                            )
                        }
                    }

                    PuzzleType.SPIRIT_LOCK -> {
                        if (spiritLockState != null) {
                            SpiritLockView(
                                puzzle = spiritLockState,
                                onAppendRune = onAppendDraftRune,
                                onRemoveLast = onRemoveLastDraftRune,
                                onSubmitDraft = onSubmitSpiritDraft
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = completionResult != null,
                    enter = fadeIn(tween(240)) +
                        slideInVertically(initialOffsetY = { it / 4 }) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ),
                    exit = fadeOut(tween(150))
                ) {
                    val result = completionResult
                    if (result != null) {
                        Column {
                            Spacer(modifier = Modifier.height(20.dp))
                            PuzzleCelebrationCard(
                                result = result,
                                onNextLevel = {
                                    val next = result.nextLevelUnlocked
                                    onDismissCompletion()
                                    onRestartLevel(next)
                                },
                                onReturn = {
                                    onDismissCompletion()
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleCelebrationCard(
    result: PuzzleCompletionResult,
    onNextLevel: () -> Unit,
    onReturn: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(2.dp, FireflyGoldAccent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("puzzle_celebration_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..3) {
                    Icon(
                        imageVector = if (i <= result.starsEarned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null,
                        tint = FireflyGoldAccent,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Text(
                text = "Level ${result.completedLevel} Cleared!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = buildString {
                    append("Score ${result.score}")
                    if (result.newBestScore) append(" · New Personal Best!")
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                RewardChip("+${result.nectarEarned} Nectar")
                RewardChip("+${result.moonstonesEarned} Shards")
                RewardChip("+${result.skillXpEarned} ${result.puzzleType.skill.displayName} XP")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("celebration_next_level_button")
                ) {
                    Text("Play Level ${result.nextLevelUnlocked}")
                }
                OutlinedButton(
                    onClick = onReturn,
                    modifier = Modifier.testTag("celebration_return_button")
                ) {
                    Text("Sanctuary")
                }
            }
        }
    }
}

@Composable
private fun RewardChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
