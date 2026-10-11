package io.github.themeshmaker.dunya.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal val DeenShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * A rectangle with continuous ("squircle") corners: each corner eases out of the straight edge
 * instead of meeting it with a circular arc, so it never shows the small kink a plain rounded
 * corner has. [smoothing] 0 is an ordinary rounded rectangle; 0.6 is close to Apple's corners.
 * Follows Figma's corner-smoothing construction (two cubics around a shortened arc).
 */
@Immutable
class SquircleShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val smoothing: Float = 0.6f
) : Shape {
    constructor(radius: Dp, smoothing: Float = 0.6f) : this(radius, radius, radius, radius, smoothing)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val budget = min(size.width, size.height) / 2f
        fun corner(radius: Dp) = cornerParams(with(density) { radius.toPx() }, smoothing, budget)
        val topLeft = corner(if (rtl) topEnd else topStart)
        val topRight = corner(if (rtl) topStart else topEnd)
        val bottomRight = corner(if (rtl) bottomStart else bottomEnd)
        val bottomLeft = corner(if (rtl) bottomEnd else bottomStart)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            corner(0f, 0f, 0f, -1f, 1f, 0f, topLeft, first = true)
            corner(w, 0f, 1f, 0f, 0f, 1f, topRight)
            corner(w, h, 0f, 1f, -1f, 0f, bottomRight)
            corner(0f, h, -1f, 0f, 0f, -1f, bottomLeft)
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is SquircleShape &&
        topStart == other.topStart && topEnd == other.topEnd && bottomEnd == other.bottomEnd &&
        bottomStart == other.bottomStart && smoothing == other.smoothing

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return 31 * result + smoothing.hashCode()
    }
}

/** One corner's measurements: the curve starts [p] from the corner; a, b, c, d place the cubics. */
private class CornerParams(val radius: Float, val p: Float, val a: Float, val b: Float, val c: Float, val d: Float, val sweep: Float)

private fun cornerParams(radiusPx: Float, smoothing: Float, budget: Float): CornerParams {
    val radius = radiusPx.coerceIn(0f, budget)
    if (radius <= 0f) return CornerParams(0f, 0f, 0f, 0f, 0f, 0f, 0f)
    // Smoothing needs room along the edges; small shapes give it up before giving up their radius.
    val s = smoothing.coerceAtMost(budget / radius - 1f).coerceAtLeast(0f)
    val p = (1f + s) * radius
    val arcMeasure = 90f * (1f - s)
    val arcSection = sin(rad(arcMeasure / 2f)) * radius * sqrt(2f)
    val alpha = (90f - arcMeasure) / 2f
    val p3ToP4 = radius * tan(rad(alpha / 2f))
    val beta = 45f * s
    val c = p3ToP4 * cos(rad(beta))
    val d = c * tan(rad(beta))
    val b = (p - arcSection - c - d) / 3f
    return CornerParams(radius, p, 2f * b, b, c, d, arcMeasure)
}

/**
 * Draws the corner at (kx, ky), arriving along unit vector u and leaving along v (the outline runs
 * clockwise on screen).
 */
private fun Path.corner(kx: Float, ky: Float, ux: Float, uy: Float, vx: Float, vy: Float, k: CornerParams, first: Boolean = false) {
    val sx = kx - ux * k.p
    val sy = ky - uy * k.p
    if (first) moveTo(sx, sy) else lineTo(sx, sy)
    if (k.radius <= 0f) return
    val along = k.a + k.b + k.c
    val arcStartX = sx + ux * along + vx * k.d
    val arcStartY = sy + uy * along + vy * k.d
    cubicTo(sx + ux * k.a, sy + uy * k.a, sx + ux * (k.a + k.b), sy + uy * (k.a + k.b), arcStartX, arcStartY)
    val cx = kx - ux * k.radius + vx * k.radius
    val cy = ky - uy * k.radius + vy * k.radius
    val start = Math.toDegrees(atan2((arcStartY - cy).toDouble(), (arcStartX - cx).toDouble())).toFloat()
    arcTo(Rect(cx - k.radius, cy - k.radius, cx + k.radius, cy + k.radius), start, k.sweep, forceMoveTo = false)
    val ex = kx + vx * k.p
    val ey = ky + vy * k.p
    val arcEndX = ex - ux * k.d - vx * along
    val arcEndY = ey - uy * k.d - vy * along
    cubicTo(
        arcEndX + ux * k.d + vx * k.c,
        arcEndY + uy * k.d + vy * k.c,
        arcEndX + ux * k.d + vx * (k.b + k.c),
        arcEndY + uy * k.d + vy * (k.b + k.c),
        ex,
        ey
    )
}

private fun rad(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()
