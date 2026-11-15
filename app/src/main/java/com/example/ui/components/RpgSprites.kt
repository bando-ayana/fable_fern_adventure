package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.WildCreatureTemperament
import com.example.rpg.FacingDirection
import com.example.rpg.RpgBattleState
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private data class CharacterAppearance(
    val skin: Color,
    val hair: Color,
    val tunic: Color,
    val cloak: Color,
    val accent: Color,
    val weaponColor: Color
)

private fun appearanceForHero(heroId: String): CharacterAppearance = when (heroId) {
    "zuri" -> CharacterAppearance(
        skin = Color(0xFF5A3422),
        hair = Color(0xFF1B120E),
        tunic = Color(0xFF1F6E4A),
        cloak = Color(0xFF14422D),
        accent = FireflyGoldAccent,
        weaponColor = Color(0xFFFFD86B)
    )
    "kofi" -> CharacterAppearance(
        skin = Color(0xFF4A2A1A),
        hair = Color(0xFF17100C),
        tunic = Color(0xFF1B5B70),
        cloak = Color(0xFF103744),
        accent = Color(0xFF6CF0FF),
        weaponColor = Color(0xFF8FF8FF)
    )
    "amara" -> CharacterAppearance(
        skin = Color(0xFF3E2215),
        hair = Color(0xFF140D0A),
        tunic = Color(0xFF683B8B),
        cloak = Color(0xFF42215E),
        accent = FireflyGoldAccent,
        weaponColor = Color(0xFFE7B6FF)
    )
    "meilin" -> CharacterAppearance(
        skin = Color(0xFFF0C9A6),
        hair = Color(0xFF1D1D24),
        tunic = Color(0xFF287872),
        cloak = Color(0xFF174A46),
        accent = Color(0xFFFFD87A),
        weaponColor = Color(0xFFA0FFE6)
    )
    "mateo" -> CharacterAppearance(
        skin = Color(0xFFC88C62),
        hair = Color(0xFF2B1D16),
        tunic = Color(0xFF20748E),
        cloak = Color(0xFF144759),
        accent = Color(0xFFFFB259),
        weaponColor = Color(0xFF7BE8FF)
    )
    "aelindra" -> CharacterAppearance(
        skin = Color(0xFFF3DEC9),
        hair = Color(0xFFDCE7EE),
        tunic = Color(0xFF2E8555),
        cloak = Color(0xFF1B5335),
        accent = StarlightVioletAccent,
        weaponColor = Color(0xFFD4B5FF)
    )
    else -> CharacterAppearance( // Thorek
        skin = Color(0xFFE4B493),
        hair = Color(0xFF8C4A27),
        tunic = Color(0xFF476A58),
        cloak = Color(0xFF2C4236),
        accent = Color(0xFF8CEEFF),
        weaponColor = Color(0xFFB3F4FF)
    )
}

fun DrawScope.drawWorldHeroSprite(
    heroId: String,
    center: Offset,
    spriteSize: Float,
    facing: FacingDirection,
    walkCycle: Float,
    isMoving: Boolean,
    shieldActive: Boolean = false
) {
    val look = appearanceForHero(heroId)
    val stride = if (isMoving) sin(walkCycle) else 0f
    val bobY = if (isMoving) -kotlin.math.abs(stride) * spriteSize * 0.06f else sin(walkCycle * 0.5f) * spriteSize * 0.02f
    val cx = center.x
    val cy = center.y + bobY

    // Soft drop shadow
    drawOval(
        color = Color(0x5508140D),
        topLeft = Offset(cx - spriteSize * 0.34f, center.y + spriteSize * 0.28f),
        size = Size(spriteSize * 0.68f, spriteSize * 0.22f)
    )

    // Living Ward barrier aura if active
    if (shieldActive) {
        drawCircle(
            color = FernEmeraldAccent.copy(alpha = 0.22f),
            radius = spriteSize * 0.62f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF86FFBE),
            radius = spriteSize * 0.62f,
            center = Offset(cx, cy),
            style = Stroke(width = spriteSize * 0.05f)
        )
    }

    // Animated legs
    val leftLegY = cy + spriteSize * 0.20f + stride * spriteSize * 0.09f
    val rightLegY = cy + spriteSize * 0.20f - stride * spriteSize * 0.09f
    drawRoundRect(
        color = Color(0xFF202B26),
        topLeft = Offset(cx - spriteSize * 0.17f, leftLegY),
        size = Size(spriteSize * 0.13f, spriteSize * 0.20f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF202B26),
        topLeft = Offset(cx + spriteSize * 0.04f, rightLegY),
        size = Size(spriteSize * 0.13f, spriteSize * 0.20f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Solari cape / cloak behind torso
    val capeSway = stride * spriteSize * 0.06f
    val capePath = Path().apply {
        moveTo(cx - spriteSize * 0.22f, cy - spriteSize * 0.04f)
        lineTo(cx + spriteSize * 0.22f, cy - spriteSize * 0.04f)
        lineTo(cx + spriteSize * 0.28f + capeSway, cy + spriteSize * 0.27f)
        lineTo(cx - spriteSize * 0.28f + capeSway, cy + spriteSize * 0.27f)
        close()
    }
    drawPath(capePath, color = look.cloak)

    // Tunic torso
    drawRoundRect(
        color = look.tunic,
        topLeft = Offset(cx - spriteSize * 0.22f, cy - spriteSize * 0.08f),
        size = Size(spriteSize * 0.44f, spriteSize * 0.34f),
        cornerRadius = CornerRadius(spriteSize * 0.12f, spriteSize * 0.12f)
    )
    // Glowing magitech circuit trim
    drawLine(
        color = look.accent,
        start = Offset(cx, cy - spriteSize * 0.05f),
        end = Offset(cx, cy + spriteSize * 0.22f),
        strokeWidth = spriteSize * 0.045f,
        cap = StrokeCap.Round
    )

    // Animated arms + held magitech staff / orb
    val armOffset = stride * spriteSize * 0.08f
    drawCircle(
        color = look.skin,
        radius = spriteSize * 0.075f,
        center = Offset(cx - spriteSize * 0.26f, cy + spriteSize * 0.08f - armOffset)
    )
    val staffX = if (facing == FacingDirection.LEFT) cx - spriteSize * 0.30f else cx + spriteSize * 0.30f
    drawLine(
        color = Color(0xFF7B4D2B),
        start = Offset(staffX, cy + spriteSize * 0.28f + armOffset * 0.5f),
        end = Offset(staffX, cy - spriteSize * 0.30f + armOffset * 0.5f),
        strokeWidth = spriteSize * 0.055f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = look.weaponColor.copy(alpha = 0.45f),
        radius = spriteSize * 0.12f,
        center = Offset(staffX, cy - spriteSize * 0.33f + armOffset * 0.5f)
    )
    drawCircle(
        color = look.weaponColor,
        radius = spriteSize * 0.065f,
        center = Offset(staffX, cy - spriteSize * 0.33f + armOffset * 0.5f)
    )
    drawCircle(
        color = look.skin,
        radius = spriteSize * 0.075f,
        center = Offset(cx + spriteSize * 0.26f, cy + spriteSize * 0.08f + armOffset)
    )

    // Hair silhouette back
    val headCenter = Offset(cx, cy - spriteSize * 0.25f)
    when (heroId) {
        "zuri" -> {
            drawCircle(look.hair, radius = spriteSize * 0.25f, center = headCenter)
            drawCircle(look.accent, radius = spriteSize * 0.045f, center = Offset(cx - spriteSize * 0.19f, cy - spriteSize * 0.13f))
            drawCircle(look.accent, radius = spriteSize * 0.045f, center = Offset(cx + spriteSize * 0.19f, cy - spriteSize * 0.13f))
        }
        "kofi" -> {
            drawRoundRect(
                color = look.hair,
                topLeft = Offset(cx - spriteSize * 0.21f, cy - spriteSize * 0.54f),
                size = Size(spriteSize * 0.42f, spriteSize * 0.24f),
                cornerRadius = CornerRadius(spriteSize * 0.08f, spriteSize * 0.08f)
            )
        }
        "amara" -> {
            drawCircle(look.hair, radius = spriteSize * 0.27f, center = Offset(cx, cy - spriteSize * 0.31f))
            drawCircle(look.accent, radius = spriteSize * 0.27f, center = Offset(cx, cy - spriteSize * 0.31f), style = Stroke(spriteSize * 0.03f))
        }
        else -> {
            drawCircle(look.hair, radius = spriteSize * 0.24f, center = headCenter)
        }
    }

    // Head
    drawCircle(
        color = look.skin,
        radius = spriteSize * 0.21f,
        center = headCenter
    )
    // Top hair crown
    drawArc(
        color = look.hair,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - spriteSize * 0.21f, cy - spriteSize * 0.47f),
        size = Size(spriteSize * 0.42f, spriteSize * 0.24f)
    )

    // Thorek dwarf beard
    if (heroId == "thorek") {
        drawArc(
            color = look.hair,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - spriteSize * 0.18f, cy - spriteSize * 0.26f),
            size = Size(spriteSize * 0.36f, spriteSize * 0.28f)
        )
    }

    // Eyes / visor depending on facing direction
    if (facing != FacingDirection.UP) {
        val eyeShiftX = when (facing) {
            FacingDirection.LEFT -> -spriteSize * 0.05f
            FacingDirection.RIGHT -> spriteSize * 0.05f
            else -> 0f
        }
        drawCircle(Color(0xFF14100D), radius = spriteSize * 0.032f, center = Offset(cx - spriteSize * 0.075f + eyeShiftX, cy - spriteSize * 0.23f))
        drawCircle(Color(0xFF14100D), radius = spriteSize * 0.032f, center = Offset(cx + spriteSize * 0.075f + eyeShiftX, cy - spriteSize * 0.23f))
        if (heroId == "zuri" || heroId == "kofi") {
            drawRoundRect(
                color = look.accent.copy(alpha = 0.85f),
                topLeft = Offset(cx - spriteSize * 0.17f + eyeShiftX * 0.6f, cy - spriteSize * 0.34f),
                size = Size(spriteSize * 0.34f, spriteSize * 0.055f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

fun DrawScope.drawWorldCreatureSprite(
    id: String,
    center: Offset,
    spriteSize: Float,
    isWildEnemy: Boolean,
    isBeautiful: Boolean,
    walkPhase: Float,
    alertActive: Boolean = false,
    harmonized: Boolean = false
) {
    val bob = sin(walkPhase) * spriteSize * 0.05f
    val cy = center.y + bob
    val cx = center.x

    // Aura & Ground shadow
    val auraColor = when {
        harmonized -> FernEmeraldAccent
        !isWildEnemy -> DewTealAccent
        isBeautiful -> StarlightVioletAccent
        else -> OrchidCoralAccent
    }
    drawOval(
        color = Color(0x5006120B),
        topLeft = Offset(cx - spriteSize * 0.36f, center.y + spriteSize * 0.26f),
        size = Size(spriteSize * 0.72f, spriteSize * 0.22f)
    )
    if (isWildEnemy && !harmonized) {
        drawCircle(
            color = auraColor.copy(alpha = if (alertActive) 0.28f else 0.14f),
            radius = spriteSize * if (alertActive) 0.56f else 0.46f,
            center = Offset(cx, cy)
        )
    }

    when {
        id.contains("thornscale") || id.contains("briar") -> {
            // Armored jagged jungle prowler
            drawOval(
                color = Color(0xFF284F36),
                topLeft = Offset(cx - spriteSize * 0.32f, cy - spriteSize * 0.14f),
                size = Size(spriteSize * 0.64f, spriteSize * 0.42f)
            )
            // Back thorns
            for (dx in listOf(-0.18f, 0f, 0.18f)) {
                val spike = Path().apply {
                    moveTo(cx + spriteSize * (dx - 0.06f), cy - spriteSize * 0.10f)
                    lineTo(cx + spriteSize * dx, cy - spriteSize * 0.36f)
                    lineTo(cx + spriteSize * (dx + 0.06f), cy - spriteSize * 0.10f)
                    close()
                }
                drawPath(spike, color = OrchidCoralAccent)
            }
            // Fierce amber eyes
            drawCircle(Color(0xFFFFCF54), radius = spriteSize * 0.055f, center = Offset(cx - spriteSize * 0.10f, cy + spriteSize * 0.02f))
            drawCircle(Color(0xFFFFCF54), radius = spriteSize * 0.055f, center = Offset(cx + spriteSize * 0.10f, cy + spriteSize * 0.02f))
        }

        id.contains("prism") || id.contains("stag") -> {
            val wingSpread = sin(walkPhase * 1.4f) * spriteSize * 0.06f
            // Translucent prism wings
            drawOval(
                color = Color(0xFF7AEBFF).copy(alpha = 0.58f),
                topLeft = Offset(cx - spriteSize * 0.48f, cy - spriteSize * 0.28f + wingSpread),
                size = Size(spriteSize * 0.34f, spriteSize * 0.26f)
            )
            drawOval(
                color = StarlightVioletAccent.copy(alpha = 0.58f),
                topLeft = Offset(cx + spriteSize * 0.14f, cy - spriteSize * 0.28f + wingSpread),
                size = Size(spriteSize * 0.34f, spriteSize * 0.26f)
            )
            // Ivory stag body & head
            drawOval(
                color = Color(0xFFEAF7F2),
                topLeft = Offset(cx - spriteSize * 0.22f, cy - spriteSize * 0.10f),
                size = Size(spriteSize * 0.44f, spriteSize * 0.36f)
            )
            // Crystal antlers
            drawLine(FireflyGoldAccent, Offset(cx - spriteSize * 0.08f, cy - spriteSize * 0.10f), Offset(cx - spriteSize * 0.22f, cy - spriteSize * 0.42f), spriteSize * 0.045f)
            drawLine(FireflyGoldAccent, Offset(cx + spriteSize * 0.08f, cy - spriteSize * 0.10f), Offset(cx + spriteSize * 0.22f, cy - spriteSize * 0.42f), spriteSize * 0.045f)
            drawCircle(Color(0xFF19353B), radius = spriteSize * 0.04f, center = Offset(cx - spriteSize * 0.08f, cy + spriteSize * 0.02f))
            drawCircle(Color(0xFF19353B), radius = spriteSize * 0.04f, center = Offset(cx + spriteSize * 0.08f, cy + spriteSize * 0.02f))
        }

        id.contains("mirefang") || id.contains("serpent") -> {
            // Coiled mossy python
            drawOval(
                color = Color(0xFF215E42),
                topLeft = Offset(cx - spriteSize * 0.35f, cy + spriteSize * 0.02f),
                size = Size(spriteSize * 0.70f, spriteSize * 0.30f)
            )
            drawCircle(
                color = Color(0xFF317E59),
                radius = spriteSize * 0.20f,
                center = Offset(cx, cy - spriteSize * 0.12f)
            )
            // Hood crest & amber fangs
            drawCircle(Color(0xFFFFC145), radius = spriteSize * 0.05f, center = Offset(cx - spriteSize * 0.08f, cy - spriteSize * 0.16f))
            drawCircle(Color(0xFFFFC145), radius = spriteSize * 0.05f, center = Offset(cx + spriteSize * 0.08f, cy - spriteSize * 0.16f))
            drawCircle(Color.White, radius = spriteSize * 0.03f, center = Offset(cx - spriteSize * 0.06f, cy - spriteSize * 0.02f))
            drawCircle(Color.White, radius = spriteSize * 0.03f, center = Offset(cx + spriteSize * 0.06f, cy - spriteSize * 0.02f))
        }

        id.contains("orchid") || id.contains("lynx") -> {
            // Luminous silver-lavender spirit cat
            drawCircle(color = StarlightVioletAccent.copy(alpha = 0.7f), radius = spriteSize * 0.29f, center = Offset(cx, cy - spriteSize * 0.02f))
            drawCircle(color = Color(0xFFE6DFF5), radius = spriteSize * 0.23f, center = Offset(cx, cy))
            // Pointed lynx ears
            val leftEar = Path().apply {
                moveTo(cx - spriteSize * 0.20f, cy - spriteSize * 0.14f)
                lineTo(cx - spriteSize * 0.24f, cy - spriteSize * 0.38f)
                lineTo(cx - spriteSize * 0.06f, cy - spriteSize * 0.20f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(cx + spriteSize * 0.20f, cy - spriteSize * 0.14f)
                lineTo(cx + spriteSize * 0.24f, cy - spriteSize * 0.38f)
                lineTo(cx + spriteSize * 0.06f, cy - spriteSize * 0.20f)
                close()
            }
            drawPath(leftEar, OrchidCoralAccent)
            drawPath(rightEar, OrchidCoralAccent)
            drawCircle(Color(0xFF281A45), radius = spriteSize * 0.042f, center = Offset(cx - spriteSize * 0.08f, cy - spriteSize * 0.02f))
            drawCircle(Color(0xFF281A45), radius = spriteSize * 0.042f, center = Offset(cx + spriteSize * 0.08f, cy - spriteSize * 0.02f))
        }

        id.contains("wyvern") || id.contains("storm") -> {
            val flap = sin(walkPhase * 1.8f) * spriteSize * 0.09f
            // Bat-like storm wings
            drawOval(
                color = Color(0xFF292E49),
                topLeft = Offset(cx - spriteSize * 0.50f, cy - spriteSize * 0.28f + flap),
                size = Size(spriteSize * 0.38f, spriteSize * 0.30f)
            )
            drawOval(
                color = Color(0xFF292E49),
                topLeft = Offset(cx + spriteSize * 0.12f, cy - spriteSize * 0.28f + flap),
                size = Size(spriteSize * 0.38f, spriteSize * 0.30f)
            )
            drawCircle(color = Color(0xFF1A1E33), radius = spriteSize * 0.24f, center = Offset(cx, cy))
            drawCircle(Color(0xFF6CF0FF), radius = spriteSize * 0.055f, center = Offset(cx - spriteSize * 0.09f, cy - spriteSize * 0.04f))
            drawCircle(Color(0xFF6CF0FF), radius = spriteSize * 0.055f, center = Offset(cx + spriteSize * 0.09f, cy - spriteSize * 0.04f))
        }

        id.contains("phoenix") || id.contains("aurora") -> {
            val wingWave = sin(walkPhase * 1.6f) * spriteSize * 0.08f
            // Radiant emerald & gold wings
            drawOval(
                color = FireflyGoldAccent.copy(alpha = 0.85f),
                topLeft = Offset(cx - spriteSize * 0.50f, cy - spriteSize * 0.24f + wingWave),
                size = Size(spriteSize * 0.38f, spriteSize * 0.24f)
            )
            drawOval(
                color = FireflyGoldAccent.copy(alpha = 0.85f),
                topLeft = Offset(cx + spriteSize * 0.12f, cy - spriteSize * 0.24f + wingWave),
                size = Size(spriteSize * 0.38f, spriteSize * 0.24f)
            )
            drawCircle(color = FernEmeraldAccent, radius = spriteSize * 0.22f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFFFFF6CC), radius = spriteSize * 0.11f, center = Offset(cx, cy - spriteSize * 0.16f))
        }

        id.contains("shadowroot") -> {
            // Towering ancient colossus
            drawRoundRect(
                color = Color(0xFF2E1E36),
                topLeft = Offset(cx - spriteSize * 0.34f, cy - spriteSize * 0.32f),
                size = Size(spriteSize * 0.68f, spriteSize * 0.62f),
                cornerRadius = CornerRadius(spriteSize * 0.16f, spriteSize * 0.16f)
            )
            drawCircle(
                color = OrchidCoralAccent.copy(alpha = 0.8f),
                radius = spriteSize * 0.13f,
                center = Offset(cx, cy)
            )
            drawCircle(Color(0xFFFFE066), radius = spriteSize * 0.055f, center = Offset(cx - spriteSize * 0.14f, cy - spriteSize * 0.14f))
            drawCircle(Color(0xFFFFE066), radius = spriteSize * 0.055f, center = Offset(cx + spriteSize * 0.14f, cy - spriteSize * 0.14f))
        }

        id.contains("lumafern") -> {
            // Friendly glowing fox spirit
            drawCircle(color = Color(0xFFD97236), radius = spriteSize * 0.22f, center = Offset(cx, cy))
            drawLine(FireflyGoldAccent, Offset(cx - spriteSize * 0.08f, cy - spriteSize * 0.18f), Offset(cx - spriteSize * 0.18f, cy - spriteSize * 0.36f), spriteSize * 0.04f)
            drawLine(FireflyGoldAccent, Offset(cx + spriteSize * 0.08f, cy - spriteSize * 0.18f), Offset(cx + spriteSize * 0.18f, cy - spriteSize * 0.36f), spriteSize * 0.04f)
            drawCircle(Color.White, radius = spriteSize * 0.03f, center = Offset(cx - spriteSize * 0.07f, cy - spriteSize * 0.02f))
            drawCircle(Color.White, radius = spriteSize * 0.03f, center = Offset(cx + spriteSize * 0.07f, cy - spriteSize * 0.02f))
        }

        id.contains("dewdrake") -> {
            // Friendly lagoon hatchling
            drawCircle(color = DewTealAccent, radius = spriteSize * 0.22f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF9EFFF4), radius = spriteSize * 0.12f, center = Offset(cx, cy + spriteSize * 0.06f))
            drawCircle(Color(0xFF14282A), radius = spriteSize * 0.035f, center = Offset(cx - spriteSize * 0.07f, cy - spriteSize * 0.04f))
            drawCircle(Color(0xFF14282A), radius = spriteSize * 0.035f, center = Offset(cx + spriteSize * 0.07f, cy - spriteSize * 0.04f))
        }

        id.contains("aureowl") -> {
            drawOval(color = Color(0xFF9C7E4C), topLeft = Offset(cx - spriteSize * 0.22f, cy - spriteSize * 0.22f), size = Size(spriteSize * 0.44f, spriteSize * 0.48f))
            drawCircle(FireflyGoldAccent, radius = spriteSize * 0.06f, center = Offset(cx - spriteSize * 0.08f, cy - spriteSize * 0.06f))
            drawCircle(FireflyGoldAccent, radius = spriteSize * 0.06f, center = Offset(cx + spriteSize * 0.08f, cy - spriteSize * 0.06f))
        }

        else -> {
            // Brambleback mossy tortoise
            drawArc(
                color = FernEmeraldAccent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - spriteSize * 0.28f, cy - spriteSize * 0.20f),
                size = Size(spriteSize * 0.56f, spriteSize * 0.48f)
            )
            drawCircle(FireflyGoldAccent, radius = spriteSize * 0.055f, center = Offset(cx, cy - spriteSize * 0.22f))
        }
    }

    // Alert exclamation or spirit star above wild enemies
    if (isWildEnemy && alertActive && !harmonized) {
        drawCircle(
            color = if (isBeautiful) StarlightVioletAccent else OrchidCoralAccent,
            radius = spriteSize * 0.14f,
            center = Offset(cx, cy - spriteSize * 0.52f)
        )
        drawCircle(
            color = Color.White,
            radius = spriteSize * 0.055f,
            center = Offset(cx, cy - spriteSize * 0.52f)
        )
    }
}

@Composable
fun BattleArenaStage(
    heroId: String,
    battleState: RpgBattleState,
    modifier: Modifier = Modifier
) {
    val opponent = battleState.mission.opponent
    val isBeautiful = opponent.temperament == WildCreatureTemperament.BEAUTIFUL
    val enemyAccent = if (isBeautiful) StarlightVioletAccent else OrchidCoralAccent

    val infinite = rememberInfiniteTransition(label = "arena_idle")
    val idlePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arena_phase"
    )

    var heroLunging by remember { mutableStateOf(false) }
    var enemyHitFlash by remember { mutableStateOf(false) }
    var heroHitFlash by remember { mutableStateOf(false) }

    LaunchedEffect(battleState.lastEnemyHitTick) {
        if (battleState.lastEnemyHitTick > 0) {
            heroLunging = true
            enemyHitFlash = true
            delay(260L)
            heroLunging = false
            enemyHitFlash = false
        }
    }

    LaunchedEffect(battleState.lastHeroHitTick) {
        if (battleState.lastHeroHitTick > 0) {
            heroHitFlash = true
            delay(260L)
            heroHitFlash = false
        }
    }

    val heroShift by animateFloatAsState(
        targetValue = if (heroLunging) 1f else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "hero_lunge"
    )
    val enemyRecoil by animateFloatAsState(
        targetValue = if (enemyHitFlash) 1f else 0f,
        animationSpec = spring(stiffness = 600f),
        label = "enemy_recoil"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Twilight jungle battle clearing backdrop
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E261C),
                        Color(0xFF173D2B),
                        Color(0xFF215238)
                    )
                )
            )
            // Canopy moonbeam shaft
            drawOval(
                color = FireflyGoldAccent.copy(alpha = 0.08f),
                topLeft = Offset(w * 0.25f, -h * 0.35f),
                size = Size(w * 0.50f, h * 1.4f)
            )
            // Background jungle trees silhouettes
            for (i in 0..6) {
                val tx = w * (0.08f + i * 0.15f)
                drawRoundRect(
                    color = Color(0xFF0A1D15),
                    topLeft = Offset(tx - w * 0.025f, h * 0.14f),
                    size = Size(w * 0.05f, h * 0.52f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawCircle(
                    color = Color(0xFF113022),
                    radius = w * 0.08f,
                    center = Offset(tx, h * 0.16f)
                )
            }
            // Mossy clearing ground
            drawOval(
                color = Color(0xFF2A6943),
                topLeft = Offset(w * 0.04f, h * 0.56f),
                size = Size(w * 0.92f, h * 0.50f)
            )

            // Floating fireflies
            for (i in 0..5) {
                val fx = w * (0.15f + i * 0.14f) + sin(idlePhase + i) * 14f
                val fy = h * (0.24f + (i % 3) * 0.16f) + sin(idlePhase * 1.3f + i) * 10f
                drawCircle(FireflyGoldAccent.copy(alpha = 0.55f), radius = 4f, center = Offset(fx, fy))
            }

            // Spell beam effect when hero lunges/strikes
            val heroCenter = Offset(w * (0.24f + heroShift * 0.14f), h * 0.60f)
            val enemyCenter = Offset(w * (0.74f + enemyRecoil * 0.06f), h * 0.54f)

            if (heroShift > 0.15f) {
                drawLine(
                    color = FireflyGoldAccent.copy(alpha = heroShift * 0.7f),
                    start = heroCenter.copy(y = heroCenter.y - 16f),
                    end = enemyCenter,
                    strokeWidth = 10f * heroShift,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = enemyAccent.copy(alpha = heroShift * 0.6f),
                    radius = 38f * heroShift,
                    center = enemyCenter
                )
            }

            // Hero sprite on left
            drawWorldHeroSprite(
                heroId = heroId,
                center = heroCenter,
                spriteSize = h * 0.44f,
                facing = FacingDirection.RIGHT,
                walkCycle = if (heroShift > 0.1f) idlePhase * 3f else idlePhase,
                isMoving = heroShift > 0.1f,
                shieldActive = battleState.wardActive
            )
            if (heroHitFlash) {
                drawCircle(
                    color = OrchidCoralAccent.copy(alpha = 0.38f),
                    radius = h * 0.24f,
                    center = heroCenter
                )
            }

            // Wild Creature sprite on right
            drawWorldCreatureSprite(
                id = opponent.id,
                center = enemyCenter,
                spriteSize = h * 0.52f,
                isWildEnemy = true,
                isBeautiful = isBeautiful,
                walkPhase = idlePhase * 1.2f,
                alertActive = !battleState.calmedSpirit && enemyHitFlash,
                harmonized = battleState.calmedSpirit
            )
            if (enemyHitFlash) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = h * 0.26f,
                    center = enemyCenter
                )
            }
        }

        // Arena zone badge top-left
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xBB0D1E16),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = opponent.habitat,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFDFF3E6),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Latest battle action banner at bottom
        battleState.combatLog.firstOrNull()?.let { latest ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCC0D1E16),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = latest,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}