package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.rpg.InteractiveZoneEngine
import com.example.rpg.InteractiveZoneState
import com.example.rpg.WorkstationVisual
import com.example.rpg.ZoneKind
import com.example.rpg.ZoneTile
import com.example.rpg.ZoneWorkstation
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.VirtualAnalogStick
import com.example.ui.components.drawWorldCreatureSprite
import com.example.ui.components.drawWorldHeroSprite
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.StarlightVioletAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveZoneScreen(
    zoneState: InteractiveZoneState,
    heroId: String,
    onStepFrame: (Float, Float, Float) -> Unit,
    onSetTapTarget: (Float, Float) -> Unit,
    onPerformWork: (String) -> Unit,
    onConfrontCreature: () -> Unit,
    onLaunchChapterPuzzle: () -> Unit,
    onExitZone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onExitZone() }

    val a11y = LocalAccessibilityUi.current
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }
    var animTime by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val baseTilePx = with(density) { 46.dp.toPx() }

    val animatedBridgeFill by animateFloatAsState(
        targetValue = zoneState.bridgeFillProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (a11y.calmMode) 0 else 750),
        label = "zone_bridge_fill"
    )
    val animatedWaterFill by animateFloatAsState(
        targetValue = zoneState.waterFillProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (a11y.calmMode) 0 else 750),
        label = "zone_water_fill"
    )

    val nearestStation = remember(
        zoneState.heroX,
        zoneState.heroY,
        zoneState.completedStationIds,
        zoneState.zoneId
    ) {
        InteractiveZoneEngine.findNearestWorkstation(zoneState)
    }

    val nextPendingStation = remember(zoneState.completedStationIds, zoneState.zoneId) {
        zoneState.workstations.firstOrNull { it.id !in zoneState.completedStationIds }
    }

    LaunchedEffect(zoneState.zoneId, zoneState.zoneCleared) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { nowNs ->
                if (lastNs != 0L) {
                    val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0f, 0.06f)
                    if (!a11y.calmMode) animTime += dt
                    onStepFrame(joyX, joyY, dt)
                }
                lastNs = nowNs
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("interactive_zone_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header strip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (a11y.calmMode) 1f else 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onExitZone,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .speakOnTouch("Exit ${zoneState.title} and return to map")
                        .testTag("zone_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Return to map"
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .speakOnTouch("${zoneState.title}. ${zoneState.objectiveHint}")
                ) {
                    Text(
                        text = zoneState.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = zoneState.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (zoneState.zoneCleared) FernEmeraldAccent.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .speakOnTouch("Stations completed: ${zoneState.completedCount} of ${zoneState.totalStations}")
                        .testTag("zone_progress_chip")
                ) {
                    Text(
                        text = if (zoneState.zoneCleared) "Cleared!" else "${zoneState.completedCount}/${zoneState.totalStations} Work",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Objective + Live Earned Points & Loot banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .speakOnTouch("${zoneState.objectiveHint}. Earned ${zoneState.totalExpEarned} EXP, ${zoneState.totalNectarEarned} Nectar, ${zoneState.totalLootEarned.size} loot items.")
                .testTag("zone_rewards_banner")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = zoneState.objectiveHint,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Points & Loot Earned: +${zoneState.totalExpEarned} XP · +${zoneState.totalNectarEarned} Nectar · +${zoneState.totalMagicEarned} Magic · ${zoneState.totalLootEarned.size} Loot",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Playable 2D World Canvas
        Box(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
        ) {
            val viewW = canvasSize.width.toFloat().coerceAtLeast(1f)
            val viewH = canvasSize.height.toFloat().coerceAtLeast(1f)
            val worldTotalW = InteractiveZoneEngine.ZONE_WIDTH * baseTilePx
            val worldTotalH = InteractiveZoneEngine.ZONE_HEIGHT * baseTilePx
            val camX = if (worldTotalW <= viewW) {
                -(viewW - worldTotalW) * 0.5f
            } else {
                (zoneState.heroX * baseTilePx - viewW * 0.5f).coerceIn(0f, worldTotalW - viewW)
            }
            val camY = if (worldTotalH <= viewH) {
                -(viewH - worldTotalH) * 0.5f
            } else {
                (zoneState.heroY * baseTilePx - viewH * 0.5f).coerceIn(0f, worldTotalH - viewH)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(camX, camY, baseTilePx) {
                        detectTapGestures { tapOffset ->
                            val worldX = (tapOffset.x + camX) / baseTilePx
                            val worldY = (tapOffset.y + camY) / baseTilePx
                            onSetTapTarget(worldX, worldY)
                        }
                    }
                    .testTag("zone_canvas")
            ) {
                drawRect(color = Color(0xFF0E261A))

                for (ty in 0 until InteractiveZoneEngine.ZONE_HEIGHT) {
                    for (tx in 0 until InteractiveZoneEngine.ZONE_WIDTH) {
                        val left = tx * baseTilePx - camX
                        val top = ty * baseTilePx - camY
                        if (left + baseTilePx < 0 || left > size.width || top + baseTilePx < 0 || top > size.height) {
                            continue
                        }
                        val tile = InteractiveZoneEngine.tileAt(zoneState, tx, ty)
                        drawZoneTile(
                            tile = tile,
                            tx = tx,
                            ty = ty,
                            left = left,
                            top = top,
                            tilePx = baseTilePx,
                            animTime = animTime,
                            bridgeFill = animatedBridgeFill,
                            waterFill = animatedWaterFill
                        )
                    }
                }

                // Tap target ring
                zoneState.tapTarget?.let { (tx, ty) ->
                    val screenPos = Offset(tx * baseTilePx - camX, ty * baseTilePx - camY)
                    drawCircle(
                        color = FireflyGoldAccent.copy(alpha = 0.7f),
                        radius = baseTilePx * 0.24f,
                        center = screenPos,
                        style = Stroke(width = 3f)
                    )
                }

                // Workstations
                zoneState.workstations.forEach { ws ->
                    val done = ws.id in zoneState.completedStationIds
                    val isNear = nearestStation?.id == ws.id
                    val center = Offset(ws.x * baseTilePx - camX, ws.y * baseTilePx - camY)
                    drawWorkstationSprite(
                        ws = ws,
                        center = center,
                        tilePx = baseTilePx,
                        animTime = animTime,
                        completed = done,
                        highlighted = isNear
                    )
                }

                // Companion creature
                val compCenter = Offset(
                    zoneState.companionX * baseTilePx - camX,
                    zoneState.companionY * baseTilePx - camY
                )
                drawWorldCreatureSprite(
                    id = zoneState.companionCreatureId,
                    center = compCenter,
                    spriteSize = baseTilePx * 0.9f,
                    isWildEnemy = zoneState.kind == ZoneKind.COMMUNITY_MISSION,
                    isBeautiful = false,
                    walkPhase = animTime * 2.2f,
                    harmonized = zoneState.zoneCleared
                )

                // Hero sprite
                val heroCenter = Offset(
                    zoneState.heroX * baseTilePx - camX,
                    zoneState.heroY * baseTilePx - camY
                )
                drawWorldHeroSprite(
                    heroId = heroId,
                    center = heroCenter,
                    spriteSize = baseTilePx * 0.94f,
                    facing = zoneState.heroFacing,
                    walkCycle = zoneState.walkCycle,
                    isMoving = zoneState.isMoving
                )
            }

            // Toast notification strip at top of canvas
            zoneState.toastText?.let { toast ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xD9102A1C),
                    border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = toast,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFF4C7),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            if (!zoneState.zoneCleared) {
                // Virtual Analog Joystick (bottom-left)
                VirtualAnalogStick(
                    onVectorChanged = { vx, vy ->
                        joyX = vx
                        joyY = vy
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                )

                // Workstation Action Button (bottom-right)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (nearestStation != null) {
                        Button(
                            onClick = { onPerformWork(nearestStation.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireflyGoldAccent,
                                contentColor = Color(0xFF1D1B09)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .speakOnTouch("${nearestStation.actionVerb} on ${nearestStation.title}")
                                .testTag("zone_work_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nearestStation.actionVerb,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (nextPendingStation != null) {
                        Surface(
                            onClick = { onSetTapTarget(nextPendingStation.x, nextPendingStation.y) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.75f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .speakOnTouch("Walk avatar to ${nextPendingStation.title}")
                                .testTag("zone_walk_to_next_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DirectionsWalk,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Walk to ${nextPendingStation.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Workstation progress strip or Zone Cleared Summary Card
        if (zoneState.zoneCleared) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, FireflyGoldAccent),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .speakOnTouch(
                        "Area cleared! Total earned: ${zoneState.totalExpEarned} EXP, ${zoneState.totalNectarEarned} Nectar, ${zoneState.totalMagicEarned} Magic, and loot ${zoneState.totalLootEarned.joinToString { it.displayName }}."
                    )
                    .testTag("zone_clear_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = FernEmeraldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${zoneState.title} Complete!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "+${zoneState.totalExpEarned} Hero XP · +${zoneState.totalNectarEarned} Nectar · +${zoneState.totalMagicEarned} Magic" +
                            if (zoneState.completionBonusMoonstones > 0) " · +${zoneState.completionBonusMoonstones} Moonstone Shards" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (zoneState.totalLootEarned.isNotEmpty()) {
                        Text(
                            text = "Loot Gained: ${zoneState.totalLootEarned.joinToString(", ") { it.displayName }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (zoneState.kind == ZoneKind.COMMUNITY_MISSION && zoneState.mission != null) {
                            Button(
                                onClick = onConfrontCreature,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .speakOnTouch("Confront ${zoneState.companionName} in battle")
                                    .testTag("zone_confront_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confront ${zoneState.companionName}")
                            }
                        } else if (zoneState.chapterBiome != null) {
                            Button(
                                onClick = onLaunchChapterPuzzle,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .speakOnTouch("Play ${zoneState.chapterBiome.signaturePuzzle.title} puzzle")
                                    .testTag("zone_play_puzzle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play ${zoneState.chapterBiome.signaturePuzzle.title}")
                            }
                        }

                        OutlinedButton(
                            onClick = onExitZone,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .speakOnTouch("Return to overworld map")
                                .testTag("zone_return_button")
                        ) {
                            Text("Return to Map")
                        }
                    }
                }
            }
        } else {
            FlowRow(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                zoneState.workstations.forEach { ws ->
                    val done = ws.id in zoneState.completedStationIds
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (done) FernEmeraldAccent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (done) FernEmeraldAccent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .speakOnTouch("${ws.title}: ${if (done) ws.completedLabel else ws.actionVerb}")
                            .testTag("zone_station_chip_${ws.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (done) FernEmeraldAccent else FireflyGoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (done) ws.completedLabel else ws.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawZoneTile(
    tile: ZoneTile,
    tx: Int,
    ty: Int,
    left: Float,
    top: Float,
    tilePx: Float,
    animTime: Float,
    bridgeFill: Float,
    waterFill: Float
) {
    val tileSize = Size(tilePx + 1f, tilePx + 1f)
    when (tile) {
        ZoneTile.GRASS -> {
            val checker = (tx + ty) % 2 == 0
            drawRect(
                color = if (checker) Color(0xFF285C3A) else Color(0xFF235233),
                topLeft = Offset(left, top),
                size = tileSize
            )
            if ((tx * 5 + ty * 9) % 4 == 0) {
                drawCircle(
                    color = Color(0xFF428B59),
                    radius = tilePx * 0.08f,
                    center = Offset(left + tilePx * 0.35f, top + tilePx * 0.62f)
                )
            }
        }

        ZoneTile.PATH -> {
            drawRect(
                color = Color(0xFF6B5B3E),
                topLeft = Offset(left, top),
                size = tileSize
            )
            drawCircle(
                color = Color(0xFF83704E),
                radius = tilePx * 0.09f,
                center = Offset(left + tilePx * 0.35f, top + tilePx * 0.45f)
            )
        }

        ZoneTile.WOOD_DECK -> {
            drawRect(
                color = Color(0xFF225C4F),
                topLeft = Offset(left, top),
                size = tileSize
            )
            // Floating hydro-pad / timber deck with brass studs
            drawRoundRect(
                color = Color(0xFF7B502B),
                topLeft = Offset(left + tilePx * 0.06f, top + tilePx * 0.06f),
                size = Size(tilePx * 0.88f, tilePx * 0.88f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawLine(
                color = Color(0xFF9F6D3D),
                start = Offset(left + tilePx * 0.12f, top + tilePx * 0.5f),
                end = Offset(left + tilePx * 0.88f, top + tilePx * 0.5f),
                strokeWidth = 2f
            )
            drawCircle(FireflyGoldAccent.copy(alpha = 0.7f), tilePx * 0.05f, Offset(left + tilePx * 0.16f, top + tilePx * 0.16f))
            drawCircle(FireflyGoldAccent.copy(alpha = 0.7f), tilePx * 0.05f, Offset(left + tilePx * 0.84f, top + tilePx * 0.16f))
        }

        ZoneTile.BRIDGE_PLANK -> {
            drawRect(
                color = Color(0xFF195F73),
                topLeft = Offset(left, top),
                size = tileSize
            )
            if (bridgeFill <= 0.05f) {
                drawRoundRect(
                    color = FireflyGoldAccent.copy(alpha = 0.75f),
                    topLeft = Offset(left + tilePx * 0.12f, top + tilePx * 0.12f),
                    size = Size(tilePx * 0.76f, tilePx * 0.76f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(
                        width = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )
            } else {
                for (i in 0 until 4) {
                    val plankAlpha = ((bridgeFill * 4f) - i).coerceIn(0f, 1f)
                    if (plankAlpha > 0.02f) {
                        val py = top + i * (tilePx * 0.25f) + tilePx * 0.03f
                        drawRoundRect(
                            color = Color(0xFF8C5B32).copy(alpha = plankAlpha),
                            topLeft = Offset(left + tilePx * 0.08f, py),
                            size = Size(tilePx * 0.84f, (tilePx * 0.19f * plankAlpha).coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
                if (bridgeFill > 0.75f) {
                    drawLine(
                        color = FireflyGoldAccent,
                        start = Offset(left + tilePx * 0.10f, top),
                        end = Offset(left + tilePx * 0.10f, top + tilePx),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = FireflyGoldAccent,
                        start = Offset(left + tilePx * 0.90f, top),
                        end = Offset(left + tilePx * 0.90f, top + tilePx),
                        strokeWidth = 3f
                    )
                }
            }
        }

        ZoneTile.WATER -> {
            drawRect(
                color = Color(0xFF195F73),
                topLeft = Offset(left, top),
                size = tileSize
            )
            val ripple = sin(animTime * 2.4f + tx + ty) * tilePx * 0.05f
            drawLine(
                color = DewTealAccent.copy(alpha = 0.5f),
                start = Offset(left + tilePx * 0.18f, top + tilePx * 0.45f + ripple),
                end = Offset(left + tilePx * 0.82f, top + tilePx * 0.45f + ripple),
                strokeWidth = 2.5f
            )
        }

        ZoneTile.CHANNEL_WATER -> {
            // Stone sluice bed filling with flowing cyan water as waterFill -> 1f
            drawRect(
                color = Color(0xFF394740),
                topLeft = Offset(left, top),
                size = tileSize
            )
            if (waterFill > 0.04f) {
                drawRoundRect(
                    color = Color(0xFF22A0B8).copy(alpha = waterFill),
                    topLeft = Offset(left + tilePx * 0.10f, top + tilePx * 0.10f),
                    size = Size(tilePx * 0.80f, tilePx * 0.80f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                val surge = sin(animTime * 4f + tx) * tilePx * 0.04f
                drawLine(
                    color = Color(0xFFA0FAFF).copy(alpha = waterFill * 0.8f),
                    start = Offset(left + tilePx * 0.2f, top + tilePx * 0.5f + surge),
                    end = Offset(left + tilePx * 0.8f, top + tilePx * 0.5f + surge),
                    strokeWidth = 2.5f
                )
            } else {
                drawRoundRect(
                    color = Color(0xFF28312C),
                    topLeft = Offset(left + tilePx * 0.14f, top + tilePx * 0.14f),
                    size = Size(tilePx * 0.72f, tilePx * 0.72f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        ZoneTile.TREE -> {
            drawRect(Color(0xFF193E28), Offset(left, top), tileSize)
            val sway = sin(animTime * 1.4f + tx * 0.6f) * 2f
            drawCircle(
                color = Color(0xFF134C2C),
                radius = tilePx * 0.42f,
                center = Offset(left + tilePx * 0.5f + sway, top + tilePx * 0.45f)
            )
            drawCircle(
                color = Color(0xFF216D41),
                radius = tilePx * 0.28f,
                center = Offset(left + tilePx * 0.46f + sway, top + tilePx * 0.36f)
            )
        }

        ZoneTile.STONE_WALL -> {
            drawRect(Color(0xFF3E3B4A), Offset(left, top), tileSize)
            drawRoundRect(
                color = Color(0xFF565169),
                topLeft = Offset(left + tilePx * 0.1f, top + tilePx * 0.1f),
                size = Size(tilePx * 0.8f, tilePx * 0.8f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

private fun DrawScope.drawWorkstationSprite(
    ws: ZoneWorkstation,
    center: Offset,
    tilePx: Float,
    animTime: Float,
    completed: Boolean,
    highlighted: Boolean
) {
    if (highlighted) {
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
    } else if (!completed) {
        val pulse = 0.48f + 0.08f * sin(animTime * 3.5f)
        drawCircle(
            color = DewTealAccent.copy(alpha = 0.5f),
            radius = tilePx * pulse,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    val baseColor = if (completed) FernEmeraldAccent else FireflyGoldAccent
    drawCircle(
        color = Color(0x88101C16),
        radius = tilePx * 0.34f,
        center = Offset(center.x, center.y + tilePx * 0.08f)
    )

    when (ws.visual) {
        WorkstationVisual.VALVE_WHEEL -> {
            val r = tilePx * 0.28f
            val spin = if (completed) animTime * 1.5f else 0f
            drawCircle(
                color = baseColor,
                radius = r,
                center = center,
                style = Stroke(width = 5f)
            )
            for (k in 0 until 4) {
                val angle = spin + k * (PI.toFloat() / 2f)
                drawLine(
                    color = baseColor,
                    start = center,
                    end = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color = DewTealAccent, radius = r * 0.28f, center = center)
        }

        WorkstationVisual.SOLAR_MIRROR -> {
            val path = Path().apply {
                moveTo(center.x, center.y - tilePx * 0.34f)
                lineTo(center.x + tilePx * 0.28f, center.y + tilePx * 0.14f)
                lineTo(center.x - tilePx * 0.28f, center.y + tilePx * 0.14f)
                close()
            }
            drawPath(path, color = if (completed) Color(0xFFFFEE96) else Color(0xFF99C2CE))
            drawPath(path, color = baseColor, style = Stroke(width = 3f))
        }

        WorkstationVisual.SPIRIT_BRAZIER -> {
            drawRoundRect(
                color = Color(0xFF5D4635),
                topLeft = Offset(center.x - tilePx * 0.24f, center.y),
                size = Size(tilePx * 0.48f, tilePx * 0.26f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawCircle(
                color = if (completed) FireflyGoldAccent else StarlightVioletAccent,
                radius = tilePx * if (completed) 0.22f else 0.15f,
                center = Offset(center.x, center.y - tilePx * 0.10f)
            )
        }

        else -> {
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(center.x - tilePx * 0.24f, center.y - tilePx * 0.24f),
                size = Size(tilePx * 0.48f, tilePx * 0.48f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawCircle(
                color = if (completed) Color.White else DewTealAccent,
                radius = tilePx * 0.12f,
                center = center
            )
        }
    }

    if (completed) {
        drawCircle(
            color = FernEmeraldAccent,
            radius = tilePx * 0.14f,
            center = Offset(center.x + tilePx * 0.24f, center.y - tilePx * 0.24f)
        )
    }
}
