package io.github.themeshmaker.dunya.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.audio.QuranAudioController
import io.github.themeshmaker.dunya.core.audio.QuranAudioState
import io.github.themeshmaker.dunya.core.audio.WordTimings
import io.github.themeshmaker.dunya.core.audio.wordAt
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.domain.KhatmahProgress
import io.github.themeshmaker.dunya.core.domain.QuranPages
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.JuzStart
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.Reciter
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.model.TafsirEntry
import io.github.themeshmaker.dunya.core.model.TafsirSource
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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
    val tab: QuranTab = QuranTab.SURAHS,
    val khatmah: KhatmahPlan? = null,
    val pages: QuranPages? = null,
    val today: LocalDate? = null
) {
    fun surah(number: Int): Surah? = surahs.getOrNull(number - 1)

    val khatmahProgress: KhatmahProgress?
        get() = if (khatmah != null && pages != null && today != null) KhatmahProgress.of(khatmah, lastRead, pages, today) else null

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
class QuranHomeViewModel @Inject constructor(
    private val content: ContentRepository,
    private val activity: ActivityRepository,
    private val clock: Clock
) : ViewModel() {
    private val surahs = MutableStateFlow<Pair<List<Surah>, List<JuzStart>>>(emptyList<Surah>() to emptyList())
    private val pages = MutableStateFlow<QuranPages?>(null)
    private val query = MutableStateFlow("")
    private val tab = MutableStateFlow(QuranTab.SURAHS)

    init {
        viewModelScope.launch {
            surahs.value = content.surahs() to content.juzStarts()
            pages.value = QuranPages(content.pageStarts())
        }
    }

    val uiState: StateFlow<QuranHomeUiState> = combine(surahs, activity.activity, query, tab, pages) { (s, j), a, q, t, p ->
        QuranHomeUiState(s, j, a.lastRead, a.bookmarks, q, t, a.khatmah, p, LocalDate.now(clock))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranHomeUiState())

    /** Starts a khatmah of [days] days, from Al-Fatiha or from the page the reader is on. */
    fun startKhatmah(days: Int, fromCurrentPage: Boolean) {
        viewModelScope.launch {
            val state = uiState.value
            val start = if (fromCurrentPage) state.lastRead?.let { state.pages?.pageOf(it) } ?: 1 else 1
            if (!fromCurrentPage) activity.setLastRead(QuranPosition(1, 1))
            activity.setKhatmah(KhatmahPlan(LocalDate.now(clock), days, start))
        }
    }

    fun endKhatmah() {
        viewModelScope.launch { activity.setKhatmah(null) }
    }

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

/** The word being recited: verse [ayah] (0 = the opening Bismillah), [word] counted without pause marks. */
data class Highlight(val ayah: Int, val word: Int?)

/** Tafsir shown in the verse sheet and the surah tafsir screen. */
data class TafsirState(
    val source: TafsirSource = TafsirSource.JALALAYN,
    val entries: List<TafsirEntry>? = null,
    val meaning: List<String> = emptyList(),
    val loading: Boolean = false,
    /** The chosen tafsir isn't on the phone yet and couldn't be downloaded. */
    val unavailable: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuranReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val activity: ActivityRepository,
    private val userData: UserDataRepository,
    private val audio: QuranAudioController,
    private val timings: WordTimings
) : ViewModel() {
    private val route = savedStateHandle.toRoute<QuranReaderRoute>()
    private val loaded = MutableStateFlow<Triple<Surah, List<Ayah>, Map<Int, Int>>?>(null)
    private val visibleAyah = MutableStateFlow<Int?>(null)

    /** Player mode: the bar with play / pause and the green word that follows the recitation. */
    val playerMode = MutableStateFlow(false)

    val audioState: StateFlow<QuranAudioState> = audio.state

    /** Which word to light up, recomputed as the recitation moves (only for this surah). */
    val highlight: StateFlow<Highlight?> = audio.state
        .mapLatest { state ->
            val ayah = state.ayah
            if (state.surah != route.surah || ayah == null) return@mapLatest null
            val verse = if (ayah == 0) timings.forVerse(state.reciter, 1, 1) else timings.forVerse(state.reciter, route.surah, ayah)
            Highlight(ayah, verse.wordAt(state.positionMs))
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _tafsir = MutableStateFlow(TafsirState())
    val tafsir: StateFlow<TafsirState> = _tafsir

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

    /** Recites this surah from [ayah] to its end in the chosen voice, and turns player mode on. */
    fun playFrom(ayah: Int, title: String, arabicUi: Boolean) {
        val state = uiState.value
        val surah = state.surah ?: return
        val reciter = Reciter.byId(state.reading.reciterId)
        playerMode.value = true
        audio.play(surah.number, ayah, surah.ayahCount, reciter, title, reciter.name(arabicUi))
    }

    fun togglePlay() = audio.toggle()

    fun next() = audio.next()

    fun previous() = audio.previous()

    fun closePlayer() {
        audio.stop()
        playerMode.value = false
    }

    fun setReciter(reciter: Reciter) {
        viewModelScope.launch {
            val reading = userData.userData.first().reading
            userData.setReadingSettings(reading.copy(reciterId = reciter.id))
        }
        // Switching voice mid-surah carries on from the same verse.
        val state = audio.state.value
        val ayah = state.ayah
        if (state.surah == route.surah && ayah != null && state.playing) {
            val surah = uiState.value.surah ?: return
            audio.play(surah.number, ayah.coerceAtLeast(1), surah.ayahCount, reciter, surah.arabicName, reciter.name(true))
        }
    }

    /** Loads the tafsir of this surah from [source] (or the user's preferred one). */
    fun loadTafsir(source: TafsirSource? = null) {
        viewModelScope.launch {
            val chosen = source ?: TafsirSource.byId(userData.userData.first().reading.tafsirId)
            if (_tafsir.value.source == chosen && _tafsir.value.entries != null) return@launch
            _tafsir.value = _tafsir.value.copy(source = chosen, loading = true, unavailable = false)
            val meaning = _tafsir.value.meaning.ifEmpty { content.meaning(route.surah) }
            val entries = content.tafsir(chosen, route.surah)
            _tafsir.value = TafsirState(chosen, entries, meaning, loading = false, unavailable = entries == null)
            if (source != null) {
                val reading = userData.userData.first().reading
                userData.setReadingSettings(reading.copy(tafsirId = chosen.id))
            }
        }
    }

    private companion object {
        const val SAVE_DELAY_MS = 600L
    }
}

/** The tafsir of a whole surah, verse by verse (Quran tab → a surah's tafsir button, or the reader's menu). */
@HiltViewModel
class SurahTafsirViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val userData: UserDataRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<QuranTafsirRoute>()
    val startAyah: Int = route.ayah

    private val _surah = MutableStateFlow<Pair<Surah, List<Ayah>>?>(null)
    val surah: StateFlow<Pair<Surah, List<Ayah>>?> = _surah

    private val _tafsir = MutableStateFlow(TafsirState(loading = true))
    val tafsir: StateFlow<TafsirState> = _tafsir

    init {
        viewModelScope.launch {
            val surah = content.surahs().firstOrNull { it.number == route.surah } ?: return@launch
            _surah.value = surah to content.ayahs(surah.number)
            load(TafsirSource.byId(userData.userData.first().reading.tafsirId), remember = false)
        }
    }

    fun select(source: TafsirSource) = viewModelScope.launch { load(source, remember = true) }

    private suspend fun load(source: TafsirSource, remember: Boolean) {
        _tafsir.value = _tafsir.value.copy(source = source, loading = true, unavailable = false)
        val meaning = _tafsir.value.meaning.ifEmpty { content.meaning(route.surah) }
        val entries = content.tafsir(source, route.surah)
        _tafsir.value = TafsirState(source, entries, meaning, loading = false, unavailable = entries == null)
        if (remember) {
            val reading = userData.userData.first().reading
            userData.setReadingSettings(reading.copy(tafsirId = source.id))
        }
    }
}
