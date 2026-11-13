package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkJungleColorScheme = darkColorScheme(
    primary = DarkBiolumMint,
    onPrimary = OnDarkBiolumMint,
    primaryContainer = DarkMossContainer,
    onPrimaryContainer = OnDarkMossContainer,
    secondary = DarkLagoonGlow,
    onSecondary = OnDarkLagoonGlow,
    secondaryContainer = DarkLagoonContainer,
    onSecondaryContainer = OnDarkLagoonContainer,
    tertiary = DarkFireflyGold,
    onTertiary = OnDarkFireflyGold,
    tertiaryContainer = DarkFireflyContainer,
    onTertiaryContainer = OnDarkFireflyContainer,
    background = CanopyNightBackground,
    onBackground = OnCanopyNight,
    surface = CanopyNightSurface,
    onSurface = OnCanopyNight,
    surfaceVariant = CanopyNightSurfaceVariant,
    onSurfaceVariant = OnCanopyNightVariant,
    outline = CanopyNightOutline,
    outlineVariant = CanopyNightOutlineVariant
)

private val LightJungleColorScheme = lightColorScheme(
    primary = MossPrimary,
    onPrimary = OnMossPrimary,
    primaryContainer = MossContainer,
    onPrimaryContainer = OnMossContainer,
    secondary = LagoonSecondary,
    onSecondary = OnLagoonSecondary,
    secondaryContainer = LagoonContainer,
    onSecondaryContainer = OnLagoonContainer,
    tertiary = FireflyTertiary,
    onTertiary = OnFireflyTertiary,
    tertiaryContainer = FireflyContainer,
    onTertiaryContainer = OnFireflyContainer,
    background = ParchmentBackground,
    onBackground = OnParchmentBackground,
    surface = ParchmentSurface,
    onSurface = OnParchmentBackground,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = OnParchmentSurfaceVariant,
    outline = MossOutline,
    outlineVariant = MossOutlineVariant
)

val FernfableShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FernfableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkJungleColorScheme else LightJungleColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FernfableTypography,
        shapes = FernfableShapes,
        content = content
    )
}
