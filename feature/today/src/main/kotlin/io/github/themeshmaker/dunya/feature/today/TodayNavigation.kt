/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
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
