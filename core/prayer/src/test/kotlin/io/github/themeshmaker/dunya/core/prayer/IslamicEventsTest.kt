package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.IslamicEvent
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import java.time.Duration
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicEventsTest {

    private val today = LocalDate.of(2026, 10, 13)

    @Test
    fun everyEventIsOnItsHijriDateAndNotOver() {
        val events = IslamicEvents.upcoming(today)
        assertEquals(IslamicEvent.entries.toSet(), events.map { it.event }.toSet())
        events.forEach { e ->
            val hijri = HijrahDate.from(e.date)
            assertEquals(e.hijri.month, hijri.get(ChronoField.MONTH_OF_YEAR))
            assertEquals(e.event.day, hijri.get(ChronoField.DAY_OF_MONTH))
            if (e.event != IslamicEvent.WHITE_DAYS) assertEquals(e.event.month, e.hijri.month)
            assertFalse(e.endDate.isBefore(today))
        }
        assertEquals(events.sortedBy { it.date }.map { it.date }, events.map { it.date })
    }

    @Test
    fun eidAlAdhaIsTheDayAfterArafahAndAshuraNineDaysAfterNewYear() {
        val events = IslamicEvents.upcoming(today).associateBy { it.event }
        assertEquals(events.getValue(IslamicEvent.ARAFAH).date.plusDays(1), events.getValue(IslamicEvent.EID_AL_ADHA).date)
        assertEquals(events.getValue(IslamicEvent.NEW_YEAR).date.plusDays(9), events.getValue(IslamicEvent.ASHURA).date)
    }

    @Test
    fun theWhiteDaysAreOngoingOnThe14th() {
        // Find a day that is the 14th of a Hijri month.
        val fourteenth = (0L..40L).map { today.plusDays(it) }.first { HijrahDate.from(it).get(ChronoField.DAY_OF_MONTH) == 14 }
        val white = IslamicEvents.upcoming(fourteenth).first { it.event == IslamicEvent.WHITE_DAYS }
        assertTrue(white.ongoing)
        assertEquals(fourteenth.minusDays(1), white.date)
        assertEquals(fourteenth.plusDays(1), white.endDate)
    }

    @Test
    fun aMoonSightingOffsetMovesEventsTheOtherWay() {
        val plain = IslamicEvents.upcoming(today).first { it.event == IslamicEvent.RAMADAN }
        val shifted = IslamicEvents.upcoming(today, offsetDays = 1).first { it.event == IslamicEvent.RAMADAN }
        assertEquals(plain.date.minusDays(1), shifted.date)
    }

    @Test
    fun ramadanLastsTheWholeMonth() {
        val ramadan = IslamicEvents.upcoming(today).first { it.event == IslamicEvent.RAMADAN }
        val days = Duration.between(ramadan.date.atStartOfDay(), ramadan.endDate.atStartOfDay()).toDays() + 1
        assertTrue(days in 29..30)
        assertEquals(10, HijrahDate.from(ramadan.endDate.plusDays(1)).get(ChronoField.MONTH_OF_YEAR))
    }

    @Test
    fun theFeaturedDayPrefersTheYearsDaysUnlessTheWhiteDaysAreHere() {
        val next = IslamicEvents.next(today)!!
        val all = IslamicEvents.upcoming(today)
        val white = all.first { it.event == IslamicEvent.WHITE_DAYS }
        if (next.event == IslamicEvent.WHITE_DAYS) {
            assertTrue(white.ongoing || white.daysAway < 3)
        } else {
            assertEquals(all.first { it.event != IslamicEvent.WHITE_DAYS }, next)
        }
    }
}

class NightTimesTest {

    private val calculator = PrayerTimeCalculator(HijriCalendar())
    private val riyadh = Coordinates(24.6877, 46.7219)
    private val settings = PrayerSettings(method = CalculationMethod.UMM_AL_QURA)
    private val date = LocalDate.of(2026, 10, 9)
    private val timeline = PrayerTimeline(
        yesterday = calculator.prayerDay(date.minusDays(1), riyadh, settings),
        today = calculator.prayerDay(date, riyadh, settings),
        tomorrow = calculator.prayerDay(date.plusDays(1), riyadh, settings)
    )

    @Test
    fun theEveningLooksAheadToTomorrowsFajr() {
        val night = NightTimes.of(timeline, timeline.today[Prayer.ISHA])
        assertEquals(timeline.today[Prayer.MAGHRIB], night.maghrib)
        assertEquals(timeline.tomorrow[Prayer.FAJR], night.fajr)
        assertEquals(Duration.between(night.maghrib, night.midnight), Duration.between(night.midnight, night.fajr))
        val length = Duration.between(night.maghrib, night.fajr)
        assertEquals(length.multipliedBy(2).dividedBy(3), Duration.between(night.maghrib, night.lastThird))
    }

    @Test
    fun beforeFajrItIsStillLastNight() {
        val night = NightTimes.of(timeline, timeline.today[Prayer.FAJR].minusSeconds(600))
        assertEquals(timeline.yesterday[Prayer.MAGHRIB], night.maghrib)
        assertEquals(timeline.today[Prayer.FAJR], night.fajr)
        assertTrue(night.lastThird.isBefore(night.fajr))
    }
}
