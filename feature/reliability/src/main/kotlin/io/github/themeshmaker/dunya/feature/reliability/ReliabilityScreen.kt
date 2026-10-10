package io.github.themeshmaker.dunya.feature.reliability

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertLogEntry
import io.github.themeshmaker.dunya.core.model.AlertOutcome
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.rememberTimeFormatter
import java.time.Duration

@Composable
internal fun ReliabilityScreenRoute(setup: Boolean, onDone: () -> Unit, viewModel: ReliabilityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refresh()
        if (!granted) context.startFirst(viewModel.intentsFor(StepId.NOTIFICATIONS))
    }

    ReliabilityScreen(
        state = state,
        setup = setup,
        onDone = onDone,
        onFix = { step ->
            val needsRuntimePermission = step == StepId.NOTIFICATIONS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsRuntimePermission) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startFirst(viewModel.intentsFor(step))
            }
        },
        onConfirm = viewModel::setConfirmed,
        onTest = viewModel::startTest
    )
}

/** Settings screens move between ROM versions; try each candidate until one opens. */
private fun Context.startFirst(intents: List<Intent>) {
    for (intent in intents) {
        try {
            startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReliabilityScreen(
    state: ReliabilityUiState,
    setup: Boolean,
    onDone: () -> Unit,
    onFix: (StepId) -> Unit,
    onConfirm: (StepId, Boolean) -> Unit,
    onTest: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_reliability_title)) },
                navigationIcon = {
                    if (!setup) {
                        IconButton(onClick = onDone) {
                            Icon(painterResource(DeenIcons.Back), contentDescription = stringResource(R.string.feature_reliability_back))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (setup) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) { Text(stringResource(R.string.feature_reliability_done)) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressHeader(state)
            state.steps.forEach { step -> StepCard(step, onFix, onConfirm) }
            TestCard(state, onTest)
            LogCard(state)
        }
    }
}

@Composable
private fun ProgressHeader(state: ReliabilityUiState) {
    val gradient = DeenTheme.gradient
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(gradient.brush)
    ) {
        GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize())
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.feature_reliability_progress, state.doneCount, state.steps.size),
                style = MaterialTheme.typography.headlineSmall,
                color = gradient.content
            )
            Text(
                stringResource(R.string.feature_reliability_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = gradient.content.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun StepCard(step: Step, onFix: (StepId) -> Unit, onConfirm: (StepId, Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    painter = painterResource(if (step.done) DeenIcons.Ok else DeenIcons.Problem),
                    contentDescription = stringResource(
                        if (step.done) R.string.feature_reliability_ok else R.string.feature_reliability_todo
                    ),
                    tint = if (step.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(stringResource(step.id.titleRes()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(
                stringResource(step.id.bodyRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!step.done && step.id != StepId.RECENTS_LOCK) {
                    Button(onClick = { onFix(step.id) }) {
                        Text(
                            stringResource(
                                if (step.id.manual) R.string.feature_reliability_open_settings else R.string.feature_reliability_allow
                            )
                        )
                    }
                } else if (step.id.manual && step.id != StepId.RECENTS_LOCK) {
                    OutlinedButton(onClick = { onFix(step.id) }) { Text(stringResource(R.string.feature_reliability_open_settings)) }
                }
                if (step.id.manual) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = step.done, onCheckedChange = { onConfirm(step.id, it) })
                        Text(stringResource(R.string.feature_reliability_did_it), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TestCard(state: ReliabilityUiState, onTest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_reliability_test_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feature_reliability_test_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val remaining = state.testAt?.let { Duration.between(state.now, it) }
            if (remaining != null && !remaining.isNegative) {
                Text(
                    stringResource(
                        R.string.feature_reliability_test_waiting,
                        String.format(currentLocale, "%d:%02d", remaining.toMinutes(), remaining.seconds % 60)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Button(onClick = onTest) {
                    Icon(painterResource(DeenIcons.Adhan), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.feature_reliability_test_button), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun LogCard(state: ReliabilityUiState) {
    val timeFormatter = rememberTimeFormatter()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.feature_reliability_log_title), style = MaterialTheme.typography.titleMedium)
            if (state.log.isEmpty()) {
                Text(
                    stringResource(R.string.feature_reliability_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.log.forEach { entry -> LogRow(entry, timeFormatter.format(entry.scheduledAt.atZone(state.zone))) }
        }
    }
}

@Composable
private fun LogRow(entry: AlertLogEntry, time: String) {
    val good = entry.outcome in setOf(AlertOutcome.PLAYED, AlertOutcome.NOTIFIED, AlertOutcome.REMINDED, AlertOutcome.VIBRATED_SILENT_MODE)
    val delay = Duration.between(entry.scheduledAt, entry.firedAt).seconds
    val name = when {
        entry.kind == AlertKind.TEST || entry.prayer == null -> stringResource(R.string.feature_reliability_log_test)
        else -> stringResource(entry.prayer!!.labelRes())
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            painter = painterResource(if (good) DeenIcons.Ok else DeenIcons.Problem),
            contentDescription = null,
            tint = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text("$name · $time", style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(stringResource(entry.outcome.labelRes()))
                    if (entry.outcome != AlertOutcome.CLEARED_BY_SYSTEM) {
                        append(" · ")
                        append(
                            if (delay < ON_TIME_SECONDS) {
                                stringResource(R.string.feature_reliability_on_time)
                            } else {
                                stringResource(R.string.feature_reliability_late_by, delay)
                            }
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val ON_TIME_SECONDS = 5L

private fun StepId.titleRes(): Int = when (this) {
    StepId.NOTIFICATIONS -> R.string.feature_reliability_step_notifications
    StepId.EXACT_ALARMS -> R.string.feature_reliability_step_exact
    StepId.BATTERY -> R.string.feature_reliability_step_battery
    StepId.BACKGROUND -> R.string.feature_reliability_step_background
    StepId.RECENTS_LOCK -> R.string.feature_reliability_step_recents
}

private fun StepId.bodyRes(): Int = when (this) {
    StepId.NOTIFICATIONS -> R.string.feature_reliability_step_notifications_body
    StepId.EXACT_ALARMS -> R.string.feature_reliability_step_exact_body
    StepId.BATTERY -> R.string.feature_reliability_step_battery_body
    StepId.BACKGROUND -> R.string.feature_reliability_step_background_body
    StepId.RECENTS_LOCK -> R.string.feature_reliability_step_recents_body
}

private fun AlertOutcome.labelRes(): Int = when (this) {
    AlertOutcome.PLAYED -> R.string.feature_reliability_outcome_played
    AlertOutcome.NOTIFIED -> R.string.feature_reliability_outcome_notified
    AlertOutcome.REMINDED -> R.string.feature_reliability_outcome_reminded
    AlertOutcome.VIBRATED_SILENT_MODE -> R.string.feature_reliability_outcome_vibrated
    AlertOutcome.SKIPPED_IN_CALL -> R.string.feature_reliability_outcome_in_call
    AlertOutcome.LATE -> R.string.feature_reliability_outcome_late
    AlertOutcome.CLEARED_BY_SYSTEM -> R.string.feature_reliability_outcome_cleared
    AlertOutcome.FALLBACK_NOTIFICATION -> R.string.feature_reliability_outcome_fallback
}
