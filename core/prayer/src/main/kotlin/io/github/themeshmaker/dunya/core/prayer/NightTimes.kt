package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.Prayer
import java.time.Duration
import java.time.Instant

/**
 * The night between Maghrib and Fajr: its middle (the end of ʿIshāʾ's preferred time) and the start
 * of its last third (the best time for qiyām, when Allah descends to the lowest heaven).
 */
data class NightTimes(val maghrib: Instant, val fajr: Instant, val midnight: Instant, val lastThird: Instant) {
    companion object {
        /** Tonight, or the night we are in if it is before today's Fajr. */
        fun of(timeline: PrayerTimeline, now: Instant): NightTimes {
            val (start, end) = if (now.isBefore(timeline.today[Prayer.FAJR])) {
                timeline.yesterday[Prayer.MAGHRIB] to timeline.today[Prayer.FAJR]
            } else {
                timeline.today[Prayer.MAGHRIB] to timeline.tomorrow[Prayer.FAJR]
            }
            val length = Duration.between(start, end)
            return NightTimes(start, end, start.plus(length.dividedBy(2)), start.plus(length.multipliedBy(2).dividedBy(3)))
        }
    }
}
