package io.github.themeshmaker.dunya.feature.reliability

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.alarm.AlarmStateStore
import io.github.themeshmaker.dunya.core.alarm.AlertScheduler
import io.github.themeshmaker.dunya.core.alarm.reliability.ReliabilityChecker
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AlertLogEntry
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Checklist steps (docs/BLUEPRINT.md §10.1). [key] is what gets stored for manual ones. */
enum class StepId(val key: String, val manual: Boolean) {
    NOTIFICATIONS("notifications", manual = false),
    EXACT_ALARMS("exact_alarms", manual = false),
    BATTERY("battery", manual = false),
    BACKGROUND("background", manual = true),
    RECENTS_LOCK("recents_lock", manual = true)
}

data class Step(val id: StepId, val done: Boolean)

data class ReliabilityUiState(
    val steps: List<Step>,
    val log: List<AlertLogEntry>,
    val zone: ZoneId,
    val testAt: Instant?,
    val now: Instant
) {
    val doneCount: Int get() = steps.count { it.done }
}

@HiltViewModel
class ReliabilityViewModel @Inject constructor(
    private val checker: ReliabilityChecker,
    private val scheduler: AlertScheduler,
    private val userData: UserDataRepository,
    alarmState: AlarmStateStore,
    private val clock: Clock
) : ViewModel() {

    private val status = MutableStateFlow(checker.status())
    private val testAt = MutableStateFlow<Instant?>(null)
    private val seconds = flow {
        while (true) {
            emit(clock.instant())
            delay(1_000)
        }
    }

    val uiState: StateFlow<ReliabilityUiState> = combine(status, userData.userData, alarmState.log, testAt, seconds) {
            s,
            data,
            log,
            test,
            now
        ->
        val confirmed = data.confirmedReliabilitySteps
        // The same checklist on every phone: the two manual steps say "if your phone has this",
        // and the settings shortcuts behind them adapt to the phone without naming it.
        val steps = listOf(
            Step(StepId.NOTIFICATIONS, s.notificationsAllowed),
            Step(StepId.EXACT_ALARMS, s.exactAlarmsAllowed),
            Step(StepId.BATTERY, s.batteryUnrestricted),
            Step(StepId.BACKGROUND, StepId.BACKGROUND.key in confirmed),
            Step(StepId.RECENTS_LOCK, StepId.RECENTS_LOCK.key in confirmed)
        )
        ReliabilityUiState(
            steps = steps,
            log = log.take(MAX_LOG_ROWS),
            zone = data.location?.zoneId ?: ZoneId.systemDefault(),
            testAt = test?.takeIf { it.isAfter(now.minusSeconds(TEST_RESULT_WINDOW_S)) },
            now = now
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState() = ReliabilityUiState(emptyList(), emptyList(), ZoneId.systemDefault(), null, clock.instant())

    /** Call on resume: the user may have changed something in system settings. */
    fun refresh() {
        status.value = checker.status()
        // Exact-alarm permission may have just been granted.
        viewModelScope.launch { scheduler.rescheduleAll("reliability screen") }
    }

    fun setConfirmed(step: StepId, confirmed: Boolean) {
        viewModelScope.launch { userData.setReliabilityStepConfirmed(step.key, confirmed) }
    }

    fun startTest() {
        val at = clock.instant().plusSeconds(TEST_DELAY_S)
        testAt.value = at
        viewModelScope.launch { scheduler.scheduleTest(at) }
    }

    fun intentsFor(step: StepId): List<Intent> = when (step) {
        StepId.NOTIFICATIONS -> checker.notificationSettingsIntents()
        StepId.EXACT_ALARMS -> checker.exactAlarmIntents()
        StepId.BATTERY -> checker.batteryIntents()
        StepId.BACKGROUND -> checker.backgroundIntents()
        StepId.RECENTS_LOCK -> emptyList()
    }

    private companion object {
        const val TEST_DELAY_S = 60L
        const val TEST_RESULT_WINDOW_S = 120L
        const val MAX_LOG_ROWS = 12
    }
}
