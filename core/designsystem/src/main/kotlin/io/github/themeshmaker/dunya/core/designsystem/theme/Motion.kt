/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion for the Sleek style: springs rather than fixed durations, so movement settles the way
 * real objects do, and the long-tail "ease out" curve iOS uses for sheets and navigation.
 */
object DeenMotion {
    /** Fast start, very long gentle settle (iOS's navigation/sheet curve). */
    val EaseOutSoft = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Symmetric, for things that move and come back. */
    val EaseInOutSoft = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    const val SCREEN_MS = 460

    /** Settles quickly with no visible overshoot: positions, sizes, selection. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.86f, stiffness = 420f)

    /** A little overshoot: things that "pop" (a check mark, a pressed card letting go). */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.58f, stiffness = 520f)

    /** Slow and soft: progress, large surfaces. */
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessLow)

    fun <T> screen(): FiniteAnimationSpec<T> = tween(SCREEN_MS, easing = EaseOutSoft)
}

/**
 * True when the user turned animations off in Android (Accessibility → Remove animations, or
 * the animator scale set to 0). Decorative motion stops; state changes still happen instantly.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}
