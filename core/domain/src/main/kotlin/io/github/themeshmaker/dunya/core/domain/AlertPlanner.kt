package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Decides which alarms the system should hold right now: the *next* occurrence of every enabled
 * alert (docs/BLUEPRINT.md §9.3). After each alarm fires, the plan is recomputed, so at most
 * 6 alerts + 5 reminders are ever scheduled.
 */
class AlertPlanner @Inject constructor(private val calculator: PrayerTimeCalculator) {

    fun plan(now: Instant, location: SavedLocation, prayerSettings: PrayerSettings, alerts: AlertSettings): List<PlannedAlert> {
        val today = now.atZone(location.zoneId).toLocalDate()
        // Three days cover every case, including after Isha and around midnight.
        val days = (0L..2L).map { calculator.prayerDay(today.plusDays(it), location.coordinates, prayerSettings) }

        return buildList {
            for (prayer in Prayer.entries) {
                val mode = alerts.modeFor(prayer)
                if (mode == AlertMode.SILENT) continue
                val times = days.map { it[prayer] }

                times.firstOrNull { it.isAfter(now) }?.let { at ->
                    val kind = if (mode == AlertMode.ADHAN) AlertKind.ADHAN else AlertKind.NOTIFICATION
                    add(PlannedAlert(prayer, kind, at, at))
                }
                if (alerts.reminderMinutes > 0 && prayer.isPrayer) {
                    val lead = Duration.ofMinutes(alerts.reminderMinutes.toLong())
                    times.firstOrNull { it.minus(lead).isAfter(now) }?.let { at ->
                        add(PlannedAlert(prayer, AlertKind.REMINDER, at.minus(lead), at))
                    }
                }
            }
        }.sortedBy { it.at }
    }
}
