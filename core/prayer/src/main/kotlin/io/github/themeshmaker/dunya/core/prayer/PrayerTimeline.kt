package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerDay
import java.time.Instant

/** The prayer that comes next, and the one whose time we are currently in. */
data class NextPrayer(val prayer: Prayer, val at: Instant, val previous: Prayer, val previousAt: Instant) {
    /** 0..1: how far we are from [previousAt] to [at]. */
    fun progress(now: Instant): Float {
        val total = (at.toEpochMilli() - previousAt.toEpochMilli()).toFloat()
        if (total <= 0f) return 1f
        return ((now.toEpochMilli() - previousAt.toEpochMilli()) / total).coerceIn(0f, 1f)
    }
}

/**
 * Three consecutive days so "next" and "previous" work across midnight: before Fajr the
 * previous prayer is yesterday's Isha; after Isha the next one is tomorrow's Fajr.
 * Sunrise is skipped; it is not a prayer.
 */
data class PrayerTimeline(val yesterday: PrayerDay, val today: PrayerDay, val tomorrow: PrayerDay) {

    fun next(now: Instant): NextPrayer {
        val events = listOf(yesterday, today, tomorrow).flatMap { day ->
            Prayer.prayers.map { prayer -> prayer to day[prayer] }
        }
        val nextIndex = events.indexOfFirst { (_, at) -> at.isAfter(now) }
        // Only possible after tomorrow's Isha, i.e. the timeline is stale; pin to the last event.
        val index = if (nextIndex <= 0) events.lastIndex else nextIndex
        val (prayer, at) = events[index]
        val (previous, previousAt) = events[index - 1]
        return NextPrayer(prayer, at, previous, previousAt)
    }
}
