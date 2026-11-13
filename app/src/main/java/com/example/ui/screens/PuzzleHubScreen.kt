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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.ui.FernfableUiState
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Brain-Teaser Sanctuary",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Play anytime! Each level builds sharper focus, gently paced for every age and skill.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            text = "Challenge Pace",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DifficultyTier.entries.forEach { tier ->
                                FilterChip(
                                    selected = uiState.selectedDifficulty == tier,
                                    onClick = { onSelectDifficulty(tier) },
                                    label = { Text(tier.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("difficulty_chip_${tier.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.testTag("gentle_mode_switch")
                            )
                        }
                    }
                }
            }

            items(PuzzleType.entries, key = { it.id }) { puzzle ->
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
                    onPlayCurrent = { onStartPuzzle(puzzle, currentLevel) },
                    onPlayWarmup = if (currentLevel > 1) {
                        { onStartPuzzle(puzzle, 1) }
                    } else null
                )
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
    onPlayCurrent: () -> Unit,
    onPlayWarmup: (() -> Unit)?
) {
    val (icon, accent) = puzzleStyle(puzzle)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("puzzle_card_${puzzle.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text(
                        text = puzzle.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    Text(
                        text = "Best $bestScore · $solvedCount won",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPlayCurrent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_puzzle_${puzzle.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Level $currentLevel")
                }
                if (onPlayWarmup != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onPlayWarmup,
                        modifier = Modifier.testTag("warmup_puzzle_${puzzle.id}")
                    ) {
                        Text("Warmup Lv.1")
                    }
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
