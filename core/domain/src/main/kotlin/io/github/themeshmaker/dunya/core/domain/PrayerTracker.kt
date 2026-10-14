/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.Prayer
import java.time.LocalDate
import java.time.YearMonth

/** Summaries of the prayers the user has ticked off. Everything stays on the phone. */
object PrayerTracker {
    /** The five prayers that count (sunrise is not a prayer). */
    val prayers: List<Prayer> = Prayer.prayers

    /**
     * Days in a row with all five prayers, ending today, or yesterday while today is still in
     * progress (so the streak doesn't drop to zero every morning).
     */
    fun streak(prayed: Map<LocalDate, Set<Prayer>>, today: LocalDate): Int {
        var day = if (complete(prayed, today)) today else today.minusDays(1)
        var count = 0
        while (complete(prayed, day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** The longest run of complete days ever recorded. */
    fun bestStreak(prayed: Map<LocalDate, Set<Prayer>>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        prayed.keys.filter { complete(prayed, it) }.sorted().forEach { day ->
            run = if (previous == day.minusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    fun complete(prayed: Map<LocalDate, Set<Prayer>>, day: LocalDate): Boolean = count(prayed, day) == prayers.size

    fun count(prayed: Map<LocalDate, Set<Prayer>>, day: LocalDate): Int = prayed[day].orEmpty().count { it.isPrayer }

    /** Prayers ticked off in [month] up to [today], out of how many were due. */
    fun monthTotals(prayed: Map<LocalDate, Set<Prayer>>, month: YearMonth, today: LocalDate): Pair<Int, Int> {
        val last = if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
        if (month.isAfter(YearMonth.from(today))) return 0 to 0
        val days = (1..last).map { month.atDay(it) }
        return days.sumOf { count(prayed, it) } to days.size * prayers.size
    }

    /** Keeps about a year and a half of history, so the file stays small. */
    fun trimmed(prayed: Map<LocalDate, Set<Prayer>>, today: LocalDate): Map<LocalDate, Set<Prayer>> {
        val oldest = today.minusDays(KEEP_DAYS)
        return prayed.filter { (day, set) -> !day.isBefore(oldest) && set.isNotEmpty() }
    }

    private const val KEEP_DAYS = 550L
}
