package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.prayer.HijriCalendar
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPlannerTest {

    private val calculator = PrayerTimeCalculator(HijriCalendar())
    private val planner = AlertPlanner(calculator)
    private val riyadh = SavedLocation("Riyadh", null, "SA", Coordinates(24.6877, 46.7219), ZoneId.of("Asia/Riyadh"), LocationSource.MANUAL)
    private val settings = PrayerSettings(CalculationMethod.UMM_AL_QURA)
    private val date = LocalDate.of(2026, 10, 9)
    private val today = calculator.prayerDay(date, riyadh.coordinates, settings)
    private val tomorrow = calculator.prayerDay(date.plusDays(1), riyadh.coordinates, settings)

    @Test
    fun defaultsGiveAnAdhanForEachOfTheFivePrayersAndNothingAtSunrise() {
        val now = today[Prayer.FAJR].minusSeconds(60)
        val plan = planner.plan(now, riyadh, settings, AlertSettings())
        assertEquals(Prayer.prayers, plan.map { it.prayer })
        assertTrue(plan.all { it.kind == AlertKind.ADHAN })
        assertEquals(today[Prayer.FAJR], plan.first().at)
    }

    @Test
    fun passedPrayersMoveToTomorrow() {
        val now = today[Prayer.ASR].plusSeconds(1)
        val plan = planner.plan(now, riyadh, settings, AlertSettings()).associateBy { it.prayer }
        assertEquals(tomorrow[Prayer.FAJR], plan.getValue(Prayer.FAJR).at)
        assertEquals(tomorrow[Prayer.ASR], plan.getValue(Prayer.ASR).at)
        assertEquals(today[Prayer.MAGHRIB], plan.getValue(Prayer.MAGHRIB).at)
        assertTrue(plan.values.all { it.at.isAfter(now) })
    }

    @Test
    fun silentPrayersAreSkippedAndNotificationModeIsKept() {
        val alerts = AlertSettings(
            modes = AlertSettings.DEFAULT_MODES + mapOf(Prayer.DHUHR to AlertMode.SILENT, Prayer.ISHA to AlertMode.NOTIFICATION)
        )
        val plan = planner.plan(today[Prayer.FAJR].minusSeconds(60), riyadh, settings, alerts).associateBy { it.prayer }
        assertTrue(Prayer.DHUHR !in plan)
        assertEquals(AlertKind.NOTIFICATION, plan.getValue(Prayer.ISHA).kind)
    }

    @Test
    fun sunriseCanNotifyButNeverPlaysAnAdhan() {
        val alerts = AlertSettings(modes = AlertSettings.DEFAULT_MODES + (Prayer.SUNRISE to AlertMode.ADHAN))
        val sunrise = planner.plan(today[Prayer.FAJR], riyadh, settings, alerts).single { it.prayer == Prayer.SUNRISE }
        assertEquals(AlertKind.NOTIFICATION, sunrise.kind)
    }

    @Test
    fun remindersComeBeforeTheirPrayer() {
        val alerts = AlertSettings(reminderMinutes = 10)
        val now = today[Prayer.MAGHRIB].minus(Duration.ofMinutes(30))
        val reminder = planner.plan(now, riyadh, settings, alerts).single { it.kind == AlertKind.REMINDER && it.prayer == Prayer.MAGHRIB }
        assertEquals(today[Prayer.MAGHRIB].minus(Duration.ofMinutes(10)), reminder.at)
        assertEquals(today[Prayer.MAGHRIB], reminder.prayerAt)
    }

    @Test
    fun aReminderWhoseMomentPassedMovesToTomorrowEvenIfThePrayerIsStillAhead() {
        val alerts = AlertSettings(reminderMinutes = 10)
        val now = today[Prayer.MAGHRIB].minus(Duration.ofMinutes(5))
        val plan = planner.plan(now, riyadh, settings, alerts)
        val reminder = plan.single { it.kind == AlertKind.REMINDER && it.prayer == Prayer.MAGHRIB }
        val adhan = plan.single { it.kind == AlertKind.ADHAN && it.prayer == Prayer.MAGHRIB }
        assertEquals(today[Prayer.MAGHRIB], adhan.at)
        assertEquals(tomorrow[Prayer.MAGHRIB].minus(Duration.ofMinutes(10)), reminder.at)
    }

    @Test
    fun planIsSortedByTime() {
        val plan = planner.plan(today[Prayer.DHUHR], riyadh, settings, AlertSettings(reminderMinutes = 15))
        assertEquals(plan.sortedBy { it.at }, plan)
    }
}
