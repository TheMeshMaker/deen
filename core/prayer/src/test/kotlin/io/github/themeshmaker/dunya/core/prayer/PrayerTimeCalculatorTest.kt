/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import java.time.Duration
import java.time.LocalDate
import java.time.chrono.HijrahDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimeCalculatorTest {

    private val calculator = PrayerTimeCalculator(HijriCalendar())
    private val makkah = Coordinates(21.4225, 39.8262)
    private val ummAlQura = PrayerSettings(method = CalculationMethod.UMM_AL_QURA)

    private fun ishaAfterMaghrib(date: LocalDate, settings: PrayerSettings): Long {
        val day = calculator.prayerDay(date, makkah, settings)
        return Duration.between(day[Prayer.MAGHRIB], day[Prayer.ISHA]).toMinutes()
    }

    @Test
    fun ummAlQuraIshaIs90MinutesOutsideRamadan() {
        val shaban = LocalDate.from(HijrahDate.of(1447, 8, 10))
        assertEquals(90, ishaAfterMaghrib(shaban, ummAlQura))
    }

    @Test
    fun ummAlQuraIshaIs120MinutesInRamadan() {
        val ramadan = LocalDate.from(HijrahDate.of(1447, 9, 10))
        assertEquals(120, ishaAfterMaghrib(ramadan, ummAlQura))
        assertEquals(90, ishaAfterMaghrib(ramadan, ummAlQura.copy(ramadanIshaExtension = false)))
    }

    @Test
    fun ramadanExtensionOnlyAppliesToUmmAlQura() {
        val ramadan = LocalDate.from(HijrahDate.of(1447, 9, 10))
        val qatar = PrayerSettings(method = CalculationMethod.QATAR) // also a 90-minute interval method
        assertEquals(90, ishaAfterMaghrib(ramadan, qatar))
    }

    @Test
    fun userOffsetsAreApplied() {
        val date = LocalDate.of(2026, 10, 9)
        val base = calculator.prayerDay(date, makkah, ummAlQura)
        val adjusted = calculator.prayerDay(date, makkah, ummAlQura.copy(offsetMinutes = mapOf(Prayer.FAJR to 2, Prayer.ISHA to -3)))
        assertEquals(2, Duration.between(base[Prayer.FAJR], adjusted[Prayer.FAJR]).toMinutes())
        assertEquals(-3, Duration.between(base[Prayer.ISHA], adjusted[Prayer.ISHA]).toMinutes())
        assertEquals(base[Prayer.DHUHR], adjusted[Prayer.DHUHR])
    }

    @Test
    fun timesAreInChronologicalOrderEvenFarNorth() {
        val oslo = Coordinates(59.9139, 10.7522)
        val settings = PrayerSettings(method = CalculationMethod.MUSLIM_WORLD_LEAGUE)
        var date = LocalDate.of(2026, 1, 1)
        repeat(366) {
            val day = calculator.prayerDay(date, oslo, settings)
            val times = Prayer.entries.map { day[it] }
            assertTrue("out of order on $date: $times", times.zipWithNext().all { (a, b) -> a.isBefore(b) })
            date = date.plusDays(1)
        }
    }
}
