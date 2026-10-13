package io.github.themeshmaker.dunya.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
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
import io.github.themeshmaker.dunya.core.designsystem.component.SectionLabel
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.ObservePrayerScheduleUseCase
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.domain.PrayerTracker
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.emphasizeNumber
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberDateFormatter
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackerUiState(
    val today: LocalDate? = null,
    val schedule: PrayerSchedule? = null,
    val prayed: Map<LocalDate, Set<Prayer>> = emptyMap()
)

@HiltViewModel
class TrackerViewModel @Inject constructor(
    observeSchedule: ObservePrayerScheduleUseCase,
    private val activity: ActivityRepository,
    private val clock: Clock
) : ViewModel() {
    val uiState: StateFlow<TrackerUiState> = combine(observeSchedule(), activity.activity) { schedule, a ->
        TrackerUiState(schedule?.date ?: LocalDate.now(clock), schedule, a.prayed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState())

    fun setPrayed(day: LocalDate, prayer: Prayer, prayed: Boolean) {
        viewModelScope.launch { activity.setPrayed(day, prayer, prayed) }
    }
}

/** The prayer tracker: streaks, this month at a glance, and ticking off any past day's prayers. */
@Composable
internal fun TrackerRoute(onBack: () -> Unit, viewModel: TrackerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackerScreen(state, onBack, viewModel::setPrayed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerScreen(state: TrackerUiState, onBack: () -> Unit, onSet: (LocalDate, Prayer, Boolean) -> Unit) {
    val today = state.today ?: return
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var editing by remember { mutableStateOf<LocalDate?>(null) }
    val shownMonth = YearMonth.parse(month)
    val look = DeenTheme.look
    val entrance = rememberEntrance()
    DeenScaffold(
        title = stringResource(R.string.feature_today_tracker_title),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
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
            item { StatsRow(state.prayed, today, shownMonth, Modifier.rise(entrance, 0)) }
            item {
                DeenCard(contentPadding = PaddingValues(vertical = 8.dp), modifier = Modifier.rise(entrance, 1)) {
                    SectionLabel(stringResource(R.string.feature_today_tracker_today_title), Modifier.padding(horizontal = 12.dp))
                    DayToggles(today, state.prayed[today].orEmpty(), state.schedule, today, onSet)
                }
            }
            item {
                DeenCard(modifier = Modifier.rise(entrance, 2)) {
                    MonthHeader(shownMonth, canGoForward = shownMonth.isBefore(YearMonth.from(today)), onMove = {
                        month =
                            shownMonth.plusMonths(it.toLong()).toString()
                    })
                    AnimatedContent(shownMonth, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "month") { m ->
                        MonthGrid(m, today, state.prayed, onPick = { editing = it })
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.feature_today_tracker_private),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
    }
    editing?.let { day ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = look.sheetColor,
            shape = look.sheetShape
        ) {
            Text(
                rememberDateFormatter().format(day),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            DayToggles(day, state.prayed[day].orEmpty(), state.schedule, today, onSet)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatsRow(prayed: Map<LocalDate, Set<Prayer>>, today: LocalDate, month: YearMonth, modifier: Modifier = Modifier) {
    val streak = remember(prayed, today) { PrayerTracker.streak(prayed, today) }
    val best = remember(prayed) { PrayerTracker.bestStreak(prayed) }
    val (done, due) = remember(prayed, month, today) { PrayerTracker.monthTotals(prayed, month, today) }
    val big = SpanStyle(fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.SemiBold)
    val streakText = pluralStringResource(R.plurals.feature_today_days_label, streak, localDigits(streak))
    val bestText = pluralStringResource(R.plurals.feature_today_days_label, best, localDigits(best))
    val card = Modifier.fillMaxHeight()
    // Equal heights, whether or not a card has a note under its figure.
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(DeenTheme.look.gap)
    ) {
        Stat(
            emphasizeNumber(streakText, localDigits(streak), big),
            null,
            stringResource(R.string.feature_today_stat_streak),
            card.weight(1f)
        )
        Stat(emphasizeNumber(bestText, localDigits(best), big), null, stringResource(R.string.feature_today_stat_best), card.weight(1f))
        val percent = if (due == 0) 0 else done * 100 / due
        Stat(
            emphasizeNumber("${localDigits(percent)}%", localDigits(percent), big),
            stringResource(R.string.feature_today_stat_of, localDigits(done), localDigits(due)),
            stringResource(R.string.feature_today_stat_month),
            card.weight(1f)
        )
    }
}

/** A figure on a small card: what it is, the figure (its number large), and an optional note under it. */
@Composable
private fun Stat(value: AnnotatedString, caption: String?, label: String, modifier: Modifier = Modifier) {
    DeenCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** The five prayers of [day] with a switch each. Today's later prayers stay off until their time. */
@Composable
private fun DayToggles(
    day: LocalDate,
    prayed: Set<Prayer>,
    schedule: PrayerSchedule?,
    today: LocalDate,
    onSet: (LocalDate, Prayer, Boolean) -> Unit
) {
    val now = remember { java.time.Instant.now() }
    PrayerTracker.prayers.forEach { prayer ->
        val started = day.isBefore(today) || (day == today && (schedule == null || !schedule.today[prayer].isAfter(now)))
        val checked = prayer in prayed
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = started || checked, role = Role.Switch) { onSet(day, prayer, !checked) }
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(prayer.labelRes(day.dayOfWeek == java.time.DayOfWeek.FRIDAY && prayer == Prayer.DHUHR)),
                style = MaterialTheme.typography.bodyLarge,
                color = if (started || checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = checked, onCheckedChange = null, enabled = started || checked)
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, canGoForward: Boolean, onMove: (Int) -> Unit) {
    val locale = currentLocale
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMove(-1) }) {
            Icon(painterResource(DeenIcons.ChevronStart), contentDescription = stringResource(R.string.feature_today_month_previous))
        }
        Text(
            "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${localDigits(month.year)}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onMove(1) }, enabled = canGoForward) {
            Icon(painterResource(DeenIcons.ChevronEnd), contentDescription = stringResource(R.string.feature_today_month_next))
        }
    }
}

/** A calendar month: a ring per day, filled by the prayers ticked off. Tap a day to change it. */
@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, prayed: Map<LocalDate, Set<Prayer>>, onPick: (LocalDate) -> Unit) {
    val locale = currentLocale
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { i ->
                Text(
                    firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, today, PrayerTracker.count(prayed, day), onPick)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, today: LocalDate, count: Int, onPick: (LocalDate) -> Unit) {
    val future = day.isAfter(today)
    val label =
        stringResource(
            R.string.feature_today_day_count,
            localDigits(day.dayOfMonth),
            localDigits(count),
            localDigits(PrayerTracker.prayers.size)
        )
    Column(
        Modifier
            .clip(CircleShape)
            .clickable(enabled = !future) { onPick(day) }
            .semantics { contentDescription = label }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (future) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)))
            } else {
                DayRing(count, PrayerTracker.prayers.size, highlight = false)
            }
        }
        Text(
            localDigits(day.dayOfMonth),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
            color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
