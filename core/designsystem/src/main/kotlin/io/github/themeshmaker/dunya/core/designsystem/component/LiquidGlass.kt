package io.github.themeshmaker.dunya.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.GlassTransformPivot
import dev.chrisbanes.haze.glass.GlassTransformTarget
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme

/*
 * The Liquid style (UiStyle.LIQUID): glass that bends, blurs and catches the light of what is
 * behind it, like iOS 26. The app draws a coloured backdrop under every screen ([LiquidBackdrop],
 * Haze source 0), the screens on top of it are source 1, and glass surfaces ([liquidGlass]) sample
 * the sources behind them: cards see the backdrop, the floating tab bar sees the screen as well.
 * Rendering is Haze's Glass effect (Apache-2.0); Android 12 and older get its simpler fallback.
 */

private val LocalLiquidState = staticCompositionLocalOf<HazeState?> { null }

/** Extra space tab screens leave at the bottom for the floating glass tab bar drawn over them. */
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }

/**
 * Turns on Liquid glass for [content]. Glass picks its light or dark response from the
 * configuration, so it follows Deen's own light/dark setting rather than the phone's.
 */
@Composable
fun ProvideLiquidGlass(dark: Boolean, content: @Composable () -> Unit) {
    val state = rememberHazeState()
    LiquidAppearance(dark) {
        CompositionLocalProvider(LocalLiquidState provides state, content = content)
    }
}

/** Glass inside [content] responds as on a light or a dark background (e.g. dark over Today's gradient). */
@Composable
fun LiquidAppearance(dark: Boolean, content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    val adjusted = remember(configuration, night) {
        Configuration(configuration).apply { uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night }
    }
    CompositionLocalProvider(LocalConfiguration provides adjusted, content = content)
}

/** Marks what glass may show: 0 for the backdrop, 1 for the screens drawn over it. */
fun Modifier.liquidSource(layer: Float): Modifier = composed {
    val state = LocalLiquidState.current
    if (state == null) this else hazeSource(state, zIndex = layer)
}

/**
 * Liquid glass in [shape]. [tint] colours it (e.g. the selected tab); [interactionSource] makes it
 * swell and brighten under the finger. Without Liquid turned on it is a plain translucent fill.
 */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.liquidGlass(
    shape: RoundedCornerShape,
    tint: Color = Color.Unspecified,
    interactionSource: InteractionSource? = null,
    pressScale: Float = 0.97f,
    clear: Boolean = false
): Modifier = composed {
    val state = LocalLiquidState.current
    val dark = DeenTheme.look.dark
    if (state == null) return@composed background(if (tint.isSpecified) tint else DeenTheme.look.card, shape)
    val glassTint = tint
    val style = remember(shape, glassTint, dark, clear, pressScale) {
        (if (clear) GlassStyle.clear else GlassStyle.regular).then {
            shape(shape)
            if (glassTint.isSpecified) this.tint(glassTint)
            // A brighter rim and highlight than stock, so cards read as separate panes on busy backdrops.
            specularIntensity(if (dark) 0.45f else 0.6f)
            ambientResponse(if (dark) 0.22f else 0.3f)
            pressed {
                lightingIntensity(1f)
                refractionMultiplier(1.1f)
                whitePointDelta(0.04f)
                scale(pressScale)
            }
        }
    }
    hazeGlass(
        input = HazeInput.Sources(state),
        style = style,
        interactionSource = interactionSource,
        interactionTransformTarget = GlassTransformTarget.MaterialAndContent,
        interactionTransformPivot = GlassTransformPivot.Pointer
    )
}

private val Color.isSpecified: Boolean get() = this != Color.Unspecified

/**
 * What the glass bends and blurs: a white (or near-black) page washed with the theme's two colours
 * in soft pools, under the star pattern. [vivid] fills it with the full theme gradient instead (the
 * Today screen, as on iOS); switching cross-fades.
 */
@Composable
fun LiquidBackdrop(vivid: Boolean, modifier: Modifier = Modifier) {
    val gradient = DeenTheme.gradient
    val look = DeenTheme.look
    val dark = look.dark
    val base = if (dark) MaterialTheme.colorScheme.background else Color.White
    val shown by animateFloatAsState(if (vivid) 1f else 0f, tween(if (look.motion) 520 else 0), label = "backdrop")
    Box(modifier.liquidSource(0f)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(base)
            val pool = if (dark) 0.42f else 0.30f
            fun glow(color: Color, center: Offset, radius: Float, alpha: Float) = drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius),
                radius,
                center
            )
            val w = size.width
            val h = size.height
            glow(gradient.end, Offset(w * 0.05f, h * 0.08f), w * 0.95f, pool)
            glow(gradient.start, Offset(w * 1.0f, h * 0.42f), w * 0.85f, pool * 0.75f)
            glow(lerp(gradient.end, gradient.start, 0.5f), Offset(w * 0.15f, h * 0.88f), w * 0.9f, pool * 0.65f)
        }
        if (shown > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shown }
                    .background(gradient.brush)
            )
        }
        GeometricPattern(
            color = lerp(MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.10f else 0.07f), gradient.pattern, shown),
            modifier = Modifier.fillMaxSize(),
            cell = LIQUID_PATTERN_CELL
        )
    }
}

val LIQUID_PATTERN_CELL: Dp = 48.dp
