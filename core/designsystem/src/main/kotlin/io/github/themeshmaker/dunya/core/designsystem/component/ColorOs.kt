package io.github.themeshmaker.dunya.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme

/*
 * The ColorOS style (UiStyle.COLOROS), matched to ColorOS 17's own apps on the phone (Settings, Clock,
 * Compass, Calculator, Weather): flat grey cards on a near-black (or light grey) page, round "pebble"
 * buttons that look slightly convex (lit from above, a bright rim along the top, a soft shadow), the
 * ColorOS blue for switches and sliders, and a floating pill-shaped tab bar. Nothing here needs blur,
 * so the style stays light on the phone.
 */

/** ColorOS 17's measured colours (dark mode from the phone; light mode its counterpart). */
object ColorOsColors {
    val blueDark = Color(0xFF428AF7)
    val blueLight = Color(0xFF2F74EB)
    val pageDark = Color(0xFF181818)
    val pageLight = Color(0xFFF2F3F5)
    val cardDark = Color(0xFF303030)
    val cardLight = Color.White
    val pebbleDark = Color(0xFF2A2B2C)
    val trackOffDark = Color(0xFF595959)
    val trackOffLight = Color(0xFFDADCE0)
    val secondaryText = Color(0xFF8C8C8C)
    val dividerDark = Color(0xFF4A4A4A)
    val dividerLight = Color(0xFFE6E7EA)

    /** The tab bar's frosted fill (laid over a blur of what scrolls beneath it). */
    val barDark = Color(0xFF222222).copy(alpha = 0.72f)
    val barLight = Color(0xFFF8F8F8).copy(alpha = 0.78f)

    /** Unselected tabs: a soft grey, not dim (ColorOS keeps every tab readable). */
    val tabIdleDark = Color(0xFFC4C4C4)
    val tabIdleLight = Color(0xFF5F6166)
}

/** ColorOS type, measured against its apps: a tab's big bold title, a pushed screen's title, a list's group label. */
object ColorOsType {
    val largeTitle = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
    val title = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val groupLabel = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
}

/** True while content sits on the theme's gradient (Today), where cards and pebbles are frosted white. */
val LocalOnGradient = staticCompositionLocalOf { false }

/**
 * A ColorOS pebble: a convex disc (or [shape]) lit from above, with a bright rim along its top
 * edge and a soft shadow, shrinking a touch while pressed. Frosted white on the gradient.
 */
fun Modifier.pebble(shape: Shape, interactionSource: MutableInteractionSource? = null, tint: Color = Color.Unspecified): Modifier =
    composed {
        val look = DeenTheme.look
        val onGradient = LocalOnGradient.current
        val dark = look.dark || onGradient
        val fill = when {
            tint != Color.Unspecified -> tint
            onGradient -> Color.White.copy(alpha = 0.18f)
            dark -> ColorOsColors.pebbleDark
            else -> Color.White
        }
        this
            .then(
                if (interactionSource != null) {
                    Modifier.pressScale(interactionSource, enabled = look.motion, pressed = 0.93f)
                } else {
                    Modifier
                }
            )
            .shadow(
                if (onGradient) 0.dp else 6.dp,
                shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black.copy(alpha = if (dark) 0.6f else 0.25f)
            )
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val rimWidth = 1.dp.toPx()
                val inset = rimWidth / 2
                val rimOutline = shape.createOutline(Size(size.width - rimWidth, size.height - rimWidth), layoutDirection, this)
                // Lit from above: a soft glow in the upper middle, a bright rim on top fading down the sides.
                val glow = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.0f), Color.Transparent),
                    center = Offset(size.width / 2, size.height * 0.18f),
                    radius = maxOf(size.width, size.height) * 0.75f
                )
                val rim = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.26f else 0.9f),
                        Color.White.copy(alpha = if (dark) 0.03f else 0.2f),
                        Color.White.copy(alpha = if (dark) 0.10f else 0.6f)
                    )
                )
                onDrawWithContent {
                    drawOutline(outline, fill)
                    drawOutline(outline, glow)
                    translate(inset, inset) { drawOutline(rimOutline, rim, style = Stroke(rimWidth)) }
                    drawContent()
                }
            }
    }

/**
 * ColorOS lists: a hairline above every row of a card but the first, from [start] (where the row's
 * text begins) to 16 dp short of the end, as in its Settings. (The first row sits at the top of its
 * column.) Nothing in the other styles.
 */
fun Modifier.listDivider(start: Dp = 16.dp): Modifier = composed {
    val look = DeenTheme.look
    if (!look.colorOs) return@composed this
    var first by remember { mutableStateOf(true) }
    val color = if (look.dark || LocalOnGradient.current) ColorOsColors.dividerDark else ColorOsColors.dividerLight
    this
        .onPlaced { first = it.positionInParent().y < 1f }
        .drawBehind {
            if (!first) {
                val end = 16.dp.toPx()
                val from = start.toPx()
                val x = if (layoutDirection == LayoutDirection.Rtl) end else from
                drawRect(color, Offset(x, 0f), Size(size.width - from - end, 0.6.dp.toPx()))
            }
        }
}

private val LocalColorOsBlur = staticCompositionLocalOf<HazeState?> { null }

/** Lets [colorOsFrost] blur what [colorOsBlurSource] draws (the screens beneath the tab bar). */
@Composable
fun ProvideColorOsBlur(content: @Composable () -> Unit) {
    val state = rememberHazeState()
    CompositionLocalProvider(LocalColorOsBlur provides state, content = content)
}

/** What ColorOS's frosted bars blur. */
fun Modifier.colorOsBlurSource(): Modifier = composed {
    val state = LocalColorOsBlur.current
    if (state == null) this else hazeSource(state)
}

/** ColorOS's frosted material: what is beneath shows through blurred, under a [tint]. Clip it first. */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.colorOsFrost(tint: Color): Modifier = composed {
    val state = LocalColorOsBlur.current ?: return@composed background(tint.copy(alpha = 1f))
    val style = remember(tint) {
        HazeBlurStyle {
            blurRadius(28.dp)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
        }
    }
    hazeBlur(input = HazeInput.Sources(state), style = style)
}

/**
 * A switch: ColorOS's (a 44×24 dp pill, blue when on, with a white thumb that glides across) in the
 * ColorOS style, Material's elsewhere. [onCheckedChange] null means the row around it handles taps.
 */
@Composable
fun DeenSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val look = DeenTheme.look
    if (!look.colorOs) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }
    val on = if (look.dark) ColorOsColors.blueDark else ColorOsColors.blueLight
    val off = if (look.dark) ColorOsColors.trackOffDark else ColorOsColors.trackOffLight
    val track by animateColorAsState(if (checked) on else off, label = "switchTrack")
    val position by animateFloatAsState(
        if (checked) 1f else 0f,
        if (look.motion) spring(dampingRatio = 0.7f, stiffness = 600f) else snap(),
        label = "switchThumb"
    )
    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier
    }
    Box(modifier.then(toggle).size(width = 44.dp, height = 24.dp), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.size(width = 44.dp, height = 24.dp)) {
            val alpha = if (enabled) 1f else 0.4f
            drawRoundRect(track.copy(alpha = track.alpha * alpha), cornerRadius = CornerRadius(size.height / 2))
            val margin = 2.5.dp.toPx()
            val radius = size.height / 2 - margin
            val x = margin + radius + (size.width - 2 * (margin + radius)) * position
            drawCircle(Color.Black.copy(alpha = 0.12f * alpha), radius + 0.6.dp.toPx(), Offset(x, size.height / 2 + 0.6.dp.toPx()))
            drawCircle(Color.White.copy(alpha = alpha), radius, Offset(x, size.height / 2))
        }
    }
}
