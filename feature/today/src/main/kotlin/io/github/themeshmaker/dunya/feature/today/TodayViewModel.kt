package io.github.themeshmaker.dunya.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.ObservePrayerScheduleUseCase
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.prayer.NextPrayer
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Ready(val schedule: PrayerSchedule, val next: NextPrayer, val now: Instant) : TodayUiState
}

@HiltViewModel
class TodayViewModel @Inject constructor(observePrayerSchedule: ObservePrayerScheduleUseCase, private val clock: Clock) : ViewModel() {

    /** Ticks on each wall-clock second, only while the screen is visible (WhileSubscribed). */
    private val seconds = flow {
        while (true) {
            emit(clock.instant())
            delay(MILLIS_PER_SECOND - clock.millis() % MILLIS_PER_SECOND)
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(observePrayerSchedule().filterNotNull(), seconds) { schedule, now ->
        TodayUiState.Ready(schedule, schedule.timeline.next(now), now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
