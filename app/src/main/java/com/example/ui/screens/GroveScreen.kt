package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.model.BiomeNode
import com.example.model.CommunityMission
import com.example.model.PuzzleType
import com.example.model.StoryChoice
import com.example.rpg.InteractableKind
import com.example.rpg.RpgWorldState
import com.example.rpg.WorldActionType
import com.example.stateHolder.FernfableUiState
import com.example.ui.components.CreaturePortrait
import com.example.ui.components.HeroAvatar
import com.example.ui.components.RpgWorldViewport
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.HeartRoseAccent
import com.example.ui.theme.StarlightVioletAccent

@Composable
fun GroveScreen(
    uiState: FernfableUiState,
    worldState: RpgWorldState,
    hasActiveBattle: Boolean = false,
    activeEncounter: BiomeNode?,
    encounterFeedback: StoryChoice?,
    userEmail: String? = null,
    isAudioMuted: Boolean = false,
    onToggleAudioMute: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenBiome: (BiomeNode) -> Unit,
    onDismissEncounter: () -> Unit,
    onChooseStoryOption: (BiomeNode, StoryChoice) -> Unit,
    onSelectHero: (String) -> Unit,
    onCraftOrEquipRelic: (String) -> Unit,
    onLaunchPuzzle: (PuzzleType) -> Unit,
    onOpenPuzzleHub: () -> Unit,
    onOpenMissions: () -> Unit = {},
    onStepWorldFrame: (Float, Float, Float) -> Unit = { _, _, _ -> },
    onSetWorldTapTarget: (Float, Float) -> Unit = { _, _ -> },
    onShowWorldDialogue: (String, String) -> Unit = { _, _ -> },
    onDismissWorldDialogue: () -> Unit = {},
    onPostWorldToast: (String) -> Unit = {},
    onInteractLantern: (String, String) -> Unit = { _, _ -> },
    onForageObject: (String, InteractableKind, String) -> Unit = { _, _, _ -> },
    onRestMana: () -> Unit = {},
    onStartWorldMission: (CommunityMission, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
    val activeHero = JungleLoreCatalog.heroById(profile.selectedHeroId)
    val equippedRelic = JungleLoreCatalog.relicById(profile.equippedRelicId)
    val nextLockedBiome = JungleLoreCatalog.biomes.firstOrNull { profile.nectar < it.unlockNectar }
    val nextQuestBiome = JungleLoreCatalog.biomes.firstOrNull {
        it.id !in profile.completedQuestIds
    } ?: JungleLoreCatalog.biomes.last()

    var showCampChapters by rememberSaveable { mutableStateOf(false) }
    var showHeroRoster by remember { mutableStateOf(false) }
    var showRelicForge by remember { mutableStateOf(false) }
    var showQuestLog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var questLogInitialTab by remember { mutableIntStateOf(0) }

    val totalMagic = activeHero.baseMagic + equippedRelic.magicBonus + (profile.heroLevel - 1)
    val totalTech = activeHero.baseTech + equippedRelic.techBonus + (profile.heroLevel - 1)
    val totalHeart = activeHero.baseHeart + equippedRelic.heartBonus + (profile.heroLevel - 1)
    val isWorldPaused = showCampChapters || showHeroRoster || showRelicForge || showQuestLog ||
            showSignOutDialog || activeEncounter != null || hasActiveBattle

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact Top RPG Mode & Hero HUD Strip
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 3.dp,
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroAvatar(
                        heroId = activeHero.id,
                        unlocked = true,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { showHeroRoster = true }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeHero.name} · Lv.${profile.heroLevel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Mana ${profile.mana}/${profile.maxMana} · ${profile.nectar} Nectar · ${profile.communityProsperity}% Town",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleAudioMute,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("world_audio_toggle")
                    ) {
                        Icon(
                            imageVector = if (isAudioMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.Rounded.MusicNote
                            },
                            contentDescription = stringResource(R.string.cd_toggle_audio),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            questLogInitialTab = 0
                            showQuestLog = true
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("top_quest_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (profile.dailyStreak > 0) "${profile.dailyStreak}d" else "Log",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showCampChapters,
                        onClick = { showCampChapters = false },
                        label = { Text("Explore World") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("mode_explore_world")
                    )
                    FilterChip(
                        selected = showCampChapters,
                        onClick = { showCampChapters = true },
                        label = { Text("Camp & Chapters") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("mode_camp_chapters")
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onOpenPuzzleHub,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("world_puzzles_shortcut")
                    ) {
                        val accessedCount = uiState.puzzleStats.values.count { it.accessed }
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$accessedCount/${PuzzleType.entries.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        if (!showCampChapters) {
            RpgWorldViewport(
                worldState = worldState,
                heroId = activeHero.id,
                heroLevel = profile.heroLevel,
                nectar = profile.nectar,
                communityProsperity = profile.communityProsperity,
                completedMissions = profile.completedMissionIds,
                isPaused = isWorldPaused,
                onStepFrame = onStepWorldFrame,
                onSetTapTarget = onSetWorldTapTarget,
                onTriggerPrimaryAction = { target ->
                    when (target.actionType) {
                        WorldActionType.BATTLE_CREATURE -> {
                            val missionId = target.creature?.missionId
                            val mission = missionId?.let { RpgMissionCatalog.missionById(it) }
                            if (mission != null) {
                                onStartWorldMission(mission, true)
                            }
                        }

                        WorldActionType.INTERACT_OBJECT -> {
                            val obj = target.interactable
                            val friendly = target.creature
                            if (obj != null) {
                                when (obj.kind) {
                                    InteractableKind.MANA_FOUNTAIN -> onRestMana()
                                    InteractableKind.MAGITECH_ANVIL -> showRelicForge = true
                                    InteractableKind.HERO_CAMPSTONE -> showHeroRoster = true
                                    InteractableKind.NPC_VILLAGER -> onShowWorldDialogue(obj.title, obj.dialogueText)
                                    InteractableKind.SPORE_LANTERN -> onInteractLantern(obj.id, obj.title)
                                    InteractableKind.HARVEST_NECTAR,
                                    InteractableKind.HARVEST_TONIC -> onForageObject(obj.id, obj.kind, obj.title)
                                    InteractableKind.PUZZLE_PEDESTAL -> onLaunchPuzzle(PuzzleType.fromId(obj.targetId))
                                    InteractableKind.CHAPTER_BEACON -> {
                                        val biome = JungleLoreCatalog.biomes.firstOrNull { it.id == obj.targetId }
                                        if (biome != null) {
                                            if (profile.nectar >= biome.unlockNectar) {
                                                onOpenBiome(biome)
                                            } else {
                                                onPostWorldToast("Gather ${biome.unlockNectar} Nectar to open ${biome.name}!")
                                            }
                                        }
                                    }
                                    InteractableKind.COMMUNITY_LANDMARK -> {
                                        val mission = RpgMissionCatalog.missionById(obj.targetId)
                                        if (mission != null) {
                                            if (mission.id in profile.completedMissionIds) {
                                                onShowWorldDialogue(
                                                    mission.communityProject,
                                                    "Built & flourishing! ${mission.communityBenefit}"
                                                )
                                            } else {
                                                onStartWorldMission(mission, false)
                                            }
                                        }
                                    }
                                    InteractableKind.FRIENDLY_CREATURE -> Unit
                                }
                            } else if (friendly != null) {
                                val creature = friendly.creatureId?.let { JungleLoreCatalog.creatureById(it) }
                                if (creature != null) {
                                    onShowWorldDialogue(
                                        "${creature.name} · ${creature.title}",
                                        creature.lore
                                    )
                                }
                            }
                        }

                        WorldActionType.NONE -> Unit
                    }
                },
                onDismissDialogue = onDismissWorldDialogue,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.syncError != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.syncError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    item {
                        // Storybook Canopy Hero Card
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.height(220.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_jungle_canopy_hero),
                                    contentDescription = stringResource(R.string.cd_jungle_hero),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0x550D1E16),
                                                    Color(0xAA0D1E16),
                                                    Color(0xEA0B1912)
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xCC163326)
                                        ) {
                                            Text(
                                                text = profile.explorerRank,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color(0xFFFFE7A3),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ResourcePill(
                                                icon = Icons.Rounded.WaterDrop,
                                                tint = FireflyGoldAccent,
                                                value = profile.nectar.toString(),
                                                label = stringResource(R.string.cd_nectar)
                                            )
                                            ResourcePill(
                                                icon = Icons.Rounded.NightsStay,
                                                tint = Color(0xFFB7C4FF),
                                                value = profile.moonstones.toString(),
                                                label = stringResource(R.string.cd_moonstone)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "Fernfable",
                                            style = MaterialTheme.typography.displaySmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Solari Magitech Canopy · Mythical Jungle RPG",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFDCEEE2)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val accessedCount = uiState.puzzleStats.values.count { it.accessed }
                                            Button(
                                                onClick = onOpenPuzzleHub,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("hero_anytime_puzzle_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Extension,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Puzzles ($accessedCount/${PuzzleType.entries.size})",
                                                    maxLines = 1
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    questLogInitialTab = 0
                                                    showQuestLog = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("hero_quest_log_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (profile.dailyStreak > 0) {
                                                        "Quests · ${profile.dailyStreak}d"
                                                    } else {
                                                        "Quest Log"
                                                    },
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Active Hero RPG Party & Gear HUD Card
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rpg_hero_hud_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HeroAvatar(
                                        heroId = activeHero.id,
                                        unlocked = true,
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.size(66.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = activeHero.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Lv. ${profile.heroLevel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${activeHero.title} · ${activeHero.heritage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = {
                                                (profile.heroExp.toFloat() / profile.expForNextLevel.toFloat()).coerceIn(0f, 1f)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "EXP ${profile.heroExp}/${profile.expForNextLevel} · Gear: ${equippedRelic.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RpgStatChip("Magic ${totalMagic + profile.arcanePowerBonus}", StarlightVioletAccent, Modifier.weight(1f))
                                    RpgStatChip("Mana ${profile.mana}/${profile.maxMana}", DewTealAccent, Modifier.weight(1f))
                                    RpgStatChip("Town ${profile.communityProsperity}%", FernEmeraldAccent, Modifier.weight(1f))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { showHeroRoster = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_hero_roster_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Heroes & Allies")
                                    }
                                    OutlinedButton(
                                        onClick = { showRelicForge = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_relic_forge_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Build,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Magitech Gear")
                                    }
                                }

                                Button(
                                    onClick = onOpenMissions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grove_open_missions_button")
                                ) {
                                    Text(
                                        text = if (profile.canopyRestored) {
                                            "Community Missions & Wild Patrols (Crown Restored!)"
                                        } else {
                                            "Go on Community Missions & Wild Battles (${profile.communityProsperity}%)"
                                        }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = "Cloud synced",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = userEmail?.takeIf { it.isNotBlank() }?.let { "Cloud Sync · $it" }
                                            ?: "Cloud Sanctuary Synced",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = onToggleAudioMute,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("grove_audio_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (isAudioMuted) {
                                                Icons.AutoMirrored.Rounded.VolumeOff
                                            } else {
                                                Icons.Rounded.MusicNote
                                            },
                                            contentDescription = stringResource(R.string.cd_toggle_audio),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(
                                                if (isAudioMuted) R.string.audio_muted else R.string.audio_rpg_music_on
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    TextButton(
                                        onClick = { showSignOutDialog = true },
                                        modifier = Modifier.testTag("sign_out_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                                            contentDescription = "Sign out",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sign Out")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Active RPG Quest Tracker Card
                        val questBiomeUnlocked = profile.nectar >= nextQuestBiome.unlockNectar
                        val allQuestsDone = JungleLoreCatalog.biomes.all { it.id in profile.completedQuestIds }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(FireflyGoldAccent.copy(alpha = 0.22f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (allQuestsDone) "All Canopy Quests Awakened!" else nextQuestBiome.questTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        val goalMsg = when {
                                            allQuestsDone -> "Every grove shines! Keep mastering all 7 mini-puzzles & welcoming allies."
                                            questBiomeUnlocked -> nextQuestBiome.questObjective
                                            nextLockedBiome != null -> {
                                                val remaining = (nextLockedBiome.unlockNectar - profile.nectar).coerceAtLeast(1)
                                                "Earn $remaining more Sunbeam Nectar from mini-puzzles to unlock ${nextLockedBiome.name}."
                                            }
                                            else -> nextQuestBiome.questObjective
                                        }
                                        Text(
                                            text = goalMsg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (questBiomeUnlocked) {
                                        Button(
                                            onClick = { onOpenBiome(nextQuestBiome) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("continue_quest_button")
                                        ) {
                                            Text("Enter Quest")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onLaunchPuzzle(nextQuestBiome.signaturePuzzle) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("quest_quick_puzzle_button")
                                    ) {
                                        Text("Play ${nextQuestBiome.signaturePuzzle.title}")
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            questLogInitialTab = 0
                                            showQuestLog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_quest_log_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Quest Log (${profile.completedQuestIds.size}/${JungleLoreCatalog.biomes.size})")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            questLogInitialTab = 2
                                            showQuestLog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_lore_log_button")
                                    ) {
                                        Text("Discovered Lore")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Mythical Jungle Chapters",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(JungleLoreCatalog.biomes, key = { it.id }) { biome ->
                        val creature = JungleLoreCatalog.creatureById(biome.creatureId)
                        val progress = uiState.creatureProgress[biome.creatureId]
                        val isUnlocked = profile.nectar >= biome.unlockNectar || (progress?.discovered == true)
                        val isQuestDone = biome.id in profile.completedQuestIds
                        val bondLevel = progress?.bondLevel ?: 0

                        BiomeTrailCard(
                            biome = biome,
                            creatureName = creature.name,
                            isUnlocked = isUnlocked,
                            isQuestDone = isQuestDone,
                            bondLevel = bondLevel,
                            currentNectar = profile.nectar,
                            onClick = { if (isUnlocked) onOpenBiome(biome) }
                        )
                    }
                }
            }
        }
    }

    if (showHeroRoster) {
        HeroRosterSheet(
            profile = profile,
            onSelectHero = { heroId ->
                onSelectHero(heroId)
                showHeroRoster = false
            },
            onDismiss = { showHeroRoster = false }
        )
    }

    if (showRelicForge) {
        MagitechWorkshopSheet(
            profile = profile,
            onCraftOrEquipRelic = onCraftOrEquipRelic,
            onDismiss = { showRelicForge = false }
        )
    }

    if (activeEncounter != null) {
        val bondLevel = uiState.creatureProgress[activeEncounter.creatureId]?.bondLevel ?: 1
        BiomeEncounterSheet(
            biome = activeEncounter,
            bondLevel = bondLevel,
            isQuestCompleted = activeEncounter.id in profile.completedQuestIds,
            encounterFeedback = encounterFeedback,
            onDismiss = onDismissEncounter,
            onChooseStoryOption = onChooseStoryOption,
            onLaunchPuzzle = onLaunchPuzzle
        )
    }

    if (showQuestLog) {
        QuestLogSheet(
            uiState = uiState,
            initialTab = questLogInitialTab,
            onOpenBiome = onOpenBiome,
            onLaunchPuzzle = onLaunchPuzzle,
            onDismiss = { showQuestLog = false }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out of Fernfable?") },
            text = {
                Text("Your Solari heroes, creature bonds, and brain-teaser progress are safely stored in your Google account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }
}

@Composable
private fun RpgStatChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ResourcePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    value: String,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xCC162920)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BiomeTrailCard(
    biome: BiomeNode,
    creatureName: String,
    isUnlocked: Boolean,
    isQuestDone: Boolean,
    bondLevel: Int,
    currentNectar: Int,
    onClick: () -> Unit
) {
    val creature = JungleLoreCatalog.creatureById(biome.creatureId)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isUnlocked) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isUnlocked) 3.dp else 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("biome_card_${biome.id}")
            .clickable(enabled = isUnlocked, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CreaturePortrait(
                creature = creature,
                discovered = isUnlocked,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${biome.index + 1}. ${biome.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = biome.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = creatureName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        BondHeartsRow(bondLevel = bondLevel)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Unlocks at ${biome.unlockNectar} Nectar ($currentNectar/${biome.unlockNectar})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isQuestDone) FernEmeraldAccent.copy(alpha = 0.16f)
                    else DewTealAccent.copy(alpha = 0.16f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = if (isQuestDone) "Clear" else "Quest",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isQuestDone) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BondHeartsRow(bondLevel: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..3) {
            Icon(
                imageVector = if (i <= bondLevel) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (i <= bondLevel) "Earned heart" else "Empty heart",
                tint = if (i <= bondLevel) HeartRoseAccent else StarlightVioletAccent.copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}