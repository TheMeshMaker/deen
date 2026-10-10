package io.github.themeshmaker.dunya.core.alarm

import android.content.Intent
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import io.github.themeshmaker.dunya.core.model.Prayer
import java.time.Instant

/** How a [PlannedAlert] travels inside alarm and service intents. */
internal object AlertIntents {
    private const val EXTRA_PRAYER = "deen.prayer"
    private const val EXTRA_KIND = "deen.kind"
    private const val EXTRA_AT = "deen.at"
    private const val EXTRA_PRAYER_AT = "deen.prayerAt"

    fun Intent.putAlert(alert: PlannedAlert): Intent = apply {
        putExtra(EXTRA_PRAYER, alert.prayer.name)
        putExtra(EXTRA_KIND, alert.kind.name)
        putExtra(EXTRA_AT, alert.at.toEpochMilli())
        putExtra(EXTRA_PRAYER_AT, alert.prayerAt.toEpochMilli())
    }

    fun Intent.readAlert(): PlannedAlert? {
        val prayer = getStringExtra(EXTRA_PRAYER)?.let { name -> Prayer.entries.firstOrNull { it.name == name } } ?: return null
        val kind = getStringExtra(EXTRA_KIND)?.let { name -> AlertKind.entries.firstOrNull { it.name == name } } ?: return null
        val at = getLongExtra(EXTRA_AT, -1).takeIf { it > 0 } ?: return null
        val prayerAt = getLongExtra(EXTRA_PRAYER_AT, at)
        return PlannedAlert(prayer, kind, Instant.ofEpochMilli(at), Instant.ofEpochMilli(prayerAt))
    }

    /**
     * Fixed PendingIntent request codes, one "slot" per prayer and kind, so re-planning replaces
     * alarms instead of piling them up. Adhan and plain notification share a slot (one or the other).
     */
    fun requestCode(prayer: Prayer, kind: AlertKind): Int = when (kind) {
        AlertKind.ADHAN, AlertKind.NOTIFICATION -> MAIN_BASE + prayer.ordinal
        AlertKind.REMINDER -> REMINDER_BASE + prayer.ordinal
        AlertKind.TEST -> TEST_CODE
    }

    val allSlots: List<Pair<Prayer, AlertKind>> =
        Prayer.entries.flatMap { listOf(it to AlertKind.ADHAN, it to AlertKind.REMINDER) } + (Prayer.FAJR to AlertKind.TEST)

    private const val MAIN_BASE = 1000
    private const val REMINDER_BASE = 1100
    private const val TEST_CODE = 1999
}
