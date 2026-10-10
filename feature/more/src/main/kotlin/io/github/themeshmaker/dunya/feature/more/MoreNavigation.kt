package io.github.themeshmaker.dunya.feature.more

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object MoreRoute

fun NavGraphBuilder.moreScreen(actions: MoreActions) {
    composable<MoreRoute> { MoreScreenRoute(actions) }
}
