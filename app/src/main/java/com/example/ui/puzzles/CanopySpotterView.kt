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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.puzzle.CanopyCharm
import com.example.puzzle.CanopySpotterPuzzle
import com.example.puzzle.CharmBase
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CanopySpotterView(
    puzzle: CanopySpotterPuzzle,
    statusMessage: String?,
    onTapCharm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val round = puzzle.currentRound ?: return

    val hintTransition = rememberInfiniteTransition(label = "spotter_hint_pulse")
    val hintPulse by hintTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotter_hint_scale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Look ${round.roundNumber} of ${round.totalRounds}: ${round.prompt}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
                if (statusMessage != null) {
                    AnimatedContent(
                        targetState = statusMessage,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        },
                        label = "spotter_status_banner"
                    ) { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Crossfade between rounds and fade/settle matched tiles gently
        AnimatedContent(
            targetState = round.roundNumber,
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(160))
            },
            label = "spotter_round_transition"
        ) { _ ->
            val columns = if (round.charms.size <= 4) 2 else 3
            val rows = round.charms.chunked(columns)

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rows.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { charm ->
                            val isSelected = charm.id in puzzle.selectedIds
                            val isHinted = puzzle.hintedId == charm.id

                            val tileAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 0.58f else 1.0f,
                                animationSpec = tween(280),
                                label = "spotter_alpha_${charm.id}"
                            )
                            val tileScale by animateFloatAsState(
                                targetValue = if (isSelected) 0.93f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "spotter_scale_${charm.id}"
                            )
                            val tileColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isHinted -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                animationSpec = tween(220),
                                label = "spotter_bg_${charm.id}"
                            )
                            val tileBorder by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isHinted -> FireflyGoldAccent
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                                animationSpec = tween(220),
                                label = "spotter_border_${charm.id}"
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = tileColor,
                                tonalElevation = if (isSelected) 6.dp else 2.dp,
                                border = BorderStroke(
                                    width = if (isSelected || isHinted) 2.5.dp else 1.5.dp,
                                    color = tileBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .scale(if (isHinted && !isSelected) hintPulse else tileScale)
                                    .alpha(tileAlpha)
                                    .testTag("spotter_charm_${charm.id}")
                                    .clickable(enabled = !isSelected) { onTapCharm(charm.id) }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CanopyCharmGlyph(
                                        charm = charm,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Matched charm",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Found ${puzzle.selectedIds.size} of ${round.targetIds.size}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CanopyCharmGlyph(
    charm: CanopyCharm,
    modifier: Modifier = Modifier
) {
    val palette = listOf(FireflyGoldAccent, DewTealAccent, OrchidCoralAccent, FernEmeraldAccent)
    val mainColor = palette[charm.tintIndex % palette.size]

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val tilt = if (charm.tilted) 24f else 0f

        rotate(degrees = tilt, pivot = Offset(cx, cy)) {
            when (charm.base) {
                CharmBase.BLOSSOM -> {
                    val count = charm.detailCount + 2 // 4..7 distinct petals
                    val orbit = minOf(w, h) * 0.24f
                    val petalR = minOf(w, h) * 0.16f
                    for (i in 0 until count) {
                        val angle = (i * 2.0 * PI / count) - PI / 2.0
                        val px = cx + (cos(angle) * orbit).toFloat()
                        val py = cy + (sin(angle) * orbit).toFloat()
                        drawCircle(color = mainColor, radius = petalR, center = Offset(px, py))
                    }
                    drawCircle(color = Color(0xFFFFF8DC), radius = minOf(w, h) * 0.14f, center = Offset(cx, cy))
                }

                CharmBase.MUSHROOM -> {
                    drawRoundRect(
                        color = Color(0xFFF3EBD8),
                        topLeft = Offset(cx - w * 0.13f, h * 0.46f),
                        size = Size(w * 0.26f, h * 0.38f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawArc(
                        color = mainColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(w * 0.12f, h * 0.16f),
                        size = Size(w * 0.76f, h * 0.64f)
                    )
                    for (i in 0 until charm.detailCount) {
                        val frac = (i + 1f) / (charm.detailCount + 1f)
                        val sx = w * (0.24f + frac * 0.52f)
                        val sy = h * (0.36f - (if (i % 2 == 0) 0.06f else 0f))
                        drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(sx, sy))
                    }
                }

                CharmBase.FERN -> {
                    drawLine(
                        color = mainColor,
                        start = Offset(cx, h * 0.86f),
                        end = Offset(cx, h * 0.16f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    for (i in 0 until charm.detailCount) {
                        val y = h * (0.72f - i * 0.13f)
                        val span = w * (0.30f - i * 0.03f)
                        drawLine(
                            color = mainColor,
                            start = Offset(cx, y),
                            end = Offset(cx - span, y - h * 0.07f),
                            strokeWidth = w * 0.07f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = mainColor,
                            start = Offset(cx, y),
                            end = Offset(cx + span, y - h * 0.07f),
                            strokeWidth = w * 0.07f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                CharmBase.LANTERN -> {
                    val frame = Path().apply {
                        moveTo(cx, h * 0.14f)
                        lineTo(w * 0.74f, h * 0.42f)
                        lineTo(w * 0.66f, h * 0.82f)
                        lineTo(w * 0.34f, h * 0.82f)
                        lineTo(w * 0.26f, h * 0.42f)
                        close()
                    }
                    drawPath(frame, color = mainColor.copy(alpha = 0.25f))
                    drawPath(frame, color = mainColor, style = Stroke(width = w * 0.07f))
                    for (i in 0 until charm.detailCount) {
                        val frac = (i + 1f) / (charm.detailCount + 1f)
                        drawCircle(
                            color = FireflyGoldAccent,
                            radius = w * 0.055f,
                            center = Offset(w * (0.32f + frac * 0.36f), h * 0.56f)
                        )
                    }
                }
            }
        }
    }
}
