/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.hadith

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.domain.DailyContent
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.HadithCollection
import io.github.themeshmaker.dunya.core.model.HadithEntry
import io.github.themeshmaker.dunya.core.model.ProphetFact
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HadithHomeUiState(
    val dailyHadith: Hadith? = null,
    val dailyFact: ProphetFact? = null,
    val nawawiCount: Int = 0,
    val factCount: Int = 0,
    val collections: List<HadithCollection> = emptyList()
)

@HiltViewModel
class HadithHomeViewModel @Inject constructor(private val content: ContentRepository, private val clock: Clock) : ViewModel() {
    private val _uiState = MutableStateFlow(HadithHomeUiState())
    val uiState: StateFlow<HadithHomeUiState> = _uiState

    init {
        viewModelScope.launch {
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            val nawawi = content.nawawi()
            val facts = content.prophetFacts()
            _uiState.value = HadithHomeUiState(
                dailyHadith = DailyContent.pick(nawawi, today, DailyContent.HADITH_SALT),
                dailyFact = DailyContent.pick(facts, today, DailyContent.FACT_SALT),
                nawawiCount = nawawi.size,
                factCount = facts.size,
                collections = content.hadithCollections()
            )
        }
    }
}

data class NawawiUiState(val hadiths: List<Hadith> = emptyList(), val reading: ReadingSettings = ReadingSettings(), val start: Int = 1)

@HiltViewModel
class NawawiViewModel @Inject constructor(savedStateHandle: SavedStateHandle, content: ContentRepository, userData: UserDataRepository) :
    ViewModel() {
    private val start = savedStateHandle.toRoute<NawawiRoute>().number
    private val hadiths = MutableStateFlow<List<Hadith>>(emptyList())

    init {
        viewModelScope.launch { hadiths.value = content.nawawi() }
    }

    val uiState: StateFlow<NawawiUiState> = combine(hadiths, userData.userData.map { it.reading }) { h, r -> NawawiUiState(h, r, start) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NawawiUiState(start = start))
}

data class FactsUiState(val facts: List<ProphetFact> = emptyList(), val todayId: Int? = null)

@HiltViewModel
class FactsViewModel @Inject constructor(content: ContentRepository, clock: Clock) : ViewModel() {
    private val _uiState = MutableStateFlow(FactsUiState())
    val uiState: StateFlow<FactsUiState> = _uiState

    init {
        viewModelScope.launch {
            val facts = content.prophetFacts()
            val today = DailyContent.pick(facts, LocalDate.now(clock.withZone(ZoneId.systemDefault())), DailyContent.FACT_SALT)
            _uiState.value = FactsUiState(facts, today?.id)
        }
    }
}

data class CollectionUiState(
    val collection: HadithCollection? = null,
    val query: String = "",
    val searching: Boolean = false,
    /** Null until a search has run. */
    val results: List<HadithEntry>? = null
)

@HiltViewModel
class HadithCollectionViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val content: ContentRepository) :
    ViewModel() {
    private val id = savedStateHandle.toRoute<HadithCollectionRoute>().id
    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(collection = content.hadithCollections().firstOrNull { it.id == id })
        }
    }

    fun setQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value, results = if (value.isBlank()) null else _uiState.value.results)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.length < MIN_QUERY && query.toIntOrNull() == null) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searching = true)
            val results = content.searchHadith(id, query, MAX_RESULTS)
            _uiState.value = _uiState.value.copy(searching = false, results = results)
        }
    }

    private companion object {
        const val MIN_QUERY = 2
        const val MAX_RESULTS = 200
    }
}

data class BookUiState(
    val collection: HadithCollection? = null,
    val entries: List<HadithEntry> = emptyList(),
    val reading: ReadingSettings = ReadingSettings(),
    val highlight: String? = null
)

@HiltViewModel
class HadithBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    content: ContentRepository,
    userData: UserDataRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<HadithBookRoute>()
    private val loaded = MutableStateFlow<Pair<HadithCollection?, List<HadithEntry>>>(null to emptyList())

    init {
        viewModelScope.launch {
            loaded.value = content.hadithCollections().firstOrNull { it.id == route.id } to content.hadithBook(route.id, route.book)
        }
    }

    val uiState: StateFlow<BookUiState> = combine(
        loaded,
        userData.userData.map {
            it.reading
        }
    ) { (c, e), r -> BookUiState(c, e, r, route.highlight) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookUiState(highlight = route.highlight))
}
