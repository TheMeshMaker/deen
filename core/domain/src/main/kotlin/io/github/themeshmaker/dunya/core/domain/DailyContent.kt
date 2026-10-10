package io.github.themeshmaker.dunya.core.domain

import java.time.LocalDate

/**
 * Picks the "of the day" items: the same for everyone on a given date, and walking through the
 * whole list before repeating.
 */
object DailyContent {
    fun <T> pick(items: List<T>, date: LocalDate, salt: Int = 0): T? {
        if (items.isEmpty()) return null
        return items[Math.floorMod(date.toEpochDay() + salt, items.size.toLong()).toInt()]
    }

    /** Offsets so the daily hadith and the daily fact don't cycle in lockstep. */
    const val HADITH_SALT = 0
    const val FACT_SALT = 17
}
