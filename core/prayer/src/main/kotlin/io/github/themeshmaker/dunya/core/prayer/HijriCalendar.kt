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
}
