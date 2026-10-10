package io.github.themeshmaker.dunya.core.model

import java.time.Instant

/** What happens at a prayer's time. Sunrise can only notify (an adhan at sunrise would be wrong). */
enum class AlertMode {
    ADHAN,
    NOTIFICATION,
    SILENT
}

data class AlertSettings(
    val modes: Map<Prayer, AlertMode> = DEFAULT_MODES,
    /** A quiet heads-up this many minutes before each prayer that has an alert; 0 = off. */
    val reminderMinutes: Int = 0,
    /** When the phone is on silent/vibrate, vibrate instead of playing the adhan. */
    val respectSilentMode: Boolean = true,
    /** Use Android's alarm-clock alarms for prayer alerts (most reliable; shows an alarm icon). */
    val useAlarmClock: Boolean = true
) {
    fun modeFor(prayer: Prayer): AlertMode {
        val mode = modes[prayer] ?: DEFAULT_MODES.getValue(prayer)
        return if (prayer == Prayer.SUNRISE && mode == AlertMode.ADHAN) AlertMode.NOTIFICATION else mode
    }

    companion object {
        val DEFAULT_MODES: Map<Prayer, AlertMode> =
            Prayer.entries.associateWith { if (it == Prayer.SUNRISE) AlertMode.SILENT else AlertMode.ADHAN }
    }
}

enum class AlertKind {
    ADHAN,
    NOTIFICATION,

    /** "Maghrib in 10 minutes". */
    REMINDER,

    /** "Test adhan in 1 minute" from the reliability screen. */
    TEST
}

/** One alarm the system should fire. [prayerAt] differs from [at] only for reminders. */
data class PlannedAlert(val prayer: Prayer, val kind: AlertKind, val at: Instant, val prayerAt: Instant)

enum class AlertOutcome {
    PLAYED,
    NOTIFIED,
    REMINDED,

    /** Phone was on silent and the user asked us to respect that. */
    VIBRATED_SILENT_MODE,
    SKIPPED_IN_CALL,

    /** Delivered too late to be useful (the OEM or Doze held it back). */
    LATE,

    /** The guardian found the alarm missing: something cleared the app's alarms. */
    CLEARED_BY_SYSTEM,

    /** Adhan service could not start (e.g. background start blocked); a notification was shown instead. */
    FALLBACK_NOTIFICATION
}

/** One line of the alarm health log shown on the reliability screen. */
data class AlertLogEntry(
    val prayer: Prayer?,
    val kind: AlertKind,
    val scheduledAt: Instant,
    val firedAt: Instant,
    val outcome: AlertOutcome
)
