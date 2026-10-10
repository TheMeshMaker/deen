package io.github.themeshmaker.dunya.core.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.themeshmaker.dunya.core.alarm.AlertIntents.putAlert
import io.github.themeshmaker.dunya.core.alarm.AlertIntents.readAlert
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertLogEntry
import io.github.themeshmaker.dunya.core.model.AlertOutcome
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Plays the adhan in the foreground (`mediaPlayback`) on the **alarm** stream: since Android 17
 * that is the only background audio allowed without while-in-use status, and our alarm-started
 * service never has it (docs/BLUEPRINT.md §9.1, §9.4).
 *
 * Stops on: end of audio, the notification's Stop action or swipe, the power button, or a
 * safety timeout.
 */
@AndroidEntryPoint
class AdhanService : Service() {

    @Inject lateinit var state: AlarmStateStore

    @Inject lateinit var notifications: AlertNotifications

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish(AlertOutcome.PLAYED) }

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var screenOffRegistered = false

    private var current: PlannedAlert? = null
    private var currentZone: ZoneId = ZoneId.systemDefault()
    private var firedAt: Instant = Instant.EPOCH

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish(AlertOutcome.PLAYED)
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish(AlertOutcome.PLAYED)
            return START_NOT_STICKY
        }
        val alert = intent?.readAlert()
        if (alert == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (current != null) finish(AlertOutcome.PLAYED, stopService = false) // a new alert replaces one still playing

        current = alert
        currentZone = intent.getStringExtra(EXTRA_ZONE)?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        firedAt = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_FIRED_AT, System.currentTimeMillis()))

        // Must be first: the system only allows a few seconds between start and startForeground.
        ServiceCompat.startForeground(
            this,
            AlertNotifications.ADHAN_NOTIFICATION_ID,
            notifications.adhanPlaying(alert, currentZone, stopIntent()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )

        val audio = getSystemService(AudioManager::class.java)
        val respectSilent = intent.getBooleanExtra(EXTRA_RESPECT_SILENT, true)
        when {
            audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION ->
                finish(AlertOutcome.SKIPPED_IN_CALL)

            respectSilent && audio.ringerMode != AudioManager.RINGER_MODE_NORMAL && alert.kind != AlertKind.TEST -> {
                vibrate()
                finish(AlertOutcome.VIBRATED_SILENT_MODE)
            }

            else -> play(audio)
        }
        return START_NOT_STICKY
    }

    private fun play(audio: AudioManager) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        // On Android 17 this may be refused for background apps; the alarm stream still plays.
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).build()
            .also { audio.requestAudioFocus(it) }
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(applicationContext, Uri.parse("android.resource://$packageName/${R.raw.adhan_default}"))
                setOnCompletionListener { finish(AlertOutcome.PLAYED) }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error $what/$extra")
                    finish(AlertOutcome.FALLBACK_NOTIFICATION)
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not play adhan", e)
            finish(AlertOutcome.FALLBACK_NOTIFICATION)
            return
        }
        ContextCompat.registerReceiver(this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        screenOffRegistered = true
        mainHandler.postDelayed(timeout, MAX_PLAY_MS)
    }

    private fun finish(outcome: AlertOutcome, stopService: Boolean = true) {
        val alert = current ?: run {
            if (stopService) stopSelf()
            return
        }
        current = null
        mainHandler.removeCallbacks(timeout)
        if (screenOffRegistered) {
            runCatching { unregisterReceiver(screenOffReceiver) }
            screenOffRegistered = false
        }
        player?.runCatching {
            stop()
            release()
        }
        player = null
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusRequest = null

        if (stopService) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (alert.kind != AlertKind.TEST) {
            // A calm, dismissible "It's time for …" stays after the adhan (silent: the adhan already sounded).
            notifications.showPrayerTime(alert, currentZone, silent = outcome == AlertOutcome.PLAYED)
        }
        val entry = AlertLogEntry(alert.prayer.takeIf { alert.kind != AlertKind.TEST }, alert.kind, alert.at, firedAt, outcome)
        scope.launch {
            state.appendLog(entry)
            if (stopService && current == null) stopSelf()
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 250, 400, 250, 700), -1))
    }

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this,
        STOP_REQUEST_CODE,
        Intent(this, AdhanService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    override fun onDestroy() {
        if (current != null) finish(AlertOutcome.PLAYED, stopService = false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AdhanService"
        private const val ACTION_STOP = "io.github.themeshmaker.dunya.action.STOP_ADHAN"
        private const val EXTRA_ZONE = "deen.zone"
        private const val EXTRA_RESPECT_SILENT = "deen.respectSilent"
        private const val EXTRA_FIRED_AT = "deen.firedAt"
        private const val STOP_REQUEST_CODE = 3000

        /** Safety net in case the audio never reports completion. */
        private const val MAX_PLAY_MS = 6 * 60 * 1000L

        internal fun intent(context: Context, alert: PlannedAlert, zone: ZoneId, respectSilent: Boolean, firedAt: Instant): Intent =
            Intent(context, AdhanService::class.java)
                .putAlert(alert)
                .putExtra(EXTRA_ZONE, zone.id)
                .putExtra(EXTRA_RESPECT_SILENT, respectSilent)
                .putExtra(EXTRA_FIRED_AT, firedAt.toEpochMilli())
    }
}
