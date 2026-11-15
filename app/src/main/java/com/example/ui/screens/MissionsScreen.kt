package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Foundation
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.model.CommunityMission
import com.example.model.LootItemType
import com.example.model.MissionCompletionSummary
import com.example.model.PuzzleType
import com.example.model.WildCreatureTemperament
import com.example.rpg.BattleActionType
import com.example.rpg.RpgBattleState
import com.example.stateHolder.FernfableUiState
import com.example.ui.components.HeroAvatar
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionsScreen(
    uiState: FernfableUiState,
    activeBattle: RpgBattleState?,
    missionSummary: MissionCompletionSummary?,
    onStartMission: (CommunityMission) -> Unit,
    onAdvanceCommunityStep: () -> Unit,
    onPerformBattleAction: (BattleActionType) -> Unit,
    onUseBattleLoot: (LootItemType) -> Unit,
    onRetreatFromBattle: () -> Unit,
    onDismissMissionSheet: () -> Unit,
    onRestMana: () -> Unit,
    onBrewLoot: (LootItemType) -> Unit,
    onLaunchPuzzle: (PuzzleType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
    val activeHero = JungleLoreCatalog.heroById(profile.selectedHeroId)
    val lootMap = RpgMissionCatalog.parseLootBag(profile.lootBagCsv)
    val completedMissions = profile.completedMissionIds

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
                Column {
                    Text(
                        text = "Community Missions & Wilds",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Build Solari Canopy landmarks, battle menacing & beautiful wild creatures, and gather XP, Magic, Mana & Loot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (profile.canopyRestored) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = FireflyGoldAccent.copy(alpha = 0.18f),
                        border = BorderStroke(2.dp, FireflyGoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_victory_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Canopy Champion",
                                tint = FireflyGoldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sanctuary Restored · Game Victory!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All Solari community structures stand radiant and the Shadowroot Sovereign is quelled! Replay missions or wild patrols anytime.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Community Prosperity & Hero Magic/Mana Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_prosperity_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroAvatar(
                                heroId = activeHero.id,
                                unlocked = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(56.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeHero.name} · Lv. ${profile.heroLevel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Solari Community Prosperity: ${profile.communityProsperity}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (profile.communityProsperity / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RpgResourceBadge(
                                icon = Icons.Rounded.Bolt,
                                label = "Mana",
                                value = "${profile.mana}/${profile.maxMana}",
                                tint = DewTealAccent,
                                modifier = Modifier.weight(1f)
                            )
                            RpgResourceBadge(
                                icon = Icons.Rounded.AutoAwesome,
                                label = "Magic",
                                value = "${profile.magicEssence} (+${profile.arcanePowerBonus})",
                                tint = StarlightVioletAccent,
                                modifier = Modifier.weight(1f)
                            )
                            RpgResourceBadge(
                                icon = Icons.Rounded.Foundation,
                                label = "Crests",
                                value = "${lootMap[LootItemType.CANOPY_CREST] ?: 0}",
                                tint = FireflyGoldAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Built Community Landmarks summary
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RpgMissionCatalog.communityMissions
                                .filter { !it.isFinalExpedition }
                                .forEach { m ->
                                    val built = m.id in completedMissions
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (built) FernEmeraldAccent.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (built) Icons.Rounded.CheckCircle else Icons.Rounded.Foundation,
                                                contentDescription = null,
                                                tint = if (built) FernEmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = m.communityProject,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (built) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                        }

                        // Battle Satchel & Sanctuary Preparation
                        Text(
                            text = "Battle Satchel & Preparation",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LootCounterChip(
                                name = "Tonic",
                                count = lootMap[LootItemType.SUNBERRY_TONIC] ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                            LootCounterChip(
                                name = "Mana Vial",
                                count = lootMap[LootItemType.MANA_VIAL] ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                            LootCounterChip(
                                name = "Sunfire",
                                count = lootMap[LootItemType.SUNFIRE_CHARM] ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = onRestMana,
                                enabled = profile.mana < profile.maxMana,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rest_mana_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Spa,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (profile.mana < profile.maxMana) "Refill Mana" else "Mana Full")
                            }
                            OutlinedButton(
                                onClick = { onBrewLoot(LootItemType.SUNBERRY_TONIC) },
                                enabled = profile.nectar >= 12,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("brew_tonic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalDrink,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Brew Tonic (12)")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Community Building Expeditions",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            items(RpgMissionCatalog.communityMissions, key = { it.id }) { mission ->
                val isCompleted = mission.id in completedMissions
                val isUnlocked = profile.communityProsperity >= mission.requiredProsperity
                val puzzleAccessed = mission.puzzleChallenge?.let { uiState.puzzleStats[it]?.accessed == true } ?: false
                MissionCard(
                    mission = mission,
                    isUnlocked = isUnlocked,
                    isCompleted = isCompleted,
                    puzzleAccessed = puzzleAccessed,
                    onEmbark = { onStartMission(mission) },
                    onAccessPuzzle = mission.puzzleChallenge?.let { puzzle ->
                        { onLaunchPuzzle(puzzle) }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wild Creature Patrols (Repeatable)",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Train hero XP, gather Arcane Magic, Mana, and battle loot in the wild.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(RpgMissionCatalog.wildPatrols, key = { it.id }) { patrol ->
                val isUnlocked = profile.communityProsperity >= patrol.requiredProsperity
                val puzzleAccessed = patrol.puzzleChallenge?.let { uiState.puzzleStats[it]?.accessed == true } ?: false
                MissionCard(
                    mission = patrol,
                    isUnlocked = isUnlocked,
                    isCompleted = patrol.id in completedMissions,
                    puzzleAccessed = puzzleAccessed,
                    onEmbark = { onStartMission(patrol) },
                    onAccessPuzzle = patrol.puzzleChallenge?.let { puzzle ->
                        { onLaunchPuzzle(puzzle) }
                    }
                )
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: CommunityMission,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    puzzleAccessed: Boolean,
    onEmbark: () -> Unit,
    onAccessPuzzle: (() -> Unit)?
) {
    val opponent = mission.opponent
    val isBeautiful = opponent.temperament == WildCreatureTemperament.BEAUTIFUL
    val temperamentColor = if (isBeautiful) StarlightVioletAccent else OrchidCoralAccent

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isUnlocked) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = if (isUnlocked) 3.dp else 0.dp,
        border = BorderStroke(
            width = if (mission.isFinalExpedition) 1.5.dp else 1.dp,
            color = when {
                mission.isFinalExpedition && isUnlocked -> FireflyGoldAccent
                isUnlocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mission_card_${mission.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = temperamentColor.copy(alpha = 0.16f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBeautiful) Icons.Rounded.AutoAwesome else Icons.Rounded.Whatshot,
                            contentDescription = null,
                            tint = temperamentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${opponent.temperament.label}: ${opponent.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FernEmeraldAccent.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (mission.isWildPatrol) "Cleared" else "Built",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = mission.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = mission.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Builds: ${mission.communityProject} — ${mission.communityBenefit}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+${opponent.rewardExp} XP · +${opponent.rewardMagic} Magic · +${opponent.rewardMana} Mana · ${opponent.guaranteedLoot.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isUnlocked) {
                Button(
                    onClick = onEmbark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("embark_mission_${mission.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            mission.isFinalExpedition && !isCompleted -> "Final Expedition (Rec. Lv.${mission.recommendedLevel})"
                            isCompleted -> "Patrol Again (Lv.${mission.recommendedLevel})"
                            else -> "Embark on Mission (Rec. Lv.${mission.recommendedLevel})"
                        }
                    )
                }

                val puzzle = mission.puzzleChallenge
                if (puzzle != null && onAccessPuzzle != null) {
                    OutlinedButton(
                        onClick = onAccessPuzzle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mission_puzzle_${mission.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (puzzleAccessed) {
                                "Mission Puzzle: ${puzzle.title} (Accessed)"
                            } else {
                                "Access Puzzle: ${puzzle.title}"
                            }
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Requires ${mission.requiredProsperity}% Community Prosperity",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RpgResourceBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LootCounterChip(
    name: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "x$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}