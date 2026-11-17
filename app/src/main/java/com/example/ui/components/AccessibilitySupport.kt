package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.FernfableAccessibilityManager

@Immutable
data class AccessibilityUiSettings(
    val calmMode: Boolean = false,
    val touchToSpeak: Boolean = false,
    val lastSpokenText: String? = null,
    val onToggleCalmMode: () -> Unit = {},
    val onToggleTouchToSpeak: () -> Unit = {},
    val onSpeakItem: (String) -> Unit = {}
)

val LocalAccessibilityUi = staticCompositionLocalOf { AccessibilityUiSettings() }
private val LocalAccessibilityProvided = staticCompositionLocalOf { false }

@Composable
fun ProvideFernfableAccessibility(
    externalManager: FernfableAccessibilityManager? = null,
    content: @Composable () -> Unit
) {
    if (LocalAccessibilityProvided.current && externalManager == null) {
        content()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember(externalManager, context.applicationContext) {
        externalManager ?: FernfableAccessibilityManager(context.applicationContext)
    }
    val calmMode by manager.calmMode.collectAsStateWithLifecycle()
    val touchToSpeak by manager.touchToSpeak.collectAsStateWithLifecycle()
    val lastSpokenText by manager.lastSpokenText.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, manager, externalManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> manager.onForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> manager.onForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (externalManager == null) {
                manager.release()
            }
        }
    }

    val uiSettings = remember(calmMode, touchToSpeak, lastSpokenText, manager) {
        AccessibilityUiSettings(
            calmMode = calmMode,
            touchToSpeak = touchToSpeak,
            lastSpokenText = lastSpokenText,
            onToggleCalmMode = manager::toggleCalmMode,
            onToggleTouchToSpeak = manager::toggleTouchToSpeak,
            onSpeakItem = manager::speakItem
        )
    }

    CompositionLocalProvider(
        LocalAccessibilityProvided provides true,
        LocalAccessibilityUi provides uiSettings
    ) {
        content()
    }
}

/**
 * Observes initial touch press on any UI element without consuming or blocking normal
 * clicks, drags, or scrolls, reading [spokenText] aloud when Touch-to-Speak is enabled.
 */
fun Modifier.speakOnTouch(spokenText: String): Modifier = composed {
    val a11y = LocalAccessibilityUi.current
    if (!a11y.touchToSpeak || spokenText.isBlank()) {
        this
    } else {
        this.pointerInput(spokenText, a11y.touchToSpeak) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press) {
                        a11y.onSpeakItem(spokenText)
                    }
                }
            }
        }
    }
}

/**
 * Always-visible, easy-to-find comfort strip giving one-tap access to:
 * 1) Calm View (simplified, low-clutter, larger-target navigation without losing features)
 * 2) Touch to Speak (reads touched items aloud for visually or cognitively impaired players)
 */
@Composable
fun ComfortAccessibilityBar(
    modifier: Modifier = Modifier
) {
    val a11y = LocalAccessibilityUi.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (a11y.calmMode) 1f else 0.94f),
            tonalElevation = if (a11y.calmMode) 2.dp else 4.dp,
            border = BorderStroke(
                width = if (a11y.calmMode || a11y.touchToSpeak) 1.5.dp else 1.dp,
                color = if (a11y.calmMode || a11y.touchToSpeak) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .testTag("comfort_accessibility_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccessibilityNew,
                    contentDescription = "Accessibility options",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .speakOnTouch("Comfort and Voice controls bar. Toggle Calm View or Touch to Speak.")
                )

                AccessibilityTogglePill(
                    label = stringResource(R.string.a11y_calm_view),
                    active = a11y.calmMode,
                    icon = Icons.Rounded.Spa,
                    spokenPrompt = stringResource(R.string.cd_a11y_calm_view),
                    onClick = a11y.onToggleCalmMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("a11y_calm_mode_toggle")
                )

                AccessibilityTogglePill(
                    label = stringResource(R.string.a11y_touch_to_speak),
                    active = a11y.touchToSpeak,
                    icon = if (a11y.touchToSpeak) Icons.Rounded.RecordVoiceOver else Icons.AutoMirrored.Rounded.VolumeOff,
                    spokenPrompt = stringResource(R.string.cd_a11y_touch_to_speak),
                    onClick = a11y.onToggleTouchToSpeak,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("a11y_tts_toggle")
                )
            }
        }

        AnimatedVisibility(visible = a11y.touchToSpeak && !a11y.lastSpokenText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .testTag("a11y_spoken_caption")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Voice: ${a11y.lastSpokenText.orEmpty()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibilityTogglePill(
    label: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    spokenPrompt: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .semantics {
                contentDescription = "$label, ${if (active) "on" else "off"}. $spokenPrompt"
            }
            .clickable(role = Role.Switch, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (active) Icons.Rounded.CheckCircle else icon,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (active) "$label: ON" else label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
