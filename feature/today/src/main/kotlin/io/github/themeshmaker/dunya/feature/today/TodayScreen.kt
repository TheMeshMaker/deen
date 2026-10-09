package io.github.themeshmaker.dunya.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.screen
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.prayer.NextPrayer
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.hijriDateText
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.rememberDateFormatter
import io.github.themeshmaker.dunya.core.ui.rememberTimeFormatter
import io.github.themeshmaker.dunya.core.ui.shortLabelRes
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun TodayRoute(onChangeLocation: () -> Unit, onChangeMethod: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val s = state) {
        TodayUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is TodayUiState.Ready -> TodayScreen(s, onChangeLocation, onChangeMethod)
    }
}

@Composable
internal fun TodayScreen(state: TodayUiState.Ready, onChangeLocation: () -> Unit, onChangeMethod: () -> Unit) {
    val schedule = state.schedule
    val zone = schedule.location.zoneId
    val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.screen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(schedule, onChangeLocation)
        NextPrayerCard(state.next, state.now, zone, isFriday)
        PrayerTimesCard(schedule, state.next, state.now, isFriday)
        AssistChip(
            onClick = onChangeMethod,
            label = {
                Text(
                    stringResource(
                        R.string.feature_today_method_summary,
                        stringResource(schedule.settings.method.labelRes()),
                        stringResource(schedule.settings.asrMethod.shortLabelRes())
                    )
                )
            },
            leadingIcon = { Icon(painterResource(DeenIcons.Tune), contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = CircleShape,
            colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(schedule: PrayerSchedule, onChangeLocation: () -> Unit) {
    Column {
        TextButton(
            onClick = onChangeLocation,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(
                painterResource(DeenIcons.Location),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = schedule.location.displayName(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            text = "${rememberDateFormatter().format(schedule.date)} · ${hijriDateText(schedule.hijriDate)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun NextPrayerCard(next: NextPrayer, now: Instant, zone: ZoneId, isFriday: Boolean) {
    val timeFormatter = rememberTimeFormatter()
    val name = stringResource(next.prayer.labelRes(isFriday && next.prayer == Prayer.DHUHR))
    val remaining = Duration.between(now, next.at).coerceAtLeast(Duration.ZERO)

    val gradient = DeenTheme.gradient
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = MaterialTheme.shapes.extraLarge, spotColor = gradient.start)
            .background(gradient.brush, MaterialTheme.shapes.extraLarge)
    ) {
        GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize())
        CompositionLocalProvider(LocalContentColor provides gradient.content) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.feature_today_next_prayer),
                    style = MaterialTheme.typography.labelLarge,
                    color = gradient.content.copy(alpha = 0.8f)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    Text(timeFormatter.format(next.at.atZone(zone)), style = MaterialTheme.typography.titleLarge)
                }
                Countdown(remaining, name)
                LinearProgressIndicator(
                    progress = { next.progress(now) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(6.dp),
                    color = gradient.content,
                    trackColor = gradient.content.copy(alpha = 0.22f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

/** HH:MM:SS for the eye; TalkBack hears a polite minute-level sentence instead of every second. */
@Composable
private fun Countdown(remaining: Duration, prayerName: String) {
    val hours = remaining.toHours().toInt()
    val minutes = (remaining.toMinutes() % 60).toInt()
    val seconds = (remaining.seconds % 60).toInt()
    val spoken = stringResource(
        R.string.feature_today_countdown_description,
        prayerName,
        pluralStringResource(R.plurals.feature_today_hours, hours, hours),
        pluralStringResource(R.plurals.feature_today_minutes, minutes, minutes)
    )
    Text(
        text = String.format(currentLocale, "%02d:%02d:%02d", hours, minutes, seconds),
        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        }
    )
}

@Composable
private fun PrayerTimesCard(schedule: PrayerSchedule, next: NextPrayer, now: Instant, isFriday: Boolean) {
    val timeFormatter = rememberTimeFormatter()
    val zone = schedule.location.zoneId
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Prayer.entries.forEach { prayer ->
                val at = schedule.today[prayer]
                val isNext = next.prayer == prayer && next.at == at
                PrayerRow(
                    name = stringResource(prayer.labelRes(isFriday && prayer == Prayer.DHUHR)),
                    time = timeFormatter.format(at.atZone(zone)),
                    isNext = isNext,
                    isMuted = (at.isBefore(now) && !isNext) || prayer == Prayer.SUNRISE
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(name: String, time: String, isNext: Boolean, isMuted: Boolean) {
    val color = when {
        isNext -> MaterialTheme.colorScheme.primary
        isMuted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val weight = if (isNext) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (isNext) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = weight, color = color, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = weight, color = color, textAlign = TextAlign.End)
    }
}
