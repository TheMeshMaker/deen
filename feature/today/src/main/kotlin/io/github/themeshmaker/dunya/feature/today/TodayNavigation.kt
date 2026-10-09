package io.github.themeshmaker.dunya.feature.today

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

fun NavGraphBuilder.todayScreen() {
    composable<TodayRoute> { TodayScreen() }
}
