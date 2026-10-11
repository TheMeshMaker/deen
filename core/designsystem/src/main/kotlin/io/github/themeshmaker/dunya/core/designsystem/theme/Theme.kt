package io.github.themeshmaker.dunya.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle

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

/**
 * The app style (UiStyle) turned into concrete values: every screen draws its cards, pages and
 * headers from here, so Classic and Sleek are the same screens in two looks.
 */
@Immutable
data class DeenLook(
    val uiStyle: UiStyle,
    /** Page background behind cards. */
    val page: Color,
    val card: Color,
    val cardShape: Shape,
    val cardBorder: BorderStroke?,
    val cardElevation: Dp,
    /** Colour of the card's soft shadow (Sleek tints it with the theme instead of plain grey). */
    val shadowColor: Color,
    val heroShape: Shape,
    /** Bottom sheets (verse options, tafsir). */
    val sheetShape: Shape,
    /** Horizontal page padding and the gap between cards. */
    val gutter: Dp,
    val gap: Dp,
    /** Sleek's animations (screen transitions, rolling numbers, entrances); off in Classic and with reduced motion. */
    val motion: Boolean = false
) {
    val sleek: Boolean get() = uiStyle == UiStyle.SLEEK
}

private fun lookFor(uiStyle: UiStyle, colors: ColorScheme, dark: Boolean, reducedMotion: Boolean): DeenLook = when (uiStyle) {
    UiStyle.CLASSIC -> DeenLook(
        uiStyle = uiStyle,
        page = colors.background,
        card = colors.surfaceContainerLow,
        cardShape = RoundedCornerShape(20.dp),
        cardBorder = null,
        cardElevation = 0.dp,
        shadowColor = Color.Black,
        heroShape = RoundedCornerShape(28.dp),
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        gutter = 16.dp,
        gap = 12.dp
    )

    // Sleek: white cards floating on a faintly tinted page under a wide, soft, theme-tinted shadow
    // (light); hairline-edged dark cards (dark). Continuous "squircle" corners throughout.
    UiStyle.SLEEK -> DeenLook(
        uiStyle = uiStyle,
        page = if (dark) colors.background else colors.surfaceContainerLow,
        card = if (dark) colors.surfaceContainer else Color.White,
        cardShape = SquircleShape(30.dp),
        // In the dark, a glass edge: lit along the top, fading into a hairline below.
        cardBorder = if (dark) {
            BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), colors.outlineVariant.copy(alpha = 0.30f))))
        } else {
            BorderStroke(0.5.dp, colors.outlineVariant.copy(alpha = 0.30f))
        },
        cardElevation = if (dark) 0.dp else 14.dp,
        shadowColor = lerp(colors.primary, Color.Black, 0.55f).copy(alpha = 0.55f),
        heroShape = SquircleShape(0.dp, 0.dp, 44.dp, 44.dp),
        sheetShape = SquircleShape(34.dp, 34.dp, 0.dp, 0.dp),
        gutter = 20.dp,
        gap = 14.dp,
        motion = !reducedMotion
    )
}

private val LocalDeenGradient = staticCompositionLocalOf { EmeraldGradient }
private val LocalDeenLook = staticCompositionLocalOf<DeenLook> { error("DeenTheme not set") }

/** Deen-specific theme values, alongside [MaterialTheme]. */
object DeenTheme {
    val gradient: DeenGradient
        @Composable
        @ReadOnlyComposable
        get() = LocalDeenGradient.current

    val look: DeenLook
        @Composable
        @ReadOnlyComposable
        get() = LocalDeenLook.current

    /** The green that marks the word being recited (the same in every colour theme, by request). */
    val recitation: Color
        @Composable
        @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF34D399) else Color(0xFF0B9A65)
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
 * @param uiStyle the layout style (Classic or Sleek), independent of the colours.
 * @param dynamicColor use the wallpaper's Material You colours instead (Android 12+). Off by
 *   default: Deen keeps its own look.
 */
@Composable
fun DeenTheme(
    style: ThemeStyle = ThemeStyle.EMERALD,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    uiStyle: UiStyle = UiStyle.CLASSIC,
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

    CompositionLocalProvider(
        LocalDeenGradient provides style.gradient(),
        LocalDeenLook provides lookFor(uiStyle, colors, dark, rememberReducedMotion())
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = if (uiStyle == UiStyle.SLEEK) SleekTypography else DeenTypography,
            shapes = DeenShapes,
            content = content
        )
    }
}
