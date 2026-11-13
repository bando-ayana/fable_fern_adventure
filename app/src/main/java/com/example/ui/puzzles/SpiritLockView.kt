package com.example.ui.puzzles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.puzzle.SpiritLockPuzzle
import com.example.puzzle.SpiritRune
import com.example.ui.theme.FireflyGoldAccent

@Composable
fun SpiritLockView(
    puzzle: SpiritLockPuzzle,
    onAppendRune: (SpiritRune) -> Unit,
    onRemoveLast: () -> Unit,
    onSubmitDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Legend for Gold & Silver deduction pips
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(FireflyGoldAccent, CircleShape)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Exact spot",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Right rune, wrong spot",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Attempt history rows
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (puzzle.attempts.isEmpty()) {
                    Text(
                        text = "No guesses yet — pick ${puzzle.secretCode.size} runes below to test the lock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    puzzle.attempts.forEachIndexed { idx, record ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                record.guess.forEach { rune ->
                                    val visual = spiritRuneVisual(rune)
                                    Surface(
                                        shape = CircleShape,
                                        color = visual.color.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, visual.color.copy(alpha = 0.6f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = visual.icon,
                                                contentDescription = visual.label,
                                                tint = visual.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Pips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(record.exactMatches) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .background(FireflyGoldAccent, CircleShape)
                                    )
                                }
                                repeat(record.misplacedMatches) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                    )
                                }
                                val emptyPips = (puzzle.secretCode.size - record.exactMatches - record.misplacedMatches)
                                    .coerceAtLeast(0)
                                repeat(emptyPips) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .background(
                                                MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Draft + Hint slots
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (slot in puzzle.secretCode.indices) {
                        val draftRune = puzzle.currentDraft.getOrNull(slot)
                        val hintRune = puzzle.revealedHintIndices[slot]
                        val displayRune = draftRune ?: hintRune
                        val isHintOnly = draftRune == null && hintRune != null
                        val visual = displayRune?.let { spiritRuneVisual(it) }

                        val slotBg by animateColorAsState(
                            targetValue = when {
                                draftRune != null && visual != null -> visual.color.copy(alpha = 0.18f)
                                isHintOnly -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            animationSpec = tween(180),
                            label = "spirit_slot_bg_$slot"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = slotBg,
                            border = BorderStroke(
                                1.5.dp,
                                if (isHintOnly) FireflyGoldAccent else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = visual to isHintOnly,
                                    transitionSpec = {
                                        (fadeIn(tween(180)) + scaleIn(
                                            initialScale = 0.65f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )) togetherWith fadeOut(tween(110))
                                    },
                                    label = "spirit_slot_rune_$slot"
                                ) { (activeVisual, hintOnly) ->
                                    if (activeVisual != null) {
                                        Icon(
                                            imageVector = activeVisual.icon,
                                            contentDescription = activeVisual.label,
                                            tint = if (hintOnly) activeVisual.color.copy(alpha = 0.65f) else activeVisual.color,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "?",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRemoveLast,
                        enabled = puzzle.currentDraft.isNotEmpty(),
                        modifier = Modifier.testTag("spirit_backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Backspace,
                            contentDescription = "Remove last rune"
                        )
                    }
                    Button(
                        onClick = onSubmitDraft,
                        enabled = puzzle.currentDraft.size == puzzle.secretCode.size,
                        modifier = Modifier.testTag("spirit_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Test")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rune Picker Palette
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            puzzle.availableRunes.forEach { rune ->
                val visual = spiritRuneVisual(rune)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.5.dp, visual.color.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .testTag("spirit_rune_${rune.name.lowercase()}")
                        .clickable(
                            enabled = puzzle.currentDraft.size < puzzle.secretCode.size
                        ) {
                            onAppendRune(rune)
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = "Pick ${visual.label}",
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
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Attempt ${puzzle.attempts.size} of ${puzzle.maxAttempts}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
