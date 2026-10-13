package io.github.themeshmaker.dunya.core.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.DeenLinks
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.ui.labelRes
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/** Notification channels and every notification the alarm pipeline shows (docs/BLUEPRINT.md §9.5). */
@Singleton
class AlertNotifications @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val channels = listOf(
            // Sound comes from AdhanService (alarm stream), so the channel itself is silent.
            NotificationChannel(
                CHANNEL_ADHAN,
                context.getString(R.string.core_alarm_channel_adhan),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
            NotificationChannel(CHANNEL_PRAYER, context.getString(R.string.core_alarm_channel_prayer), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.core_alarm_channel_reminder),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(CHANNEL_MASJID, context.getString(R.string.core_alarm_channel_masjid), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(
                CHANNEL_AFTER,
                context.getString(R.string.core_alarm_channel_after),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_FRIDAY,
                context.getString(R.string.core_alarm_channel_friday),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(channels)
    }

    /** Ongoing notification while the adhan plays; its Stop action and swipe both silence it. */
    fun adhanPlaying(alert: PlannedAlert, zone: ZoneId, stopIntent: PendingIntent): Notification =
        NotificationCompat.Builder(context, CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_deen)
            .setContentTitle(title(alert, zone))
            .setContentText(
                if (alert.kind == AlertKind.TEST) {
                    context.getString(R.string.core_alarm_test_body)
                } else {
                    context.getString(R.string.core_alarm_time_for, prayerName(alert, zone))
                }
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .setDeleteIntent(stopIntent)
            .addAction(0, context.getString(R.string.core_alarm_stop), stopIntent)
            .build()

    /** "It's time for Maghrib": after the adhan, or instead of it in notification mode. */
    @SuppressLint("MissingPermission") // NotificationManagerCompat.notify is a no-op without permission
    fun showPrayerTime(alert: PlannedAlert, zone: ZoneId, silent: Boolean) {
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_stat_deen)
            .setContentTitle(title(alert, zone))
            .setContentText(context.getString(R.string.core_alarm_time_for, prayerName(alert, zone)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(silent)
            .setAutoCancel(true)
            .setWhen(alert.prayerAt.toEpochMilli())
            .setShowWhen(true)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.core_alarm_dua_after_adhan), openAppIntent(DeenLinks.adhkar(AdhkarCategory.ADHAN)))
            .build()
        manager.notify(PRAYER_NOTIFICATION_ID, notification)
    }

    /** "Time to head to the masjid": a few minutes' walk before the prayer. */
    @SuppressLint("MissingPermission")
    fun showMasjid(alert: PlannedAlert, zone: ZoneId) {
        if (!manager.areNotificationsEnabled()) return
        val minutes = Duration.between(alert.at, alert.prayerAt).toMinutes().toInt().coerceAtLeast(1)
        val mosqueAdhkar = openAppIntent(DeenLinks.adhkar(AdhkarCategory.MOSQUE))
        val notification = NotificationCompat.Builder(context, CHANNEL_MASJID)
            .setSmallIcon(R.drawable.ic_stat_deen)
            .setContentTitle(context.getString(R.string.core_alarm_masjid_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.core_alarm_masjid_body,
                    minutes,
                    prayerName(alert, zone),
                    time(alert, zone),
                    minutes
                )
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(Duration.between(alert.at, alert.prayerAt).toMillis().coerceAtLeast(60_000))
            .setContentIntent(mosqueAdhkar)
            .addAction(0, context.getString(R.string.core_alarm_masjid_action), mosqueAdhkar)
            .build()
        manager.notify(MASJID_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showReminder(alert: PlannedAlert, zone: ZoneId) {
        if (!manager.areNotificationsEnabled()) return
        val minutes = Duration.between(alert.at, alert.prayerAt).toMinutes().toInt().coerceAtLeast(1)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_deen)
            .setContentTitle(
                context.resources.getQuantityString(R.plurals.core_alarm_reminder_title, minutes, prayerName(alert, zone), minutes)
            )
            .setContentText(context.getString(R.string.core_alarm_reminder_body, time(alert, zone)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(Duration.between(alert.at, alert.prayerAt).toMillis().coerceAtLeast(60_000))
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    /** "Have you prayed Dhuhr?" a few minutes after its adhan, with the adhkar for after the prayer. */
    @SuppressLint("MissingPermission")
    fun showAfterAdhan(alert: PlannedAlert, zone: ZoneId) {
        if (!manager.areNotificationsEnabled()) return
        val minutes = Duration.between(alert.prayerAt, alert.at).toMinutes().toInt().coerceAtLeast(1)
        val afterPrayer = openAppIntent(DeenLinks.adhkar(AdhkarCategory.AFTER_PRAYER))
        val body = context.resources.getQuantityString(R.plurals.core_alarm_after_body, minutes, minutes)
        val notification = NotificationCompat.Builder(context, CHANNEL_AFTER)
            .setSmallIcon(R.drawable.ic_stat_deen)
            .setContentTitle(context.getString(R.string.core_alarm_after_title, prayerName(alert, zone)))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(AFTER_TIMEOUT_MS)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.core_alarm_after_action), afterPrayer)
            .build()
        manager.notify(AFTER_NOTIFICATION_ID, notification)
    }

    /** Friday morning: "Read Surah Al-Kahf", opening the reader at the surah. */
    @SuppressLint("MissingPermission")
    fun showKahf() {
        if (!manager.areNotificationsEnabled()) return
        val body = context.getString(R.string.core_alarm_kahf_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIDAY)
            .setSmallIcon(R.drawable.ic_stat_deen)
            .setContentTitle(context.getString(R.string.core_alarm_kahf_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(DeenLinks.quran(KAHF_SURAH)))
            .build()
        manager.notify(KAHF_NOTIFICATION_ID, notification)
    }

    private fun prayerName(alert: PlannedAlert, zone: ZoneId): String {
        val isFriday = alert.prayerAt.atZone(zone).dayOfWeek == DayOfWeek.FRIDAY
        return context.getString(alert.prayer.labelRes(isFriday && alert.prayer == Prayer.DHUHR))
    }

    private fun title(alert: PlannedAlert, zone: ZoneId): String = if (alert.kind == AlertKind.TEST) {
        context.getString(R.string.core_alarm_test_title)
    } else {
        context.getString(R.string.core_alarm_title, prayerName(alert, zone), time(alert, zone))
    }

    /** Follows the phone's 12/24-hour setting and language. */
    private fun time(alert: PlannedAlert, zone: ZoneId): String = android.text.format.DateFormat.getTimeFormat(context)
        .apply { timeZone = TimeZone.getTimeZone(zone) }
        .format(Date(alert.prayerAt.toEpochMilli()))

    /** Opens the app, optionally at an in-app [link] (see [DeenLinks]). */
    private fun openAppIntent(link: String? = null): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        link?.let {
            // Reach the running activity too (MainActivity is singleTop), not only a fresh start.
            launch.putExtra(
                DeenLinks.EXTRA_OPEN,
                it
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE + (link?.let { LINK_CODES.indexOf(it) + 1 } ?: 0),
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val CHANNEL_ADHAN = "adhan_playback"
        const val CHANNEL_PRAYER = "prayer_notification"
        const val CHANNEL_REMINDER = "prayer_reminder"
        const val CHANNEL_MASJID = "masjid_reminder"
        const val CHANNEL_AFTER = "after_adhan_reminder"
        const val CHANNEL_FRIDAY = "friday_reminder"

        internal const val ADHAN_NOTIFICATION_ID = 1
        private const val PRAYER_NOTIFICATION_ID = 2
        private const val REMINDER_NOTIFICATION_ID = 3
        private const val MASJID_NOTIFICATION_ID = 4
        private const val AFTER_NOTIFICATION_ID = 5
        private const val KAHF_NOTIFICATION_ID = 6
        private const val KAHF_SURAH = 18
        private const val AFTER_TIMEOUT_MS = 45L * 60 * 1000
        private const val OPEN_APP_REQUEST_CODE = 2100

        /** Each in-app link gets its own PendingIntent (request code), or they would overwrite each other's extras. */
        private val LINK_CODES = listOf(
            DeenLinks.adhkar(AdhkarCategory.ADHAN),
            DeenLinks.adhkar(AdhkarCategory.MOSQUE),
            DeenLinks.adhkar(AdhkarCategory.AFTER_PRAYER),
            DeenLinks.quran(KAHF_SURAH)
        )
    }
}
