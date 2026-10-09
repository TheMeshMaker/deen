package io.github.themeshmaker.dunya.feature.tasbih

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TasbihRoute

fun NavGraphBuilder.tasbihScreen() {
    composable<TasbihRoute> { TasbihScreen() }
}
