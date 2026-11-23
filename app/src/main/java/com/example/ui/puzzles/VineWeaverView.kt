package com.example.ui.puzzles

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.puzzle.PipeDir
import com.example.puzzle.VineTile
import com.example.puzzle.VineWeaverBoard
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent

@Composable
fun VineWeaverView(
    board: VineWeaverBoard,
    onRotateTile: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val a11y = LocalAccessibilityUi.current
    val waterShimmer = if (a11y.calmMode) {
        0.85f
    } else {
        val flowTransition = rememberInfiniteTransition(label = "vine_water_shimmer")
        val animated by flowTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(950, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vine_shimmer_alpha"
        )
        animated
    }
    val bloomScale by animateFloatAsState(
        targetValue = if (board.moonflowerBloomed) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "moonflower_bloom_scale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DewTealAccent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, DewTealAccent.copy(alpha = 0.5f)),
                modifier = Modifier.speakOnTouch("Spring water enters at top-left tile")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = DewTealAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Spring enters top-left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (board.moonflowerBloomed) FireflyGoldAccent.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (board.moonflowerBloomed) FireflyGoldAccent else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .scale(bloomScale)
                    .speakOnTouch(
                        if (board.moonflowerBloomed) "Moonflower at bottom-right is watered and blooming"
                        else "Connect water to Moonflower at bottom-right"
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFlorist,
                        contentDescription = null,
                        tint = if (board.moonflowerBloomed) FireflyGoldAccent else OrchidCoralAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Moonflower exits bottom-right",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (r in 0 until board.size) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (c in 0 until board.size) {
                            val tile = board.tiles[r * board.size + c]
                            val isStart = r == 0 && c == 0
                            val isEnd = r == board.size - 1 && c == board.size - 1
                            VineTileCell(
                                tile = tile,
                                isStart = isStart,
                                isEnd = isEnd,
                                waterShimmer = waterShimmer,
                                onClick = { onRotateTile(r, c) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .speakOnTouch(
                                        "Row ${r + 1} Column ${c + 1}, ${if (tile.isPowered) "watered" else "dry"} vine tile, tap to rotate"
                                    )
                                    .testTag("vine_tile_${r}_${c}")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tap any tile to rotate it 90° clockwise.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VineTileCell(
    tile: VineTile,
    isStart: Boolean,
    isEnd: Boolean,
    waterShimmer: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVineColor = DewTealAccent
    val dryVineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
    val leafHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val startGlow = DewTealAccent
    val endGlow = FireflyGoldAccent

    var cumulativeSteps by remember(tile.row, tile.col, tile.shape) { mutableIntStateOf(tile.rotation) }
    var lastModRotation by remember(tile.row, tile.col, tile.shape) { mutableIntStateOf(tile.rotation) }

    LaunchedEffect(tile.rotation) {
        if (tile.rotation != lastModRotation) {
            val delta = (tile.rotation - lastModRotation).mod(4)
            cumulativeSteps += delta
            lastModRotation = tile.rotation
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = cumulativeSteps * 90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "vine_rot_${tile.row}_${tile.col}"
    )

    val tileSurfaceColor by animateColorAsState(
        targetValue = if (tile.isPowered) DewTealAccent.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(240),
        label = "vine_bg_${tile.row}_${tile.col}"
    )
    val vineChannelColor by animateColorAsState(
        targetValue = if (tile.isPowered) activeVineColor else dryVineColor,
        animationSpec = tween(240),
        label = "vine_channel_${tile.row}_${tile.col}"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tileSurfaceColor,
        border = BorderStroke(
            width = if (isStart || isEnd) 2.dp else 1.dp,
            color = when {
                isStart -> startGlow
                isEnd -> endGlow
                tile.isPowered -> DewTealAccent.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val center = Offset(cx, cy)
                val strokeW = size.minDimension * 0.22f
                val coreW = size.minDimension * 0.10f

                rotate(degrees = animatedRotation, pivot = center) {
                    drawCircle(
                        color = vineChannelColor,
                        radius = strokeW * 0.65f,
                        center = center
                    )

                    tile.shape.baseConnections.forEach { dir ->
                        val end = when (dir) {
                            PipeDir.NORTH -> Offset(cx, 0f)
                            PipeDir.EAST -> Offset(size.width, cy)
                            PipeDir.SOUTH -> Offset(cx, size.height)
                            PipeDir.WEST -> Offset(0f, cy)
                        }
                        drawLine(
                            color = vineChannelColor,
                            start = center,
                            end = end,
                            strokeWidth = strokeW,
                            cap = StrokeCap.Butt
                        )
                        drawLine(
                            color = if (tile.isPowered) Color(0xFFB8F7FF).copy(alpha = waterShimmer) else leafHighlight,
                            start = center,
                            end = end,
                            strokeWidth = coreW,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Entry/Exit marker pips remain anchored at left/right edges
                if (isStart) {
                    drawCircle(
                        color = startGlow,
                        radius = size.minDimension * 0.10f,
                        center = Offset(size.minDimension * 0.12f, cy)
                    )
                }
                if (isEnd) {
                    drawCircle(
                        color = endGlow,
                        radius = size.minDimension * 0.10f,
                        center = Offset(size.width - size.minDimension * 0.12f, cy)
                    )
                }
            }
        }
    }
}
