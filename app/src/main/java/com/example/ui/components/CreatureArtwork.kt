package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.model.MythicalCreature
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@Composable
fun CreaturePortrait(
    creature: MythicalCreature,
    discovered: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!discovered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Undiscovered mythical creature silhouette",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (creature.drawableRes != null) {
            Image(
                painter = painterResource(id = creature.drawableRes),
                contentDescription = "${creature.name}, ${creature.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CanvasCreatureIllustration(
                creatureId = creature.id,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CanvasCreatureIllustration(
    creatureId: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (creatureId) {
            "glimmertoad" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0E343B), Color(0xFF19535F))
                    )
                )
                // Glowing toadstool cap
                drawOval(
                    color = DewTealAccent.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.14f, h * 0.62f),
                    size = Size(w * 0.72f, h * 0.25f)
                )
                // Friendly round amphibian body
                drawOval(
                    color = FernEmeraldAccent,
                    topLeft = Offset(w * 0.25f, h * 0.34f),
                    size = Size(w * 0.50f, h * 0.40f)
                )
                // Luminous belly & throat sac
                drawOval(
                    color = FireflyGoldAccent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.35f, h * 0.48f),
                    size = Size(w * 0.30f, h * 0.22f)
                )
                // Big curious eyes
                drawCircle(Color(0xFFFFF8E7), radius = w * 0.085f, center = Offset(w * 0.37f, h * 0.36f))
                drawCircle(Color(0xFFFFF8E7), radius = w * 0.085f, center = Offset(w * 0.63f, h * 0.36f))
                drawCircle(Color(0xFF13221C), radius = w * 0.042f, center = Offset(w * 0.38f, h * 0.36f))
                drawCircle(Color(0xFF13221C), radius = w * 0.042f, center = Offset(w * 0.62f, h * 0.36f))
                // Crystal back spots
                drawCircle(Color(0xFF8FF3FF), radius = w * 0.032f, center = Offset(cx, h * 0.45f))
                drawCircle(Color(0xFF8FF3FF), radius = w * 0.025f, center = Offset(w * 0.32f, h * 0.50f))
                drawCircle(Color(0xFF8FF3FF), radius = w * 0.025f, center = Offset(w * 0.68f, h * 0.50f))
            }

            "velvetmoth" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF211B40), Color(0xFF392B68))
                    )
                )
                // Left and right upper wings
                drawOval(
                    color = StarlightVioletAccent,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.36f, h * 0.34f)
                )
                drawOval(
                    color = StarlightVioletAccent,
                    topLeft = Offset(w * 0.52f, h * 0.22f),
                    size = Size(w * 0.36f, h * 0.34f)
                )
                // Lower wings
                drawOval(
                    color = OrchidCoralAccent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.18f, h * 0.48f),
                    size = Size(w * 0.28f, h * 0.26f)
                )
                drawOval(
                    color = OrchidCoralAccent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.54f, h * 0.48f),
                    size = Size(w * 0.28f, h * 0.26f)
                )
                // Wing starlight eyespots
                drawCircle(FireflyGoldAccent, radius = w * 0.06f, center = Offset(w * 0.28f, h * 0.38f))
                drawCircle(FireflyGoldAccent, radius = w * 0.06f, center = Offset(w * 0.72f, h * 0.38f))
                // Fuzzy moth body
                drawRoundRect(
                    color = Color(0xFFFFF4DC),
                    topLeft = Offset(cx - w * 0.055f, h * 0.30f),
                    size = Size(w * 0.11f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
                // Plumed antennae
                val antLeft = Path().apply {
                    moveTo(cx - w * 0.02f, h * 0.31f)
                    quadraticTo(w * 0.38f, h * 0.16f, w * 0.32f, h * 0.18f)
                }
                val antRight = Path().apply {
                    moveTo(cx + w * 0.02f, h * 0.31f)
                    quadraticTo(w * 0.62f, h * 0.16f, w * 0.68f, h * 0.18f)
                }
                drawPath(antLeft, FireflyGoldAccent, style = Stroke(width = 4f))
                drawPath(antRight, FireflyGoldAccent, style = Stroke(width = 4f))
            }

            else -> {
                // Brambleback the mossy guardian tortoise
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1A3A2C), Color(0xFF2B593F))
                    )
                )
                // Mossy shell dome
                drawArc(
                    color = Color(0xFF498B5A),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.18f, h * 0.26f),
                    size = Size(w * 0.64f, h * 0.64f)
                )
                // Golden shell rim
                drawRoundRect(
                    color = FireflyGoldAccent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.16f, h * 0.56f),
                    size = Size(w * 0.68f, h * 0.09f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                // Tortoise head
                drawCircle(
                    color = Color(0xFF79B473),
                    radius = w * 0.11f,
                    center = Offset(w * 0.78f, h * 0.56f)
                )
                drawCircle(
                    color = Color(0xFF13221C),
                    radius = w * 0.025f,
                    center = Offset(w * 0.81f, h * 0.53f)
                )
                // Sprout on shell
                drawLine(
                    color = Color(0xFFB5E48C),
                    start = Offset(cx, h * 0.28f),
                    end = Offset(cx, h * 0.16f),
                    strokeWidth = 5f
                )
                drawCircle(Color(0xFFF6C257), radius = w * 0.045f, center = Offset(cx, h * 0.16f))
            }
        }
    }
}
