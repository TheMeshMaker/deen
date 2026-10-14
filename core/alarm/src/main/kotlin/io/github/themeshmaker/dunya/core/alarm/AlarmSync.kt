/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.datastore.toPreferences
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AlertLogEntry
import io.github.themeshmaker.dunya.core.model.AlertOutcome
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the alarms in step with the user's settings: copies them into device-protected storage
 * (for direct boot) and re-plans whenever they change. Started by the Application once the
 * user has unlocked the phone, because the user's settings live in credential-encrypted storage.
 */
@Singleton
class AlarmSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userData: UserDataRepository,
    private val state: AlarmStateStore,
    private val scheduler: AlertScheduler,
    private val clock: Clock
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            repairMissing(state, scheduler, clock)
            userData.userData
                .map { data -> if (data.onboardingComplete) Triple(data.location, data.prayerSettings, data.alertSettings) else null }
                .distinctUntilChanged()
                .collect { inputs ->
                    state.setInputs(
                        location = inputs?.first?.toPreferences(),
                        prayer = inputs?.second?.toPreferences(),
                        alerts = inputs?.third?.toPreferences()
                    )
                    scheduler.rescheduleAll("settings")
                }
        }
        AlarmGuardianWorker.enqueue(context)
    }
}

/**
 * Logs planned alarms that never arrived, so the reliability screen can say so, then re-plans:
 * - past ones with no log entry (the phone dropped them: force-stop, OEM cleaner, …);
 * - future ones the system no longer holds.
 */
internal suspend fun repairMissing(state: AlarmStateStore, scheduler: AlertScheduler, clock: Clock) {
    val now = clock.instant()
    val logged = state.log.first().map { it.prayer to it.scheduledAt }.toSet()
    val missed = state.planned().filter { alert ->
        alert.at.isBefore(now.minus(MISSED_GRACE)) && (alert.prayer to alert.at) !in logged
    }
    val missing = missed + scheduler.missingAlerts()
    if (missing.isEmpty()) return
    missing.forEach { state.appendLog(AlertLogEntry(it.prayer, it.kind, it.at, now, AlertOutcome.CLEARED_BY_SYSTEM)) }
    scheduler.rescheduleAll("repair ${missing.size} missing")
}

/** An alarm may run a little late; only call it missed after this. */
private val MISSED_GRACE: Duration = Duration.ofMinutes(15)

/** Every 6 hours: check that our alarms still exist and put back any that were cleared. */
class AlarmGuardianWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun state(): AlarmStateStore
        fun scheduler(): AlertScheduler
        fun clock(): Clock
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        repairMissing(deps.state(), deps.scheduler(), deps.clock())
        deps.scheduler().rescheduleAll("guardian")
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "alarm-guardian"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AlarmGuardianWorker>(6, TimeUnit.HOURS).build()
            )
        }
    }
}
