package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.HijriDate
import io.github.themeshmaker.dunya.core.model.IslamicEvent
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * One coming (or current) occurrence of [event], from [date] to [endDate]. [daysAway] is 0 while
 * it is [ongoing].
 */
data class UpcomingEvent(val event: IslamicEvent, val date: LocalDate, val endDate: LocalDate, val hijri: HijriDate, val daysAway: Long) {
    val ongoing: Boolean get() = daysAway == 0L
}

object IslamicEvents {
    /**
     * The next occurrence of each event that hasn't ended by [today], soonest first, using the same
     * moon-sighting correction as the Hijri date shown in the app.
     */
    fun upcoming(today: LocalDate, offsetDays: Int = 0): List<UpcomingEvent> {
        val shifted = HijrahDate.from(today.plusDays(offsetDays.toLong()))
        val year = shifted.get(ChronoField.YEAR)
        val month = shifted.get(ChronoField.MONTH_OF_YEAR)
        return IslamicEvent.entries.mapNotNull { event ->
            // This Hijri year and the next cover every yearly event; the next 13 months cover the white days.
            val candidates = if (event == IslamicEvent.WHITE_DAYS) {
                (0..13).map { step -> addMonths(year, month, step) }
            } else {
                listOf(year to event.month, year + 1 to event.month)
            }
            candidates.firstNotNullOfOrNull { (y, m) -> occurrence(event, y, m, today, offsetDays) }
        }.sortedWith(compareBy({ it.date }, { it.event.ordinal }))
    }

    /** The one event to feature: the soonest, preferring a yearly day to the monthly white days. */
    fun next(today: LocalDate, offsetDays: Int = 0): UpcomingEvent? {
        val all = upcoming(today, offsetDays)
        val yearly = all.firstOrNull { it.event != IslamicEvent.WHITE_DAYS }
        val white = all.firstOrNull { it.event == IslamicEvent.WHITE_DAYS }
        return when {
            yearly == null -> white
            white != null && (white.ongoing || white.daysAway < 3) && !yearly.ongoing && yearly.daysAway > 7 -> white
            else -> yearly
        }
    }

    private fun occurrence(event: IslamicEvent, year: Int, month: Int, today: LocalDate, offsetDays: Int): UpcomingEvent? {
        val start = runCatching { HijrahDate.of(year, month, event.day) }.getOrNull() ?: return null
        val length = if (event.days == 0) start.lengthOfMonth() else event.days
        val date = LocalDate.from(start).minusDays(offsetDays.toLong())
        val end = date.plusDays(length - 1L)
        if (end.isBefore(today)) return null
        val away = ChronoUnit.DAYS.between(today, date).coerceAtLeast(0)
        return UpcomingEvent(event, date, end, HijriDate(year, month, event.day), away)
    }

    private fun addMonths(year: Int, month: Int, step: Int): Pair<Int, Int> {
        val index = (month - 1) + step
        return (year + index / 12) to (index % 12 + 1)
    }
}
