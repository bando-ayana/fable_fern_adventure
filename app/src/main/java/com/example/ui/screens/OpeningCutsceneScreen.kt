package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.rpg.FacingDirection
import com.example.ui.components.drawWorldCreatureSprite
import com.example.ui.components.drawWorldHeroSprite
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class CutsceneChapter(
    val chapterLabel: String,
    val title: String,
    val narration: String,
    val highlightGoal: String
)

private val cutsceneChapters = listOf(
    CutsceneChapter(
        chapterLabel = "Prologue · 1 of 4",
        title = "The Solari Magitech Canopy",
        narration = "Suspended high above emerald mist stands Solari Canopy—a thriving civilization where scholars and botanists braid solar-crystal brass with living roots in peaceful harmony.",
        highlightGoal = "Step into the boots of Solari's Vanguard Explorers."
    ),
    CutsceneChapter(
        chapterLabel = "Prologue · 2 of 4",
        title = "The Shadowroot Awakening",
        narration = "Beneath the lower boughs, tangled Shadowroot vines have severed the Sun-Bridges, choked the aqueducts, and stirred wild creatures—from prickly prowlers to proud mythic spirits.",
        highlightGoal = "The canopy community needs a courageous pathfinder to restore balance."
    ),
    CutsceneChapter(
        chapterLabel = "Prologue · 3 of 4",
        title = "Your Goal: Awaken the Canopy Crown",
        narration = "Explore the living wilds, battle or soothe creatures to gain Hero EXP, Magic, Mana & Loot, and rebuild Community Missions until Town Prosperity reaches 100%.",
        highlightGoal = "Reach 100% Town Prosperity & defeat the Shadowroot Sovereign to win!"
    ),
    CutsceneChapter(
        chapterLabel = "Prologue · 4 of 4",
        title = "Elder Nia's Training Glade",
        narration = "Every great explorer starts with a few sparks. Enter Elder Nia's sunlit Training Glade to practice walking, lighting spore-lanterns, solving a puzzle shrine, and winning your first wild battle.",
        highlightGoal = "Complete the 3 quick lessons to unlock Solari Canopy + 20 Starter Nectar!"
    )
)

@Composable
fun OpeningCutsceneScreen(
    page: Int,
    heroId: String,
    isAudioMuted: Boolean,
    onToggleAudioMute: () -> Unit,
    onPageChange: (Int) -> Unit,
    onBeginTraining: () -> Unit,
    onSkipIntro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safePage = page.coerceIn(0, cutsceneChapters.lastIndex)
    val chapter = cutsceneChapters[safePage]

    BackHandler(enabled = safePage > 0) {
        onPageChange(safePage - 1)
    }

    val infinite = rememberInfiniteTransition(label = "cutscene_ambient")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cutscene_phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1C14),
                        Color(0xFF132D20),
                        Color(0xFF0C1A13)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top controls bar: Audio + Skip Intro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xCC183828),
                    border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = FireflyGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = chapter.chapterLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFEBA8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleAudioMute,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cutscene_audio_toggle")
                    ) {
                        Icon(
                            imageVector = if (isAudioMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.Rounded.MusicNote
                            },
                            contentDescription = stringResource(R.string.cd_toggle_audio),
                            tint = FireflyGoldAccent
                        )
                    }
                    TextButton(
                        onClick = onSkipIntro,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cutscene_skip_button")
                    ) {
                        Text(
                            text = stringResource(R.string.intro_skip),
                            color = Color(0xFFCDE7D8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Center Animated Storybook Visual & Narration
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnimatedContent(
                    targetState = safePage,
                    transitionSpec = { fadeIn(tween(340)) togetherWith fadeOut(tween(240)) },
                    label = "cutscene_stage"
                ) { targetPage ->
                    CutsceneStageArtwork(
                        page = targetPage,
                        heroId = heroId,
                        phase = phase
                    )
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = chapter.narration,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (safePage == 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CutscenePillarRow(
                                    icon = Icons.Rounded.Flag,
                                    tint = FernEmeraldAccent,
                                    text = "Rebuild Solari Community Missions (0% -> 100% Prosperity)"
                                )
                                CutscenePillarRow(
                                    icon = Icons.Rounded.Pets,
                                    tint = OrchidCoralAccent,
                                    text = "Battle & Soothe Wild Creatures for EXP, Magic, Mana & Loot"
                                )
                                CutscenePillarRow(
                                    icon = Icons.Rounded.Extension,
                                    tint = StarlightVioletAccent,
                                    text = "Access 7 Puzzle Shrines in-world to unlock Puzzles Tab replays"
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = FireflyGoldAccent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Explore,
                                    contentDescription = null,
                                    tint = FireflyGoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chapter.highlightGoal,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cutsceneChapters.indices.forEach { idx ->
                        val active = idx == safePage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) FireflyGoldAccent
                                    else Color(0xFF476957)
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (safePage > 0) {
                        OutlinedButton(
                            onClick = { onPageChange(safePage - 1) },
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cutscene_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    }

                    Button(
                        onClick = {
                            if (safePage < cutsceneChapters.lastIndex) {
                                onPageChange(safePage + 1)
                            } else {
                                onBeginTraining()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FireflyGoldAccent,
                            contentColor = Color(0xFF1A2812)
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag(
                                if (safePage < cutsceneChapters.lastIndex) "cutscene_next_button"
                                else "cutscene_start_training_button"
                            )
                    ) {
                        Text(
                            text = if (safePage < cutsceneChapters.lastIndex) {
                                "Continue Story"
                            } else {
                                stringResource(R.string.intro_begin_training)
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CutscenePillarRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(tint.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CutsceneStageArtwork(
    page: Int,
    heroId: String,
    phase: Float
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.5.dp, FireflyGoldAccent.copy(alpha = 0.45f)),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (page) {
                0 -> {
                    Image(
                        painter = painterResource(id = R.drawable.img_jungle_canopy_hero),
                        contentDescription = stringResource(R.string.cd_jungle_hero),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x880B1A13))
                            )
                        )
                        for (i in 0..9) {
                            val fx = size.width * (0.08f + i * 0.095f) + cos(phase + i) * 14f
                            val fy = size.height * (0.18f + (i % 4) * 0.17f) + sin(phase * 1.4f + i) * 12f
                            drawCircle(
                                color = FireflyGoldAccent.copy(alpha = 0.65f),
                                radius = 5f + (i % 3) * 2f,
                                center = Offset(fx, fy)
                            )
                        }
                    }
                }

                1 -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color(0xFF141126), Color(0xFF1F223A), Color(0xFF122C21))
                            )
                        )
                        // Creeping Shadowroot silhouette & mist
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(OrchidCoralAccent.copy(alpha = 0.25f), Color.Transparent),
                                center = Offset(w * 0.70f, h * 0.48f),
                                radius = w * 0.35f
                            ),
                            radius = w * 0.35f,
                            center = Offset(w * 0.70f, h * 0.48f)
                        )
                        drawOval(
                            color = Color(0xFF1B3A2A),
                            topLeft = Offset(w * 0.05f, h * 0.62f),
                            size = Size(w * 0.90f, h * 0.42f)
                        )
                        drawWorldCreatureSprite(
                            id = "shadowroot",
                            center = Offset(w * 0.70f, h * 0.52f),
                            spriteSize = h * 0.56f,
                            isWildEnemy = true,
                            isBeautiful = false,
                            walkPhase = phase * 1.3f,
                            alertActive = true
                        )
                        drawWorldHeroSprite(
                            heroId = heroId,
                            center = Offset(w * 0.26f, h * 0.62f),
                            spriteSize = h * 0.44f,
                            facing = FacingDirection.RIGHT,
                            walkCycle = phase,
                            isMoving = false,
                            shieldActive = true
                        )
                    }
                }

                2 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF133627), Color(0xFF1C4634), Color(0xFF23304A))
                                )
                            )
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CreatureMedallion(
                                drawableRes = R.drawable.img_creature_lumafern,
                                label = "Lumafern",
                                accent = FireflyGoldAccent
                            )
                            CreatureMedallion(
                                drawableRes = R.drawable.img_creature_dewdrake,
                                label = "Dewdrake",
                                accent = DewTealAccent
                            )
                            CreatureMedallion(
                                drawableRes = R.drawable.img_creature_aureowl,
                                label = "Aureowl",
                                accent = StarlightVioletAccent
                            )
                        }
                    }
                }

                else -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color(0xFF133C29), Color(0xFF1F5A3B), Color(0xFF286E46))
                            )
                        )
                        // Training lantern glow on center
                        val pulse = 0.7f + 0.3f * sin(phase * 2f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(FireflyGoldAccent.copy(alpha = 0.45f * pulse), Color.Transparent),
                                center = Offset(w * 0.50f, h * 0.46f),
                                radius = w * 0.25f
                            ),
                            radius = w * 0.25f,
                            center = Offset(w * 0.50f, h * 0.46f)
                        )
                        drawOval(
                            color = Color(0xFF337A4B),
                            topLeft = Offset(w * 0.08f, h * 0.62f),
                            size = Size(w * 0.84f, h * 0.38f)
                        )
                        drawWorldHeroSprite(
                            heroId = heroId,
                            center = Offset(w * 0.28f, h * 0.60f),
                            spriteSize = h * 0.44f,
                            facing = FacingDirection.RIGHT,
                            walkCycle = phase * 2.2f,
                            isMoving = true
                        )
                        drawWorldCreatureSprite(
                            id = "lumafern",
                            center = Offset(w * 0.48f, h * 0.56f),
                            spriteSize = h * 0.36f,
                            isWildEnemy = false,
                            isBeautiful = false,
                            walkPhase = phase
                        )
                        drawWorldCreatureSprite(
                            id = "mossback_sproutling",
                            center = Offset(w * 0.74f, h * 0.60f),
                            spriteSize = h * 0.42f,
                            isWildEnemy = true,
                            isBeautiful = false,
                            walkPhase = phase * 1.5f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatureMedallion(
    drawableRes: Int,
    label: String,
    accent: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, accent),
            modifier = Modifier.size(92.dp)
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
