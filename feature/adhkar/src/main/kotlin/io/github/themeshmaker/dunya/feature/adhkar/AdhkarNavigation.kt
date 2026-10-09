package io.github.themeshmaker.dunya.feature.adhkar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AdhkarRoute

fun NavGraphBuilder.adhkarScreen() {
    composable<AdhkarRoute> { AdhkarScreen() }
}
