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
    /** Optional "have you prayed?" reminder this many minutes after each prayer's adhan; missing or 0 = off. */
    val afterAdhanMinutes: Map<Prayer, Int> = emptyMap(),
    /** When the phone is on silent/vibrate, vibrate instead of playing the adhan. */
    val respectSilentMode: Boolean = true,
    /** Use Android's alarm-clock alarms for prayer alerts (most reliable; shows an alarm icon). */
    val useAlarmClock: Boolean = true,
    /** Adhan recording for every prayer; see [AdhanVoice]. */
    val voice: String = AdhanVoice.DEFAULT,
    /** A different recording for Fajr, or null to use [voice]. */
    val fajrVoice: String? = null,
    val masjid: MasjidReminder = MasjidReminder()
) {
    fun voiceFor(prayer: Prayer): String = if (prayer == Prayer.FAJR) fajrVoice ?: voice else voice

    fun afterAdhanFor(prayer: Prayer): Int = if (prayer.isPrayer) afterAdhanMinutes[prayer] ?: 0 else 0

    val afterAdhanOn: Boolean get() = Prayer.prayers.any { afterAdhanFor(it) > 0 }

    fun modeFor(prayer: Prayer): AlertMode {
        val mode = modes[prayer] ?: DEFAULT_MODES.getValue(prayer)
        return if (prayer == Prayer.SUNRISE && mode == AlertMode.ADHAN) AlertMode.NOTIFICATION else mode
    }

    companion object {
        val DEFAULT_MODES: Map<Prayer, AlertMode> =
            Prayer.entries.associateWith { if (it == Prayer.SUNRISE) AlertMode.SILENT else AlertMode.ADHAN }

        /** Choices for the after-adhan reminder (0 = off). */
        val AFTER_ADHAN_CHOICES: List<Int> = listOf(0, 5, 10, 15, 20)
        const val AFTER_ADHAN_DEFAULT = 10
    }
}

/**
 * Adhan recordings. Bundled ones are ids; [CUSTOM] is a file the user picked, copied into the app
 * (one slot for Fajr, one for the other prayers).
 */
object AdhanVoice {
    const val DEFAULT = "default"
    const val TRADITIONAL = "traditional"
    const val AAQIB = "aaqib"
    const val CUSTOM = "custom"

    val bundled: List<String> = listOf(DEFAULT, TRADITIONAL, AAQIB)
}

/**
 * "Time to head to the masjid": a reminder some minutes before each chosen prayer. A hidden
 * option (Settings → About, tap the version seven times).
 */
data class MasjidReminder(val enabled: Boolean = false, val minutesBefore: Int = 20, val prayers: Set<Prayer> = Prayer.prayers.toSet()) {
    fun isOnFor(prayer: Prayer): Boolean = enabled && prayer.isPrayer && prayer in prayers

    companion object {
        val MINUTES_RANGE: IntRange = 5..90
    }
}

enum class AlertKind {
    ADHAN,
    NOTIFICATION,

    /** "Maghrib in 10 minutes". */
    REMINDER,

    /** "Time to head to the masjid". */
    MASJID,

    /** "Have you prayed Dhuhr?" some minutes after the adhan. */
    AFTER_ADHAN,

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
