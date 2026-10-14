/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.StyleSpan
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.alarm.AlarmStateStore
import io.github.themeshmaker.dunya.core.domain.ObservePrayerScheduleUseCase
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.labelRes
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Draws every placed widget from the alarm pipeline's device-protected copy of the settings
 * (so it also works before the first unlock), then asks to be woken again at the next prayer
 * or midnight, whichever comes first.
 */
@Singleton
class PrayerWidgetRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmState: AlarmStateStore,
    private val schedules: ObservePrayerScheduleUseCase,
    private val clock: Clock
) {
    suspend fun updateAll() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
        if (ids.isEmpty()) {
            cancelRefresh()
            return
        }
        val views = render()
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private suspend fun render(): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_widget)
        views.setOnClickPendingIntent(R.id.widget_root, openApp())
        val inputs = alarmState.inputs()
        if (inputs == null) {
            views.setTextViewText(R.id.widget_next_name, context.getString(R.string.feature_widget_setup))
            listOf(R.id.widget_next_time, R.id.widget_place, R.id.widget_hijri).forEach { views.setTextViewText(it, "") }
            return views
        }
        val zone = inputs.location.zoneId
        val now = clock.instant()
        val schedule = schedules.schedule(now.atZone(zone).toLocalDate(), inputs.location, inputs.prayerSettings)
        val next = schedule.timeline.next(now)
        val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY

        views.setTextViewText(R.id.widget_place, inputs.location.let { if (isArabic()) it.arabicName ?: it.name else it.name })
        views.setTextViewText(R.id.widget_hijri, hijri(schedule.hijriDate.day, schedule.hijriDate.month, schedule.hijriDate.year))
        val nextIsFriday = next.at.atZone(zone).dayOfWeek == DayOfWeek.FRIDAY
        views.setTextViewText(R.id.widget_next_name, context.getString(next.prayer.labelRes(nextIsFriday && next.prayer == Prayer.DHUHR)))
        views.setTextViewText(R.id.widget_next_time, time(next.at, zone))
        views.setChronometer(
            R.id.widget_countdown,
            SystemClock.elapsedRealtime() + (next.at.toEpochMilli() - now.toEpochMilli()),
            context.getString(R.string.feature_widget_in),
            true
        )
        views.setChronometerCountDown(R.id.widget_countdown, true)

        COLUMNS.forEach { (prayer, nameId, timeId) ->
            val highlight = prayer == next.prayer && next.at.atZone(zone).toLocalDate() == schedule.date
            views.setTextViewText(nameId, bold(context.getString(prayer.labelRes(isFriday && prayer == Prayer.DHUHR)), highlight))
            views.setTextViewText(timeId, bold(time(schedule.today[prayer], zone), highlight))
        }
        scheduleRefresh(minOf(next.at, schedule.date.plusDays(1).atStartOfDay(zone).toInstant()))
        return views
    }

    private fun isArabic() = context.resources.configuration.locales[0].language == "ar"

    private fun hijri(day: Int, month: Int, year: Int): String {
        val months = context.resources.getStringArray(UiR.array.core_ui_hijri_months)
        return context.getString(UiR.string.core_ui_hijri_date, day, months[month - 1], year)
    }

    private fun time(at: Instant, zone: java.time.ZoneId): String =
        DateFormat.getTimeFormat(context).apply { timeZone = TimeZone.getTimeZone(zone) }.format(Date(at.toEpochMilli()))

    private fun bold(text: String, on: Boolean): CharSequence = if (!on) {
        text
    } else {
        SpannableString(text).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }

    private fun openApp(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            OPEN_REQUEST_CODE,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Inexact on purpose: a widget a minute late is fine, and it needs no exact-alarm permission. */
    private fun scheduleRefresh(at: Instant) {
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC, at.toEpochMilli() + REFRESH_SLACK_MS, refreshIntent())
    }

    private fun cancelRefresh() {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent())
    }

    private fun refreshIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REFRESH_REQUEST_CODE,
        Intent(context, PrayerWidgetProvider::class.java).setAction(PrayerWidgetProvider.ACTION_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private companion object {
        const val OPEN_REQUEST_CODE = 4000
        const val REFRESH_REQUEST_CODE = 4001
        const val REFRESH_SLACK_MS = 1_000L

        val COLUMNS = listOf(
            Triple(Prayer.FAJR, R.id.widget_fajr_name, R.id.widget_fajr_time),
            Triple(Prayer.DHUHR, R.id.widget_dhuhr_name, R.id.widget_dhuhr_time),
            Triple(Prayer.ASR, R.id.widget_asr_name, R.id.widget_asr_time),
            Triple(Prayer.MAGHRIB, R.id.widget_maghrib_name, R.id.widget_maghrib_time),
            Triple(Prayer.ISHA, R.id.widget_isha_name, R.id.widget_isha_time)
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetDependencies {
    fun renderer(): PrayerWidgetRenderer
}

class PrayerWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> refresh(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)

    override fun onDisabled(context: Context) = refresh(context)

    private fun refresh(context: Context) {
        val renderer = EntryPointAccessors.fromApplication(context.applicationContext, WidgetDependencies::class.java).renderer()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                renderer.updateAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        internal const val ACTION_REFRESH = "io.github.themeshmaker.dunya.action.REFRESH_WIDGET"
    }
}

/** Redraws the widget whenever the place, method or language-relevant settings change. */
@Singleton
class WidgetSync @Inject constructor(private val userData: UserDataRepository, private val renderer: PrayerWidgetRenderer) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            renderer.updateAll()
            // The alarm sync copies settings to device-protected storage first; the widget reads that copy.
            userData.userData.map { it.location to it.prayerSettings }.distinctUntilChanged().drop(1).collect {
                kotlinx.coroutines.delay(SETTLE_MS)
                renderer.updateAll()
            }
        }
    }

    private companion object {
        const val SETTLE_MS = 1_000L
    }
}
