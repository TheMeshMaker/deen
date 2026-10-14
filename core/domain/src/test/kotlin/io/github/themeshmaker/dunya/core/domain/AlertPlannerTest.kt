/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.MasjidReminder
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

    @Test
    fun afterAdhanRemindersFollowEachPrayerAndAreOffByDefault() {
        val now = today[Prayer.DHUHR].minus(Duration.ofMinutes(30))
        assertTrue(planner.plan(now, riyadh, settings, AlertSettings()).none { it.kind == AlertKind.AFTER_ADHAN })

        val alerts = AlertSettings(afterAdhanMinutes = mapOf(Prayer.DHUHR to 15, Prayer.SUNRISE to 10))
        val after = planner.plan(now, riyadh, settings, alerts).filter { it.kind == AlertKind.AFTER_ADHAN }
        val dhuhr = after.single()
        assertEquals(Prayer.DHUHR, dhuhr.prayer)
        assertEquals(today[Prayer.DHUHR].plus(Duration.ofMinutes(15)), dhuhr.at)
        assertEquals(today[Prayer.DHUHR], dhuhr.prayerAt)

        // Ten minutes after the adhan, the reminder at +15 is still today's.
        val later = planner.plan(today[Prayer.DHUHR].plus(Duration.ofMinutes(10)), riyadh, settings, alerts)
        assertEquals(today[Prayer.DHUHR].plus(Duration.ofMinutes(15)), later.single { it.kind == AlertKind.AFTER_ADHAN }.at)
    }

    @Test
    fun masjidRemindersAreOffByDefault() {
        val plan = planner.plan(today[Prayer.FAJR].minusSeconds(3_600), riyadh, settings, AlertSettings())
        assertTrue(plan.none { it.kind == AlertKind.MASJID })
    }

    @Test
    fun masjidRemindersComeTheChosenMinutesBeforeTheChosenPrayersEvenIfSilent() {
        val alerts = AlertSettings(
            modes = AlertSettings.DEFAULT_MODES + (Prayer.ISHA to AlertMode.SILENT),
            masjid = MasjidReminder(enabled = true, minutesBefore = 25, prayers = setOf(Prayer.MAGHRIB, Prayer.ISHA))
        )
        val now = today[Prayer.ASR].plusSeconds(1)
        val masjid = planner.plan(now, riyadh, settings, alerts).filter { it.kind == AlertKind.MASJID }.associateBy { it.prayer }
        assertEquals(setOf(Prayer.MAGHRIB, Prayer.ISHA), masjid.keys)
        assertEquals(today[Prayer.MAGHRIB].minus(Duration.ofMinutes(25)), masjid.getValue(Prayer.MAGHRIB).at)
        assertEquals(today[Prayer.ISHA], masjid.getValue(Prayer.ISHA).prayerAt)
    }

    @Test
    fun aMasjidReminderAlreadyDueMovesToTheNextDay() {
        val alerts = AlertSettings(masjid = MasjidReminder(enabled = true, minutesBefore = 30, prayers = setOf(Prayer.DHUHR)))
        // 10 minutes before Dhuhr: today's 30-minute reminder is already past.
        val now = today[Prayer.DHUHR].minus(Duration.ofMinutes(10))
        val reminder = planner.plan(now, riyadh, settings, alerts).single { it.kind == AlertKind.MASJID }
        assertEquals(tomorrow[Prayer.DHUHR].minus(Duration.ofMinutes(30)), reminder.at)
    }

    @Test
    fun theKahfReminderIsAnHourAfterFridaySunrise() {
        // 9 October 2026 is a Friday.
        val alerts = AlertSettings(kahfReminder = true)
        val before = planner.plan(today[Prayer.FAJR], riyadh, settings, alerts).single { it.kind == AlertKind.KAHF }
        assertEquals(today[Prayer.SUNRISE].plus(Duration.ofHours(1)), before.at)

        val nextFriday = calculator.prayerDay(date.plusDays(7), riyadh.coordinates, settings)
        val after = planner.plan(today[Prayer.DHUHR], riyadh, settings, alerts).single { it.kind == AlertKind.KAHF }
        assertEquals(nextFriday[Prayer.SUNRISE].plus(Duration.ofHours(1)), after.at)

        assertTrue(planner.plan(today[Prayer.FAJR], riyadh, settings, AlertSettings()).none { it.kind == AlertKind.KAHF })
    }
}
