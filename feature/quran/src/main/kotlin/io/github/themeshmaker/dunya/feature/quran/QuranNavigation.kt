/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.quran

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object QuranRoute

/** Opens [surah] at [ayah] (1 = the beginning). */
@Serializable
data class QuranReaderRoute(val surah: Int, val ayah: Int = 1)

/** The tafsir of a whole surah, scrolled to [ayah]. */
@Serializable
data class QuranTafsirRoute(val surah: Int, val ayah: Int = 1)

fun NavGraphBuilder.quranScreens(
    onOpen: (surah: Int, ayah: Int) -> Unit,
    onBack: () -> Unit,
    onOpenSurahInPlace: (Int) -> Unit,
    onOpenTafsir: (surah: Int, ayah: Int) -> Unit
) {
    composable<QuranRoute> { QuranHomeScreenRoute(onOpen = onOpen, onOpenTafsir = { onOpenTafsir(it, 1) }) }
    composable<QuranReaderRoute> { QuranReaderScreenRoute(onBack = onBack, onOpenSurah = onOpenSurahInPlace, onOpenTafsir = onOpenTafsir) }
    composable<QuranTafsirRoute> { SurahTafsirScreenRoute(onBack = onBack) }
}
