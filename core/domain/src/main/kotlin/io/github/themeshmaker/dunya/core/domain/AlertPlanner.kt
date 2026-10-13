package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Decides which alarms the system should hold right now: the *next* occurrence of every enabled
 * alert (docs/BLUEPRINT.md §9.3). After each alarm fires, the plan is recomputed, so at most
 * 6 alerts + 5 reminders + 5 masjid reminders + 5 after-adhan reminders + the Friday Al-Kahf reminder
 * are ever scheduled.
 */
class AlertPlanner @Inject constructor(private val calculator: PrayerTimeCalculator) {

    fun plan(now: Instant, location: SavedLocation, prayerSettings: PrayerSettings, alerts: AlertSettings): List<PlannedAlert> {
        val today = now.atZone(location.zoneId).toLocalDate()
        // Three days cover every case, including after Isha and around midnight.
        val days = (0L..2L).map { calculator.prayerDay(today.plusDays(it), location.coordinates, prayerSettings) }

        return buildList {
            for (prayer in Prayer.entries) {
                val mode = alerts.modeFor(prayer)
                val times = days.map { it[prayer] }

                fun addAhead(kind: AlertKind, minutes: Int) {
                    val lead = Duration.ofMinutes(minutes.toLong())
                    times.firstOrNull { it.minus(lead).isAfter(now) }?.let { at -> add(PlannedAlert(prayer, kind, at.minus(lead), at)) }
                }

                if (mode != AlertMode.SILENT) {
                    times.firstOrNull { it.isAfter(now) }?.let { at ->
                        val kind = if (mode == AlertMode.ADHAN) AlertKind.ADHAN else AlertKind.NOTIFICATION
                        add(PlannedAlert(prayer, kind, at, at))
                    }
                    if (alerts.reminderMinutes > 0 && prayer.isPrayer) addAhead(AlertKind.REMINDER, alerts.reminderMinutes)
                }
                // Independent of the prayer's own alert: someone may keep Isha silent but still walk to the masjid.
                if (alerts.masjid.isOnFor(prayer)) addAhead(AlertKind.MASJID, alerts.masjid.minutesBefore)
                // Also independent: a nudge to pray, whatever the adhan setting.
                val after = alerts.afterAdhanFor(prayer)
                if (after > 0) {
                    val delay = Duration.ofMinutes(after.toLong())
                    times.firstOrNull {
                        it.plus(delay).isAfter(now)
                    }?.let { at -> add(PlannedAlert(prayer, AlertKind.AFTER_ADHAN, at.plus(delay), at)) }
                }
            }
            if (alerts.kahfReminder) nextKahf(now, location, prayerSettings)?.let { add(it) }
        }.sortedBy { it.at }
    }

    /** Next Friday, an hour after sunrise (within the coming week, so always found). */
    private fun nextKahf(now: Instant, location: SavedLocation, prayerSettings: PrayerSettings): PlannedAlert? {
        val today = now.atZone(location.zoneId).toLocalDate()
        return (0L..7L).map { today.plusDays(it) }
            .filter { it.dayOfWeek == DayOfWeek.FRIDAY }
            .map { calculator.prayerDay(it, location.coordinates, prayerSettings)[Prayer.SUNRISE].plus(KAHF_AFTER_SUNRISE) }
            .firstOrNull { it.isAfter(now) }
            ?.let { PlannedAlert(Prayer.SUNRISE, AlertKind.KAHF, it, it) }
    }

    private companion object {
        val KAHF_AFTER_SUNRISE: Duration = Duration.ofHours(1)
    }
}
