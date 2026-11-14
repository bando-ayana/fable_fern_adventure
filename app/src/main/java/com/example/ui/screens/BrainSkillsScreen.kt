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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.JungleLoreCatalog
import com.example.data.local.PuzzleStatsEntity
import com.example.model.BadgeDefinition
import com.example.model.BrainSkill
import com.example.model.PuzzleType
import com.example.stateHolder.FernfableUiState
import com.example.ui.components.SkillRadarChart
import com.example.ui.theme.FireflyGoldAccent

@Composable
fun BrainSkillsScreen(
    uiState: FernfableUiState,
    onTrainSkill: (PuzzleType) -> Unit,
    onExploreRpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
    val totalStars = uiState.puzzleStats.values.sumOf { it.totalStars }
    val discoveredCreatures = uiState.creatureProgress.values.count { it.discovered }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Brain Skills & Mastery",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${profile.explorerRank} · ${profile.totalXp} Total Cognitive XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SkillRadarChart(
                            memoryXp = profile.memoryXp,
                            logicXp = profile.logicXp,
                            spatialXp = profile.spatialXp,
                            deductionXp = profile.deductionXp,
                            observationXp = profile.observationXp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBlock("Puzzles Won", profile.totalPuzzlesSolved.toString())
                            StatBlock("Stars", totalStars.toString())
                            StatBlock("Best Streak", profile.bestStreak.toString())
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Cognitive Pillars",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(BrainSkill.entries, key = { it.name }) { skill ->
                val xp = when (skill) {
                    BrainSkill.MEMORY -> profile.memoryXp
                    BrainSkill.LOGIC -> profile.logicXp
                    BrainSkill.SPATIAL -> profile.spatialXp
                    BrainSkill.DEDUCTION -> profile.deductionXp
                    BrainSkill.OBSERVATION -> profile.observationXp
                }
                val matchingPuzzle = PuzzleType.entries.first { it.skill == skill }
                val isPuzzleAccessed = uiState.puzzleStats[matchingPuzzle]?.accessed == true
                val (icon, accent) = skillStyle(skill)
                val level = (xp / 60) + 1
                val levelProgress = (xp % 60) / 60f

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = accent.copy(alpha = 0.16f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = skill.displayName,
                                    tint = accent
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
                                    text = skill.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tier $level · $xp XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isPuzzleAccessed) {
                                    "${skill.subtitle} · ${matchingPuzzle.title} unlocked"
                                } else {
                                    "Access ${matchingPuzzle.title} in RPG to train here"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { levelProgress },
                                color = accent,
                                trackColor = accent.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalButton(
                            onClick = {
                                if (isPuzzleAccessed) onTrainSkill(matchingPuzzle) else onExploreRpg()
                            },
                            modifier = Modifier.testTag("train_skill_${skill.name.lowercase()}")
                        ) {
                            if (!isPuzzleAccessed) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(if (isPuzzleAccessed) "Replay" else "Find in RPG")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Sanctuary Badges",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(JungleLoreCatalog.badges, key = { it.id }) { badge ->
                val unlocked = isBadgeUnlocked(
                    badge = badge,
                    totalSolved = profile.totalPuzzlesSolved,
                    discoveredCreatures = discoveredCreatures,
                    memoryXp = profile.memoryXp,
                    logicXp = profile.logicXp,
                    spatialXp = profile.spatialXp,
                    deductionXp = profile.deductionXp,
                    observationXp = profile.observationXp,
                    puzzleStats = uiState.puzzleStats
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (unlocked) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (unlocked) FireflyGoldAccent else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (unlocked) Icons.Rounded.Verified else Icons.Rounded.EmojiEvents,
                            contentDescription = if (unlocked) "Unlocked badge" else "Locked badge",
                            tint = if (unlocked) FireflyGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (unlocked) "Earned" else badge.thresholdLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (unlocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isBadgeUnlocked(
    badge: BadgeDefinition,
    totalSolved: Int,
    discoveredCreatures: Int,
    memoryXp: Int,
    logicXp: Int,
    spatialXp: Int,
    deductionXp: Int,
    observationXp: Int,
    puzzleStats: Map<PuzzleType, PuzzleStatsEntity>
): Boolean = when (badge.id) {
    "first_spark" -> totalSolved >= 1
    "kindred_spirit" -> discoveredCreatures >= 3
    "echo_keeper" -> memoryXp >= 100
    "rune_scholar" -> logicXp >= 100
    "stream_weaver" -> spatialXp >= 100
    "cipher_whisperer" -> deductionXp >= 100
    "keen_eyed" -> observationXp >= 100
    "canopy_polymath" -> PuzzleType.entries.all { (puzzleStats[it]?.solvedCount ?: 0) >= 1 }
    "canopy_guardian" -> totalSolved >= 15
    else -> false
}
