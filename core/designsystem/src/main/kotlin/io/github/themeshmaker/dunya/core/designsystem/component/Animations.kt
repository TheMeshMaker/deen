package io.github.themeshmaker.dunya.core.designsystem.component

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import kotlinx.coroutines.delay

/** Shrinks slightly while pressed and springs back on release, like a physical button. */
fun Modifier.pressScale(interaction: InteractionSource, enabled: Boolean = true, pressed: Float = 0.965f): Modifier = composed {
    if (!enabled) return@composed this
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, DeenMotion.bouncy(), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Remembers when a screen first appeared, so only the content that is there on arrival
 * animates in (not every row that scrolls into view later).
 */
class Entrance internal constructor(private val startedAt: Long, internal val enabled: Boolean) {
    internal fun isArrival(): Boolean = SystemClock.uptimeMillis() - startedAt < ARRIVAL_WINDOW_MS

    private companion object {
        const val ARRIVAL_WINDOW_MS = 650L
    }
}

@Composable
fun rememberEntrance(): Entrance {
    val motion = DeenTheme.look.motion
    return remember(motion) { Entrance(SystemClock.uptimeMillis(), motion) }
}

/** Fades in while rising a little, one item after another ([index] sets the delay). */
fun Modifier.rise(entrance: Entrance, index: Int, distance: Dp = 24.dp): Modifier = composed {
    val animate = remember { entrance.enabled && entrance.isArrival() }
    if (!animate) return@composed this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, MAX_STAGGERED) * STAGGER_MS)
        progress.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 210f))
    }
    val px = with(LocalDensity.current) { distance.toPx() }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * px
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
    }
}

private const val STAGGER_MS = 45L
private const val MAX_STAGGERED = 10

/**
 * Text whose digits roll to their new value, like a mechanical counter (each character
 * animates on its own, so only the digits that change move). [down] = the value is
 * decreasing (a countdown): new digits drop in from above.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    down: Boolean = false
) {
    if (!DeenTheme.look.motion) {
        Text(text, style = style, color = color, fontWeight = fontWeight, modifier = modifier)
        return
    }
    // Tabular digits keep every column the same width while it rolls.
    val tabular = style.copy(fontFeatureSettings = "tnum")
    // Numbers read left to right in Arabic too; a right-to-left Row would reverse the digits.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        RollingRow(text, tabular, modifier, color, fontWeight, down)
    }
}

@Composable
private fun RollingRow(text: String, tabular: TextStyle, modifier: Modifier, color: Color, fontWeight: FontWeight?, down: Boolean) {
    Row(modifier) {
        text.forEachIndexed { index, char ->
            // Keyed by position from the end, so "9" → "10" grows on the left like a real counter.
            val slot = text.length - index
            androidx.compose.runtime.key(slot) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val direction = if (down) -1 else 1
                        (
                            slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { h ->
                                direction * h
                            } + fadeIn(tween(180))
                            ) togetherWith
                            (
                                slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { h ->
                                    -direction * h
                                } + fadeOut(tween(140))
                                ) using
                            SizeTransform(clip = true)
                    },
                    label = "digit"
                ) { c ->
                    Text(c.toString(), style = tabular, color = color, fontWeight = fontWeight)
                }
            }
        }
    }
}

/** A slow sideways drift for the background star pattern (one cell, then seamlessly again). */
@Composable
fun rememberPatternDrift(cell: Dp, periodMs: Int = 26_000): Float {
    if (!DeenTheme.look.motion) return 0f
    val px = with(LocalDensity.current) { cell.toPx() }
    val drift by rememberInfiniteTransition(label = "pattern").animateFloat(
        initialValue = 0f,
        targetValue = px,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    return drift
}

/** 0 → 1 once when first shown (e.g. a progress ring drawing itself in). */
@Composable
fun rememberDrawIn(durationMs: Int = 1_100): Float {
    val motion = DeenTheme.look.motion
    val progress = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(motion) {
        if (motion) progress.animateTo(1f, tween(durationMs, easing = DeenMotion.EaseOutSoft)) else progress.snapTo(1f)
    }
    return progress.value
}
