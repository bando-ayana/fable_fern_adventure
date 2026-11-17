package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BrainSkill
import com.example.model.DifficultyTier
import com.example.model.PuzzleType
import com.example.stateHolder.FernfableUiState
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@Composable
fun PuzzleHubScreen(
    uiState: FernfableUiState,
    onSelectDifficulty: (DifficultyTier) -> Unit,
    onToggleGentleMode: (Boolean) -> Unit,
    onStartPuzzle: (PuzzleType, Int?) -> Unit,
    onNavigateToGrove: () -> Unit = {},
    onNavigateToMissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val a11y = LocalAccessibilityUi.current
    val accessedPuzzles = PuzzleType.entries.filter { uiState.puzzleStats[it]?.accessed == true }
    val lockedPuzzles = PuzzleType.entries.filter { uiState.puzzleStats[it]?.accessed != true }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (a11y.calmMode) 18.dp else 14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.speakOnTouch(
                        "Accessed Puzzles and High Scores. ${accessedPuzzles.size} of ${PuzzleType.entries.size} puzzles unlocked."
                    )
                ) {
                    Text(
                        text = "Accessed Puzzles & High Scores",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (!a11y.calmMode) {
                        Text(
                            text = "Access puzzles during RPG Chapters & Community Missions—then view your stats and replay them here at higher paces for bigger points!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // Difficulty Ladder & Cozy Assist Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Challenge Pace (Higher Pace = Higher Points)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DifficultyTier.entries.forEach { tier ->
                                val multText = when (tier) {
                                    DifficultyTier.SPROUT -> "1x"
                                    DifficultyTier.EXPLORER -> "1.35x"
                                    DifficultyTier.ELDER -> "1.75x"
                                }
                                FilterChip(
                                    selected = uiState.selectedDifficulty == tier,
                                    onClick = { onSelectDifficulty(tier) },
                                    label = { Text("${tier.label} $multText") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .speakOnTouch("Difficulty ${tier.label}, $multText score multiplier")
                                        .testTag("difficulty_chip_${tier.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .speakOnTouch(
                                    "Cozy Assist is ${if (uiState.profile.gentleMode) "ON" else "OFF"}. Helpful conflict cues and extra calm hints."
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cozy Assist",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (uiState.profile.gentleMode) {
                                        "ON — Helpful conflict cues, forgiving turns & extra calm hints"
                                    } else {
                                        "OFF — Pure mastery deduction without automatic cues"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.profile.gentleMode,
                                onCheckedChange = onToggleGentleMode,
                                modifier = Modifier
                                    .speakOnTouch("Toggle Cozy Assist")
                                    .testTag("gentle_mode_switch")
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accessed & Replayable (${accessedPuzzles.size}/${PuzzleType.entries.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (accessedPuzzles.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("puzzles_empty_accessed_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Discover Your First Puzzle in the RPG!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Puzzles unlock here as soon as you access them during Grove Story Chapters, Community Missions, or Creature Encounters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onNavigateToGrove,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .speakOnTouch("Go to Grove to find puzzles")
                                        .testTag("hub_go_to_grove_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Explore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play Grove")
                                }
                                FilledTonalButton(
                                    onClick = onNavigateToMissions,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .speakOnTouch("Go to Missions to find puzzles")
                                        .testTag("hub_go_to_missions_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Missions")
                                }
                            }
                        }
                    }
                }
            } else {
                items(accessedPuzzles, key = { it.id }) { puzzle ->
                    val stats = uiState.puzzleStats[puzzle]
                    val currentLevel = stats?.currentLevel ?: 1
                    val stars = stats?.totalStars ?: 0
                    val bestScore = stats?.bestScore ?: 0
                    val solved = stats?.solvedCount ?: 0

                    PuzzleSanctuaryCard(
                        puzzle = puzzle,
                        currentLevel = currentLevel,
                        totalStars = stars,
                        bestScore = bestScore,
                        solvedCount = solved,
                        onPlayLevel = { lvl -> onStartPuzzle(puzzle, lvl) }
                    )
                }
            }

            if (lockedPuzzles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Discover in RPG Gameplay (${lockedPuzzles.size} Remaining)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Access these puzzles inside Chapters or Community Missions to add them to your replay collection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(lockedPuzzles, key = { "locked_${it.id}" }) { puzzle ->
                    LockedPuzzleCard(
                        puzzle = puzzle,
                        onNavigateToGrove = onNavigateToGrove,
                        onNavigateToMissions = onNavigateToMissions
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleSanctuaryCard(
    puzzle: PuzzleType,
    currentLevel: Int,
    totalStars: Int,
    bestScore: Int,
    solvedCount: Int,
    onPlayLevel: (Int) -> Unit
) {
    val a11y = LocalAccessibilityUi.current
    val (icon, accent) = puzzleStyle(puzzle)
    val replayableLevels = (1..currentLevel).toList()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .speakOnTouch("${puzzle.title}, ${puzzle.skill.displayName}. Level $currentLevel, best score $bestScore points.")
            .testTag("puzzle_card_${puzzle.id}")
    ) {
        Column(
            modifier = Modifier.padding(if (a11y.calmMode) 18.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (a11y.calmMode) 12.dp else 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = puzzle.title,
                            tint = accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = puzzle.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!a11y.calmMode) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = puzzle.skill.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = puzzle.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Level $currentLevel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "Personal Best Score",
                        tint = FireflyGoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Best $bestScore pts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Stars earned",
                        tint = FireflyGoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$totalStars",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!a11y.calmMode) {
                    Text(
                        text = "$solvedCount won",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val playLabel = if (solvedCount > 0) {
                "Play Level $currentLevel · Beat $bestScore pts"
            } else {
                "Play Level $currentLevel"
            }
            Button(
                onClick = { onPlayLevel(currentLevel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .speakOnTouch("Play ${puzzle.title} Level $currentLevel")
                    .testTag("play_puzzle_${puzzle.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = playLabel)
            }

            if (currentLevel > 1 || solvedCount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Replay unlocked levels for higher points:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(replayableLevels, key = { "${puzzle.id}_lv_$it" }) { lvl ->
                            OutlinedButton(
                                onClick = { onPlayLevel(lvl) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .speakOnTouch("Replay ${puzzle.title} Level $lvl")
                                    .testTag(
                                        if (lvl == 1) "warmup_puzzle_${puzzle.id}" else "replay_${puzzle.id}_lv_$lvl"
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lv.$lvl")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedPuzzleCard(
    puzzle: PuzzleType,
    onNavigateToGrove: () -> Unit,
    onNavigateToMissions: () -> Unit
) {
    val a11y = LocalAccessibilityUi.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .speakOnTouch("Locked puzzle ${puzzle.title}. ${puzzle.discoveryHint}")
            .testTag("locked_puzzle_card_${puzzle.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked puzzle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = puzzle.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!a11y.calmMode) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = puzzle.skill.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = puzzle.discoveryHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onNavigateToGrove,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .speakOnTouch("Open Grove to find ${puzzle.title}")
                        .testTag("locked_go_grove_${puzzle.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Grove")
                }
                OutlinedButton(
                    onClick = onNavigateToMissions,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .speakOnTouch("Open Missions to find ${puzzle.title}")
                        .testTag("locked_go_missions_${puzzle.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Missions")
                }
            }
        }
    }
}

fun puzzleStyle(puzzle: PuzzleType): Pair<ImageVector, Color> = when (puzzle) {
    PuzzleType.FIREFLY_ECHO -> Icons.Rounded.AutoFixHigh to FireflyGoldAccent
    PuzzleType.MUSHROOM_MELODY -> Icons.Rounded.Spa to OrchidCoralAccent
    PuzzleType.FERN_MOSAIC -> Icons.Rounded.Eco to FernEmeraldAccent
    PuzzleType.CANOPY_SPOTTER -> Icons.Rounded.Visibility to DewTealAccent
    PuzzleType.GROVE_GRID -> Icons.Rounded.GridView to FernEmeraldAccent
    PuzzleType.VINE_WEAVER -> Icons.Rounded.Route to DewTealAccent
    PuzzleType.SPIRIT_LOCK -> Icons.Rounded.LockOpen to StarlightVioletAccent
}

fun skillStyle(skill: BrainSkill): Pair<ImageVector, Color> = when (skill) {
    BrainSkill.MEMORY -> Icons.Rounded.Psychology to FireflyGoldAccent
    BrainSkill.LOGIC -> Icons.Rounded.GridView to FernEmeraldAccent
    BrainSkill.SPATIAL -> Icons.Rounded.Route to DewTealAccent
    BrainSkill.DEDUCTION -> Icons.Rounded.LockOpen to StarlightVioletAccent
    BrainSkill.OBSERVATION -> Icons.Rounded.Visibility to OrchidCoralAccent
}
