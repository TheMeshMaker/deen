package io.github.themeshmaker.dunya.feature.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.HijriDate
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.prayer.HijriCalendar
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimetableRow(val date: LocalDate, val hijri: HijriDate, val times: Map<Prayer, Instant>, val isToday: Boolean) {
    val isFriday: Boolean get() = date.dayOfWeek == DayOfWeek.FRIDAY
}

/** Which month is on screen: a Gregorian month, or a Hijri year and month. */
sealed interface TimetableMonth {
    data class Gregorian(val month: YearMonth) : TimetableMonth

    data class Hijri(val year: Int, val month: Int) : TimetableMonth
}

data class TimetableUiState(
    val calendar: CalendarType = CalendarType.GREGORIAN,
    val month: TimetableMonth? = null,
    val rows: List<TimetableRow> = emptyList(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val location: SavedLocation? = null
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val userData: UserDataRepository,
    private val calculator: PrayerTimeCalculator,
    private val hijriCalendar: HijriCalendar,
    private val clock: Clock
) : ViewModel() {
    /** Months away from the current one; 0 = this month. */
    private val offset = MutableStateFlow(0)

    val uiState: StateFlow<TimetableUiState> = combine(userData.userData, offset) { data, monthOffset ->
        val location = data.location
        val settings = data.prayerSettings
        if (location == null || settings == null) return@combine TimetableUiState(calendar = data.timetableCalendar)
        val zone = location.zoneId
        val today = LocalDate.now(clock.withZone(zone))
        val (month, dates) = when (data.timetableCalendar) {
            CalendarType.GREGORIAN -> {
                val ym = YearMonth.from(today).plusMonths(monthOffset.toLong())
                TimetableMonth.Gregorian(ym) to (1..ym.lengthOfMonth()).map { ym.atDay(it) }
            }

            CalendarType.HIJRI -> {
                val now = hijriCalendar.toHijri(today, settings.hijriOffsetDays)
                val index = now.month - 1 + monthOffset
                val year = now.year + Math.floorDiv(index, 12)
                val month = Math.floorMod(index, 12) + 1
                TimetableMonth.Hijri(year, month) to hijriCalendar.monthDates(year, month, settings.hijriOffsetDays)
            }
        }
        TimetableUiState(
            calendar = data.timetableCalendar,
            month = month,
            rows = dates.map { date ->
                TimetableRow(
                    date = date,
                    hijri = hijriCalendar.toHijri(date, settings.hijriOffsetDays),
                    times = calculator.prayerDay(date, location.coordinates, settings).times,
                    isToday = date == today
                )
            },
            zone = zone,
            location = location
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimetableUiState())

    fun previous() {
        offset.value -= 1
    }

    fun next() {
        offset.value += 1
    }

    fun thisMonth() {
        offset.value = 0
    }

    fun setCalendar(calendar: CalendarType) {
        offset.value = 0
        viewModelScope.launch { userData.setTimetableCalendar(calendar) }
    }
}
