package com.example.ui.puzzles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.puzzle.GroveGridEngine
import com.example.puzzle.GroveGridPuzzle
import com.example.puzzle.GroveRune
import com.example.ui.components.speakOnTouch

@Composable
fun GroveGridView(
    puzzle: GroveGridPuzzle,
    selectedCell: Int?,
    gentleMode: Boolean,
    onSelectCell: (Int) -> Unit,
    onPlaceRune: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val conflicts = remember(puzzle.board, gentleMode) {
        if (gentleMode) GroveGridEngine.conflictingIndices(puzzle.board) else emptySet()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val gridRuleText = "Fill each row, column, and 2x2 mossy box with all 4 runes."
        Text(
            text = gridRuleText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.speakOnTouch(gridRuleText)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4x4 Grid grouped into 2x2 sub-groves so box boundaries are unmistakable
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (boxRow in 0 until 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (boxCol in 0 until 2) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (ir in 0 until 2) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            for (ic in 0 until 2) {
                                                val r = boxRow * 2 + ir
                                                val c = boxCol * 2 + ic
                                                val idx = r * 4 + c
                                                val value = puzzle.board[idx]
                                                val isGiven = puzzle.givenMask[idx]
                                                val isSelected = selectedCell == idx
                                                val hasConflict = idx in conflicts
                                                val rune = GroveRune.fromId(value)
                                                val visual = rune?.let { groveRuneVisual(it) }

                                                val cellBg by animateColorAsState(
                                                    targetValue = when {
                                                        hasConflict -> MaterialTheme.colorScheme.errorContainer
                                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                        isGiven -> MaterialTheme.colorScheme.surfaceVariant
                                                        else -> MaterialTheme.colorScheme.surface
                                                    },
                                                    animationSpec = tween(180),
                                                    label = "grove_cell_bg_$idx"
                                                )
                                                val cellBorder by animateColorAsState(
                                                    targetValue = when {
                                                        hasConflict -> MaterialTheme.colorScheme.error
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.outlineVariant
                                                    },
                                                    animationSpec = tween(180),
                                                    label = "grove_cell_border_$idx"
                                                )

                                                val cellSpeech = buildString {
                                                    append("Row ${r + 1} Column ${c + 1}: ")
                                                    append(visual?.label ?: "empty")
                                                    if (isGiven) append(", locked clue")
                                                    if (isSelected) append(", selected")
                                                    if (hasConflict) append(", duplicate conflict")
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = cellBg,
                                                    border = BorderStroke(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = cellBorder
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .speakOnTouch(cellSpeech)
                                                        .testTag("grove_cell_$idx")
                                                        .clickable(enabled = !isGiven) {
                                                            onSelectCell(idx)
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        AnimatedContent(
                                                            targetState = visual,
                                                            transitionSpec = {
                                                                (fadeIn(tween(180)) + scaleIn(
                                                                    initialScale = 0.65f,
                                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                                )) togetherWith fadeOut(tween(100))
                                                            },
                                                            label = "grove_rune_pop_$idx"
                                                        ) { activeVisual ->
                                                            if (activeVisual != null) {
                                                                Icon(
                                                                    imageVector = activeVisual.icon,
                                                                    contentDescription = activeVisual.label,
                                                                    tint = activeVisual.color,
                                                                    modifier = Modifier.size(if (isGiven) 26.dp else 30.dp)
                                                                )
                                                            }
                                                        }
                                                        if (visual != null && isGiven) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp)
                                                                    .size(5.dp)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.outline,
                                                                        RoundedCornerShape(50)
                                                                    )
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
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Rune Selector Bar
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            GroveRune.entries.forEach { rune ->
                val visual = groveRuneVisual(rune)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.5.dp, visual.color.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .minimumInteractiveComponentSize()
                        .speakOnTouch("Place ${visual.label}")
                        .testTag("grove_picker_${rune.id}")
                        .clickable(enabled = selectedCell != null) {
                            onPlaceRune(rune.id)
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = "Place ${visual.label}",
                            tint = visual.color,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = visual.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .height(64.dp)
                    .widthIn(min = 52.dp)
                    .minimumInteractiveComponentSize()
                    .speakOnTouch("Clear selected cell")
                    .testTag("grove_picker_clear")
                    .clickable(enabled = selectedCell != null) {
                        onPlaceRune(0)
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Clear selected cell",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
