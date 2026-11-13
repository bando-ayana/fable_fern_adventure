package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.JungleLoreCatalog
import com.example.model.MythicalCreature
import com.example.model.PuzzleType
import com.example.ui.FernfableUiState
import com.example.ui.components.CreaturePortrait
import com.example.ui.theme.FireflyGoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryScreen(
    uiState: FernfableUiState,
    onFeedCreature: (String) -> Unit,
    onPlaySignaturePuzzle: (PuzzleType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCreature by remember { mutableStateOf<MythicalCreature?>(null) }
    val discoveredCount = JungleLoreCatalog.creatures.count { creature ->
        uiState.creatureProgress[creature.id]?.discovered == true
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "Creature Sanctuary",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "$discoveredCount of ${JungleLoreCatalog.creatures.size} mythical friends befriended · Share Nectar to deepen your bond",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(JungleLoreCatalog.creatures, key = { it.id }) { creature ->
                val progress = uiState.creatureProgress[creature.id]
                val discovered = progress?.discovered == true
                val bondLevel = progress?.bondLevel ?: 0

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = if (discovered) 3.dp else 0.dp,
                    border = BorderStroke(
                        1.dp,
                        if (discovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("creature_card_${creature.id}")
                        .clickable(enabled = discovered) { selectedCreature = creature }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        CreaturePortrait(
                            creature = creature,
                            discovered = discovered,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (discovered) creature.name else "???",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (discovered) creature.title else "Found in ${creature.habitat}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (discovered) {
                            BondHeartsRow(bondLevel = bondLevel)
                        }
                    }
                }
            }
        }
    }

    val currentSelected = selectedCreature
    if (currentSelected != null) {
        val progress = uiState.creatureProgress[currentSelected.id]
        val bondLevel = progress?.bondLevel ?: 1
        val bondXp = progress?.bondXp ?: 0
        val nextGoalXp = when (bondLevel) {
            1 -> 50
            2 -> 120
            else -> 120
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCreature = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CreaturePortrait(
                        creature = currentSelected,
                        discovered = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(92.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSelected.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = currentSelected.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BondHeartsRow(bondLevel = bondLevel)
                    }
                }

                Text(
                    text = currentSelected.lore,
                    style = MaterialTheme.typography.bodyLarge
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Habitat: ${currentSelected.habitat}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Favorite Treat: ${currentSelected.favoriteTreat}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Bond Progress ($bondXp / $nextGoalXp XP)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { (bondXp.toFloat() / nextGoalXp.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onFeedCreature(currentSelected.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("feed_creature_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = FireflyGoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Treat (10)")
                    }
                    FilledTonalButton(
                        onClick = {
                            selectedCreature = null
                            onPlaySignaturePuzzle(currentSelected.signaturePuzzle)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("creature_play_puzzle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentSelected.signaturePuzzle.title)
                    }
                }
            }
        }
    }
}
