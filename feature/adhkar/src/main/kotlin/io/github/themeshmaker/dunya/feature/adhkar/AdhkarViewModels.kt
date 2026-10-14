package io.github.themeshmaker.dunya.feature.adhkar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.ChallengeStreak
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.domain.DailyChallengePicker
import io.github.themeshmaker.dunya.core.domain.DailyContent
import io.github.themeshmaker.dunya.core.domain.PrayerTracker
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.ActivityData
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.Challenge
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.Dhikr
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.QuranReading
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.SunnahAct
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.model.TasbihDhikr
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
    val today: LocalDate? = null,
    val challenges: ChallengesUiState? = null
)

/** Today's challenges, ready to show, with how far along they are and the streak. */
data class ChallengesUiState(
    val date: LocalDate,
    val items: List<ChallengeView>,
    /** Done today: ticked, or (adhkar, prayers) seen to be done. */
    val done: Set<ChallengeKind>,
    val streak: Int,
    val best: Int,
    val showTranslation: Boolean,
    val haptics: Boolean
) {
    val allDone: Boolean get() = items.all { it.kind in done }
}

/** One of today's challenges with what the screen needs to show it. */
sealed interface ChallengeView {
    val kind: ChallengeKind

    data class Dua(val dhikr: Dhikr, val arabic: String, val category: AdhkarCategory) : ChallengeView {
        override val kind get() = ChallengeKind.DUA
    }

    /** [surah]: the surah the reading is in (for pages, where the reader last stopped); [start]: where the reader opens. */
    data class Quran(val reading: QuranReading, val surah: Surah?, val start: QuranPosition) : ChallengeView {
        override val kind get() = ChallengeKind.QURAN
    }

    data class Tasbih(val dhikr: TasbihDhikr, val count: Int) : ChallengeView {
        override val kind get() = ChallengeKind.TASBIH
    }

    /** [done] of the set's adhkar counted today. */
    data class Adhkar(val category: AdhkarCategory, val done: Int) : ChallengeView {
        override val kind get() = ChallengeKind.ADHKAR
    }

    /** [ticked] of the five prayers ticked off today. */
    data class Prayers(val ticked: Int) : ChallengeView {
        override val kind get() = ChallengeKind.PRAYERS
    }

    data class HadithOfTheDay(val hadith: Hadith?) : ChallengeView {
        override val kind get() = ChallengeKind.HADITH
    }

    data class Sunnah(val act: SunnahAct) : ChallengeView {
        override val kind get() = ChallengeKind.SUNNAH
    }
}

@HiltViewModel
class AdhkarHomeViewModel @Inject constructor(
    content: ContentRepository,
    private val activity: ActivityRepository,
    userData: UserDataRepository,
    private val clock: Clock
) : ViewModel() {
    private val categories = MutableStateFlow<List<AdhkarCategory>>(emptyList())
    private val surahs = MutableStateFlow<Map<Int, Surah>>(emptyMap())
    private val hadiths = MutableStateFlow<List<Hadith>>(emptyList())

    init {
        viewModelScope.launch { categories.value = content.adhkar() }
        viewModelScope.launch { surahs.value = content.surahs().associateBy { it.number } }
        viewModelScope.launch { hadiths.value = content.nawawi() }
    }

    val uiState: StateFlow<AdhkarHomeUiState> = combine(
        combine(categories, surahs, hadiths, ::Triple),
        activity.activity,
        userData.userData.map { it.reading }
    ) { (cats, names, nawawi), a, reading ->
        val today = today()
        val counts = a.adhkarCountsOn(today)
        AdhkarHomeUiState(
            categories = cats,
            done = cats.associate { cat -> cat.id to cat.items.count { (counts["${cat.id}/${it.id}"] ?: 0) >= it.count } },
            suggested = suggestedCategory(LocalTime.now(clock.withZone(ZoneId.systemDefault()))),
            tasbih = a.tasbih,
            today = today,
            challenges = if (cats.isEmpty()) null else challenges(today, cats, names, nawawi, a, reading)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdhkarHomeUiState())

    /** Ticks one of today's challenges on or off (tasbih, adhkar and prayers also tick themselves). */
    fun setChallengeDone(kind: ChallengeKind, done: Boolean) {
        viewModelScope.launch { activity.setChallengeDone(today(), kind, done) }
    }

    private fun challenges(
        today: LocalDate,
        cats: List<AdhkarCategory>,
        surahs: Map<Int, Surah>,
        nawawi: List<Hadith>,
        a: ActivityData,
        reading: ReadingSettings
    ): ChallengesUiState {
        val counts = a.adhkarCountsOn(today)
        fun doneIn(category: AdhkarCategory) = category.items.count { (counts["${category.id}/${it.id}"] ?: 0) >= it.count }
        val items = DailyChallengePicker.pick(today, cats).items.mapNotNull { challenge ->
            when (challenge) {
                is Challenge.Dua -> {
                    val category = cats.firstOrNull { it.id == challenge.ref.categoryId }
                    val dhikr = category?.items?.firstOrNull { it.id == challenge.ref.dhikrId }
                    if (category == null || dhikr == null) {
                        null
                    } else {
                        ChallengeView.Dua(dhikr, listOfNotNull(dhikr.preface, dhikr.text).joinToString(" "), category)
                    }
                }

                is Challenge.Quran -> {
                    val start = when (val q = challenge.reading) {
                        is QuranReading.Passage -> QuranPosition(q.surah, q.from)
                        is QuranReading.Pages -> a.lastRead ?: QuranPosition(1, 1)
                    }
                    ChallengeView.Quran(challenge.reading, surahs[start.surah], start)
                }

                is Challenge.Tasbih -> ChallengeView.Tasbih(challenge.dhikr, challenge.count)

                is Challenge.Adhkar -> cats.firstOrNull { it.id == challenge.categoryId }?.let { ChallengeView.Adhkar(it, doneIn(it)) }

                Challenge.Prayers -> ChallengeView.Prayers(a.prayedOn(today).size)

                Challenge.Hadith -> ChallengeView.HadithOfTheDay(DailyContent.pick(nawawi, today, DailyContent.HADITH_SALT))

                is Challenge.Sunnah -> ChallengeView.Sunnah(challenge.act)
            }
        }
        // Adhkar and prayers done elsewhere count even if they were finished before the tick was saved.
        val seen = items.mapNotNullTo(mutableSetOf()) { item ->
            item.kind.takeIf {
                (item is ChallengeView.Adhkar && item.done >= item.category.items.size) ||
                    (item is ChallengeView.Prayers && item.ticked >= PrayerTracker.prayers.size)
            }
        }
        val saved = a.challenges[today].orEmpty()
        if (!saved.containsAll(seen)) viewModelScope.launch { (seen - saved).forEach { activity.setChallengeDone(today, it, true) } }
        return ChallengesUiState(
            date = today,
            items = items,
            done = saved + seen,
            streak = ChallengeStreak.current(a.challenges, today),
            best = ChallengeStreak.best(a.challenges),
            showTranslation = reading.showTranslation,
            haptics = reading.haptics
        )
    }

    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

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
        val day = today()
        val finishesSet = next >= item.dhikr.count &&
            uiState.value.items.all { it.dhikr.id == item.dhikr.id || it.count >= it.dhikr.count }
        viewModelScope.launch {
            activity.setAdhkarCount(day, categoryId, item.dhikr.id, next)
            if (finishesSet && DailyChallengePicker.adhkarFor(day)?.categoryId == categoryId) {
                activity.setChallengeDone(day, ChallengeKind.ADHKAR, true)
            }
        }
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

/** Today's tasbih challenge as the tasbih screen shows it. */
data class TasbihChallenge(val dhikr: TasbihDhikr, val count: Int, val done: Boolean)

@HiltViewModel
class TasbihViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activity: ActivityRepository,
    userData: UserDataRepository,
    private val clock: Clock
) : ViewModel() {
    private val state = MutableStateFlow<TasbihState?>(null)
    private val preset = savedStateHandle.toRoute<TasbihRoute>()

    val challenge: StateFlow<TasbihChallenge?> = activity.activity.map { a ->
        val day = today()
        DailyChallengePicker.tasbihFor(day)?.let {
            TasbihChallenge(it.dhikr, it.count, ChallengeKind.TASBIH in a.challenges[day].orEmpty())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reading: StateFlow<ReadingSettings> = userData.userData.map { it.reading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettings())

    val uiState: StateFlow<TasbihState?> = state

    init {
        viewModelScope.launch {
            val saved = rollOver(activity.activity.first().tasbih)
            // Opened for a challenge: that dhikr and count (carrying on if it was already set up).
            val dhikr = preset.dhikr
            state.value = if (dhikr != null && (saved.dhikrId != dhikr || saved.target != preset.target)) {
                saved.copy(dhikrId = dhikr, target = preset.target ?: saved.target, count = 0)
            } else {
                saved
            }
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
        val completed = next.target > 0 && next.count == next.target
        val goal = challenge.value
        if (completed && goal != null && !goal.done && next.dhikrId == goal.dhikr.id && next.target == goal.count) {
            viewModelScope.launch { activity.setChallengeDone(today(), ChallengeKind.TASBIH, true) }
        }
        return completed
    }

    /** Sets the counter up for today's challenge. */
    fun takeChallenge() {
        val goal = challenge.value ?: return
        state.value = state.value?.copy(dhikrId = goal.dhikr.id, target = goal.count, count = 0)
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
        val today = today()
        return if (s.day == today) s else s.copy(day = today, todayTotal = 0)
    }

    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
}
