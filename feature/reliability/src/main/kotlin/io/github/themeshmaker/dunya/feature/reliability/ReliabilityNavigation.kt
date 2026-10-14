/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.reliability

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/** @param setup true right after onboarding: no back arrow, a Done button instead. */
@Serializable
data class ReliabilityRoute(val setup: Boolean = false)

fun NavGraphBuilder.reliabilityScreen(onDone: (setup: Boolean) -> Unit) {
    composable<ReliabilityRoute> { entry ->
        val setup = entry.toRoute<ReliabilityRoute>().setup
        ReliabilityScreenRoute(setup = setup, onDone = { onDone(setup) })
    }
}
