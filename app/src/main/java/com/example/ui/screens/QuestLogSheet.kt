package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.JungleLoreCatalog
import com.example.model.BiomeNode
import com.example.model.LoreCategory
import com.example.model.PuzzleType
import com.example.ui.FernfableUiState
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestLogSheet(
    uiState: FernfableUiState,
    initialTab: Int = 0,
    onOpenBiome: (BiomeNode) -> Unit,
    onLaunchPuzzle: (PuzzleType) -> Unit,
    onDismiss: () -> Unit
) {
    val profile = uiState.profile
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var selectedLoreCategory by remember { mutableStateOf<LoreCategory?>(null) }

    val discoveredCreatureIds = remember(uiState.creatureProgress) {
        uiState.creatureProgress.values.filter { it.discovered }.map { it.creatureId }.toSet()
    }
    val completedBiomes = remember(profile.completedQuestIds) {
        JungleLoreCatalog.biomes.filter { it.id in profile.completedQuestIds }
    }
    val activeBiomes = remember(profile.completedQuestIds) {
        JungleLoreCatalog.biomes.filter { it.id !in profile.completedQuestIds }
    }
    val loreWithStatus = remember(profile, discoveredCreatureIds) {
        JungleLoreCatalog.loreEntries.map { entry ->
            val unlocked = JungleLoreCatalog.isLoreDiscovered(
                entry = entry,
                nectar = profile.nectar,
                totalPuzzlesSolved = profile.totalPuzzlesSolved,
                completedQuests = profile.completedQuestIds,
                discoveredCreatures = discoveredCreatureIds,
                savedLoreIds = profile.discoveredLoreIds
            )
            entry to unlocked
        }
    }
    val discoveredLoreCount = loreWithStatus.count { it.second }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Explorer's Quest Log",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${completedBiomes.size}/${JungleLoreCatalog.biomes.size} Chapters · $discoveredLoreCount/${JungleLoreCatalog.loreEntries.size} Lore",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Objectives (${activeBiomes.size})") },
                    modifier = Modifier.testTag("quest_tab_objectives")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Completed") },
                    modifier = Modifier.testTag("quest_tab_completed")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Lore ($discoveredLoreCount)") },
                    modifier = Modifier.testTag("quest_tab_lore")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> ObjectivesTabContent(
                        uiState = uiState,
                        activeBiomes = activeBiomes,
                        completedCount = completedBiomes.size,
                        discoveredCreatureCount = discoveredCreatureIds.size,
                        onOpenBiome = { biome ->
                            onDismiss()
                            onOpenBiome(biome)
                        },
                        onLaunchPuzzle = { puzzle ->
                            onDismiss()
                            onLaunchPuzzle(puzzle)
                        }
                    )

                    1 -> CompletedTasksTabContent(
                        uiState = uiState,
                        completedBiomes = completedBiomes,
                        onOpenBiome = { biome ->
                            onDismiss()
                            onOpenBiome(biome)
                        }
                    )

                    else -> DiscoveredLoreTabContent(
                        loreWithStatus = loreWithStatus,
                        selectedCategory = selectedLoreCategory,
                        onSelectCategory = { selectedLoreCategory = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ObjectivesTabContent(
    uiState: FernfableUiState,
    activeBiomes: List<BiomeNode>,
    completedCount: Int,
    discoveredCreatureCount: Int,
    onOpenBiome: (BiomeNode) -> Unit,
    onLaunchPuzzle: (PuzzleType) -> Unit
) {
    val profile = uiState.profile
    val totalChapters = JungleLoreCatalog.biomes.size

    // Campaign summary card
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
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
                    text = "Canopy Campaign",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount / $totalChapters Cleared",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { completedCount.toFloat() / totalChapters.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (activeBiomes.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Every Sanctuary chapter quest is cleared! Keep training brain skills and completing milestones below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    } else {
        Text(
            text = "Current Chapter Quests",
            style = MaterialTheme.typography.titleMedium
        )
        activeBiomes.forEach { biome ->
            val unlocked = profile.nectar >= biome.unlockNectar
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (unlocked) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quest_objective_${biome.id}")
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
                        Text(
                            text = biome.questTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        QuestStatusPill(
                            text = if (unlocked) "Active" else "${profile.nectar}/${biome.unlockNectar} Nectar",
                            tint = if (unlocked) FernEmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = biome.questObjective,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rewards: +35–70 EXP · Sunbeam Nectar · +1 Moonstone Shard",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (unlocked) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onOpenBiome(biome) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quest_enter_${biome.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enter Chapter")
                            }
                            OutlinedButton(
                                onClick = { onLaunchPuzzle(biome.signaturePuzzle) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quest_puzzle_${biome.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(biome.signaturePuzzle.title)
                            }
                        }
                    }
                }
            }
        }
    }

    // Sanctuary RPG Milestones
    Text(
        text = "Sanctuary Milestones",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp)
    )

    val unlockedHeroes = JungleLoreCatalog.heroes.count { profile.nectar >= it.unlockNectar }
    val solvedPuzzleTypes = PuzzleType.entries.count { (uiState.puzzleStats[it]?.solvedCount ?: 0) >= 1 }
    val forgedRelics = profile.unlockedRelicIds.size

    MilestoneProgressCard(
        title = "Unite Canopy Allies",
        subtitle = "Gather Sunbeam Nectar so travelers of all races join Solari",
        current = unlockedHeroes,
        target = JungleLoreCatalog.heroes.size,
        accent = StarlightVioletAccent
    )
    MilestoneProgressCard(
        title = "Canopy Polymath",
        subtitle = "Win at least one level in every jungle mini-puzzle",
        current = solvedPuzzleTypes,
        target = PuzzleType.entries.size,
        accent = DewTealAccent
    )
    MilestoneProgressCard(
        title = "Solari Magitech Armory",
        subtitle = "Forge and collect Moonstone-powered relics",
        current = forgedRelics,
        target = JungleLoreCatalog.relics.size,
        accent = FireflyGoldAccent
    )
    MilestoneProgressCard(
        title = "Creature Sanctuary Bonds",
        subtitle = "Discover and befriend every mythical creature",
        current = discoveredCreatureCount,
        target = JungleLoreCatalog.creatures.size,
        accent = OrchidCoralAccent
    )
}

@Composable
private fun CompletedTasksTabContent(
    uiState: FernfableUiState,
    completedBiomes: List<BiomeNode>,
    onOpenBiome: (BiomeNode) -> Unit
) {
    val profile = uiState.profile
    val solvedPuzzles = PuzzleType.entries.mapNotNull { type ->
        val stats = uiState.puzzleStats[type]
        if (stats != null && stats.solvedCount > 0) type to stats else null
    }
    val forgedRelics = JungleLoreCatalog.relics.filter { it.id in profile.unlockedRelicIds && it.shardCost > 0 }

    if (completedBiomes.isEmpty() && solvedPuzzles.isEmpty() && forgedRelics.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quest_completed_empty")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Your adventure chronicle begins here!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter Whispering Moss Hollow or play any mini-puzzle to record your first completed task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (completedBiomes.isNotEmpty()) {
        Text(
            text = "Cleared Story Chapters (${completedBiomes.size})",
            style = MaterialTheme.typography.titleMedium
        )
        completedBiomes.forEach { biome ->
            val creature = JungleLoreCatalog.creatureById(biome.creatureId)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, FernEmeraldAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quest_completed_${biome.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Completed quest",
                        tint = FernEmeraldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = biome.questTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${biome.name} · Befriended ${creature.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { onOpenBiome(biome) },
                        modifier = Modifier.testTag("quest_revisit_${biome.id}")
                    ) {
                        Text("Revisit")
                    }
                }
            }
        }
    }

    if (solvedPuzzles.isNotEmpty()) {
        Text(
            text = "Completed Mini-Puzzle Mastery",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        solvedPuzzles.forEach { (type, stats) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stats.solvedCount} won · Reached Lv. ${stats.currentLevel} · Best ${stats.bestScore}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    QuestStatusPill("${stats.totalStars} ★", FireflyGoldAccent)
                }
            }
        }
    }

    if (forgedRelics.isNotEmpty()) {
        Text(
            text = "Forged Magitech Relics",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        forgedRelics.forEach { relic ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Forged",
                        tint = FireflyGoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = relic.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = relic.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredLoreTabContent(
    loreWithStatus: List<Pair<com.example.model.LoreEntry, Boolean>>,
    selectedCategory: LoreCategory?,
    onSelectCategory: (LoreCategory?) -> Unit
) {
    val filtered = if (selectedCategory == null) loreWithStatus
    else loreWithStatus.filter { it.first.category == selectedCategory }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All (${loreWithStatus.size})") },
                modifier = Modifier.testTag("lore_filter_all")
            )
        }
        items(LoreCategory.entries, key = { it.name }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                label = { Text(category.label) },
                modifier = Modifier.testTag("lore_filter_${category.name.lowercase()}")
            )
        }
    }

    filtered.forEach { (entry, discovered) ->
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (discovered) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                width = 1.dp,
                color = if (discovered) FireflyGoldAccent.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lore_card_${entry.id}")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (discovered) Icons.Rounded.AutoStories else Icons.Rounded.Lock,
                            contentDescription = if (discovered) "Discovered lore" else "Locked lore",
                            tint = if (discovered) FireflyGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (discovered) entry.title else "Undiscovered Chronicle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    QuestStatusPill(
                        text = entry.category.label,
                        tint = if (discovered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (discovered) entry.chronicle else entry.unlockHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (discovered) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MilestoneProgressCard(
    title: String,
    subtitle: String,
    current: Int,
    target: Int,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$current / $target",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                color = accent,
                trackColor = accent.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuestStatusPill(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
