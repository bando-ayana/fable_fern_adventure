package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.rpg.InteractableKind
import com.example.rpg.NearestWorldTarget
import com.example.rpg.RpgWorldEngine
import com.example.rpg.RpgWorldState
import com.example.rpg.WorldActionType
import com.example.rpg.WorldCreatureActor
import com.example.rpg.WorldInteractable
import com.example.rpg.WorldTile
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun RpgWorldViewport(
    worldState: RpgWorldState,
    heroId: String,
    heroLevel: Int,
    nectar: Int,
    communityProsperity: Int,
    completedMissions: Set<String>,
    isPaused: Boolean,
    onStepFrame: (joyX: Float, joyY: Float, deltaSec: Float) -> Unit,
    onSetTapTarget: (Float, Float) -> Unit,
    onTriggerPrimaryAction: (NearestWorldTarget) -> Unit,
    onDismissDialogue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }
    var animTime by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val tilePx = remember(density) { with(density) { 54.dp.toPx() } }

    LaunchedEffect(isPaused) {
        if (isPaused) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val deltaSec = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    animTime = (animTime + deltaSec) % 1000f
                    onStepFrame(joyX, joyY, deltaSec)
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
        worldState.harvestedIds,
        nectar,
        communityProsperity,
        completedMissions
    ) {
        RpgWorldEngine.findNearestTarget(
            state = worldState,
            nectar = nectar,
            communityProsperity = communityProsperity,
            completedMissions = completedMissions
        )
    }

    val worldPixelW = RpgWorldEngine.MAP_WIDTH * tilePx
    val worldPixelH = RpgWorldEngine.MAP_HEIGHT * tilePx
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E2318))
            .onSizeChanged { canvasSize = it }
            .testTag("rpg_world_viewport")
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
            val minTx = ((camX / tilePx).toInt() - 1).coerceAtLeast(0)
            val maxTx = (((camX + size.width) / tilePx).toInt() + 1).coerceAtMost(RpgWorldEngine.MAP_WIDTH - 1)
            val minTy = ((camY / tilePx).toInt() - 1).coerceAtLeast(0)
            val maxTy = (((camY + size.height) / tilePx).toInt() + 1).coerceAtMost(RpgWorldEngine.MAP_HEIGHT - 1)

            val bridgeBuilt = "mission_sunbridge" in completedMissions
            val aqueductRestored = "mission_aqueduct_gate" in completedMissions

            // 1. Draw Terrain Tiles
            for (ty in minTy..maxTy) {
                for (tx in minTx..maxTx) {
                    val tile = RpgWorldEngine.tileAt(tx, ty)
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
                        bridgeBuilt = bridgeBuilt,
                        aqueductRestored = aqueductRestored
                    )
                }
            }

            // 2. Tap-to-walk destination marker
            worldState.tapTarget?.let { (tx, ty) ->
                val sx = tx * tilePx - camX
                val sy = ty * tilePx - camY
                val pulse = 0.7f + 0.3f * sin(animTime * 6f)
                drawCircle(
                    color = FireflyGoldAccent.copy(alpha = 0.25f * pulse),
                    radius = tilePx * 0.38f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = FireflyGoldAccent,
                    radius = tilePx * 0.26f * pulse,
                    center = Offset(sx, sy),
                    style = Stroke(width = 3f)
                )
            }

            // 3. Draw Environment Interactables
            worldState.interactables.forEach { obj ->
                val sx = obj.x * tilePx - camX
                val sy = obj.y * tilePx - camY
                if (sx < -tilePx || sx > size.width + tilePx || sy < -tilePx || sy > size.height + tilePx) {
                    return@forEach
                }
                val isTargeted = nearestTarget.interactable?.id == obj.id
                drawInteractableObject(
                    obj = obj,
                    center = Offset(sx, sy),
                    tilePx = tilePx,
                    animTime = animTime,
                    isLit = obj.id in worldState.litLanternIds,
                    isHarvested = obj.id in worldState.harvestedIds,
                    completedMissions = completedMissions,
                    isTargeted = isTargeted
                )
            }

            // 4. Draw Creatures & Hero sorted by Y for natural depth overlap
            data class DepthRenderable(val y: Float, val draw: () -> Unit)

            val renderables = mutableListOf<DepthRenderable>()
            renderables += DepthRenderable(worldState.heroY) {
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
                if (cx >= -tilePx && cx <= size.width + tilePx && cy >= -tilePx && cy <= size.height + tilePx) {
                    renderables += DepthRenderable(actor.y) {
                        val isTargeted = nearestTarget.creature?.id == actor.id
                        val isCleared = actor.missionId != null && actor.missionId in completedMissions
                        if (isTargeted) {
                            drawCircle(
                                color = if (actor.isWildEnemy) OrchidCoralAccent.copy(alpha = 0.75f)
                                else FireflyGoldAccent.copy(alpha = 0.75f),
                                radius = tilePx * 0.56f,
                                center = Offset(cx, cy + tilePx * 0.14f),
                                style = Stroke(width = 3.5f)
                            )
                        }
                        drawWorldCreatureSprite(
                            id = actor.creatureId ?: actor.id,
                            center = Offset(cx, cy),
                            spriteSize = tilePx * if (actor.id == "wild_shadowroot") 1.15f else 0.88f,
                            isWildEnemy = actor.isWildEnemy,
                            isBeautiful = actor.isBeautiful,
                            walkPhase = actor.walkPhase,
                            alertActive = actor.alertActive,
                            harmonized = isCleared
                        )
                    }
                }
            }

            renderables.sortedBy { it.y }.forEach { it.draw() }
        }

        // Top Zone Header & Minimap Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xCC0D1E16),
                border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = FireflyGoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = worldState.activeZone.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = worldState.activeZone.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC5DFCE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Compact Live Radar Minimap
            WorldMinimap(
                worldState = worldState,
                completedMissions = completedMissions,
                modifier = Modifier.size(width = 88.dp, height = 66.dp)
            )
        }

        // Floating World Toast (e.g. "+5 Sunbeam Nectar!", "Mana Restored!", "Spore-Lantern Lit!")
        val toast = worldState.worldToast
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 66.dp)
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

        // In-World NPC / Creature Dialogue Banner
        AnimatedVisibility(
            visible = worldState.dialogueSpeaker != null && worldState.dialogueBody != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 140.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                border = BorderStroke(1.5.dp, FireflyGoldAccent.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("world_dialogue_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubble,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
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
                    IconButton(onClick = onDismissDialogue) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close dialogue"
                        )
                    }
                }
            }
        }

        // Bottom Controls Overlay: Virtual Joystick (Left) + Contextual World Action (Right)
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
                modifier = Modifier.testTag("world_virtual_joystick")
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (nearestTarget.actionType != WorldActionType.NONE) {
                    val isBattle = nearestTarget.actionType == WorldActionType.BATTLE_CREATURE
                    Button(
                        onClick = { onTriggerPrimaryAction(nearestTarget) },
                        colors = if (isBattle) {
                            ButtonDefaults.buttonColors(containerColor = OrchidCoralAccent)
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("world_interact_button")
                    ) {
                        Icon(
                            imageVector = when {
                                isBattle -> Icons.Rounded.Pets
                                nearestTarget.interactable?.kind == InteractableKind.PUZZLE_PEDESTAL -> Icons.Rounded.Extension
                                nearestTarget.interactable?.kind == InteractableKind.MANA_FOUNTAIN -> Icons.Rounded.Spa
                                nearestTarget.interactable?.kind == InteractableKind.HARVEST_NECTAR ||
                                    nearestTarget.interactable?.kind == InteractableKind.HARVEST_TONIC -> Icons.Rounded.LocalFlorist
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
                        color = Color(0xBB0D1E16)
                    ) {
                        Text(
                            text = "Drag stick or tap map to walk · Lv.$heroLevel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6EDE0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VirtualAnalogStick(
    onVectorChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val baseSize = 112.dp
    val maxRadiusPx = with(LocalDensity.current) { 36.dp.toPx() }

    Box(
        modifier = modifier
            .size(baseSize)
            .clip(CircleShape)
            .background(Color(0x880D1E16))
            .pointerInput(maxRadiusPx) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val candidate = knobOffset + dragAmount
                    val dist = hypot(candidate.x, candidate.y)
                    val clamped = if (dist > maxRadiusPx) {
                        Offset(candidate.x / dist * maxRadiusPx, candidate.y / dist * maxRadiusPx)
                    } else {
                        candidate
                    }
                    knobOffset = clamped
                    onVectorChanged(
                        (clamped.x / maxRadiusPx).coerceIn(-1f, 1f),
                        (clamped.y / maxRadiusPx).coerceIn(-1f, 1f)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = FireflyGoldAccent.copy(alpha = 0.35f),
                radius = size.minDimension * 0.44f,
                center = c,
                style = Stroke(width = 2.5f)
            )
            // Inner puck
            val puckCenter = c + knobOffset
            drawCircle(
                color = FireflyGoldAccent.copy(alpha = 0.85f),
                radius = size.minDimension * 0.21f,
                center = puckCenter
            )
            drawCircle(
                color = Color(0xFF143123),
                radius = size.minDimension * 0.11f,
                center = puckCenter
            )
        }
    }
}

@Composable
private fun WorldMinimap(
    worldState: RpgWorldState,
    completedMissions: Set<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0A1912),
        border = BorderStroke(1.dp, Color(0x66FFE7A3)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val cellW = size.width / RpgWorldEngine.MAP_WIDTH
            val cellH = size.height / RpgWorldEngine.MAP_HEIGHT
            for (ty in 0 until RpgWorldEngine.MAP_HEIGHT) {
                for (tx in 0 until RpgWorldEngine.MAP_WIDTH) {
                    val color = when (RpgWorldEngine.tileAt(tx, ty)) {
                        WorldTile.CLIFF_TREE -> Color(0xFF163826)
                        WorldTile.WATER -> Color(0xFF287D8F)
                        WorldTile.SOLAR_WALKWAY -> Color(0xFF9C7E46)
                        WorldTile.SHRINE_STONE -> Color(0xFF5D5378)
                        else -> Color(0xFF2D6B44)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(tx * cellW, ty * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
            // Wild creatures dots
            worldState.creatures.forEach { actor ->
                val cleared = actor.missionId != null && actor.missionId in completedMissions
                val dotColor = when {
                    !actor.isWildEnemy -> DewTealAccent
                    cleared -> FernEmeraldAccent
                    else -> OrchidCoralAccent
                }
                drawCircle(
                    color = dotColor,
                    radius = 2.6f,
                    center = Offset(actor.x * cellW, actor.y * cellH)
                )
            }
            // Hero dot
            drawCircle(
                color = FireflyGoldAccent,
                radius = 4f,
                center = Offset(worldState.heroX * cellW, worldState.heroY * cellH)
            )
        }
    }
}

internal fun DrawScope.drawWorldTile(
    tile: WorldTile,
    tx: Int,
    ty: Int,
    left: Float,
    top: Float,
    tilePx: Float,
    animTime: Float,
    bridgeBuilt: Boolean,
    aqueductRestored: Boolean
) {
    val tileSize = Size(tilePx + 1f, tilePx + 1f)
    when (tile) {
        WorldTile.GRASS -> {
            val checker = (tx + ty) % 2 == 0
            drawRect(
                color = if (checker) Color(0xFF275B39) else Color(0xFF245435),
                topLeft = Offset(left, top),
                size = tileSize
            )
            if ((tx * 7 + ty * 13) % 5 == 0) {
                drawCircle(
                    color = Color(0xFF3E8453),
                    radius = tilePx * 0.08f,
                    center = Offset(left + tilePx * 0.35f, top + tilePx * 0.65f)
                )
            }
        }

        WorldTile.MOSS_PATH -> {
            drawRect(
                color = Color(0xFF6B5B3E),
                topLeft = Offset(left, top),
                size = tileSize
            )
            drawCircle(
                color = Color(0xFF83704D),
                radius = tilePx * 0.10f,
                center = Offset(left + tilePx * 0.3f, top + tilePx * 0.4f)
            )
            drawCircle(
                color = Color(0xFF564830),
                radius = tilePx * 0.07f,
                center = Offset(left + tilePx * 0.7f, top + tilePx * 0.7f)
            )
        }

        WorldTile.SOLAR_WALKWAY -> {
            drawRect(
                color = Color(0xFF5B3F28),
                topLeft = Offset(left, top),
                size = tileSize
            )
            // Solari plank & brass circuit inlay
            drawRect(
                color = Color(0xFF755234),
                topLeft = Offset(left + 2f, top + 2f),
                size = Size(tilePx - 4f, tilePx * 0.44f)
            )
            drawLine(
                color = FireflyGoldAccent.copy(alpha = 0.35f),
                start = Offset(left, top + tilePx * 0.5f),
                end = Offset(left + tilePx, top + tilePx * 0.5f),
                strokeWidth = 2f
            )
        }

        WorldTile.WATER -> {
            drawRect(
                color = Color(0xFF195F73),
                topLeft = Offset(left, top),
                size = tileSize
            )
            val ripple = sin(animTime * 2.5f + tx * 0.8f + ty) * tilePx * 0.06f
            drawLine(
                color = Color(0xFF6CDDF0).copy(alpha = 0.45f),
                start = Offset(left + tilePx * 0.18f, top + tilePx * 0.42f + ripple),
                end = Offset(left + tilePx * 0.78f, top + tilePx * 0.42f + ripple),
                strokeWidth = 2.5f
            )
        }

        WorldTile.SUN_BRIDGE_SITE -> {
            drawRect(
                color = Color(0xFF195F73),
                topLeft = Offset(left, top),
                size = tileSize
            )
            val plankColor = if (bridgeBuilt) Color(0xFFB58038) else Color(0xFF6B5642)
            drawRect(
                color = plankColor,
                topLeft = Offset(left + tilePx * 0.08f, top),
                size = Size(tilePx * 0.84f, tilePx)
            )
            if (bridgeBuilt) {
                drawLine(
                    color = FireflyGoldAccent,
                    start = Offset(left + tilePx * 0.10f, top),
                    end = Offset(left + tilePx * 0.10f, top + tilePx),
                    strokeWidth = 4f
                )
                drawLine(
                    color = FireflyGoldAccent,
                    start = Offset(left + tilePx * 0.90f, top),
                    end = Offset(left + tilePx * 0.90f, top + tilePx),
                    strokeWidth = 4f
                )
            }
        }

        WorldTile.AQUEDUCT_CHANNEL -> {
            drawRect(
                color = if (aqueductRestored) Color(0xFF217A8B) else Color(0xFF3E4E46),
                topLeft = Offset(left, top),
                size = tileSize
            )
            drawRoundRect(
                color = if (aqueductRestored) Color(0xFF72F2FF).copy(alpha = 0.4f) else Color(0xFF2B3530),
                topLeft = Offset(left + tilePx * 0.15f, top + tilePx * 0.15f),
                size = Size(tilePx * 0.70f, tilePx * 0.70f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }

        WorldTile.SHRINE_STONE -> {
            drawRect(
                color = Color(0xFF3D3654),
                topLeft = Offset(left, top),
                size = tileSize
            )
            drawRect(
                color = StarlightVioletAccent.copy(alpha = 0.25f),
                topLeft = Offset(left + 3f, top + 3f),
                size = Size(tilePx - 6f, tilePx - 6f),
                style = Stroke(width = 1.5f)
            )
        }

        WorldTile.CLIFF_TREE -> {
            drawRect(
                color = Color(0xFF1A4029),
                topLeft = Offset(left, top),
                size = tileSize
            )
            val sway = sin(animTime * 1.6f + tx * 0.5f) * 2.5f
            // Tree shadow & trunk
            drawCircle(
                color = Color(0x4406110B),
                radius = tilePx * 0.42f,
                center = Offset(left + tilePx * 0.5f, top + tilePx * 0.60f)
            )
            drawRoundRect(
                color = Color(0xFF4A2E1B),
                topLeft = Offset(left + tilePx * 0.40f, top + tilePx * 0.45f),
                size = Size(tilePx * 0.20f, tilePx * 0.42f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Layered emerald canopy
            drawCircle(
                color = Color(0xFF144E2E),
                radius = tilePx * 0.42f,
                center = Offset(left + tilePx * 0.5f + sway, top + tilePx * 0.40f)
            )
            drawCircle(
                color = Color(0xFF217042),
                radius = tilePx * 0.30f,
                center = Offset(left + tilePx * 0.46f + sway, top + tilePx * 0.34f)
            )
        }
    }
}

internal fun DrawScope.drawInteractableObject(
    obj: WorldInteractable,
    center: Offset,
    tilePx: Float,
    animTime: Float,
    isLit: Boolean,
    isHarvested: Boolean,
    completedMissions: Set<String>,
    isTargeted: Boolean
) {
    if (isTargeted) {
        drawCircle(
            color = FireflyGoldAccent.copy(alpha = 0.28f),
            radius = tilePx * 0.58f,
            center = center
        )
        drawCircle(
            color = FireflyGoldAccent,
            radius = tilePx * 0.58f,
            center = center,
            style = Stroke(width = 3f)
        )
    }

    when (obj.kind) {
        InteractableKind.MANA_FOUNTAIN -> {
            drawCircle(Color(0xFF294C58), radius = tilePx * 0.42f, center = center)
            drawCircle(DewTealAccent, radius = tilePx * 0.30f, center = center)
            val shimmer = 0.6f + 0.4f * sin(animTime * 4f)
            drawCircle(Color(0xFFB4FFFF).copy(alpha = shimmer), radius = tilePx * 0.14f, center = center)
        }

        InteractableKind.MAGITECH_ANVIL -> {
            drawRoundRect(
                color = Color(0xFF3E352B),
                topLeft = Offset(center.x - tilePx * 0.30f, center.y - tilePx * 0.20f),
                size = Size(tilePx * 0.60f, tilePx * 0.42f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawCircle(FireflyGoldAccent, radius = tilePx * 0.13f, center = center)
        }

        InteractableKind.HERO_CAMPSTONE -> {
            drawCircle(Color(0xFF2C4D3B), radius = tilePx * 0.36f, center = center)
            drawCircle(FireflyGoldAccent, radius = tilePx * 0.20f, center = center, style = Stroke(width = 3.5f))
        }

        InteractableKind.NPC_VILLAGER -> {
            // Friendly Solari citizen sprite
            drawOval(Color(0x44000000), Offset(center.x - tilePx * 0.22f, center.y + tilePx * 0.20f), Size(tilePx * 0.44f, tilePx * 0.16f))
            drawRoundRect(
                color = Color(0xFF1F6B56),
                topLeft = Offset(center.x - tilePx * 0.16f, center.y - tilePx * 0.04f),
                size = Size(tilePx * 0.32f, tilePx * 0.30f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawCircle(Color(0xFF4F2B19), radius = tilePx * 0.15f, center = Offset(center.x, center.y - tilePx * 0.18f))
            drawCircle(FireflyGoldAccent, radius = tilePx * 0.07f, center = Offset(center.x + tilePx * 0.22f, center.y - tilePx * 0.34f))
        }

        InteractableKind.SPORE_LANTERN -> {
            if (isLit) {
                val glowR = tilePx * (0.90f + 0.10f * sin(animTime * 3.5f))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(FireflyGoldAccent.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = glowR
                    ),
                    radius = glowR,
                    center = center
                )
            }
            drawRoundRect(
                color = Color(0xFF5D432C),
                topLeft = Offset(center.x - tilePx * 0.07f, center.y - tilePx * 0.08f),
                size = Size(tilePx * 0.14f, tilePx * 0.34f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawCircle(
                color = if (isLit) Color(0xFFFFEA8A) else Color(0xFF79867B),
                radius = tilePx * 0.18f,
                center = Offset(center.x, center.y - tilePx * 0.14f)
            )
        }

        InteractableKind.HARVEST_NECTAR, InteractableKind.HARVEST_TONIC -> {
            drawCircle(Color(0xFF174F2F), radius = tilePx * 0.34f, center = center)
            drawCircle(Color(0xFF287A47), radius = tilePx * 0.26f, center = Offset(center.x - 4f, center.y - 4f))
            if (!isHarvested) {
                val berryColor = if (obj.kind == InteractableKind.HARVEST_NECTAR) FireflyGoldAccent else OrchidCoralAccent
                drawCircle(berryColor, radius = tilePx * 0.08f, center = Offset(center.x - 8f, center.y - 5f))
                drawCircle(berryColor, radius = tilePx * 0.08f, center = Offset(center.x + 7f, center.y + 4f))
                drawCircle(berryColor, radius = tilePx * 0.08f, center = Offset(center.x + 4f, center.y - 8f))
            }
        }

        InteractableKind.PUZZLE_PEDESTAL -> {
            drawRoundRect(
                color = Color(0xFF39325A),
                topLeft = Offset(center.x - tilePx * 0.24f, center.y - tilePx * 0.24f),
                size = Size(tilePx * 0.48f, tilePx * 0.48f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            val pulse = 0.55f + 0.45f * sin(animTime * 3.2f)
            drawCircle(StarlightVioletAccent.copy(alpha = pulse), radius = tilePx * 0.16f, center = center)
            drawCircle(FireflyGoldAccent, radius = tilePx * 0.07f, center = center)
        }

        InteractableKind.CHAPTER_BEACON -> {
            drawCircle(FernEmeraldAccent.copy(alpha = 0.30f), radius = tilePx * 0.45f, center = center)
            drawRoundRect(
                color = FireflyGoldAccent,
                topLeft = Offset(center.x - tilePx * 0.16f, center.y - tilePx * 0.30f),
                size = Size(tilePx * 0.32f, tilePx * 0.56f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawCircle(Color.White, radius = tilePx * 0.09f, center = Offset(center.x, center.y - tilePx * 0.08f))
        }

        InteractableKind.COMMUNITY_LANDMARK -> {
            val built = obj.targetId in completedMissions
            drawRoundRect(
                color = if (built) FireflyGoldAccent.copy(alpha = 0.85f) else Color(0xFF6B5946),
                topLeft = Offset(center.x - tilePx * 0.32f, center.y - tilePx * 0.26f),
                size = Size(tilePx * 0.64f, tilePx * 0.52f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            if (built) {
                drawCircle(DewTealAccent, radius = tilePx * 0.16f, center = center)
            }
        }

        InteractableKind.FRIENDLY_CREATURE -> Unit
    }
}
