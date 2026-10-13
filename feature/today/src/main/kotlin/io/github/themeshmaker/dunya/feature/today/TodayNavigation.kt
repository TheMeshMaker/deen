package io.github.themeshmaker.dunya.feature.today

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object PrayerTrackerRoute

fun NavGraphBuilder.todayScreen(actions: TodayActions) {
    composable<TodayRoute> { TodayRoute(actions) }
}

fun NavGraphBuilder.trackerScreen(onBack: () -> Unit) {
    composable<PrayerTrackerRoute> { TrackerRoute(onBack) }
}
