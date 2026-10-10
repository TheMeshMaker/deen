package io.github.themeshmaker.dunya.feature.timetable

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TimetableRoute

fun NavGraphBuilder.timetableScreen(onBack: () -> Unit) {
    composable<TimetableRoute> { TimetableScreenRoute(onBack = onBack) }
}
