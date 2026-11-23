package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.FireflyGoldAccent
import kotlin.math.PI
import kotlin.math.sin

private data class FireflySeed(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val phase: Float,
    val speed: Float
)

@Composable
fun JungleAtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val calmMode = LocalAccessibilityUi.current.calmMode
    val bgTop = MaterialTheme.colorScheme.background
    val bgTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (calmMode) 0.12f else 0.25f)
    val glowColor = FireflyGoldAccent

    val seeds = remember {
        listOf(
            FireflySeed(0.14f, 0.16f, 3.2f, 0.0f, 1.0f),
            FireflySeed(0.82f, 0.12f, 2.6f, 1.2f, 1.2f),
            FireflySeed(0.32f, 0.42f, 2.2f, 2.5f, 0.9f),
            FireflySeed(0.75f, 0.58f, 3.0f, 3.8f, 1.1f),
            FireflySeed(0.18f, 0.78f, 2.5f, 4.9f, 1.3f),
            FireflySeed(0.88f, 0.85f, 2.8f, 5.4f, 0.8f)
        )
    }

    val phase = if (calmMode) {
        0f
    } else {
        val infinite = rememberInfiniteTransition(label = "fireflies")
        val animated by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "firefly_phase"
        )
        animated
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(bgTop, bgTop, bgTint)
                )
            )
            if (!calmMode) {
                seeds.forEach { seed ->
                    val driftY = sin(phase * seed.speed + seed.phase) * 14f
                    val driftX = sin(phase * 0.7f + seed.phase) * 10f
                    val pulseAlpha = 0.20f + 0.22f * ((sin(phase * 1.4f + seed.phase) + 1f) * 0.5f)
                    val center = Offset(
                        x = size.width * seed.xFraction + driftX,
                        y = size.height * seed.yFraction + driftY
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = pulseAlpha * 0.35f),
                        radius = seed.radiusDp * 5.2f,
                        center = center
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = pulseAlpha),
                        radius = seed.radiusDp * 1.6f,
                        center = center
                    )
                }
            }
        }
        content()
    }
}
