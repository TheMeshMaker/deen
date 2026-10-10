package io.github.themeshmaker.dunya.feature.hadith

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data object HadithRoute

/** An-Nawawi's Forty, opened at hadith [number]. */
@Serializable
data class NawawiRoute(val number: Int = 1)

@Serializable
data object ProphetFactsRoute

@Serializable
data class HadithCollectionRoute(val id: String)

/** One book (kitab) of a collection; [highlight] scrolls to and marks that hadith number. */
@Serializable
data class HadithBookRoute(val id: String, val book: Int, val highlight: String? = null)

fun NavGraphBuilder.hadithScreens(
    onOpenNawawi: (Int) -> Unit,
    onOpenFacts: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenBook: (id: String, book: Int, highlight: String?) -> Unit,
    onBack: () -> Unit
) {
    composable<HadithRoute> { HadithHomeScreenRoute(onOpenNawawi, onOpenFacts, onOpenCollection) }
    composable<NawawiRoute> { NawawiScreenRoute(onBack) }
    composable<ProphetFactsRoute> { FactsScreenRoute(onBack) }
    composable<HadithCollectionRoute> { entry ->
        val id = entry.toRoute<HadithCollectionRoute>().id
        HadithCollectionScreenRoute(onBack = onBack, onOpenBook = { book, highlight -> onOpenBook(id, book, highlight) })
    }
    composable<HadithBookRoute> { HadithBookScreenRoute(onBack) }
}
