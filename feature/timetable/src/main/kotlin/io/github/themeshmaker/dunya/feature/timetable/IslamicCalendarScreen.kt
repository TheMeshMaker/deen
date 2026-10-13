package io.github.themeshmaker.dunya.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.GradientHero
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.ObservePrayerScheduleUseCase
import io.github.themeshmaker.dunya.core.prayer.IslamicEvents
import io.github.themeshmaker.dunya.core.prayer.UpcomingEvent
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.detailRes
import io.github.themeshmaker.dunya.core.ui.emphasizeNumber
import io.github.themeshmaker.dunya.core.ui.eventWhen
import io.github.themeshmaker.dunya.core.ui.hijriDateText
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberDateFormatter
import java.time.LocalDate
import java.time.format.TextStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class IslamicCalendarUiState(val today: LocalDate? = null, val events: List<UpcomingEvent> = emptyList())

@HiltViewModel
class IslamicCalendarViewModel @Inject constructor(observeSchedule: ObservePrayerScheduleUseCase) : ViewModel() {
    val uiState: StateFlow<IslamicCalendarUiState> = observeSchedule().map { schedule ->
        if (schedule == null) {
            IslamicCalendarUiState()
        } else {
            IslamicCalendarUiState(schedule.date, IslamicEvents.upcoming(schedule.date, schedule.settings.hijriOffsetDays))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IslamicCalendarUiState())
}

/** The coming days of the Islamic year, soonest first, with the sunnah fasts of every week and month. */
@Composable
internal fun IslamicCalendarScreenRoute(onBack: () -> Unit, viewModel: IslamicCalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IslamicCalendarScreen(state, onBack)
}

@Composable
internal fun IslamicCalendarScreen(state: IslamicCalendarUiState, onBack: () -> Unit) {
    val today = state.today
    val look = DeenTheme.look
    val entrance = rememberEntrance()
    DeenScaffold(
        title = stringResource(R.string.feature_timetable_islamic_calendar),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        if (today == null) return@DeenScaffold
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = look.gutter,
                end = look.gutter,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            itemsIndexed(state.events, key = { _, e -> e.event.name }) { index, event ->
                val modifier = Modifier.rise(entrance, index)
                if (index == 0) NextEventHero(event, today, modifier) else EventRow(event, today, modifier)
            }
            item {
                DeenCard(modifier = Modifier.rise(entrance, state.events.size)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(DeenIcons.Water), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(R.string.feature_timetable_sunnah_fasts),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.feature_timetable_sunnah_fasts_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.feature_timetable_calendar_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/** The soonest day, large on the theme's gradient, with a big countdown. */
@Composable
private fun NextEventHero(event: UpcomingEvent, today: LocalDate, modifier: Modifier = Modifier) {
    val gradient = DeenTheme.gradient
    GradientHero(modifier.fillMaxWidth(), shape = DeenTheme.look.cardShape) {
        CompositionLocalProvider(LocalContentColor provides gradient.content) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.feature_timetable_next_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = gradient.content.copy(alpha = 0.8f)
                )
                Text(stringResource(event.event.labelRes()), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${rememberDateFormatter().format(event.date)} · ${hijriDateText(event.hijri)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = gradient.content.copy(alpha = 0.85f)
                )
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                    if (event.daysAway > 1) {
                        val count = localDigits(event.daysAway)
                        Text(
                            emphasizeNumber(
                                pluralStringResource(R.plurals.feature_timetable_days_to_go, event.daysAway.toInt(), count),
                                count,
                                SpanStyle(fontSize = MaterialTheme.typography.displayMedium.fontSize, fontWeight = FontWeight.Light)
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(eventWhen(event.daysAway, event.date.isBefore(today)), style = MaterialTheme.typography.displaySmall)
                    }
                }
                Text(
                    stringResource(event.event.detailRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = gradient.content.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: UpcomingEvent, today: LocalDate, modifier: Modifier = Modifier) {
    val locale = currentLocale
    DeenCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // A calendar leaf: the Gregorian day and month.
            Column(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    localDigits(event.date.dayOfMonth),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    event.date.month.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(event.event.labelRes()), style = MaterialTheme.typography.titleMedium)
                Text(
                    hijriDateText(event.hijri),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(event.event.detailRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    eventWhen(event.daysAway, event.date.isBefore(today)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
