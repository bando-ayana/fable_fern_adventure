package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

@Composable
fun HeroAvatar(
    heroId: String,
    unlocked: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked traveler",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f

                val (bgTop, bgBottom, skinColor, hairColor, outfitColor, accentColor) = when (heroId) {
                    "zuri" -> HeroPalette(
                        Color(0xFF173D2D), Color(0xFF286B4E),
                        Color(0xFF5A3422), Color(0xFF1F1410),
                        Color(0xFF1B5E3F), FireflyGoldAccent
                    )
                    "kofi" -> HeroPalette(
                        Color(0xFF123244), Color(0xFF1F5D75),
                        Color(0xFF4A2A1A), Color(0xFF1A120E),
                        Color(0xFF1E5668), Color(0xFF6CF0FF)
                    )
                    "amara" -> HeroPalette(
                        Color(0xFF2B1C3D), Color(0xFF51336E),
                        Color(0xFF3E2215), Color(0xFF160E0B),
                        Color(0xFF6B3E8E), FireflyGoldAccent
                    )
                    "meilin" -> HeroPalette(
                        Color(0xFF1A3A3A), Color(0xFF2B6F6C),
                        Color(0xFFF0C9A6), Color(0xFF1D1D24),
                        DewTealAccent, Color(0xFFFFD87A)
                    )
                    "mateo" -> HeroPalette(
                        Color(0xFF17344D), Color(0xFF285F82),
                        Color(0xFFC88C62), Color(0xFF2B1D16),
                        Color(0xFF20748E), Color(0xFFFFB259)
                    )
                    "aelindra" -> HeroPalette(
                        Color(0xFF1C352A), Color(0xFF36664D),
                        Color(0xFFF3DEC9), Color(0xFFDCE7EE),
                        FernEmeraldAccent, StarlightVioletAccent
                    )
                    else -> HeroPalette( // Thorek
                        Color(0xFF31241E), Color(0xFF5B4133),
                        Color(0xFFE4B493), Color(0xFF8C4A27),
                        Color(0xFF476A58), Color(0xFF8CEEFF)
                    )
                }

                drawRect(brush = Brush.verticalGradient(listOf(bgTop, bgBottom)))

                // Magitech aura ring behind head
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f),
                    radius = w * 0.34f,
                    center = Offset(cx, h * 0.44f),
                    style = Stroke(width = w * 0.03f)
                )

                // Shoulders / Solari magitech tunic
                drawRoundRect(
                    color = outfitColor,
                    topLeft = Offset(w * 0.16f, h * 0.68f),
                    size = Size(w * 0.68f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
                )
                // Glowing circuit V-collar
                val collar = Path().apply {
                    moveTo(w * 0.34f, h * 0.68f)
                    lineTo(cx, h * 0.84f)
                    lineTo(w * 0.66f, h * 0.68f)
                }
                drawPath(collar, color = accentColor, style = Stroke(width = w * 0.035f))

                // Hair back silhouette
                when (heroId) {
                    "zuri" -> {
                        // Flowing gold-cuffed locs
                        for (dx in listOf(-0.22f, -0.15f, 0.15f, 0.22f)) {
                            drawRoundRect(
                                color = hairColor,
                                topLeft = Offset(cx + w * dx - w * 0.045f, h * 0.26f),
                                size = Size(w * 0.09f, h * 0.46f),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                            drawCircle(
                                color = FireflyGoldAccent,
                                radius = w * 0.028f,
                                center = Offset(cx + w * dx, h * 0.64f)
                            )
                        }
                    }
                    "amara" -> {
                        // Regal braided crown
                        drawCircle(color = hairColor, radius = w * 0.24f, center = Offset(cx, h * 0.31f))
                        drawCircle(
                            color = FireflyGoldAccent,
                            radius = w * 0.24f,
                            center = Offset(cx, h * 0.31f),
                            style = Stroke(width = w * 0.025f)
                        )
                    }
                    "kofi" -> {
                        // Sculpted high-top locs
                        drawRoundRect(
                            color = hairColor,
                            topLeft = Offset(cx - w * 0.21f, h * 0.14f),
                            size = Size(w * 0.42f, h * 0.26f),
                            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                        )
                    }
                    "aelindra" -> {
                        // Long silver-white elven hair + pointed ears
                        drawRoundRect(
                            color = hairColor,
                            topLeft = Offset(cx - w * 0.26f, h * 0.24f),
                            size = Size(w * 0.52f, h * 0.50f),
                            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
                        )
                    }
                    else -> {
                        drawCircle(color = hairColor, radius = w * 0.24f, center = Offset(cx, h * 0.36f))
                    }
                }

                // Pointed ears for Elf
                if (heroId == "aelindra") {
                    drawCircle(skinColor, radius = w * 0.07f, center = Offset(w * 0.25f, h * 0.45f))
                    drawCircle(skinColor, radius = w * 0.07f, center = Offset(w * 0.75f, h * 0.45f))
                }

                // Neck & Head
                drawRoundRect(
                    color = skinColor,
                    topLeft = Offset(cx - w * 0.07f, h * 0.56f),
                    size = Size(w * 0.14f, h * 0.16f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawOval(
                    color = skinColor,
                    topLeft = Offset(cx - w * 0.19f, h * 0.26f),
                    size = Size(w * 0.38f, h * 0.39f)
                )

                // Hair bangs / crown cap
                drawArc(
                    color = hairColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.20f, h * 0.24f),
                    size = Size(w * 0.40f, h * 0.22f)
                )

                // Dwarf beard for Thorek
                if (heroId == "thorek") {
                    drawArc(
                        color = hairColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(cx - w * 0.19f, h * 0.46f),
                        size = Size(w * 0.38f, h * 0.30f)
                    )
                }

                // Warm eyes
                drawCircle(Color(0xFF14110F), radius = w * 0.028f, center = Offset(cx - w * 0.075f, h * 0.44f))
                drawCircle(Color(0xFF14110F), radius = w * 0.028f, center = Offset(cx + w * 0.075f, h * 0.44f))
                drawCircle(Color.White, radius = w * 0.009f, center = Offset(cx - w * 0.068f, h * 0.432f))
                drawCircle(Color.White, radius = w * 0.009f, center = Offset(cx + w * 0.082f, h * 0.432f))

                // Gentle smile
                drawArc(
                    color = if (heroId == "thorek") Color(0xFFFFE7D1) else Color(0xFF281710),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.07f, h * 0.49f),
                    size = Size(w * 0.14f, h * 0.08f),
                    style = Stroke(width = w * 0.022f)
                )

                // Solari magitech visor/headband accent on founding heroes
                if (heroId == "zuri" || heroId == "kofi") {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.85f),
                        topLeft = Offset(cx - w * 0.18f, h * 0.33f),
                        size = Size(w * 0.36f, h * 0.045f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
    }
}

private data class HeroPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val skin: Color,
    val hair: Color,
    val outfit: Color,
    val accent: Color
)
