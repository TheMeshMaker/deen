package io.github.themeshmaker.dunya.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle

/** The two-colour gradient each theme uses for its hero surfaces, plus what goes on top of it. */
@Immutable
data class DeenGradient(val start: Color, val end: Color, val content: Color, val pattern: Color) {
    val brush: Brush = Brush.linearGradient(listOf(start, end))
}

private val EmeraldGradient = DeenGradient(
    start = Color(0xFF064E3B),
    end = Color(0xFF0E8A62),
    content = Color.White,
    pattern = Color.White.copy(alpha = 0.10f)
)

private val KiswahGradient = DeenGradient(
    start = Color(0xFF0D0D0D),
    end = Color(0xFF4A3812),
    content = Color(0xFFF6E7B8),
    pattern = Color(0xFFD4AF37).copy(alpha = 0.22f)
)

private val DesertGradient = DeenGradient(
    start = Color(0xFF7C2D12),
    end = Color(0xFFC2622D),
    content = Color.White,
    pattern = Color.White.copy(alpha = 0.12f)
)

fun ThemeStyle.gradient(): DeenGradient = when (this) {
    ThemeStyle.EMERALD -> EmeraldGradient
    ThemeStyle.KISWAH -> KiswahGradient
    ThemeStyle.DESERT -> DesertGradient
}

private val LocalDeenGradient = staticCompositionLocalOf { EmeraldGradient }

/** Deen-specific theme values, alongside [MaterialTheme]. */
object DeenTheme {
    val gradient: DeenGradient
        @Composable
        @ReadOnlyComposable
        get() = LocalDeenGradient.current
}

private fun ThemeStyle.lightScheme(): ColorScheme = when (this) {
    ThemeStyle.EMERALD -> emeraldLight
    ThemeStyle.KISWAH -> kiswahLight
    ThemeStyle.DESERT -> desertLight
}.withWhiteBackground()

private fun ThemeStyle.darkScheme(): ColorScheme = when (this) {
    ThemeStyle.EMERALD -> emeraldDark
    ThemeStyle.KISWAH -> kiswahDark.copy(background = Color(0xFF0B0B0B), surface = Color(0xFF0B0B0B))
    ThemeStyle.DESERT -> desertDark
}

/** Clean white pages; cards keep the theme's faint tint (surfaceContainerLow) so they still stand out. */
private fun ColorScheme.withWhiteBackground(): ColorScheme = copy(
    background = Color.White,
    surface = Color.White,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White
)

/**
 * OLED Black: pure black backgrounds. Containers stay near-black so cards are still
 * distinguishable.
 */
private fun ColorScheme.toOledBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF151515),
    surfaceContainerHighest = Color(0xFF1C1C1C),
    surfaceBright = Color(0xFF242424)
)

/**
 * @param style one of the (free) colour themes; Emerald green-and-white by default.
 * @param dynamicColor use the wallpaper's Material You colours instead (Android 12+). Off by
 *   default: Deen keeps its own look.
 */
@Composable
fun DeenTheme(
    style: ThemeStyle = ThemeStyle.EMERALD,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED_BLACK -> true
    }
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        useDynamic && dark -> dynamicDarkColorScheme(LocalContext.current)
        useDynamic -> dynamicLightColorScheme(LocalContext.current)
        dark -> style.darkScheme()
        else -> style.lightScheme()
    }
    val colors = if (themeMode == ThemeMode.OLED_BLACK) base.toOledBlack() else base

    CompositionLocalProvider(LocalDeenGradient provides style.gradient()) {
        MaterialTheme(
            colorScheme = colors,
            typography = DeenTypography,
            shapes = DeenShapes,
            content = content
        )
    }
}
