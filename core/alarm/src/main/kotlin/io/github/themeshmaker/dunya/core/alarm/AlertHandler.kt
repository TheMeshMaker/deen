/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.alarm.AlertIntents.readAlert
import io.github.themeshmaker.dunya.core.model.AdhanVoice
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertLogEntry
import io.github.themeshmaker.dunya.core.model.AlertOutcome
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** What happens when an alert alarm fires (docs/BLUEPRINT.md §9.2). */
class AlertHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val state: AlarmStateStore,
    private val scheduler: AlertScheduler,
    private val notifications: AlertNotifications,
    private val clock: Clock
) {
    suspend fun onAlarm(intent: Intent) {
        val alert = intent.readAlert() ?: return
        val firedAt = clock.instant()
        val inputs = state.inputs()
        val zone = inputs?.location?.zoneId ?: ZoneId.systemDefault()
        val ahead = alert.kind == AlertKind.REMINDER || alert.kind == AlertKind.MASJID
        val after = alert.kind == AlertKind.AFTER_ADHAN
        val kahf = alert.kind == AlertKind.KAHF
        val late = alert.kind != AlertKind.TEST && Duration.between(alert.at, firedAt) > MAX_LATENESS
        val settings = inputs?.alertSettings
        try {
            when {
                // Not tied to a minute: any time on Friday is fine, so it is never "late".
                kahf -> {
                    notifications.showKahf()
                    log(alert, firedAt, AlertOutcome.REMINDED)
                }

                // A heads-up that arrives after the prayer has started is useless: drop it.
                ahead && !firedAt.isBefore(alert.prayerAt) -> log(alert, firedAt, AlertOutcome.LATE)

                // "Have you prayed?" long after the moment would only nag: drop it.
                after && late -> log(alert, firedAt, AlertOutcome.LATE)

                // An adhan 20 minutes late would mislead; say what the time was, quietly.
                late && !ahead && !after -> {
                    notifications.showPrayerTime(alert, zone, silent = true)
                    log(alert, firedAt, AlertOutcome.LATE)
                }

                alert.kind == AlertKind.ADHAN || alert.kind == AlertKind.TEST -> startAdhan(
                    alert,
                    zone,
                    respectSilent = settings?.respectSilentMode ?: true,
                    // The test alarm is filed under Fajr but should sound like the everyday adhan.
                    voice =
                        settings?.let { if (alert.kind == AlertKind.TEST) it.voice else it.voiceFor(alert.prayer) } ?: AdhanVoice.DEFAULT,
                    firedAt = firedAt
                )

                alert.kind == AlertKind.NOTIFICATION -> {
                    notifications.showPrayerTime(alert, zone, silent = false)
                    log(alert, firedAt, AlertOutcome.NOTIFIED)
                }

                alert.kind == AlertKind.REMINDER -> {
                    notifications.showReminder(alert, zone)
                    log(alert, firedAt, AlertOutcome.REMINDED)
                }

                alert.kind == AlertKind.MASJID -> {
                    notifications.showMasjid(alert, zone)
                    log(alert, firedAt, AlertOutcome.REMINDED)
                }

                after -> {
                    notifications.showAfterAdhan(alert, zone)
                    log(alert, firedAt, AlertOutcome.REMINDED)
                }
            }
        } finally {
            if (alert.kind != AlertKind.TEST) scheduler.rescheduleAll("fired ${alert.prayer} ${alert.kind}")
        }
    }

    private suspend fun startAdhan(alert: PlannedAlert, zone: ZoneId, respectSilent: Boolean, voice: String, firedAt: Instant) {
        try {
            // Allowed from the background because this broadcast came from an exact alarm.
            ContextCompat.startForegroundService(context, AdhanService.intent(context, alert, zone, respectSilent, voice, firedAt))
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (e.g. the alarm was inexact): notify instead.
            Log.w(TAG, "Adhan service not allowed to start", e)
            notifications.showPrayerTime(alert, zone, silent = false)
            log(alert, firedAt, AlertOutcome.FALLBACK_NOTIFICATION)
        }
    }

    private suspend fun log(alert: PlannedAlert, firedAt: Instant, outcome: AlertOutcome) {
        state.appendLog(AlertLogEntry(alert.prayer.takeIf { alert.kind != AlertKind.TEST }, alert.kind, alert.at, firedAt, outcome))
    }

    private companion object {
        const val TAG = "AlertHandler"
        val MAX_LATENESS: Duration = Duration.ofMinutes(10)
    }
}
