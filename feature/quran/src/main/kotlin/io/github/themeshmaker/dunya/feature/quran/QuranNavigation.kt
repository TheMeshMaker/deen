package io.github.themeshmaker.dunya.feature.quran

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object QuranRoute

/** Opens [surah] at [ayah] (1 = the beginning). */
@Serializable
data class QuranReaderRoute(val surah: Int, val ayah: Int = 1)

fun NavGraphBuilder.quranScreens(onOpen: (surah: Int, ayah: Int) -> Unit, onBack: () -> Unit, onOpenSurahInPlace: (Int) -> Unit) {
    composable<QuranRoute> { QuranHomeScreenRoute(onOpen = onOpen) }
    composable<QuranReaderRoute> { QuranReaderScreenRoute(onBack = onBack, onOpenSurah = onOpenSurahInPlace) }
}
