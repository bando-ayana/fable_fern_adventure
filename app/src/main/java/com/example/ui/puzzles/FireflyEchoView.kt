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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.puzzle.FireflyEchoEngine
import com.example.stateHolder.FireflyEchoUiState
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@Composable
fun FireflyEchoView(
    state: FireflyEchoUiState,
    onTapBlossom: (Int) -> Unit,
    onReplayHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blossomColors = listOf(
        FireflyGoldAccent,
        DewTealAccent,
        OrchidCoralAccent,
        FernEmeraldAccent,
        StarlightVioletAccent,
        Color(0xFFE07A5F)
    )
    val blossomIcons = listOf(
        Icons.Rounded.WbSunny,
        Icons.Rounded.Spa,
        Icons.Rounded.LocalFlorist,
        Icons.Rounded.Eco,
        Icons.Rounded.Bedtime,
        Icons.Rounded.AutoAwesome
    )

    val ambientTransition = rememberInfiniteTransition(label = "firefly_aura")
    val shimmerPulse by ambientTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firefly_shimmer_pulse"
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
            AnimatedContent(
                targetState = state.statusBanner,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "firefly_status_banner"
            ) { banner ->
                Text(
                    text = banner,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sequence progress dots with spring pop
        val seqSize = state.currentRound?.sequence?.size ?: 0
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until seqSize) {
                val filled = i < state.userInput.size
                val dotScale by animateFloatAsState(
                    targetValue = if (filled) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "seq_dot_scale_$i"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (filled) FireflyGoldAccent else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(200),
                    label = "seq_dot_color_$i"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(dotScale)
                        .background(color = dotColor, shape = CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2x3 Lantern Blossom Pad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until 3) {
                        val idx = row * 3 + col
                        val isLit = state.activeBlossomIdx == idx
                        val isError = state.errorFlashIdx == idx
                        val baseColor = blossomColors[idx]

                        val scale by animateFloatAsState(
                            targetValue = when {
                                isLit -> 1.08f
                                isError -> 0.94f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "blossom_scale_$idx"
                        )
                        val containerColor by animateColorAsState(
                            targetValue = when {
                                isError -> MaterialTheme.colorScheme.errorContainer
                                isLit -> baseColor
                                else -> MaterialTheme.colorScheme.surface
                            },
                            animationSpec = tween(180),
                            label = "blossom_color_$idx"
                        )
                        val glowStrength by animateFloatAsState(
                            targetValue = if (isLit) 1f else 0f,
                            animationSpec = tween(220),
                            label = "blossom_glow_$idx"
                        )

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = containerColor,
                            tonalElevation = if (isLit) 8.dp else 2.dp,
                            border = BorderStroke(
                                width = if (isLit) 3.dp else 1.5.dp,
                                color = if (isLit) baseColor else baseColor.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .scale(scale)
                                .drawBehind {
                                    if (glowStrength > 0.01f) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    baseColor.copy(alpha = 0.48f * glowStrength),
                                                    Color.Transparent
                                                )
                                            ),
                                            radius = size.maxDimension * (0.68f + 0.08f * shimmerPulse)
                                        )
                                    }
                                }
                                .testTag("firefly_blossom_$idx")
                                .clickable(enabled = !state.isPlayingSequence) {
                                    onTapBlossom(idx)
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = blossomIcons[idx],
                                    contentDescription = FireflyEchoEngine.blossomNames[idx],
                                    tint = if (isLit) Color.White else baseColor,
                                    modifier = Modifier.size(if (isLit) 38.dp else 34.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = FireflyEchoEngine.blossomNames[idx],
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLit) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onReplayHint,
            enabled = !state.isPlayingSequence,
            modifier = Modifier.testTag("firefly_replay_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Replay,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Replay Glow Whisper")
        }
    }
}
