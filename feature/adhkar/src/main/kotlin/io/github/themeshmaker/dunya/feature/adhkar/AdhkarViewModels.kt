package io.github.themeshmaker.dunya.feature.adhkar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.Dhikr
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.TasbihState
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdhkarHomeUiState(
    val categories: List<AdhkarCategory> = emptyList(),
    /** Category id → how many of its adhkar are complete today. */
    val done: Map<String, Int> = emptyMap(),
    val suggested: String? = null,
    val tasbih: TasbihState = TasbihState(),
    val today: LocalDate? = null
)

@HiltViewModel
class AdhkarHomeViewModel @Inject constructor(content: ContentRepository, activity: ActivityRepository, private val clock: Clock) :
    ViewModel() {
    private val categories = MutableStateFlow<List<AdhkarCategory>>(emptyList())

    init {
        viewModelScope.launch { categories.value = content.adhkar() }
    }

    val uiState: StateFlow<AdhkarHomeUiState> = combine(categories, activity.activity) { cats, a ->
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        val counts = a.adhkarCountsOn(today)
        AdhkarHomeUiState(
            categories = cats,
            done = cats.associate { cat -> cat.id to cat.items.count { (counts["${cat.id}/${it.id}"] ?: 0) >= it.count } },
            suggested = suggestedCategory(LocalTime.now(clock.withZone(ZoneId.systemDefault()))),
            tasbih = a.tasbih,
            today = today
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdhkarHomeUiState())

    /** A rough guide by clock time; the prayer times themselves would be more exact. */
    private fun suggestedCategory(time: LocalTime): String? = when (time.hour) {
        in 4..10 -> AdhkarCategory.MORNING
        in 15..19 -> AdhkarCategory.EVENING
        in 21..23, in 0..1 -> AdhkarCategory.SLEEP
        else -> null
    }
}

/** A dhikr with its Arabic ready to show (Quran passages resolved from the Tanzil text). */
data class DhikrView(val dhikr: Dhikr, val arabic: String, val count: Int)

data class AdhkarReaderUiState(
    val category: AdhkarCategory? = null,
    val items: List<DhikrView> = emptyList(),
    val reading: ReadingSettings = ReadingSettings()
) {
    val doneCount: Int get() = items.count { it.count >= it.dhikr.count }
}

@HiltViewModel
class AdhkarReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val activity: ActivityRepository,
    userData: UserDataRepository,
    private val clock: Clock
) : ViewModel() {
    private val categoryId = savedStateHandle.toRoute<AdhkarReaderRoute>().category
    private val loaded = MutableStateFlow<Pair<AdhkarCategory, Map<Int, String>>?>(null)

    init {
        viewModelScope.launch {
            val category = content.adhkar().firstOrNull { it.id == categoryId } ?: return@launch
            loaded.value = category to category.items.associate { it.id to arabicOf(it) }
        }
    }

    val uiState: StateFlow<AdhkarReaderUiState> = combine(
        loaded,
        activity.activity,
        userData.userData.map {
            it.reading
        }
    ) { l, a, reading ->
        val counts = a.adhkarCountsOn(today())
        AdhkarReaderUiState(
            category = l?.first,
            items = l?.first?.items.orEmpty().map { d -> DhikrView(d, l?.second?.get(d.id).orEmpty(), counts["$categoryId/${d.id}"] ?: 0) },
            reading = reading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdhkarReaderUiState())

    /** Counts one recitation; returns true when this completed the dhikr. */
    fun count(item: DhikrView): Boolean {
        if (item.count >= item.dhikr.count) return false
        val next = item.count + 1
        viewModelScope.launch { activity.setAdhkarCount(today(), categoryId, item.dhikr.id, next) }
        return next >= item.dhikr.count
    }

    fun reset() {
        viewModelScope.launch { activity.resetAdhkar(today(), categoryId) }
    }

    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private suspend fun arabicOf(dhikr: Dhikr): String {
        if (!dhikr.isQuran) return listOfNotNull(dhikr.preface, dhikr.text).joinToString(" ")
        val passages = dhikr.quran.map { range ->
            content.ayahs(range.surah).filter { it.number in range.from..range.to }.joinToString(" ") { ayah ->
                listOfNotNull(ayah.bismillah, ayah.text).joinToString(" ") + " " + endOfAyah(ayah.number)
            }
        }
        return (listOfNotNull(dhikr.preface) + passages).joinToString(if (dhikr.quran.size > 1) "\n\n" else " ")
    }
}

/** U+06DD followed by Arabic-Indic digits; Amiri Quran draws them as one verse-end ornament. */
internal fun endOfAyah(number: Int): String = "۝" + number.toString().map { '٠' + (it - '0') }.joinToString("")

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val activity: ActivityRepository,
    userData: UserDataRepository,
    private val clock: Clock
) : ViewModel() {
    private val state = MutableStateFlow<TasbihState?>(null)

    val reading: StateFlow<ReadingSettings> = userData.userData.map { it.reading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettings())

    val uiState: StateFlow<TasbihState?> = state

    init {
        viewModelScope.launch {
            state.value = rollOver(activity.activity.first().tasbih)
            // Persist every change, but never queue up stale writes behind fast tapping.
            var last: TasbihState? = null
            state.collect { current ->
                if (current != null && current != last) {
                    last = current
                    activity.setTasbih(current)
                }
            }
        }
    }

    /** Adds one; returns true when a round of [TasbihState.target] was just completed. */
    fun count(): Boolean {
        val current = rollOver(state.value ?: return false)
        val wrapped = current.target > 0 && current.count >= current.target
        val next = current.copy(
            count = if (wrapped) 1 else current.count + 1,
            todayTotal = current.todayTotal + 1,
            allTimeTotal = current.allTimeTotal + 1
        )
        state.value = next
        return next.target > 0 && next.count == next.target
    }

    fun setTarget(target: Int) {
        state.value = state.value?.copy(target = target, count = 0)
    }

    fun setDhikr(id: String) {
        state.value = state.value?.copy(dhikrId = id, count = 0)
    }

    fun reset() {
        state.value = state.value?.copy(count = 0)
    }

    private fun rollOver(s: TasbihState): TasbihState {
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        return if (s.day == today) s else s.copy(day = today, todayTotal = 0)
    }
}
