/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.adhkar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AdhkarRoute

@Serializable
data class AdhkarReaderRoute(val category: String)

/** The tasbih; [dhikr] and [target] set it up for a challenge. */
@Serializable
data class TasbihRoute(val dhikr: String? = null, val target: Int? = null)

fun NavGraphBuilder.adhkarScreens(
    onOpenCategory: (String) -> Unit,
    onOpenTasbih: (TasbihRoute) -> Unit,
    onOpenQuran: (surah: Int, ayah: Int) -> Unit,
    onOpenTracker: () -> Unit,
    onOpenHadith: (number: Int) -> Unit,
    onBack: () -> Unit
) {
    composable<AdhkarRoute> {
        AdhkarHomeScreenRoute(
            onOpenCategory = onOpenCategory,
            onOpenTasbih = onOpenTasbih,
            links = ChallengeLinks(onOpenQuran, onOpenTasbih, onOpenCategory, onOpenTracker, onOpenHadith)
        )
    }
    composable<AdhkarReaderRoute> { AdhkarReaderScreenRoute(onBack = onBack) }
    composable<TasbihRoute> { TasbihScreenRoute(onBack = onBack) }
}
