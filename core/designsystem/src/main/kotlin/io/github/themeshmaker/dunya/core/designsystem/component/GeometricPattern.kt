package io.github.themeshmaker.dunya.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decorative background of small, isolated 8-pointed stars (two overlapping squares)
 * on staggered rows.
 *
 * The stars are deliberately small and spaced apart: in a tight grid, the gaps
 * between 8-pointed stars form cross shapes, which this app avoids.
 */
@Composable
fun GeometricPattern(color: Color, modifier: Modifier = Modifier, cell: Dp = 40.dp, strokeWidth: Dp = 1.dp) {
    val cellPx = with(LocalDensity.current) { cell.toPx() }
    val star = remember(cellPx) { octagramPath(outerRadius = cellPx * 0.28f) }
    // Clipped: the tiling deliberately overdraws its edges so no half-empty border shows.
    Canvas(modifier.clipToBounds()) {
        val stroke = Stroke(width = strokeWidth.toPx())
        var row = 0
        var y = 0f
        while (y < size.height + cellPx) {
            var x = if (row % 2 == 0) 0f else cellPx / 2f
            while (x < size.width + cellPx) {
                translate(x, y) { drawPath(star, color, style = stroke) }
                x += cellPx
            }
            y += cellPx * 0.75f
            row++
        }
    }
}

/** Outline of two squares rotated 45° from each other, centred on the origin. */
private fun octagramPath(outerRadius: Float): Path {
    // Inner vertices sit where the two squares' edges cross.
    val innerRadius = outerRadius * 0.7654f
    return Path().apply {
        for (i in 0 until 16) {
            val angle = Math.toRadians(i * 22.5 - 90.0)
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val px = (radius * cos(angle)).toFloat()
            val py = (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}
