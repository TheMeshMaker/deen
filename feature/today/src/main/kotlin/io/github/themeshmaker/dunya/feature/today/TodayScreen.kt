package io.github.themeshmaker.dunya.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.Entrance
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.LightSystemBarIcons
import io.github.themeshmaker.dunya.core.designsystem.component.RollingText
import io.github.themeshmaker.dunya.core.designsystem.component.pressScale
import io.github.themeshmaker.dunya.core.designsystem.component.rememberDrawIn
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rememberPatternDrift
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.component.screen
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.prayer.NextPrayer
import io.github.themeshmaker.dunya.core.ui.DailyFactCard
import io.github.themeshmaker.dunya.core.ui.DailyHadithCard
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.hijriDateText
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberDateFormatter
import io.github.themeshmaker.dunya.core.ui.rememberTimeFormatter
import io.github.themeshmaker.dunya.core.ui.shortLabelRes
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Everything Today can open. */
class TodayActions(
    val onChangeLocation: () -> Unit,
    val onChangeMethod: () -> Unit,
    val onOpenReliability: () -> Unit,
    val onOpenQibla: () -> Unit,
    val onOpenTimetable: () -> Unit,
    val onOpenTasbih: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenQuran: (surah: Int, ayah: Int) -> Unit,
    val onOpenHadith: (number: Int) -> Unit,
    val onOpenFacts: () -> Unit
)

@Composable
internal fun TodayRoute(actions: TodayActions, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extras by viewModel.extras.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    when (val s = state) {
        TodayUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        is TodayUiState.Ready -> if (DeenTheme.look.sleek) {
            SleekToday(s, extras, actions, viewModel::cycleAlert)
        } else {
            ClassicToday(s, extras, actions, viewModel::cycleAlert)
        }
    }
}

// ---------------------------------------------------------------------------------------------- Classic

@Composable
internal fun ClassicToday(state: TodayUiState.Ready, extras: TodayExtras, actions: TodayActions, onCycleAlert: (Prayer) -> Unit) {
    val schedule = state.schedule
    val zone = schedule.location.zoneId
    val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY
    val look = DeenTheme.look
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(look.page)
            .windowInsetsPadding(WindowInsets.screen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = look.gutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(look.gap)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Header(schedule, actions.onChangeLocation, Modifier.weight(1f))
            IconButton(onClick = actions.onOpenSettings) {
                Icon(painterResource(DeenIcons.Settings), contentDescription = stringResource(R.string.feature_today_settings))
            }
        }
        if (state.alarmSetupNeeded) SetupBanner(actions.onOpenReliability)
        NextPrayerCard(state.next, state.now, zone, isFriday)
        QuickActions(actions)
        DeenCard(contentPadding = PaddingValues(vertical = 8.dp)) {
            PrayerList(schedule, state.next, state.now, isFriday, state.alerts, onCycleAlert)
        }
        BelowTheTimes(schedule, extras, actions)
    }
}

@Composable
private fun Header(schedule: PrayerSchedule, onChangeLocation: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                Countdown(remaining, name, MaterialTheme.typography.displayMedium.fontSize.value)
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

// ---------------------------------------------------------------------------------------------- Sleek

/**
 * Sleek: the gradient runs edge to edge under the status bar, the countdown sits in a ring,
 * and the prayer list floats up over the bottom of the header.
 */
@Composable
internal fun SleekToday(state: TodayUiState.Ready, extras: TodayExtras, actions: TodayActions, onCycleAlert: (Prayer) -> Unit) {
    val schedule = state.schedule
    val zone = schedule.location.zoneId
    val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY
    val look = DeenTheme.look
    val gradient = DeenTheme.gradient
    val timeFormatter = rememberTimeFormatter()
    val name = stringResource(state.next.prayer.labelRes(isFriday && state.next.prayer == Prayer.DHUHR))
    val scroll = rememberScrollState()
    val entrance = rememberEntrance()
    val drift = rememberPatternDrift(PATTERN_CELL)
    LightSystemBarIcons()
    Box(
        Modifier
            .fillMaxSize()
            .background(look.page)
    ) {
        Column(Modifier.verticalScroll(scroll)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(look.heroShape)
                    .background(gradient.brush)
            ) {
                GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize(), cell = PATTERN_CELL, offsetX = drift)
                CompositionLocalProvider(LocalContentColor provides gradient.content) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            // Parallax: the header's content moves at half speed and fades as it scrolls away.
                            .graphicsLayer {
                                if (look.motion) {
                                    translationY = scroll.value * 0.45f
                                    alpha = (1f - scroll.value / 900f).coerceIn(0f, 1f)
                                }
                            }
                            .windowInsetsPadding(WindowInsets.screen)
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = actions.onChangeLocation,
                                colors = ButtonDefaults.textButtonColors(contentColor = gradient.content)
                            ) {
                                Icon(painterResource(DeenIcons.Location), contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(
                                    schedule.location.displayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = actions.onOpenSettings) {
                                Icon(
                                    painterResource(DeenIcons.Settings),
                                    contentDescription = stringResource(R.string.feature_today_settings)
                                )
                            }
                        }
                        Text(
                            "${rememberDateFormatter().format(schedule.date)} · ${hijriDateText(schedule.hijriDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = gradient.content.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            Modifier
                                .padding(top = 20.dp)
                                .widthIn(max = 260.dp)
                                .fillMaxWidth(0.68f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            val drawIn = rememberDrawIn()
                            val progress by animateFloatAsState(state.next.progress(state.now), DeenMotion.gentle(), label = "ring")
                            ProgressRing(progress * drawIn, gradient.content)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // When the next prayer changes, the old name lifts away and the new one rises in.
                                AnimatedContent(
                                    targetState = name,
                                    transitionSpec = {
                                        (fadeIn(tween(300)) + slideInVertically(DeenMotion.snappy()) { it / 2 }) togetherWith
                                            (fadeOut(tween(200)) + slideOutVertically(DeenMotion.snappy()) { -it / 2 })
                                    },
                                    label = "nextPrayer"
                                ) { shown -> Text(shown, style = MaterialTheme.typography.headlineMedium) }
                                Countdown(Duration.between(state.now, state.next.at).coerceAtLeast(Duration.ZERO), name, 34f)
                                Text(
                                    stringResource(R.string.feature_today_at, timeFormatter.format(state.next.at.atZone(zone))),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = gradient.content.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .offset(y = (-44).dp)
                    .padding(horizontal = look.gutter),
                verticalArrangement = Arrangement.spacedBy(look.gap)
            ) {
                DeenCard(contentPadding = PaddingValues(vertical = 10.dp), modifier = Modifier.rise(entrance, 0, distance = 40.dp)) {
                    PrayerList(schedule, state.next, state.now, isFriday, state.alerts, onCycleAlert)
                }
                if (state.alarmSetupNeeded) Box(Modifier.rise(entrance, 1)) { SetupBanner(actions.onOpenReliability) }
                Box(Modifier.rise(entrance, 2)) { QuickActions(actions) }
                BelowTheTimes(schedule, extras, actions, entrance, firstIndex = 3)
            }
        }
        // Keeps the (white) status-bar icons readable once the header scrolls away.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(gradient.start)
        )
    }
}

private val PATTERN_CELL = 48.dp

@Composable
private fun ProgressRing(progress: Float, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(color.copy(alpha = 0.2f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(
            color,
            -90f,
            360f * progress.coerceIn(0f, 1f),
            false,
            Offset(inset, inset),
            arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

// ---------------------------------------------------------------------------------------------- Shared

/** HH:MM:SS for the eye; TalkBack hears a polite minute-level sentence instead of every second. */
@Composable
private fun Countdown(remaining: Duration, prayerName: String, sizeSp: Float) {
    val hours = remaining.toHours().toInt()
    val minutes = (remaining.toMinutes() % 60).toInt()
    val seconds = (remaining.seconds % 60).toInt()
    val spoken = stringResource(
        R.string.feature_today_countdown_description,
        prayerName,
        pluralStringResource(R.plurals.feature_today_hours, hours, hours),
        pluralStringResource(R.plurals.feature_today_minutes, minutes, minutes)
    )
    RollingText(
        text = String.format(currentLocale, "%02d:%02d:%02d", hours, minutes, seconds),
        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum", fontSize = sizeSp.sp),
        fontWeight = if (DeenTheme.look.sleek) FontWeight.Light else FontWeight.Medium,
        color = LocalContentColor.current,
        down = true,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        }
    )
}

@Composable
private fun QuickActions(actions: TodayActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickAction(DeenIcons.Qibla, stringResource(R.string.feature_today_qibla), actions.onOpenQibla)
        QuickAction(DeenIcons.Calendar, stringResource(R.string.feature_today_timetable), actions.onOpenTimetable)
        QuickAction(DeenIcons.Tasbih, stringResource(R.string.feature_today_tasbih), actions.onOpenTasbih)
        QuickAction(DeenIcons.Settings, stringResource(R.string.feature_today_settings), actions.onOpenSettings)
    }
}

@Composable
private fun QuickAction(icon: Int, label: String, onClick: () -> Unit) {
    val look = DeenTheme.look
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .pressScale(interaction, enabled = look.motion, pressed = 0.9f)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(if (look.sleek) CircleShape else MaterialTheme.shapes.large)
                .background(if (look.sleek) look.card else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .then(if (look.sleek) Modifier.border(look.cardBorder ?: BorderStroke(0.dp, Color.Transparent), CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
    }
}

/** Continue reading, hadith of the day, fact of the day and the method chip. */
@Composable
private fun BelowTheTimes(
    schedule: PrayerSchedule,
    extras: TodayExtras,
    actions: TodayActions,
    entrance: Entrance? = null,
    firstIndex: Int = 0
) {
    fun Modifier.enter(i: Int) = if (entrance != null) rise(entrance, firstIndex + i) else this
    val lastRead = extras.lastRead
    val surah = extras.lastReadSurah
    if (lastRead != null && surah != null) {
        DeenCard(onClick = { actions.onOpenQuran(lastRead.surah, lastRead.ayah) }, modifier = Modifier.enter(0)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(DeenIcons.Quran), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.feature_today_continue_quran),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(
                            R.string.feature_today_quran_position,
                            if (isArabicUi) surah.arabicName else surah.transliteration,
                            localDigits(lastRead.ayah)
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (!isArabicUi) Text(surah.arabicName, style = DeenTextStyles.arabicBody, color = MaterialTheme.colorScheme.primary)
            }
        }
    } else {
        DeenCard(onClick = { actions.onOpenQuran(1, 1) }, modifier = Modifier.enter(0)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(DeenIcons.Quran), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.feature_today_start_quran),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    extras.hadith?.let { DailyHadithCard(it, onClick = { actions.onOpenHadith(it.number) }, modifier = Modifier.enter(1)) }
    extras.fact?.let { DailyFactCard(it, onClick = actions.onOpenFacts, modifier = Modifier.enter(2)) }
    AssistChip(
        onClick = actions.onChangeMethod,
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
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun PrayerList(
    schedule: PrayerSchedule,
    next: NextPrayer,
    now: Instant,
    isFriday: Boolean,
    alerts: AlertSettings,
    onCycleAlert: (Prayer) -> Unit
) {
    val timeFormatter = rememberTimeFormatter()
    val zone = schedule.location.zoneId
    Prayer.entries.forEach { prayer ->
        val at = schedule.today[prayer]
        val isNext = next.prayer == prayer && next.at == at
        PrayerRow(
            name = stringResource(prayer.labelRes(isFriday && prayer == Prayer.DHUHR)),
            time = timeFormatter.format(at.atZone(zone)),
            isNext = isNext,
            isMuted = (at.isBefore(now) && !isNext) || prayer == Prayer.SUNRISE,
            mode = alerts.modeFor(prayer),
            onCycleAlert = { onCycleAlert(prayer) }
        )
    }
}

@Composable
private fun PrayerRow(name: String, time: String, isNext: Boolean, isMuted: Boolean, mode: AlertMode, onCycleAlert: () -> Unit) {
    val sleek = DeenTheme.look.sleek
    val gradient = DeenTheme.gradient
    val color = when {
        isNext && sleek -> gradient.content
        isNext -> MaterialTheme.colorScheme.primary
        isMuted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val weight = if (isNext) FontWeight.Bold else FontWeight.Normal
    val pill by animateFloatAsState(
        if (isNext) 1f else 0f,
        if (DeenTheme.look.motion) tween(500, easing = DeenMotion.EaseOutSoft) else snap(),
        label = "nextPill"
    )
    val shownColor by animateColorAsState(color, if (DeenTheme.look.motion) tween(400) else snap(), label = "rowColor")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                when {
                    sleek -> Modifier.background(gradient.brush, CircleShape, alpha = pill)
                    isNext -> Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
                    else -> Modifier
                }
            )
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = weight, color = shownColor, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = weight, color = shownColor, textAlign = TextAlign.End)
        val modeLabel = stringResource(mode.labelRes())
        val description = stringResource(R.string.feature_today_alert_for, name, modeLabel)
        IconButton(onClick = onCycleAlert, modifier = Modifier.semantics { contentDescription = description }) {
            Icon(
                painter = painterResource(mode.iconRes()),
                contentDescription = null,
                tint = when {
                    isNext && sleek -> gradient.content
                    mode == AlertMode.SILENT -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun AlertMode.iconRes(): Int = when (this) {
    AlertMode.ADHAN -> DeenIcons.Adhan
    AlertMode.NOTIFICATION -> DeenIcons.Notification
    AlertMode.SILENT -> DeenIcons.Silent
}

private fun AlertMode.labelRes(): Int = when (this) {
    AlertMode.ADHAN -> R.string.feature_today_mode_adhan
    AlertMode.NOTIFICATION -> R.string.feature_today_mode_notification
    AlertMode.SILENT -> R.string.feature_today_mode_silent
}

/** Shown while notifications or exact alarms are off; opens the reliability checklist. */
@Composable
private fun SetupBanner(onOpenReliability: () -> Unit) {
    DeenCard(onClick = onOpenReliability, containerColor = MaterialTheme.colorScheme.errorContainer) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(DeenIcons.Problem), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feature_today_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    stringResource(R.string.feature_today_setup_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
