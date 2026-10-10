package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.HijriDate
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import javax.inject.Inject

/**
 * Umm al-Qura Hijri calendar. java.time's default HijrahChronology is "Hijrah-umalqura",
 * the official Saudi calendar (valid for 1300–1600 AH, i.e. 1882–2174 CE).
 */
class HijriCalendar @Inject constructor() {
    /** @param offsetDays user correction for local moon sighting, usually -1..+1. */
    fun toHijri(date: LocalDate, offsetDays: Int = 0): HijriDate {
        val hijrah = HijrahDate.from(date.plusDays(offsetDays.toLong()))
        return HijriDate(
            year = hijrah.get(ChronoField.YEAR),
            month = hijrah.get(ChronoField.MONTH_OF_YEAR),
            day = hijrah.get(ChronoField.DAY_OF_MONTH)
        )
    }

    /**
     * The Gregorian dates that make up Hijri [month] of [year] (as shown with the same
     * [offsetDays] correction as [toHijri]).
     */
    fun monthDates(year: Int, month: Int, offsetDays: Int = 0): List<LocalDate> {
        val first = HijrahDate.of(year, month, 1)
        val start = LocalDate.from(first).minusDays(offsetDays.toLong())
        return (0 until first.lengthOfMonth()).map { start.plusDays(it.toLong()) }
    }
}
