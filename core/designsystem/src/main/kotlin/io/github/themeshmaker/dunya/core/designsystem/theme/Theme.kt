package io.github.themeshmaker.dunya.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.model.CustomColors
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

private val AndalusGradient = DeenGradient(
    start = Color(0xFF0A2A66),
    end = Color(0xFF2474C6),
    content = Color.White,
    pattern = Color.White.copy(alpha = 0.12f)
)

private val RoseGradient = DeenGradient(
    start = Color(0xFF5B0E2D),
    end = Color(0xFFC2456F),
    content = Color.White,
    pattern = Color.White.copy(alpha = 0.12f)
)

private val MidnightGradient = DeenGradient(
    start = Color(0xFF070B24),
    end = Color(0xFF2E2F7A),
    content = Color(0xFFF3E7C4),
    pattern = Color(0xFFE9D8A6).copy(alpha = 0.18f)
)

/** Seeds for the themes whose schemes are generated (see [seededScheme]). */
private fun ThemeStyle.seed(custom: CustomColors): Color = when (this) {
    ThemeStyle.ANDALUS -> Color(0xFF1D5FAF)
    ThemeStyle.ROSE -> Color(0xFFB23A64)
    ThemeStyle.MIDNIGHT -> Color(0xFF3F4AB8)
    else -> moreColourful(Color(custom.start), Color(custom.end))
}

/** The custom gradient, with white or near-black content depending on how light the colours are. */
fun CustomColors.gradient(): DeenGradient {
    val a = Color(start)
    val b = Color(end)
    val light = (a.luminance() + b.luminance()) / 2f > 0.55f
    val content = if (light) Color(0xFF15171A) else Color.White
    return DeenGradient(start = a, end = b, content = content, pattern = content.copy(alpha = if (light) 0.10f else 0.12f))
}

fun ThemeStyle.gradient(custom: CustomColors = CustomColors()): DeenGradient = when (this) {
    ThemeStyle.EMERALD -> EmeraldGradient
    ThemeStyle.KISWAH -> KiswahGradient
    ThemeStyle.DESERT -> DesertGradient
    ThemeStyle.ANDALUS -> AndalusGradient
    ThemeStyle.ROSE -> RoseGradient
    ThemeStyle.MIDNIGHT -> MidnightGradient
    ThemeStyle.CUSTOM -> custom.gradient()
}

/**
 * The app style (UiStyle) turned into concrete values: every screen draws its cards, pages and
 * headers from here, so Classic, Sleek, Glass and Heritage are the same screens in four looks.
 */
@Immutable
data class DeenLook(
    val uiStyle: UiStyle,
    /** Page background behind cards (always opaque; also used for sticky headers). */
    val page: Color,
    /** Glass: a soft gradient drawn over the whole page instead of [page]. */
    val pageBrush: Brush? = null,
    val card: Color,
    /** Bottom sheets and other surfaces that must never be see-through. */
    val sheetColor: Color = card,
    /** Heritage: a second, inner rule inside each card's frame. */
    val cardOrnament: Color? = null,
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
    /** Animations (screen transitions, rolling numbers, entrances); off in Classic and with reduced motion. */
    val motion: Boolean = false
) {
    /** The immersive layout (full-bleed header, floating tab bar, large titles): Sleek and Glass. */
    val sleek: Boolean get() = uiStyle.immersive
    val glass: Boolean get() = uiStyle == UiStyle.GLASS
    val heritage: Boolean get() = uiStyle == UiStyle.HERITAGE
}

private fun lookFor(
    uiStyle: UiStyle,
    colors: ColorScheme,
    gradient: DeenGradient,
    dark: Boolean,
    reducedMotion: Boolean,
    pageHeight: Float
): DeenLook = when (uiStyle) {
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

    // Glass: frosted, see-through cards over a soft wash of the theme's two colours, with a bright
    // edge along the top of each card. Sleek's layout and motion.
    UiStyle.GLASS -> {
        val base = if (dark) colors.background else Color.White
        val top = lerp(base, gradient.end, if (dark) 0.34f else 0.22f)
        val middle = lerp(base, gradient.start, if (dark) 0.24f else 0.09f)
        val bottom = lerp(base, gradient.end, if (dark) 0.18f else 0.15f)
        DeenLook(
            uiStyle = uiStyle,
            page = middle,
            pageBrush = Brush.verticalGradient(
                0f to top,
                0.55f to middle,
                1f to bottom,
                startY = 0f,
                endY = pageHeight.coerceAtLeast(1f)
            ),
            card = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.62f),
            sheetColor = if (dark) colors.surfaceContainer else lerp(Color.White, gradient.end, 0.04f),
            cardShape = SquircleShape(26.dp),
            cardBorder = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    if (dark) {
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.30f))
                    }
                )
            ),
            cardElevation = 0.dp,
            shadowColor = Color.Black,
            heroShape = SquircleShape(0.dp, 0.dp, 40.dp, 40.dp),
            sheetShape = SquircleShape(32.dp, 32.dp, 0.dp, 0.dp),
            gutter = 18.dp,
            gap = 12.dp,
            motion = !reducedMotion
        )
    }

    // Heritage: parchment pages and cards framed like a manuscript page (cut corners, a double
    // rule in gold), with Amiri headings. Classic's layout.
    UiStyle.HERITAGE -> {
        val gold = lerp(colors.primary, Color(0xFFB08D3C), 0.55f)
        val card = if (dark) Color(0xFF1D1912) else Color(0xFFFFFBF1)
        DeenLook(
            uiStyle = uiStyle,
            page = if (dark) lerp(Color(0xFF14110B), colors.primary, 0.04f) else lerp(Color(0xFFF5EEDC), colors.primary, 0.03f),
            card = card,
            sheetColor = card,
            cardOrnament = gold.copy(alpha = 0.45f),
            cardShape = CutCornerShape(10.dp),
            cardBorder = BorderStroke(1.dp, gold.copy(alpha = 0.70f)),
            cardElevation = 0.dp,
            shadowColor = Color.Black,
            heroShape = CutCornerShape(18.dp),
            sheetShape = CutCornerShape(topStart = 18.dp, topEnd = 18.dp),
            gutter = 16.dp,
            gap = 12.dp,
            motion = !reducedMotion
        )
    }
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

private fun ThemeStyle.lightScheme(custom: CustomColors): ColorScheme = when (this) {
    ThemeStyle.EMERALD -> emeraldLight
    ThemeStyle.KISWAH -> kiswahLight
    ThemeStyle.DESERT -> desertLight
    else -> seededScheme(seed(custom), dark = false)
}.withWhiteBackground()

private fun ThemeStyle.darkScheme(custom: CustomColors): ColorScheme = when (this) {
    ThemeStyle.EMERALD -> emeraldDark
    ThemeStyle.KISWAH -> kiswahDark.copy(background = Color(0xFF0B0B0B), surface = Color(0xFF0B0B0B))
    ThemeStyle.DESERT -> desertDark
    ThemeStyle.MIDNIGHT -> seededScheme(seed(custom), dark = true).copy(background = Color(0xFF070914), surface = Color(0xFF070914))
    else -> seededScheme(seed(custom), dark = true)
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
 * @param uiStyle the look (Classic, Sleek, Glass or Heritage), independent of the colours.
 * @param customColors the two colours of [ThemeStyle.CUSTOM].
 * @param dynamicColor use the wallpaper's Material You colours instead (Android 12+). Off by
 *   default: Deen keeps its own look.
 */
@Composable
fun DeenTheme(
    style: ThemeStyle = ThemeStyle.EMERALD,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    uiStyle: UiStyle = UiStyle.CLASSIC,
    customColors: CustomColors = CustomColors(),
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
        dark -> remember(style, customColors) { style.darkScheme(customColors) }
        else -> remember(style, customColors) { style.lightScheme(customColors) }
    }
    val colors = if (themeMode == ThemeMode.OLED_BLACK) base.toOledBlack() else base
    val gradient = style.gradient(customColors)
    val pageHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    val language = LocalConfiguration.current.locales[0].language

    CompositionLocalProvider(
        LocalDeenGradient provides gradient,
        LocalDeenLook provides lookFor(uiStyle, colors, gradient, dark, rememberReducedMotion(), pageHeight)
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = when (uiStyle) {
                UiStyle.SLEEK, UiStyle.GLASS -> SleekTypography
                UiStyle.HERITAGE -> HeritageTypography
                UiStyle.CLASSIC -> DeenTypography
            }.let { if (language in PhoneFontLanguages) it.withPhoneFont() else it },
            shapes = DeenShapes,
            content = content
        )
    }
}
