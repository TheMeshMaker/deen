package io.github.themeshmaker.dunya.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.JuzStart
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.Surah
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class QuranTab {
    SURAHS,
    JUZ,
    BOOKMARKS
}

data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val juz: List<JuzStart> = emptyList(),
    val lastRead: QuranPosition? = null,
    val bookmarks: List<QuranPosition> = emptyList(),
    val query: String = "",
    val tab: QuranTab = QuranTab.SURAHS
) {
    fun surah(number: Int): Surah? = surahs.getOrNull(number - 1)

    /** Matches the number, the Arabic name (diacritics ignored), the transliteration or the English meaning. */
    val filtered: List<Surah>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return surahs
            val number = q.toIntOrNull()
            val plain = q.filter { it.isLetterOrDigit() }.lowercase()
            return surahs.filter { s ->
                s.number == number ||
                    s.arabicName.contains(q) ||
                    s.transliteration.filter { it.isLetterOrDigit() }.lowercase().contains(plain) ||
                    s.translation.lowercase().contains(q.lowercase())
            }
        }
}

@HiltViewModel
class QuranHomeViewModel @Inject constructor(private val content: ContentRepository, activity: ActivityRepository) : ViewModel() {
    private val surahs = MutableStateFlow<Pair<List<Surah>, List<JuzStart>>>(emptyList<Surah>() to emptyList())
    private val query = MutableStateFlow("")
    private val tab = MutableStateFlow(QuranTab.SURAHS)

    init {
        viewModelScope.launch { surahs.value = content.surahs() to content.juzStarts() }
    }

    val uiState: StateFlow<QuranHomeUiState> = combine(surahs, activity.activity, query, tab) { (s, j), a, q, t ->
        QuranHomeUiState(s, j, a.lastRead, a.bookmarks, q, t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranHomeUiState())

    fun setQuery(value: String) {
        query.value = value
        if (value.isNotBlank()) tab.value = QuranTab.SURAHS
    }

    fun setTab(value: QuranTab) {
        tab.value = value
    }
}

data class ReaderUiState(
    val surah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    /** Verse number → juz that starts there, within this surah. */
    val juzStarts: Map<Int, Int> = emptyMap(),
    val bookmarks: Set<Int> = emptySet(),
    val reading: ReadingSettings = ReadingSettings(),
    val startAyah: Int = 1
) {
    val loaded: Boolean get() = surah != null && ayahs.isNotEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class QuranReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val activity: ActivityRepository,
    userData: UserDataRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<QuranReaderRoute>()
    private val loaded = MutableStateFlow<Triple<Surah, List<Ayah>, Map<Int, Int>>?>(null)
    private val visibleAyah = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            val surah = content.surahs().firstOrNull { it.number == route.surah } ?: return@launch
            val juz = content.juzStarts().filter { it.surah == surah.number }.associate { it.ayah to it.juz }
            loaded.value = Triple(surah, content.ayahs(surah.number), juz)
        }
        // Remember where the reader is, but not on every frame of a fling.
        viewModelScope.launch {
            visibleAyah.filterNotNull().distinctUntilChanged().debounce(SAVE_DELAY_MS).collect { ayah ->
                activity.setLastRead(QuranPosition(route.surah, ayah))
            }
        }
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        loaded,
        activity.activity.map { a -> a.bookmarks.filter { it.surah == route.surah }.map { it.ayah }.toSet() },
        userData.userData.map { it.reading }
    ) { l, bookmarks, reading ->
        ReaderUiState(l?.first, l?.second.orEmpty(), l?.third.orEmpty(), bookmarks, reading, route.ayah.coerceAtLeast(1))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState(startAyah = route.ayah.coerceAtLeast(1)))

    fun onVisible(ayah: Int) {
        visibleAyah.value = ayah
    }

    fun toggleBookmark(ayah: Int) {
        viewModelScope.launch { activity.toggleBookmark(QuranPosition(route.surah, ayah)) }
    }

    private companion object {
        const val SAVE_DELAY_MS = 600L
    }
}
