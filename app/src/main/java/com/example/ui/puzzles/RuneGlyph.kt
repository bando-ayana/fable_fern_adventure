package com.example.ui.puzzles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.puzzle.GroveRune
import com.example.puzzle.SpiritRune
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FernEmeraldAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.OrchidCoralAccent
import com.example.ui.theme.StarlightVioletAccent

data class RuneVisual(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

fun groveRuneVisual(rune: GroveRune): RuneVisual = when (rune) {
    GroveRune.SUN -> RuneVisual(Icons.Rounded.WbSunny, FireflyGoldAccent, "Sun")
    GroveRune.MOON -> RuneVisual(Icons.Rounded.Bedtime, StarlightVioletAccent, "Moon")
    GroveRune.FERN -> RuneVisual(Icons.Rounded.Eco, FernEmeraldAccent, "Fern")
    GroveRune.DEWDROP -> RuneVisual(Icons.Rounded.WaterDrop, DewTealAccent, "Dew")
}

fun spiritRuneVisual(rune: SpiritRune): RuneVisual = when (rune) {
    SpiritRune.SUN -> RuneVisual(Icons.Rounded.WbSunny, FireflyGoldAccent, "Sun")
    SpiritRune.MOON -> RuneVisual(Icons.Rounded.Bedtime, StarlightVioletAccent, "Moon")
    SpiritRune.FERN -> RuneVisual(Icons.Rounded.Eco, FernEmeraldAccent, "Fern")
    SpiritRune.DEW -> RuneVisual(Icons.Rounded.WaterDrop, DewTealAccent, "Dew")
    SpiritRune.ORCHID -> RuneVisual(Icons.Rounded.LocalFlorist, OrchidCoralAccent, "Orchid")
    SpiritRune.STAR -> RuneVisual(Icons.Rounded.AutoAwesome, Color(0xFFE08B3B), "Star")
}
