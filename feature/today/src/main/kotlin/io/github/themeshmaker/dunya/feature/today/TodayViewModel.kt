/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.alarm.reliability.ReliabilityChecker
import io.github.themeshmaker.dunya.core.alarm.reliability.ReliabilityStatus
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.domain.DailyContent
import io.github.themeshmaker.dunya.core.domain.ObservePrayerScheduleUseCase
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.domain.QuranPages
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.ProphetFact
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.prayer.NextPrayer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The slower-changing parts of Today: today's hadith and fact, where Quran reading left off, the
 * prayers ticked off and the khatmah plan (with the page index its progress needs).
 */
data class TodayExtras(
    val hadith: Hadith? = null,
    val fact: ProphetFact? = null,
    val lastRead: QuranPosition? = null,
    val lastReadSurah: Surah? = null,
    val prayed: Map<LocalDate, Set<Prayer>> = emptyMap(),
    val khatmah: KhatmahPlan? = null,
    val pages: QuranPages? = null,
    val surahs: List<Surah> = emptyList()
)

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Ready(
        val schedule: PrayerSchedule,
        val next: NextPrayer,
        val now: Instant,
        val alerts: AlertSettings,
        /** Notifications or exact alarms are off: the adhan can't work properly. */
        val alarmSetupNeeded: Boolean
    ) : TodayUiState
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    observePrayerSchedule: ObservePrayerScheduleUseCase,
    private val userData: UserDataRepository,
    private val reliability: ReliabilityChecker,
    private val content: ContentRepository,
    private val activityRepository: ActivityRepository,
    private val clock: Clock
) : ViewModel() {
    private val activity = activityRepository

    private val daily = MutableStateFlow<Pair<Hadith?, ProphetFact?>>(null to null)
    private val surahs = MutableStateFlow<List<Surah>>(emptyList())
    private val pages = MutableStateFlow<QuranPages?>(null)
    private var dailyFor: LocalDate? = null

    val extras: StateFlow<TodayExtras> = combine(daily, surahs, pages, activity.activity) { (hadith, fact), list, index, a ->
        TodayExtras(
            hadith = hadith,
            fact = fact,
            lastRead = a.lastRead,
            lastReadSurah = a.lastRead?.let { pos -> list.getOrNull(pos.surah - 1) },
            prayed = a.prayed,
            khatmah = a.khatmah,
            pages = index,
            surahs = list
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayExtras())

    val uiStyleSleek: StateFlow<Boolean> = userData.userData.map { it.uiStyle.immersive }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Ticks on each wall-clock second, only while the screen is visible (WhileSubscribed). */
    private val seconds = flow {
        while (true) {
            emit(clock.instant())
            delay(MILLIS_PER_SECOND - clock.millis() % MILLIS_PER_SECOND)
        }
    }

    private val status = MutableStateFlow(reliability.status())

    val uiState: StateFlow<TodayUiState> = combine(
        observePrayerSchedule().filterNotNull(),
        userData.userData.map { it.alertSettings },
        status,
        seconds
    ) { schedule, alerts, s: ReliabilityStatus, now ->
        TodayUiState.Ready(
            schedule = schedule,
            next = schedule.timeline.next(now),
            now = now,
            alerts = alerts,
            alarmSetupNeeded = !s.notificationsAllowed || !s.exactAlarmsAllowed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    /** Call on resume: permissions may have changed in system settings. */
    fun refresh() {
        status.value = reliability.status()
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        if (dailyFor != today) {
            dailyFor = today
            viewModelScope.launch {
                daily.value = DailyContent.pick(content.nawawi(), today, DailyContent.HADITH_SALT) to
                    DailyContent.pick(content.prophetFacts(), today, DailyContent.FACT_SALT)
                if (surahs.value.isEmpty()) surahs.value = content.surahs()
                if (pages.value == null) pages.value = QuranPages(content.pageStarts())
            }
        }
    }

    /** Ticks [prayer] of [day] (the location's date) as prayed, or unticks it. */
    fun setPrayed(day: LocalDate, prayer: Prayer, prayed: Boolean) {
        viewModelScope.launch { activityRepository.setPrayed(day, prayer, prayed) }
    }

    /** Adhan → notification → silent → adhan. Sunrise only toggles notification / silent. */
    fun cycleAlert(prayer: Prayer) {
        viewModelScope.launch {
            val alerts = userData.userData.first().alertSettings
            val next = when (alerts.modeFor(prayer)) {
                AlertMode.ADHAN -> AlertMode.NOTIFICATION
                AlertMode.NOTIFICATION -> AlertMode.SILENT
                AlertMode.SILENT -> if (prayer == Prayer.SUNRISE) AlertMode.NOTIFICATION else AlertMode.ADHAN
            }
            userData.setAlertSettings(alerts.copy(modes = alerts.modes + (prayer to next)))
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
