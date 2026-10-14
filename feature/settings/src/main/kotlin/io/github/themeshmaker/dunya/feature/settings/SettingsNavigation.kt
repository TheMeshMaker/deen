/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data class AdhanVoiceRoute(val fajr: Boolean)

@Serializable
data object AboutRoute

fun NavGraphBuilder.settingsScreens(
    onBack: () -> Unit,
    onOpenVoice: (fajr: Boolean) -> Unit,
    onOpenReliability: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenMethod: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenAbout: () -> Unit
) {
    composable<SettingsRoute> {
        SettingsScreenRoute(
            onBack = onBack,
            onOpenVoice = onOpenVoice,
            onOpenReliability = onOpenReliability,
            onOpenLocation = onOpenLocation,
            onOpenMethod = onOpenMethod,
            onOpenTimetable = onOpenTimetable,
            onOpenAbout = onOpenAbout
        )
    }
    composable<AdhanVoiceRoute> { entry -> AdhanVoiceScreenRoute(fajr = entry.toRoute<AdhanVoiceRoute>().fajr, onBack = onBack) }
    composable<AboutRoute> { AboutScreenRoute(onBack = onBack) }
}
