package io.github.themeshmaker.dunya.core.designsystem.component

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.GlassTransformPivot
import dev.chrisbanes.haze.glass.GlassTransformTarget
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import kotlin.math.roundToInt

/*
 * The Liquid style (UiStyle.LIQUID): glass that bends, blurs and catches the light of what is
 * behind it, and behaves like iOS 26/27's Liquid Glass:
 * - the app draws a coloured backdrop under every screen ([LiquidBackdrop], Haze source 0), the
 *   screens on top of it are source 1, and glass surfaces ([liquidGlass]) sample what is behind them:
 *   cards see the backdrop, the floating tab bar sees the screen as well;
 * - the light on the glass follows the phone: tilt it and the highlights slide, then settle
 *   ([LiquidLight], from the gravity sensor);
 * - touched glass swells toward the finger and lights up from the touch point, then springs back
 *   with a little give, like a drop of liquid;
 * - content scrolling under the top edge melts into a progressive blur ([LiquidScrollEdge]).
 * Rendering is Haze's Glass and Blur (Apache-2.0); Android 12 and older get Haze's simpler fallback.
 * With animations turned off in Android, the light stays still and nothing swells.
 */

private val LocalLiquidState = staticCompositionLocalOf<HazeState?> { null }
private val LocalLiquidLight = staticCompositionLocalOf<LiquidLight?> { null }

/** Extra space tab screens leave at the bottom for the floating glass tab bar drawn over them. */
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }

/**
 * Turns on Liquid glass for [content]. Glass picks its light or dark response from the
 * configuration, so it follows Deen's own light/dark setting rather than the phone's.
 */
@Composable
fun ProvideLiquidGlass(dark: Boolean, content: @Composable () -> Unit) {
    val state = rememberHazeState()
    val light = rememberLiquidLight(DeenTheme.look.motion)
    LiquidAppearance(dark) {
        CompositionLocalProvider(LocalLiquidState provides state, LocalLiquidLight provides light, content = content)
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
 * Liquid glass in [shape]. [tint] colours it (e.g. the selected tab); with [interactionSource] it
 * swells by [pressScale] toward the finger, brightens from the touch point and springs back on
 * release. [clear] lets more of the background through (controls over busy content, the tab lens).
 * Without Liquid turned on it is a plain translucent fill.
 */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.liquidGlass(
    shape: RoundedCornerShape,
    tint: Color = Color.Unspecified,
    interactionSource: InteractionSource? = null,
    pressScale: Float = PRESS_CARD,
    clear: Boolean = false
): Modifier = composed {
    val state = LocalLiquidState.current
    val light = LocalLiquidLight.current
    val look = DeenTheme.look
    val dark = look.dark
    if (state == null) return@composed background(if (tint.isSpecified) tint else look.card, shape)
    val glassTint = tint
    val swell = if (look.motion) pressScale else 1f
    val style = remember(shape, glassTint, dark, clear, swell, light) {
        (if (clear) GlassStyle.clear else GlassStyle.regular).then {
            shape(shape)
            if (glassTint.isSpecified) this.tint(glassTint)
            // A wide band of bending around the edges, as on iOS (Haze's stock band is narrow): what
            // is behind the glass visibly flows around its rim. Clear glass (the tab lens) bends most
            // and barely blurs. (Strength, band as a fraction of the shape, displacement, depth, blur.)
            if (clear) optics(0.95f, 0.5f, 24.dp, 1f, 3.dp) else optics(0.82f, 0.4f, 20.dp, 1f, 14.dp)
            // A brighter rim and highlight than stock, so panes read as separate on busy backdrops.
            specularIntensity(if (dark) 0.5f else 0.62f)
            ambientResponse(if (dark) 0.24f else 0.3f)
            if (light != null) lightPosition(light.alignment)
            interactionLightRadiusFraction(0.85f)
            interactionPositionAnimationSpec(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium))
            // (Haze's own press transform can only shrink glass; the iOS swell is [pressSwell] below.)
            pressed {
                lightingIntensity(1f)
                refractionMultiplier(1.18f)
                whitePointDelta(0.05f)
            }
        }
    }
    (if (interactionSource != null && swell > 1f) pressSwell(interactionSource, swell) else this).hazeGlass(
        input = HazeInput.Sources(state),
        style = style,
        // Cards are many and large: cheaper glass for them; bars and controls keep the balanced quality.
        performanceMode = if (pressScale == PRESS_CARD) HazePerformanceMode.Performance else HazePerformanceMode.Balanced,
        interactionSource = interactionSource,
        interactionTransformTarget = GlassTransformTarget.MaterialAndContent,
        interactionTransformPivot = GlassTransformPivot.Pointer
    )
}

/**
 * Pressed glass swells toward the finger (quickly, without overshoot) and springs back with a little
 * give when let go, as iOS's Liquid Glass does.
 */
private fun Modifier.pressSwell(interactionSource: InteractionSource, to: Float): Modifier = composed {
    var pressedAt by remember { mutableStateOf<Offset?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressedAt = interaction.pressPosition
                is PressInteraction.Release, is PressInteraction.Cancel -> pressedAt = null
            }
        }
    }
    val pressed = pressedAt != null
    val scale by animateFloatAsState(
        if (pressed) to else 1f,
        if (pressed) spring(dampingRatio = 0.8f, stiffness = 520f) else spring(dampingRatio = 0.5f, stiffness = 280f),
        label = "swell"
    )
    // Remember where the finger was, so the glass shrinks back toward the same point.
    var origin by remember { mutableStateOf(TransformOrigin.Center) }
    pressedAt?.let { at ->
        if (size.width > 0 && size.height > 0) {
            origin = TransformOrigin((at.x / size.width).coerceIn(0f, 1f), (at.y / size.height).coerceIn(0f, 1f))
        }
    }
    onSizeChanged { size = it }.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = origin
    }
}

/** How much touched glass swells: a little for cards, more for small round controls (as on iOS). */
const val PRESS_CARD = 1.025f
const val PRESS_CONTROL = 1.12f

private val Color.isSpecified: Boolean get() = this != Color.Unspecified

/**
 * The scroll-edge effect: under the status bar (or a top bar), content scrolling past melts into a
 * blur that fades out downwards, tinted with [tint] so the system icons stay readable.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun LiquidScrollEdge(tint: Color, modifier: Modifier = Modifier) {
    val state = LocalLiquidState.current ?: return
    val style = remember(tint) {
        HazeBlurStyle {
            blurRadius(18.dp)
            progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
            colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha = 0.35f))))
        }
    }
    Box(modifier.hazeBlur(input = HazeInput.Sources(state), style = style))
}

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

// ---------------------------------------------------------------------------------------------- Light

/**
 * Where the light on the glass comes from, as a fraction of each surface: from above by default,
 * sliding as the phone tilts (how far it has moved from the way it was being held), then easing
 * back, as the highlights on iOS's Liquid Glass do.
 */
@Stable
internal class LiquidLight {
    private val position = mutableStateOf(REST)
    private var reference: Offset? = null

    /** Read while drawing, so the glass redraws as the light moves without rebuilding its style. */
    val alignment: Alignment = LightAlignment(position)

    fun onGravity(gravity: Offset) {
        if (!gravity.x.isFinite() || !gravity.y.isFinite()) return
        val held = reference?.let { it + (gravity - it) * SETTLE } ?: gravity
        reference = held
        val tilt = gravity - held
        val target = Offset(
            (REST.x + tilt.x * GAIN).coerceIn(REST.x - REACH, REST.x + REACH),
            (REST.y - tilt.y * GAIN).coerceIn(REST.y - REACH, REST.y + REACH)
        )
        val current = position.value
        val next = current + (target - current) * SMOOTHING
        // Every move redraws all the glass on screen, so the hand's tiny tremors are ignored.
        if ((next - current).getDistance() > MIN_MOVE) position.value = next
    }

    fun reset() {
        reference = null
        position.value = REST
    }

    private class LightAlignment(private val light: State<Offset>) : Alignment {
        override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
            val p = light.value
            return IntOffset((space.width * p.x).roundToInt(), (space.height * p.y).roundToInt())
        }
    }

    private companion object {
        val REST = Offset(0.5f, 0.22f)
        const val REACH = 0.3f
        const val GAIN = 0.12f
        const val SMOOTHING = 0.25f

        /** How fast "the way it is held" catches up with the phone (per reading, ~15 a second). */
        const val SETTLE = 0.06f

        /** The smallest move worth redrawing the glass for. */
        const val MIN_MOVE = 0.006f
    }
}

/** A [LiquidLight] fed by the gravity sensor while the app is in front (none with animations off). */
@Composable
private fun rememberLiquidLight(motion: Boolean): LiquidLight? {
    val context = LocalContext.current
    val light = remember { LiquidLight() }
    if (!motion) return null
    LifecycleResumeEffect(light, context) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size >= 2) light.onGravity(rotated(Offset(event.values[0], event.values[1]), context))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // About 15 readings a second: enough for the light to glide, and far less redrawing than 50.
        val registered = sensor != null && manager.registerListener(listener, sensor, SENSOR_PERIOD_US)
        onPauseOrDispose {
            if (registered) manager.unregisterListener(listener)
            light.reset()
        }
    }
    return light
}

private const val SENSOR_PERIOD_US = 66_000

/** Gravity in screen terms, whichever way the phone is turned. */
private fun rotated(gravity: Offset, context: Context): Offset = when (displayRotation(context)) {
    Surface.ROTATION_90 -> Offset(-gravity.y, gravity.x)
    Surface.ROTATION_180 -> -gravity
    Surface.ROTATION_270 -> Offset(gravity.y, -gravity.x)
    else -> gravity
}

@Suppress("DEPRECATION")
private fun displayRotation(context: Context): Int = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display.rotation
    } else {
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation
    }
}.getOrNull() ?: Surface.ROTATION_0
