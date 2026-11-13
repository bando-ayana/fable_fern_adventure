package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.JungleLoreCatalog
import com.example.data.local.PlayerProfileEntity
import com.example.model.BiomeNode
import com.example.model.PuzzleType
import com.example.model.StoryChoice
import com.example.ui.components.CreaturePortrait
import com.example.ui.components.HeroAvatar
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroRosterSheet(
    profile: PlayerProfileEntity,
    onSelectHero: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Solari Vanguard & Allies",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Begin with Solari's thriving magitech pioneers, then welcome travelers of every race as Sunbeam Nectar grows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            JungleLoreCatalog.heroes.forEach { hero ->
                val isUnlocked = profile.nectar >= hero.unlockNectar
                val isSelected = profile.selectedHeroId == hero.id

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        isUnlocked -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_card_${hero.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HeroAvatar(
                                heroId = hero.id,
                                unlocked = isUnlocked,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${hero.name} · ${hero.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = hero.heritage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Magic ${hero.baseMagic} · Tech ${hero.baseTech} · Heart ${hero.baseHeart}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = hero.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = FireflyGoldAccent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = hero.specialty,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (isSelected) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Active hero",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isUnlocked) {
                                FilledTonalButton(
                                    onClick = { onSelectHero(hero.id) },
                                    modifier = Modifier.testTag("select_hero_${hero.id}")
                                ) {
                                    Text("Play Hero")
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${hero.unlockNectar} Nectar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagitechWorkshopSheet(
    profile: PlayerProfileEntity,
    onCraftOrEquipRelic: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val unlockedIds = profile.unlockedRelicIds

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Solari Magitech Forge",
                    style = MaterialTheme.typography.headlineSmall
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = "Moonstone Shards",
                            tint = DewTealAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.moonstones} Shards",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Craft and equip Solari solar-crystal gear with Moonstone Shards earned from quests & puzzles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            JungleLoreCatalog.relics.forEach { relic ->
                val isUnlocked = relic.id in unlockedIds
                val isEquipped = profile.equippedRelicId == relic.id

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isEquipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isEquipped) 2.dp else 1.dp,
                        color = if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("relic_card_${relic.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = relic.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = relic.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    if (relic.magicBonus > 0) append("+${relic.magicBonus} Magic  ")
                                    if (relic.techBonus > 0) append("+${relic.techBonus} Tech  ")
                                    if (relic.heartBonus > 0) append("+${relic.heartBonus} Heart  ")
                                    if (relic.bonusNectar > 0) append("+${relic.bonusNectar} Nectar")
                                }.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = FernEmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        when {
                            isEquipped -> {
                                Text(
                                    text = "Equipped",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isUnlocked -> {
                                FilledTonalButton(
                                    onClick = { onCraftOrEquipRelic(relic.id) },
                                    modifier = Modifier.testTag("equip_relic_${relic.id}")
                                ) {
                                    Text("Equip")
                                }
                            }
                            else -> {
                                Button(
                                    enabled = profile.moonstones >= relic.shardCost,
                                    onClick = { onCraftOrEquipRelic(relic.id) },
                                    modifier = Modifier.testTag("forge_relic_${relic.id}")
                                ) {
                                    Text("Forge (${relic.shardCost})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiomeEncounterSheet(
    biome: BiomeNode,
    bondLevel: Int,
    isQuestCompleted: Boolean,
    encounterFeedback: StoryChoice?,
    onDismiss: () -> Unit,
    onChooseStoryOption: (BiomeNode, StoryChoice) -> Unit,
    onLaunchPuzzle: (PuzzleType) -> Unit
) {
    val creature = JungleLoreCatalog.creatureById(biome.creatureId)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreaturePortrait(
                    creature = creature,
                    discovered = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = biome.questTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = biome.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${creature.name} · ${creature.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BondHeartsRow(bondLevel = bondLevel)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isQuestCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isQuestCompleted) {
                            "Chapter Cleared — Revisit anytime for bonus EXP & puzzles!"
                        } else {
                            "Quest: ${biome.questObjective}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = encounterFeedback?.responseText ?: biome.storyIntro,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (encounterFeedback == null) {
                biome.storyChoices.forEach { choice ->
                    FilledTonalButton(
                        onClick = { onChooseStoryOption(biome, choice) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("story_choice_${choice.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = choice.label,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${choice.rewardExp} EXP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLaunchPuzzle(biome.signaturePuzzle)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("encounter_anytime_puzzle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jump to ${biome.signaturePuzzle.title} Mini-Puzzle")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val puzzleToOpen = encounterFeedback.opensPuzzle ?: biome.signaturePuzzle
                    Button(
                        onClick = {
                            onDismiss()
                            onLaunchPuzzle(puzzleToOpen)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("encounter_play_puzzle_button")
                    ) {
                        Text("Play ${puzzleToOpen.title}")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("encounter_done_button")
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
