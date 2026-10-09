package io.github.themeshmaker.dunya.core.model

import java.time.Instant
import java.time.LocalDate

/** The five daily prayers plus sunrise, which ends Fajr time. Declared in chronological order. */
enum class Prayer {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA
    ;

    /** Sunrise is shown in the timetable but is not a prayer. */
    val isPrayer: Boolean get() = this != SUNRISE

    companion object {
        val prayers: List<Prayer> = entries.filter { it.isPrayer }
    }
}

/** All times for one calendar day at one place. */
data class PrayerDay(val date: LocalDate, val times: Map<Prayer, Instant>) {
    operator fun get(prayer: Prayer): Instant = times.getValue(prayer)
}

/** A date in the Umm al-Qura Hijri calendar. [month] is 1-based (9 = Ramadan). */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val isRamadan: Boolean get() = month == 9
}
