package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Foundation
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.LootItemType
import com.example.model.MissionCompletionSummary
import com.example.model.PuzzleType
import com.example.model.WildCreatureTemperament
import com.example.rpg.BattleActionType
import com.example.rpg.BattlePhase
import com.example.rpg.RpgBattleEngine
import com.example.rpg.RpgBattleState
import com.example.ui.components.BattleArenaStage
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.HeartRoseAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionBattleSheet(
    battleState: RpgBattleState,
    heroId: String = "zuri",
    missionSummary: MissionCompletionSummary?,
    onAdvanceCommunityStep: () -> Unit,
    onPerformAction: (BattleActionType) -> Unit,
    onUseLoot: (LootItemType) -> Unit,
    onRetreat: () -> Unit,
    onDismiss: () -> Unit,
    isRecordingMissionReward: Boolean,
    onLaunchPuzzle: (PuzzleType, Int?, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val opponent = battleState.mission.opponent
    val isBeautiful = opponent.temperament == WildCreatureTemperament.BEAUTIFUL
    val enemyAccent = if (isBeautiful) StarlightVioletAccent else OrchidCoralAccent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedContent(
                targetState = battleState.phase,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "mission_battle_phase"
            ) { phase ->
                when (phase) {
                    BattlePhase.COMMUNITY_PREP -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = FernEmeraldAccent.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Foundation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Community Project: ${battleState.mission.communityProject}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = battleState.mission.communityBenefit,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text(
                                text = battleState.mission.title,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = battleState.mission.summary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = enemyAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, enemyAccent.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Wild Report · ${opponent.temperament.label}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = enemyAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${opponent.name} (${opponent.title})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = opponent.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Button(
                                onClick = onAdvanceCommunityStep,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("begin_mission_action_button")
                            ) {
                                Text(battleState.mission.communityActionLabel)
                            }
                            val puzzleChallenge = battleState.mission.puzzleChallenge
                            if (puzzleChallenge != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onLaunchPuzzle(puzzleChallenge, null, true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("battle_prep_puzzle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Extension,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scout & Access ${puzzleChallenge.title} Puzzle")
                                }
                            }
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Return Later")
                            }
                        }
                    }

                    BattlePhase.ACTIVE_COMBAT -> {
                        val enemyHpFraction by animateFloatAsState(
                            targetValue = (battleState.enemyHp.toFloat() / battleState.enemyMaxHp.toFloat()).coerceIn(0f, 1f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "enemy_hp"
                        )
                        val heroHpFraction by animateFloatAsState(
                            targetValue = (battleState.heroHp.toFloat() / battleState.heroMaxHp.toFloat()).coerceIn(0f, 1f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "hero_hp"
                        )
                        val heroManaFraction by animateFloatAsState(
                            targetValue = (battleState.heroMana.toFloat() / battleState.heroMaxMana.toFloat()).coerceIn(0f, 1f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "hero_mana"
                        )
                        val creaturePulse by animateFloatAsState(
                            targetValue = if (battleState.lastEnemyHitTick % 2 == 1) 0.96f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "enemy_hit_scale"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BattleArenaStage(
                                heroId = heroId,
                                battleState = battleState,
                                modifier = Modifier.testTag("battle_visual_arena")
                            )

                            // Wild Creature Arena Card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = enemyAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.5.dp, enemyAccent.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(creaturePulse)
                                    .testTag("battle_enemy_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    enemyAccent.copy(alpha = 0.14f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(enemyAccent.copy(alpha = 0.25f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isBeautiful) Icons.Rounded.AutoAwesome else Icons.Rounded.Whatshot,
                                                contentDescription = opponent.name,
                                                tint = enemyAccent,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = opponent.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${opponent.temperament.label} · Round ${battleState.roundNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = enemyAccent
                                            )
                                        }
                                        Text(
                                            text = "${battleState.enemyHp}/${battleState.enemyMaxHp} HP",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { enemyHpFraction },
                                        color = enemyAccent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                    )

                                    Text(
                                        text = "Telegraph: ${battleState.upcomingEnemyMove} (~${battleState.upcomingEnemyDamage} DMG)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Hero HP & Mana Status Card
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = battleState.heroName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (battleState.wardActive) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = FernEmeraldAccent.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Ward Shielded",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = "Health",
                                            tint = HeartRoseAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        LinearProgressIndicator(
                                            progress = { heroHpFraction },
                                            color = HeartRoseAccent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${battleState.heroHp}/${battleState.heroMaxHp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bolt,
                                            contentDescription = "Mana",
                                            tint = DewTealAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        LinearProgressIndicator(
                                            progress = { heroManaFraction },
                                            color = DewTealAccent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${battleState.heroMana}/${battleState.heroMaxMana}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Combat Log
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    battleState.combatLog.take(3).forEachIndexed { idx, line ->
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (idx == 0) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Primary Tactical Combat Actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onPerformAction(BattleActionType.SOLAR_STRIKE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("battle_solar_strike_button")
                                ) {
                                    Text("Solar Strike (+6 Mana)")
                                }
                                FilledTonalButton(
                                    onClick = { onPerformAction(BattleActionType.ARCANE_SPELL) },
                                    enabled = battleState.canCastArcaneSpell,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("battle_arcane_spell_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Spell (-${RpgBattleEngine.ARCANE_SPELL_COST})")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onPerformAction(BattleActionType.GUARDIAN_WARD) },
                                    enabled = battleState.canCastWard,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("battle_ward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ward (-${RpgBattleEngine.WARD_SPELL_COST})")
                                }
                                OutlinedButton(
                                    onClick = { onPerformAction(BattleActionType.SOOTHE_SPIRIT) },
                                    enabled = battleState.canSootheSpirit,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("battle_soothe_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Spa,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (battleState.canSootheSpirit) "Calm (-8)" else "Weaken <45%")
                                }
                            }

                            // Battle Satchel Loot Items
                            val usableItems = listOf(
                                LootItemType.SUNBERRY_TONIC,
                                LootItemType.MANA_VIAL,
                                LootItemType.SUNFIRE_CHARM
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                usableItems.forEach { item ->
                                    val qty = battleState.lootAvailable[item] ?: 0
                                    FilledTonalButton(
                                        onClick = { onUseLoot(item) },
                                        enabled = qty > 0,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("battle_loot_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocalDrink,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${item.displayName.split(" ").first()} ($qty)",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = onRetreat,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("battle_retreat_button")
                            ) {
                                Text("Retreat Safely")
                            }
                        }
                    }

                    BattlePhase.VICTORY, BattlePhase.RETREATED -> {
                        if (missionSummary == null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isRecordingMissionReward) {
                                    Text(
                                        text = "Recording mission rewards...",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                } else {
                                    Text(
                                        text = "Mission rewards could not be recorded. Please return to Missions and try again.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("mission_recording_failed_done_button")
                                    ) {
                                        Text("Back to Missions")
                                    }
                                }
                            }
                        } else if (!missionSummary.victory) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Safe Tactical Retreat",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "You regrouped cleanly back at Solari Canopy. Refill your Mana, brew a Sunberry Tonic, and return stronger!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mission_retreat_done_button")
                                ) {
                                    Text("Back to Missions")
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (missionSummary.gameBeatenJustNow) Icons.Rounded.EmojiEvents else Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = FireflyGoldAccent,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = when {
                                        missionSummary.gameBeatenJustNow -> "Canopy Crown Awakened · You Beat the Game!"
                                        missionSummary.calmedSpirit -> "${opponent.name} Harmonized!"
                                        else -> "Mission Accomplished!"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                                if (missionSummary.leveledUpTo != null) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = FireflyGoldAccent.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = "Level Up! Hero reached Lv. ${missionSummary.leveledUpTo}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Community built: ${missionSummary.mission.communityProject}" +
                                        if (missionSummary.prosperityGained > 0) " (+${missionSummary.prosperityGained}% Prosperity)" else "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Spoils & Growth Earned",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "+${missionSummary.expGained} Hero XP · +${missionSummary.magicGained} Arcane Magic · +${missionSummary.manaGained} Mana",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "+${missionSummary.nectarGained} Sunbeam Nectar · +${missionSummary.moonstonesGained} Moonstone Shards",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (missionSummary.lootEarned.isNotEmpty()) {
                                            Text(
                                                text = "Loot Found: ${missionSummary.lootEarned.joinToString(", ") { it.displayName }}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mission_victory_done_button")
                                ) {
                                    Text("Claim Spoils & Continue")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}