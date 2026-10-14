/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.timetable

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TimetableRoute

@Serializable
data object IslamicCalendarRoute

fun NavGraphBuilder.timetableScreen(onBack: () -> Unit) {
    composable<TimetableRoute> { TimetableScreenRoute(onBack = onBack) }
}

fun NavGraphBuilder.islamicCalendarScreen(onBack: () -> Unit) {
    composable<IslamicCalendarRoute> { IslamicCalendarScreenRoute(onBack = onBack) }
}
