package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimelineTest {

    private val calculator = PrayerTimeCalculator(HijriCalendar())
    private val riyadh = Coordinates(24.6877, 46.7219)
    private val settings = PrayerSettings(method = CalculationMethod.UMM_AL_QURA)
    private val today = LocalDate.of(2026, 10, 9)

    private val timeline = PrayerTimeline(
        yesterday = calculator.prayerDay(today.minusDays(1), riyadh, settings),
        today = calculator.prayerDay(today, riyadh, settings),
        tomorrow = calculator.prayerDay(today.plusDays(1), riyadh, settings)
    )

    @Test
    fun beforeFajrThePreviousPrayerIsYesterdaysIsha() {
        val now = timeline.today[Prayer.FAJR].minusSeconds(60)
        val next = timeline.next(now)
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(timeline.today[Prayer.FAJR], next.at)
        assertEquals(Prayer.ISHA, next.previous)
        assertEquals(timeline.yesterday[Prayer.ISHA], next.previousAt)
    }

    @Test
    fun sunriseIsSkipped() {
        val now = timeline.today[Prayer.FAJR].plusSeconds(60)
        val next = timeline.next(now)
        assertEquals(Prayer.DHUHR, next.prayer)
        assertEquals(Prayer.FAJR, next.previous)
    }

    @Test
    fun atTheExactPrayerTimeTheNextOneIsShown() {
        val next = timeline.next(timeline.today[Prayer.ASR])
        assertEquals(Prayer.MAGHRIB, next.prayer)
        assertEquals(Prayer.ASR, next.previous)
    }

    @Test
    fun afterIshaTheNextPrayerIsTomorrowsFajr() {
        val now = timeline.today[Prayer.ISHA].plusSeconds(60)
        val next = timeline.next(now)
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(timeline.tomorrow[Prayer.FAJR], next.at)
    }

    @Test
    fun progressGoesFromZeroToOne() {
        val next = timeline.next(timeline.today[Prayer.DHUHR].plusSeconds(1))
        assertEquals(0f, next.progress(next.previousAt), 0.001f)
        assertEquals(1f, next.progress(next.at), 0.001f)
    }
}
