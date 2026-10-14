/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.HijriDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriCalendarTest {

    private val calendar = HijriCalendar()

    /** Pairs from the official Umm al-Qura calendar (ummulqura.org.sa), via the Makkah fixture. */
    @Test
    fun matchesOfficialUmmAlQuraDates() {
        val official = mapOf(
            "2016-01-05" to HijriDate(1437, 3, 25),
            "2016-02-04" to HijriDate(1437, 4, 25),
            "2016-03-05" to HijriDate(1437, 5, 25),
            "2016-04-03" to HijriDate(1437, 6, 25),
            "2016-05-02" to HijriDate(1437, 7, 25),
            "2016-06-04" to HijriDate(1437, 8, 28),
            "2016-07-08" to HijriDate(1437, 10, 3),
            "2016-08-02" to HijriDate(1437, 10, 28),
            "2016-09-04" to HijriDate(1437, 12, 3),
            "2016-10-06" to HijriDate(1438, 1, 5),
            "2016-11-05" to HijriDate(1438, 2, 5),
            "2016-12-04" to HijriDate(1438, 3, 5)
        )
        official.forEach { (gregorian, hijri) ->
            assertEquals(gregorian, hijri, calendar.toHijri(LocalDate.parse(gregorian)))
        }
    }

    @Test
    fun offsetShiftsTheHijriDay() {
        val date = LocalDate.parse("2016-01-05")
        assertEquals(HijriDate(1437, 3, 26), calendar.toHijri(date, offsetDays = 1))
        assertEquals(HijriDate(1437, 3, 24), calendar.toHijri(date, offsetDays = -1))
    }

    @Test
    fun monthDatesCoverExactlyThatHijriMonth() {
        listOf(0, 1, -1).forEach { offset ->
            val dates = calendar.monthDates(1448, 9, offset)
            assertTrue(dates.size in 29..30)
            dates.forEachIndexed { i, date ->
                assertEquals(HijriDate(1448, 9, i + 1), calendar.toHijri(date, offset))
            }
            assertEquals(1448 to 8, calendar.toHijri(dates.first().minusDays(1), offset).let { it.year to it.month })
            assertEquals(1448 to 10, calendar.toHijri(dates.last().plusDays(1), offset).let { it.year to it.month })
        }
    }
}
