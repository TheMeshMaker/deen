/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.themeshmaker.dunya.feature.onboarding.location.LocationPickerScreen
import io.github.themeshmaker.dunya.feature.onboarding.method.MethodPickerScreen
import kotlinx.serialization.Serializable

/** First-run flow: welcome → location → calculation method. */
@Serializable
data object OnboardingRoute

/** Change location later (from the Today screen). */
@Serializable
data object LocationPickerRoute

/** Change calculation method / Asr later (from the Today screen). */
@Serializable
data object MethodPickerRoute

fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable<OnboardingRoute> { OnboardingScreen(onFinished = onFinished) }
}

fun NavGraphBuilder.locationPickerScreen(onDone: () -> Unit) {
    composable<LocationPickerRoute> { LocationPickerScreen(onDone = onDone) }
}

fun NavGraphBuilder.methodPickerScreen(onDone: () -> Unit) {
    composable<MethodPickerRoute> { MethodPickerScreen(onDone = onDone) }
}
