package com.example.ui.puzzles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.puzzle.MushroomKind
import com.example.puzzle.MushroomMelodyPuzzle
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@Composable
fun MushroomMelodyView(
    puzzle: MushroomMelodyPuzzle,
    selectedSlotIdx: Int?,
    gentleMode: Boolean,
    onTapSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val a11y = LocalAccessibilityUi.current
    val haloPulse = if (a11y.calmMode) {
        0.22f
    } else {
        val haloTransition = rememberInfiniteTransition(label = "mushroom_halo")
        val animated by haloTransition.animateFloat(
            initialValue = 0.16f,
            targetValue = 0.34f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mushroom_halo_alpha"
        )
        animated
    }

    val instructionText = if (selectedSlotIdx == null) {
        "Tap any mushroom, then tap another spot to swap them!"
    } else {
        "Now tap the spot you want to swap with #${selectedSlotIdx + 1}."
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = instructionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.speakOnTouch(instructionText)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mossy Log Row of Glowing Mushrooms
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                puzzle.currentOrder.forEachIndexed { idx, kind ->
                    val isSelected = selectedSlotIdx == idx
                    val capColor = mushroomColor(kind)

                    val slotScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.07f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "mushroom_slot_scale_$idx"
                    )
                    val slotBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(200),
                        label = "mushroom_slot_bg_$idx"
                    )
                    val slotBorder by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else capColor.copy(alpha = 0.55f),
                        animationSpec = tween(200),
                        label = "mushroom_slot_border_$idx"
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = slotBg,
                        tonalElevation = if (isSelected) 6.dp else 2.dp,
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.5.dp,
                            color = slotBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.78f)
                            .scale(slotScale)
                            .speakOnTouch(
                                buildString {
                                    append("Spot #${idx + 1}: ${kind.displayName}")
                                    if (isSelected) append(", selected")
                                }
                            )
                            .testTag("mushroom_slot_$idx")
                            .clickable { onTapSlot(idx) }
                    ) {
                        AnimatedContent(
                            targetState = kind,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(
                                    initialScale = 0.78f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )) togetherWith fadeOut(tween(120))
                            },
                            label = "mushroom_kind_swap_$idx"
                        ) { animatedKind ->
                            val animatedColor = mushroomColor(animatedKind)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                GlowingMushroomGlyph(
                                    kind = animatedKind,
                                    capColor = animatedColor,
                                    haloAlpha = if (isSelected) haloPulse + 0.14f else haloPulse,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                                Text(
                                    text = animatedKind.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fairy Logic Clue Cards with animated check glow
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Fairy Log Clues",
                style = MaterialTheme.typography.titleSmall
            )
            puzzle.clues.forEachIndexed { idx, clue ->
                val satisfied = clue.isSatisfied(puzzle.currentOrder)
                val showGreen = gentleMode && satisfied
                val clueBg by animateColorAsState(
                    targetValue = if (showGreen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(220),
                    label = "clue_bg_$idx"
                )
                val clueBorder by animateColorAsState(
                    targetValue = if (showGreen) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(220),
                    label = "clue_border_$idx"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (showGreen) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "clue_icon_scale_$idx"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = clueBg,
                    border = BorderStroke(1.dp, clueBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .speakOnTouch("Clue ${idx + 1}: ${clue.text}${if (showGreen) ", matched" else ""}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showGreen) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (showGreen) "Clue matched" else "Clue pending",
                            tint = if (showGreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(iconScale)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${idx + 1}. ${clue.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (showGreen) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowingMushroomGlyph(
    kind: MushroomKind,
    capColor: Color,
    haloAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(4.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Soft pulsing bioluminescent halo
        drawCircle(
            color = capColor.copy(alpha = haloAlpha),
            radius = minOf(w, h) * 0.47f,
            center = Offset(cx, h * 0.44f)
        )
        // Creamy stem
        drawRoundRect(
            color = Color(0xFFF6EEDB),
            topLeft = Offset(cx - w * 0.14f, h * 0.46f),
            size = Size(w * 0.28f, h * 0.42f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        // Mushroom cap
        drawArc(
            color = capColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.10f, h * 0.16f),
            size = Size(w * 0.80f, h * 0.68f)
        )
        // Glowing spots (count equals kind.spotCount for colorblind accessibility!)
        val spotOffsets = when (kind.spotCount) {
            1 -> listOf(Offset(cx, h * 0.34f))
            2 -> listOf(Offset(w * 0.36f, h * 0.36f), Offset(w * 0.64f, h * 0.36f))
            3 -> listOf(Offset(cx, h * 0.28f), Offset(w * 0.33f, h * 0.40f), Offset(w * 0.67f, h * 0.40f))
            4 -> listOf(
                Offset(w * 0.38f, h * 0.29f), Offset(w * 0.62f, h * 0.29f),
                Offset(w * 0.30f, h * 0.41f), Offset(w * 0.70f, h * 0.41f)
            )
            else -> listOf(
                Offset(cx, h * 0.25f),
                Offset(w * 0.34f, h * 0.34f), Offset(w * 0.66f, h * 0.34f),
                Offset(w * 0.28f, h * 0.43f), Offset(w * 0.72f, h * 0.43f)
            )
        }
        spotOffsets.forEach { pt ->
            drawCircle(color = Color(0xFFFFFCEB), radius = minOf(w, h) * 0.055f, center = pt)
        }
    }
}

private fun mushroomColor(kind: MushroomKind): Color = when (kind) {
    MushroomKind.AMBER_CAP -> FireflyGoldAccent
    MushroomKind.DEW_BELL -> DewTealAccent
    MushroomKind.CORAL_PUFF -> OrchidCoralAccent
    MushroomKind.MOSS_SPIRE -> FernEmeraldAccent
    MushroomKind.STAR_MOREL -> StarlightVioletAccent
}
