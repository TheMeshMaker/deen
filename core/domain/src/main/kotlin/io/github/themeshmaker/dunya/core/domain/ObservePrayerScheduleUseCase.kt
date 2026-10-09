package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.HijriDate
import io.github.themeshmaker.dunya.core.model.PrayerDay
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.prayer.HijriCalendar
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeline
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Everything the Today screen needs for one local day. */
data class PrayerSchedule(
    val location: SavedLocation,
    val settings: PrayerSettings,
    val date: LocalDate,
    val hijriDate: HijriDate,
    val timeline: PrayerTimeline
) {
    val today: PrayerDay get() = timeline.today
}

/**
 * Today's prayer schedule for the saved location, recalculated whenever settings change and
 * again after midnight in the location's time zone. Emits null until onboarding has set a
 * location and a method.
 */
class ObservePrayerScheduleUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val calculator: PrayerTimeCalculator,
    private val hijriCalendar: HijriCalendar,
    private val clock: Clock
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<PrayerSchedule?> = userDataRepository.userData
        .map { it.location to it.prayerSettings }
        .distinctUntilChanged()
        .flatMapLatest { (location, settings) ->
            if (location == null || settings == null) {
                flowOf(null)
            } else {
                localDates(location.zoneId).map { date -> schedule(date, location, settings) }
            }
        }
        .distinctUntilChanged()

    fun schedule(date: LocalDate, location: SavedLocation, settings: PrayerSettings): PrayerSchedule {
        fun day(d: LocalDate) = calculator.prayerDay(d, location.coordinates, settings)
        return PrayerSchedule(
            location = location,
            settings = settings,
            date = date,
            hijriDate = hijriCalendar.toHijri(date, settings.hijriOffsetDays),
            timeline = PrayerTimeline(day(date.minusDays(1)), day(date), day(date.plusDays(1)))
        )
    }

    /**
     * The local date now, then again just after each midnight. Re-checks at least every minute
     * so a manual clock or time-zone change is picked up quickly.
     */
    private fun localDates(zone: ZoneId): Flow<LocalDate> = flow {
        while (true) {
            val now = clock.instant()
            val today = now.atZone(zone).toLocalDate()
            emit(today)
            val untilMidnight = Duration.between(now, today.plusDays(1).atStartOfDay(zone).toInstant()).toMillis()
            delay(untilMidnight.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS))
        }
    }.distinctUntilChanged()

    private companion object {
        const val MIN_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 60_000L
    }
}
