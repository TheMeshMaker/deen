package io.github.themeshmaker.dunya.feature.adhkar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AdhkarRoute

@Serializable
data class AdhkarReaderRoute(val category: String)

@Serializable
data object TasbihRoute

fun NavGraphBuilder.adhkarScreens(onOpenCategory: (String) -> Unit, onOpenTasbih: () -> Unit, onBack: () -> Unit) {
    composable<AdhkarRoute> { AdhkarHomeScreenRoute(onOpenCategory = onOpenCategory, onOpenTasbih = onOpenTasbih) }
    composable<AdhkarReaderRoute> { AdhkarReaderScreenRoute(onBack = onBack) }
    composable<TasbihRoute> { TasbihScreenRoute(onBack = onBack) }
}
