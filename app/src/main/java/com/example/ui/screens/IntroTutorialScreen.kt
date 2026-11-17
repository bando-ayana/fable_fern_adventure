package com.example.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RpgMissionCatalog
import com.example.model.CommunityMission
import com.example.model.PuzzleType
import com.example.rpg.InteractableKind
import com.example.rpg.IntroStage
import com.example.rpg.IntroTutorialEngine
import com.example.rpg.IntroTutorialState
import com.example.rpg.WorldActionType
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.VirtualAnalogStick
import com.example.ui.components.drawInteractableObject
import com.example.ui.components.drawWorldCreatureSprite
import com.example.ui.components.drawWorldHeroSprite
import com.example.ui.components.drawWorldTile
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import kotlin.math.sin

@Composable
fun IntroTutorialScreen(
    introState: IntroTutorialState,
    heroId: String,
    heroLevel: Int,
    mana: Int,
    maxMana: Int,
    nectar: Int,
    isAudioMuted: Boolean,
    hasActiveBattle: Boolean,
    onToggleAudioMute: () -> Unit,
    onSkipIntro: () -> Unit,
    onGraduateToWorld: () -> Unit,
    onStepFrame: (Float, Float, Float) -> Unit,
    onSetTapTarget: (Float, Float) -> Unit,
    onInteractLantern: (String, String) -> Unit,
    onLaunchIntroPuzzle: (PuzzleType) -> Unit,
    onStartIntroBattle: (CommunityMission, Boolean) -> Unit,
    onRestMana: () -> Unit,
    onShowDialogue: (String, String) -> Unit,
    onDismissDialogue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val worldState = introState.worldState
    val stage = introState.stage
    val calmMode = LocalAccessibilityUi.current.calmMode
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }
    var animTime by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val tilePx = remember(density) { with(density) { 54.dp.toPx() } }

    LaunchedEffect(hasActiveBattle) {
        if (hasActiveBattle) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    animTime = (animTime + dt) % 1000f
                    onStepFrame(joyX, joyY, dt)
                }
                lastNanos = frameNanos
            }
        }
    }

    val nearestTarget = remember(
        worldState.heroX,
        worldState.heroY,
        worldState.creatures,
        worldState.litLanternIds,
        introState.sproutlingDefeated
    ) {
        IntroTutorialEngine.findNearestTarget(introState)
    }

    val worldPixelW = IntroTutorialEngine.MAP_WIDTH * tilePx
    val worldPixelH = IntroTutorialEngine.MAP_HEIGHT * tilePx
    val viewW = canvasSize.width.toFloat().coerceAtLeast(1f)
    val viewH = canvasSize.height.toFloat().coerceAtLeast(1f)
    val camX = if (worldPixelW <= viewW) {
        (worldPixelW - viewW) / 2f
    } else {
        (worldState.heroX * tilePx - viewW / 2f).coerceIn(0f, worldPixelW - viewW)
    }
    val camY = if (worldPixelH <= viewH) {
        (worldPixelH - viewH) / 2f
    } else {
        (worldState.heroY * tilePx - viewH / 2f).coerceIn(0f, worldPixelH - viewH)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Guided Lesson Coach Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (calmMode) 1f else 0.96f),
            tonalElevation = if (calmMode) 2.dp else 4.dp,
            border = BorderStroke(1.5.dp, FireflyGoldAccent.copy(alpha = 0.55f)),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .speakOnTouch("Lesson: ${stage.title}. ${stage.instruction}")
                .testTag("intro_lesson_card")
        ) {
            Column(
                modifier = Modifier.padding(if (calmMode) 14.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = FireflyGoldAccent.copy(alpha = 0.22f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stage.badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (!calmMode) {
                            Text(
                                text = "Lv.$heroLevel · Mana $mana/$maxMana · $nectar Nectar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleAudioMute,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .speakOnTouch(if (isAudioMuted) "Audio muted button" else "Audio on button. Tap to mute.")
                                .testTag("intro_audio_toggle")
                        ) {
                            Icon(
                                imageVector = if (isAudioMuted) {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                } else {
                                    Icons.Rounded.MusicNote
                                },
                                contentDescription = stringResource(R.string.cd_toggle_audio),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(
                            onClick = onSkipIntro,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .speakOnTouch("Skip Intro button")
                                .testTag("intro_skip_button")
                        ) {
                            Text(
                                text = stringResource(R.string.intro_skip),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (!calmMode) {
                    // 3-step visual training checklist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LessonStepChip(
                            label = "1. Lantern",
                            done = introState.lanternLit,
                            active = stage == IntroStage.LESSON_MOVE_AND_LANTERN,
                            modifier = Modifier.weight(1f)
                        )
                        LessonStepChip(
                            label = "2. Shrine",
                            done = introState.shrinePuzzleSolved,
                            active = stage == IntroStage.LESSON_PUZZLE_SHRINE,
                            modifier = Modifier.weight(1f)
                        )
                        LessonStepChip(
                            label = "3. Battle",
                            done = introState.sproutlingDefeated,
                            active = stage == IntroStage.LESSON_WILD_CREATURE_BATTLE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = stage.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stage.instruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (stage == IntroStage.GRADUATION_READY) {
                    val graduateLabel = stringResource(R.string.intro_graduate_cta)
                    Button(
                        onClick = onGraduateToWorld,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FireflyGoldAccent,
                            contentColor = Color(0xFF1A2812)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .speakOnTouch("$graduateLabel button")
                            .testTag("intro_graduate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = graduateLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Playable Training Glade 2D World Viewport
        Box(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E2318))
                .onSizeChanged { canvasSize = it }
                .testTag("intro_world_viewport")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camX, camY, tilePx) {
                        detectTapGestures { offset ->
                            val worldX = (offset.x + camX) / tilePx
                            val worldY = (offset.y + camY) / tilePx
                            onSetTapTarget(worldX, worldY)
                        }
                    }
            ) {
                for (ty in 0 until IntroTutorialEngine.MAP_HEIGHT) {
                    for (tx in 0 until IntroTutorialEngine.MAP_WIDTH) {
                        val tile = IntroTutorialEngine.tileAt(tx, ty)
                        val left = tx * tilePx - camX
                        val top = ty * tilePx - camY
                        drawWorldTile(
                            tile = tile,
                            tx = tx,
                            ty = ty,
                            left = left,
                            top = top,
                            tilePx = tilePx,
                            animTime = animTime,
                            bridgeBuilt = true,
                            aqueductRestored = true
                        )
                    }
                }

                // Pulsing golden objective beacon guiding the player to the current lesson target
                IntroTutorialEngine.objectivePosition(introState)?.let { (ox, oy) ->
                    val sx = ox * tilePx - camX
                    val sy = oy * tilePx - camY
                    val pulse = 0.65f + 0.35f * sin(animTime * 4.5f)
                    drawCircle(
                        color = FireflyGoldAccent.copy(alpha = 0.22f * pulse),
                        radius = tilePx * (0.68f + 0.14f * pulse),
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = FireflyGoldAccent,
                        radius = tilePx * (0.62f + 0.10f * pulse),
                        center = Offset(sx, sy),
                        style = Stroke(width = 3.5f)
                    )
                }

                worldState.tapTarget?.let { (tx, ty) ->
                    val sx = tx * tilePx - camX
                    val sy = ty * tilePx - camY
                    val pulse = 0.7f + 0.3f * sin(animTime * 6f)
                    drawCircle(
                        color = FireflyGoldAccent.copy(alpha = 0.25f * pulse),
                        radius = tilePx * 0.36f,
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = FireflyGoldAccent,
                        radius = tilePx * 0.24f * pulse,
                        center = Offset(sx, sy),
                        style = Stroke(width = 3f)
                    )
                }

                worldState.interactables.forEach { obj ->
                    val sx = obj.x * tilePx - camX
                    val sy = obj.y * tilePx - camY
                    drawInteractableObject(
                        obj = obj,
                        center = Offset(sx, sy),
                        tilePx = tilePx,
                        animTime = animTime,
                        isLit = obj.id in worldState.litLanternIds,
                        isHarvested = false,
                        completedMissions = emptySet(),
                        isTargeted = nearestTarget.interactable?.id == obj.id
                    )
                }

                data class DepthSprite(val y: Float, val draw: () -> Unit)
                val sprites = mutableListOf<DepthSprite>()
                sprites += DepthSprite(worldState.heroY) {
                    val hx = worldState.heroX * tilePx - camX
                    val hy = worldState.heroY * tilePx - camY
                    drawWorldHeroSprite(
                        heroId = heroId,
                        center = Offset(hx, hy),
                        spriteSize = tilePx * 0.92f,
                        facing = worldState.heroFacing,
                        walkCycle = worldState.walkCycle,
                        isMoving = worldState.isMoving
                    )
                }
                worldState.creatures.forEach { actor ->
                    val cx = actor.x * tilePx - camX
                    val cy = actor.y * tilePx - camY
                    sprites += DepthSprite(actor.y) {
                        if (nearestTarget.creature?.id == actor.id) {
                            drawCircle(
                                color = if (actor.isWildEnemy) OrchidCoralAccent else FireflyGoldAccent,
                                radius = tilePx * 0.56f,
                                center = Offset(cx, cy + tilePx * 0.12f),
                                style = Stroke(width = 3.5f)
                            )
                        }
                        drawWorldCreatureSprite(
                            id = actor.creatureId ?: actor.id,
                            center = Offset(cx, cy),
                            spriteSize = tilePx * 0.88f,
                            isWildEnemy = actor.isWildEnemy,
                            isBeautiful = actor.isBeautiful,
                            walkPhase = actor.walkPhase,
                            alertActive = actor.alertActive,
                            harmonized = actor.isWildEnemy && introState.sproutlingDefeated
                        )
                    }
                }
                sprites.sortedBy { it.y }.forEach { it.draw() }
            }

            // Glade Name Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xCC0D1E16),
                border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.intro_glade_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE7F7ED),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Floating Toast
            val toast = worldState.worldToast
            androidx.compose.animation.AnimatedVisibility(
                visible = toast != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                if (toast != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xEE183F2D),
                        border = BorderStroke(1.5.dp, FireflyGoldAccent)
                    ) {
                        Text(
                            text = toast.text,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFF4CC),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Dialogue Box
            androidx.compose.animation.AnimatedVisibility(
                visible = worldState.dialogueSpeaker != null && worldState.dialogueBody != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 132.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.5.dp, FireflyGoldAccent.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .speakOnTouch("${worldState.dialogueSpeaker ?: ""}: ${worldState.dialogueBody ?: ""}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = worldState.dialogueSpeaker ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = worldState.dialogueBody ?: "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = onDismissDialogue,
                            modifier = Modifier.speakOnTouch("Close dialogue button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close dialogue"
                            )
                        }
                    }
                }
            }

            // Bottom Controls Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                VirtualAnalogStick(
                    onVectorChanged = { x, y ->
                        joyX = x
                        joyY = y
                    },
                    modifier = Modifier
                        .speakOnTouch("Movement stick. Drag in any direction to walk.")
                        .testTag("intro_virtual_joystick")
                )

                if (nearestTarget.actionType != WorldActionType.NONE) {
                    val isBattle = nearestTarget.actionType == WorldActionType.BATTLE_CREATURE
                    Button(
                        onClick = {
                            when (nearestTarget.actionType) {
                                WorldActionType.BATTLE_CREATURE -> {
                                    onStartIntroBattle(RpgMissionCatalog.introMission, true)
                                }
                                WorldActionType.INTERACT_OBJECT -> {
                                    val obj = nearestTarget.interactable
                                    if (obj != null) {
                                        when (obj.kind) {
                                            InteractableKind.SPORE_LANTERN -> onInteractLantern(obj.id, obj.title)
                                            InteractableKind.PUZZLE_PEDESTAL -> onLaunchIntroPuzzle(PuzzleType.fromId(obj.targetId))
                                            InteractableKind.MANA_FOUNTAIN -> onRestMana()
                                            InteractableKind.NPC_VILLAGER -> onShowDialogue(obj.title, obj.dialogueText)
                                            else -> Unit
                                        }
                                    } else {
                                        onShowDialogue(
                                            "Lumafern",
                                            "Yip! Light the Spore-Lantern and try the Firefly Shrine so we can explore Solari together!"
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        },
                        colors = if (isBattle) {
                            ButtonDefaults.buttonColors(containerColor = OrchidCoralAccent)
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .speakOnTouch("Action button: ${nearestTarget.label}")
                            .testTag("intro_interact_button")
                    ) {
                        Icon(
                            imageVector = when {
                                isBattle -> Icons.Rounded.Pets
                                nearestTarget.interactable?.kind == InteractableKind.PUZZLE_PEDESTAL -> Icons.Rounded.Extension
                                nearestTarget.interactable?.kind == InteractableKind.MANA_FOUNTAIN -> Icons.Rounded.Spa
                                else -> Icons.Rounded.AutoAwesome
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = nearestTarget.label,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xBB0D1E16),
                        modifier = Modifier.speakOnTouch("Follow the golden ring in the glade.")
                    ) {
                        Text(
                            text = "Follow the golden beacon ring!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFF2C2),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonStepChip(
    label: String,
    done: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        done -> FernEmeraldAccent.copy(alpha = 0.20f)
        active -> FireflyGoldAccent.copy(alpha = 0.24f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val borderColor = when {
        done -> FernEmeraldAccent
        active -> FireflyGoldAccent
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = when {
                    done -> FernEmeraldAccent
                    active -> FireflyGoldAccent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (done || active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
