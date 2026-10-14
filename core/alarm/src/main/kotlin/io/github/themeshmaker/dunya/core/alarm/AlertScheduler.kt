/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.alarm.AlertIntents.putAlert
import io.github.themeshmaker.dunya.core.domain.AlertPlanner
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import io.github.themeshmaker.dunya.core.model.Prayer
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Turns the plan into AlarmManager alarms (docs/BLUEPRINT.md §9.1):
 * - adhan / prayer notification → `setAlarmClock` (exits Doze, not throttled, respected by OEMs);
 * - reminders (before the prayer, masjid) → `setExactAndAllowWhileIdle`;
 * - no exact-alarm permission → `setAndAllowWhileIdle` (may be late; the UI warns about it).
 *
 * Everything here reads only device-protected storage, so it also works before first unlock.
 */
@Singleton
class AlertScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val state: AlarmStateStore,
    private val planner: AlertPlanner,
    private val clock: Clock
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Recomputes the plan and replaces every scheduled alarm. Safe to call any number of times. */
    suspend fun rescheduleAll(reason: String) = mutex.withLock {
        val inputs = state.inputs()
        val plan = inputs?.let { planner.plan(clock.instant(), it.location, it.prayerSettings, it.alertSettings) }.orEmpty()
        val useAlarmClock = inputs?.alertSettings?.useAlarmClock ?: true

        val wanted = plan.map { AlertIntents.requestCode(it.prayer, it.kind) }.toSet()
        AlertIntents.allSlots
            .filter { (prayer, kind) -> kind != AlertKind.TEST && AlertIntents.requestCode(prayer, kind) !in wanted }
            .forEach { (prayer, kind) -> cancel(prayer, kind) }
        plan.forEach { schedule(it, useAlarmClock) }
        state.setPlanned(plan)
        Log.i(TAG, "Rescheduled ${plan.size} alerts ($reason), exact=${canScheduleExact()}")
    }

    /** "Test adhan in 1 minute": a real alarm through the same path as prayer alerts. */
    suspend fun scheduleTest(at: Instant) {
        schedule(PlannedAlert(Prayer.FAJR, AlertKind.TEST, at, at), useAlarmClock = true)
    }

    /** Alarms we planned that the system no longer holds (an OEM or the user cleared them). */
    suspend fun missingAlerts(): List<PlannedAlert> {
        val now = clock.instant()
        return state.planned().filter { it.at.isAfter(now) && pendingIntent(it, PendingIntent.FLAG_NO_CREATE) == null }
    }

    private fun schedule(alert: PlannedAlert, useAlarmClock: Boolean) {
        val operation = pendingIntent(alert, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val at = alert.at.toEpochMilli()
        try {
            when {
                canScheduleExact() && useAlarmClock && alert.kind in ALARM_CLOCK_KINDS ->
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showAppIntent()), operation)

                canScheduleExact() -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)

                else -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun cancel(prayer: Prayer, kind: AlertKind) {
        val placeholder = PlannedAlert(prayer, kind, Instant.EPOCH, Instant.EPOCH)
        pendingIntent(placeholder, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(alert: PlannedAlert, flags: Int): PendingIntent? {
        val intent = Intent(context, AlertReceiver::class.java).putAlert(alert)
        return PendingIntent.getBroadcast(
            context,
            AlertIntents.requestCode(alert.prayer, alert.kind),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Where the system's "next alarm" chip takes the user. */
    private fun showAppIntent(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, SHOW_APP_REQUEST_CODE, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val TAG = "AlertScheduler"
        const val SHOW_APP_REQUEST_CODE = 2000
        val ALARM_CLOCK_KINDS = setOf(AlertKind.ADHAN, AlertKind.NOTIFICATION, AlertKind.TEST)
    }
}
