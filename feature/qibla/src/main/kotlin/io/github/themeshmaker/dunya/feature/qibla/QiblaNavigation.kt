package io.github.themeshmaker.dunya.feature.qibla

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object QiblaRoute

fun NavGraphBuilder.qiblaScreen(onBack: () -> Unit) {
    composable<QiblaRoute> { QiblaScreenRoute(onBack = onBack) }
}
