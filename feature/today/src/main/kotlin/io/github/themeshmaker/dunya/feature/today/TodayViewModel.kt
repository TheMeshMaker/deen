package io.github.themeshmaker.dunya.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.alarm.reliability.ReliabilityChecker
import io.github.themeshmaker.dunya.core.alarm.reliability.ReliabilityStatus
import io.github.themeshmaker.dunya.core.domain.ObservePrayerScheduleUseCase
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.prayer.NextPrayer
import java.time.Clock
import java.time.Instant
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
    private val clock: Clock
) : ViewModel() {

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
