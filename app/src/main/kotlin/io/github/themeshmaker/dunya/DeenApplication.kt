package io.github.themeshmaker.dunya

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.os.UserManagerCompat
import dagger.hilt.android.HiltAndroidApp
import io.github.themeshmaker.dunya.core.alarm.AlarmSync
import io.github.themeshmaker.dunya.core.alarm.AlertNotifications
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class DeenApplication : Application() {

    @Inject lateinit var alarmSync: AlarmSync

    @Inject lateinit var notifications: AlertNotifications

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()

        // The process can start before the first unlock (direct boot) to play Fajr. The user's
        // settings live in credential-encrypted storage, so syncing waits until unlock.
        if (UserManagerCompat.isUserUnlocked(this)) {
            alarmSync.start(appScope)
        } else {
            ContextCompat.registerReceiver(
                this,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        unregisterReceiver(this)
                        alarmSync.start(appScope)
                    }
                },
                IntentFilter(Intent.ACTION_USER_UNLOCKED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }
}
