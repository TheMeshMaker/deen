/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Receivers fetch dependencies through a Hilt entry point rather than @AndroidEntryPoint field
// injection: onReceive is abstract, so Kotlin can't make the super call Hilt needs.

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReceiverDependencies {
    fun alertHandler(): AlertHandler
    fun alertScheduler(): AlertScheduler
}

private fun Context.receiverDependencies(): ReceiverDependencies =
    EntryPointAccessors.fromApplication(applicationContext, ReceiverDependencies::class.java)

/** Runs [block] off the main thread while keeping the broadcast alive (goAsync). */
private fun BroadcastReceiver.handleAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Receives every prayer alert alarm. */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val handler = context.receiverDependencies().alertHandler()
        handleAsync { handler.onAlarm(intent) }
    }
}

/** Reboot, app update, clock / time-zone change, exact-alarm permission granted: re-plan. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = context.receiverDependencies().alertScheduler()
        handleAsync { scheduler.rescheduleAll(intent.action ?: "system event") }
    }
}
