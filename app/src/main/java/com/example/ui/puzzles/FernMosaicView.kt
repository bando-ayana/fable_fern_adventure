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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.puzzle.FernMosaicPuzzle
import com.example.puzzle.LeafShape
import com.example.puzzle.LeafTint
import com.example.puzzle.LeafToken
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent

@Composable
fun FernMosaicView(
    puzzle: FernMosaicPuzzle,
    gentleMode: Boolean,
    statusMessage: String?,
    onSelectSlot: (Int) -> Unit,
    onSelectChoice: (LeafToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val round = puzzle.currentRound ?: return

    val a11y = LocalAccessibilityUi.current
    val selectedPulse = if (a11y.calmMode) {
        1.0f
    } else {
        val pulseTransition = rememberInfiniteTransition(label = "mosaic_slot_pulse")
        val animated by pulseTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mosaic_selected_pulse"
        )
        animated
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val headerSpeech = buildString {
            append("Pattern ${round.roundNumber} of ${round.totalRounds}.")
            if (gentleMode) {
                append(" ")
                append(statusMessage ?: round.patternHint)
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (a11y.calmMode) 0.92f else 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .speakOnTouch(headerSpeech)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pattern ${round.roundNumber} of ${round.totalRounds}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (gentleMode) {
                    AnimatedContent(
                        targetState = statusMessage ?: round.patternHint,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        },
                        label = "mosaic_status_text"
                    ) { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animate transitions between rounds cleanly
        AnimatedContent(
            targetState = round.roundNumber,
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(initialScale = 0.94f)) togetherWith fadeOut(tween(160))
            },
            label = "mosaic_round_transition"
        ) { targetRound ->
            val currentRound = puzzle.rounds.firstOrNull { it.roundNumber == targetRound } ?: round

            // render based on currentRound
            val columns = if (currentRound.sequence.size <= 6) 3 else 4
            val rows = currentRound.sequence.chunked(columns)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rows.forEachIndexed { rIdx, rowTokens ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowTokens.forEachIndexed { cIdx, token ->
                                    val seqIdx = rIdx * columns + cIdx
                                    val isMissing = seqIdx in round.missingIndices
                                    val placed = round.placedAnswers[seqIdx]
                                    val displayToken = if (isMissing) placed else token
                                    val isSelectedSlot = isMissing && puzzle.selectedSlotIdx == seqIdx

                                    val slotBg by animateColorAsState(
                                        targetValue = when {
                                            placed != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            isSelectedSlot -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        animationSpec = tween(220),
                                        label = "mosaic_slot_bg_$seqIdx"
                                    )
                                    val slotBorder by animateColorAsState(
                                        targetValue = when {
                                            isSelectedSlot -> MaterialTheme.colorScheme.primary
                                            placed != null -> FernEmeraldAccent
                                            isMissing -> FireflyGoldAccent
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        },
                                        animationSpec = tween(220),
                                        label = "mosaic_slot_border_$seqIdx"
                                    )
                                    val placedScale by animateFloatAsState(
                                        targetValue = if (placed != null) 1.04f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "mosaic_placed_scale_$seqIdx"
                                    )

                                    val slotSpeech = when {
                                        isMissing && placed != null -> "Placed ${placed.tint.name.lowercase()} ${placed.shape.label}"
                                        isMissing -> "Missing slot${if (isSelectedSlot) ", selected" else ""}"
                                        else -> "${token.tint.name.lowercase()} ${token.shape.label}"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = slotBg,
                                        border = BorderStroke(
                                            width = if (isSelectedSlot || placed != null) 2.5.dp else 1.5.dp,
                                            color = slotBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .scale(if (isSelectedSlot && placed == null) selectedPulse else placedScale)
                                            .speakOnTouch(slotSpeech)
                                            .testTag("mosaic_slot_$seqIdx")
                                            .clickable(enabled = isMissing && placed == null) {
                                                onSelectSlot(seqIdx)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            AnimatedContent(
                                                targetState = displayToken,
                                                transitionSpec = {
                                                    (fadeIn(tween(220)) + scaleIn(
                                                        initialScale = 0.65f,
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                    )) togetherWith fadeOut(tween(140))
                                                },
                                                label = "mosaic_leaf_fill_$seqIdx"
                                            ) { animatedLeaf ->
                                                if (animatedLeaf != null) {
                                                    LeafTokenGlyph(
                                                        token = animatedLeaf,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(8.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "?",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color = FireflyGoldAccent,
                                                        fontWeight = FontWeight.Bold
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

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Choose the matching botanical tile:",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    round.choices.forEachIndexed { idx, choice ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            border = BorderStroke(1.5.dp, leafTintColor(choice.tint).copy(alpha = 0.65f)),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.92f)
                                .speakOnTouch("${choice.tint.name.lowercase()} ${choice.shape.label}${if (choice.tilted) ", tilted" else ""}")
                                .testTag("mosaic_choice_$idx")
                                .clickable { onSelectChoice(choice) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                LeafTokenGlyph(
                                    token = choice,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                                Text(
                                    text = choice.shape.label,
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
    }
}

@Composable
fun LeafTokenGlyph(
    token: LeafToken,
    modifier: Modifier = Modifier
) {
    val color = leafTintColor(token.tint)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val angle = if (token.tilted) 35f else 0f

        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            when (token.shape) {
                LeafShape.HEARTLEAF -> {
                    val path = Path().apply {
                        moveTo(cx, h * 0.85f)
                        cubicTo(w * 0.08f, h * 0.56f, w * 0.14f, h * 0.16f, cx, h * 0.30f)
                        cubicTo(w * 0.86f, h * 0.16f, w * 0.92f, h * 0.56f, cx, h * 0.85f)
                        close()
                    }
                    drawPath(path, color = color)
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(cx, h * 0.80f),
                        end = Offset(cx, h * 0.38f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                LeafShape.CURLFERN -> {
                    val stem = Path().apply {
                        moveTo(w * 0.30f, h * 0.86f)
                        quadraticTo(w * 0.32f, h * 0.24f, w * 0.68f, h * 0.28f)
                        quadraticTo(w * 0.76f, h * 0.44f, w * 0.58f, h * 0.46f)
                    }
                    drawPath(stem, color = color, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round))
                    drawCircle(color = color, radius = w * 0.10f, center = Offset(w * 0.58f, h * 0.44f))
                    drawCircle(color = color, radius = w * 0.07f, center = Offset(w * 0.28f, h * 0.52f))
                    drawCircle(color = color, radius = w * 0.07f, center = Offset(w * 0.42f, h * 0.64f))
                }

                LeafShape.TRIFOIL -> {
                    drawCircle(color = color, radius = w * 0.19f, center = Offset(cx, h * 0.30f))
                    drawCircle(color = color, radius = w * 0.19f, center = Offset(w * 0.30f, h * 0.56f))
                    drawCircle(color = color, radius = w * 0.19f, center = Offset(w * 0.70f, h * 0.56f))
                    drawLine(
                        color = color,
                        start = Offset(cx, h * 0.52f),
                        end = Offset(cx, h * 0.86f),
                        strokeWidth = w * 0.09f,
                        cap = StrokeCap.Round
                    )
                }

                LeafShape.MOONPETAL -> {
                    val outer = Path().apply {
                        moveTo(cx, h * 0.12f)
                        quadraticTo(w * 0.86f, h * 0.50f, cx, h * 0.88f)
                        quadraticTo(w * 0.14f, h * 0.50f, cx, h * 0.12f)
                        close()
                    }
                    drawPath(outer, color = color)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = w * 0.09f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

private fun leafTintColor(tint: LeafTint): Color = when (tint) {
    LeafTint.EMERALD -> FernEmeraldAccent
    LeafTint.GOLD -> FireflyGoldAccent
    LeafTint.TEAL -> DewTealAccent
    LeafTint.CORAL -> OrchidCoralAccent
}
