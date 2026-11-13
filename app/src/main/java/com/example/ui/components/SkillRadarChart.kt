package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkillRadarChart(
    memoryXp: Int,
    logicXp: Int,
    spatialXp: Int,
    deductionXp: Int,
    observationXp: Int,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val accentFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val accentStroke = MaterialTheme.colorScheme.primary
    val maxScale = maxOf(120, memoryXp, logicXp, spatialXp, deductionXp, observationXp).toFloat()

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(42.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = minOf(cx, cy)

            fun pointAt(axis: Int, radius: Float): Offset {
                val angle = -PI / 2.0 + axis * (2.0 * PI / 5.0)
                return Offset(
                    x = cx + (cos(angle) * radius).toFloat(),
                    y = cy + (sin(angle) * radius).toFloat()
                )
            }

            // Concentric pentagon rings
            listOf(0.33f, 0.66f, 1.0f).forEach { factor ->
                val r = maxR * factor
                val ring = Path().apply {
                    for (i in 0 until 5) {
                        val pt = pointAt(i, r)
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                    close()
                }
                drawPath(ring, color = gridColor, style = Stroke(width = 2f))
            }

            // 5 spokes
            for (i in 0 until 5) {
                drawLine(gridColor, Offset(cx, cy), pointAt(i, maxR), strokeWidth = 2f)
            }

            val values = listOf(
                memoryXp to FireflyGoldAccent,
                logicXp to FernEmeraldAccent,
                spatialXp to DewTealAccent,
                deductionXp to StarlightVioletAccent,
                observationXp to OrchidCoralAccent
            )
            val points = values.mapIndexed { idx, (xp, _) ->
                val frac = (xp / maxScale).coerceIn(0.14f, 1f)
                pointAt(idx, maxR * frac)
            }

            val poly = Path().apply {
                points.forEachIndexed { idx, pt ->
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }
            drawPath(poly, color = accentFill)
            drawPath(poly, color = accentStroke, style = Stroke(width = 5f))

            points.forEachIndexed { idx, pt ->
                val color = values[idx].second
                drawCircle(color = color, radius = 8f, center = pt)
                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 3.5f, center = pt)
            }
        }

        Text(
            text = "Memory",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Text(
            text = "Logic",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp)
        )
        Text(
            text = "Spatial",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 14.dp, end = 16.dp)
        )
        Text(
            text = "Deduction",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 14.dp, start = 10.dp)
        )
        Text(
            text = "Observe",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp)
        )
    }
}
