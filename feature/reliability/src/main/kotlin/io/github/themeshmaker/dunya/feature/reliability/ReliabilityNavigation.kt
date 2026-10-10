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
