package io.github.themeshmaker.dunya.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import io.github.themeshmaker.dunya.core.designsystem.component.LocalFloatingBarInset
import io.github.themeshmaker.dunya.core.designsystem.component.LocalOnGradient
import io.github.themeshmaker.dunya.core.designsystem.component.PRESS_CONTROL
import io.github.themeshmaker.dunya.core.designsystem.component.RollingText
import io.github.themeshmaker.dunya.core.designsystem.component.ThinProgress
import io.github.themeshmaker.dunya.core.designsystem.component.liquidGlass
import io.github.themeshmaker.dunya.core.designsystem.component.pageBackground
import io.github.themeshmaker.dunya.core.designsystem.component.pebble
import io.github.themeshmaker.dunya.core.designsystem.component.pressScale
import io.github.themeshmaker.dunya.core.designsystem.component.rememberDrawIn
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rememberPatternDrift
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.component.screen
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenOnGradient
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.KhatmahProgress
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.domain.PrayerTracker
import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.prayer.IslamicEvents
import io.github.themeshmaker.dunya.core.prayer.NextPrayer
import io.github.themeshmaker.dunya.core.prayer.NightTimes
import io.github.themeshmaker.dunya.core.prayer.UpcomingEvent
import io.github.themeshmaker.dunya.core.ui.DailyFactCard
import io.github.themeshmaker.dunya.core.ui.DailyHadithCard
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.eventWhen
import io.github.themeshmaker.dunya.core.ui.hijriDateText
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberDateFormatter
import io.github.themeshmaker.dunya.core.ui.rememberShortDateFormatter
import io.github.themeshmaker.dunya.core.ui.rememberTimeFormatter
import io.github.themeshmaker.dunya.core.ui.shortLabelRes
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.math.cos
import kotlin.math.sin

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
    val onOpenFacts: () -> Unit,
    val onOpenTracker: () -> Unit,
    val onOpenCalendar: () -> Unit
)

/** What the prayer rows can change: the alert mode, and whether the prayer was prayed. */
internal class PrayerRowActions(val onCycleAlert: (Prayer) -> Unit, val onTogglePrayed: (Prayer, Boolean) -> Unit)

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

        is TodayUiState.Ready -> {
            val rows = PrayerRowActions(
                onCycleAlert = viewModel::cycleAlert,
                onTogglePrayed = { prayer, prayed -> viewModel.setPrayed(s.schedule.date, prayer, prayed) }
            )
            val look = DeenTheme.look
            when {
                look.liquid -> LiquidToday(s, extras, actions, rows)

                // ColorOS: laid out like its Weather app, on the theme's gradient with frosted cards.
                look.colorOs -> LiquidToday(s, extras, actions, rows, ownBackground = true)

                look.sleek -> SleekToday(s, extras, actions, rows)

                else -> ClassicToday(s, extras, actions, rows)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Classic

@Composable
internal fun ClassicToday(state: TodayUiState.Ready, extras: TodayExtras, actions: TodayActions, rows: PrayerRowActions) {
    val schedule = state.schedule
    val zone = schedule.location.zoneId
    val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY
    val look = DeenTheme.look
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground(look)
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
            PrayerList(state, extras.prayed[schedule.date].orEmpty(), isFriday, rows)
        }
        BelowTheTimes(state, extras, actions)
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
internal fun SleekToday(state: TodayUiState.Ready, extras: TodayExtras, actions: TodayActions, rows: PrayerRowActions) {
    val schedule = state.schedule
    val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY
    val look = DeenTheme.look
    val gradient = DeenTheme.gradient
    val scroll = rememberScrollState()
    val entrance = rememberEntrance()
    val drift = rememberPatternDrift(PATTERN_CELL)
    LightSystemBarIcons()
    Box(
        Modifier
            .fillMaxSize()
            .pageBackground(look)
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
                        DateLine(schedule)
                        RingCountdown(state, isFriday, Modifier.padding(top = 20.dp))
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
                    PrayerList(state, extras.prayed[schedule.date].orEmpty(), isFriday, rows)
                }
                if (state.alarmSetupNeeded) Box(Modifier.rise(entrance, 1)) { SetupBanner(actions.onOpenReliability) }
                Box(Modifier.rise(entrance, 2)) { QuickActions(actions) }
                BelowTheTimes(state, extras, actions, entrance, firstIndex = 3)
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

// ---------------------------------------------------------------------------------------------- Liquid

/**
 * Liquid, like Today on iOS: the theme's full gradient (the app's backdrop) behind everything, the
 * countdown ring on it, and the times, actions and cards as panes of dark glass that bend it.
 */
@Composable
internal fun LiquidToday(
    state: TodayUiState.Ready,
    extras: TodayExtras,
    actions: TodayActions,
    rows: PrayerRowActions,
    ownBackground: Boolean = false
) {
    val schedule = state.schedule
    val isFriday = schedule.date.dayOfWeek == DayOfWeek.FRIDAY
    val gradient = DeenTheme.gradient
    val entrance = rememberEntrance()
    LightSystemBarIcons()
    // Liquid's gradient is the app's backdrop; the ColorOS style draws its own here.
    if (ownBackground) {
        Box(
            Modifier
                .fillMaxSize()
                .background(gradient.brush)
        ) {
            GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize(), cell = PATTERN_CELL)
        }
    }
    DeenOnGradient {
        CompositionLocalProvider(LocalContentColor provides gradient.content, LocalOnGradient provides ownBackground) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.screen)
                    .padding(horizontal = DeenTheme.look.gutter),
                verticalArrangement = Arrangement.spacedBy(DeenTheme.look.gap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassChip(onClick = actions.onChangeLocation) {
                        Icon(painterResource(DeenIcons.Location), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            schedule.location.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    GlassChip(onClick = actions.onOpenSettings, circle = true) {
                        Icon(
                            painterResource(DeenIcons.Settings),
                            contentDescription = stringResource(R.string.feature_today_settings),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                DateLine(schedule)
                RingCountdown(state, isFriday, Modifier.padding(vertical = 8.dp))
                DeenCard(contentPadding = PaddingValues(vertical = 10.dp), modifier = Modifier.rise(entrance, 0, distance = 40.dp)) {
                    PrayerList(state, extras.prayed[schedule.date].orEmpty(), isFriday, rows)
                }
                if (state.alarmSetupNeeded) Box(Modifier.rise(entrance, 1)) { SetupBanner(actions.onOpenReliability) }
                Box(Modifier.rise(entrance, 2)) { QuickActions(actions) }
                BelowTheTimes(state, extras, actions, entrance, firstIndex = 3)
                // Clear of the floating tab bar.
                Spacer(Modifier.navigationBarsPadding().height(LocalFloatingBarInset.current))
            }
        }
    }
    if (ownBackground) {
        // Content scrolling under the status bar fades into the gradient instead of colliding with the icons.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 12.dp)))
                .background(Brush.verticalGradient(listOf(gradient.start, gradient.start.copy(alpha = 0f))))
        )
    }
}

/** A small capsule (or circle) of glass with content in it, e.g. the place name. */
@Composable
private fun GlassChip(onClick: () -> Unit, circle: Boolean = false, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .then(if (circle) Modifier.size(44.dp) else Modifier.height(40.dp))
            .then(
                if (DeenTheme.look.colorOs) {
                    Modifier.pebble(RoundedCornerShape(percent = 50), interaction)
                } else {
                    Modifier.liquidGlass(RoundedCornerShape(percent = 50), interactionSource = interaction, pressScale = PRESS_CONTROL)
                }
            )
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (circle) 0.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (circle) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { content() } else content()
    }
}

// ---------------------------------------------------------------------------------------------- Shared

@Composable
private fun DateLine(schedule: PrayerSchedule) {
    val gradient = DeenTheme.gradient
    Text(
        "${rememberDateFormatter().format(schedule.date)} · ${hijriDateText(schedule.hijriDate)}",
        style = MaterialTheme.typography.bodyMedium,
        color = gradient.content.copy(alpha = 0.8f),
        textAlign = TextAlign.Center
    )
}

/** The next prayer's name, countdown and time inside the ring. */
@Composable
private fun RingCountdown(state: TodayUiState.Ready, isFriday: Boolean, modifier: Modifier = Modifier) {
    val gradient = DeenTheme.gradient
    val timeFormatter = rememberTimeFormatter()
    val zone = state.schedule.location.zoneId
    val name = stringResource(state.next.prayer.labelRes(isFriday && state.next.prayer == Prayer.DHUHR))
    Box(
        modifier
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
            ) { shown -> Text(shown, style = MaterialTheme.typography.headlineMedium, color = gradient.content) }
            CompositionLocalProvider(LocalContentColor provides gradient.content) {
                Countdown(Duration.between(state.now, state.next.at).coerceAtLeast(Duration.ZERO), name, 34f)
            }
            Text(
                stringResource(R.string.feature_today_at, timeFormatter.format(state.next.at.atZone(zone))),
                style = MaterialTheme.typography.titleMedium,
                color = gradient.content.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Time elapsed towards the next prayer. Sleek adds a soft light behind the countdown that slowly
 * breathes, an arc that brightens towards its end, and a glowing tip where time has reached.
 */
@Composable
private fun ProgressRing(progress: Float, color: Color) {
    val look = DeenTheme.look
    // (Not in Liquid: a light that never stops changing would keep every pane of glass re-rendering.)
    val breath = if (look.motion && !look.liquid) {
        val value by rememberInfiniteTransition(label = "halo").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breath"
        )
        value
    } else {
        0.5f
    }
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val shown = progress.coerceIn(0f, 1f)
        if (look.sleek) {
            val halo = size.minDimension * (0.52f + 0.05f * breath)
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = 0.10f + 0.07f * breath), Color.Transparent), center, halo),
                radius = halo
            )
        }
        drawArc(color.copy(alpha = 0.2f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        if (!look.sleek) {
            drawArc(color, -90f, 360f * shown, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            return@Canvas
        }
        if (shown <= 0f) return@Canvas
        rotate(-90f) {
            drawArc(
                Brush.sweepGradient(0f to color.copy(alpha = 0.35f), shown to color, 1f to color.copy(alpha = 0.35f), center = center),
                0f,
                360f * shown,
                false,
                Offset(inset, inset),
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        val angle = Math.toRadians(360.0 * shown - 90.0)
        val radius = (size.minDimension - stroke) / 2f
        val tip = center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
        drawCircle(color.copy(alpha = 0.18f + 0.12f * breath), radius = stroke * 1.9f, center = tip)
        drawCircle(color, radius = stroke * 0.95f, center = tip)
    }
}

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
        QuickAction(DeenIcons.Ok, stringResource(R.string.feature_today_tracker_short), actions.onOpenTracker)
    }
}

@Composable
private fun QuickAction(icon: Int, label: String, onClick: () -> Unit) {
    val look = DeenTheme.look
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .pressScale(interaction, enabled = look.motion && !look.liquid && !look.colorOs, pressed = 0.9f)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interaction,
                indication = if (look.liquid || look.colorOs) null else androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .then(
                    when {
                        look.colorOs -> Modifier.pebble(CircleShape, interaction)

                        look.liquid -> Modifier.liquidGlass(
                            RoundedCornerShape(percent = 50),
                            interactionSource = interaction,
                            pressScale = PRESS_CONTROL
                        )

                        look.sleek ->
                            Modifier
                                .clip(CircleShape)
                                .background(look.card)
                                .border(look.cardBorder ?: BorderStroke(0.dp, Color.Transparent), CircleShape)

                        else ->
                            Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (look.liquid || look.colorOs) LocalContentColor.current else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1
        )
    }
}

/** The tracker, the next Islamic day, the khatmah or continue reading, hadith and fact of the day, and the method chip. */
@Composable
private fun BelowTheTimes(
    state: TodayUiState.Ready,
    extras: TodayExtras,
    actions: TodayActions,
    entrance: Entrance? = null,
    firstIndex: Int = 0
) {
    val schedule = state.schedule
    fun Modifier.enter(i: Int) = if (entrance != null) rise(entrance, firstIndex + i) else this
    TrackerCard(extras.prayed, schedule.date, actions.onOpenTracker, Modifier.enter(0))
    val event = remember(schedule.date, schedule.settings.hijriOffsetDays) {
        IslamicEvents.next(schedule.date, schedule.settings.hijriOffsetDays)
    }
    if (event != null) UpcomingCard(event, schedule.date, actions.onOpenCalendar, Modifier.enter(1))

    val plan = extras.khatmah
    val pages = extras.pages
    val lastRead = extras.lastRead
    val surah = extras.lastReadSurah
    if (plan != null && pages != null) {
        val progress = remember(plan, lastRead, pages, schedule.date) { KhatmahProgress.of(plan, lastRead, pages, schedule.date) }
        val target = lastRead?.takeIf { pages.pageOf(it) >= plan.startPage && !progress.complete } ?: pages.start(progress.todayFrom)
        KhatmahCard(progress, onOpen = { actions.onOpenQuran(target.surah, target.ayah) }, modifier = Modifier.enter(2))
    } else if (lastRead != null && surah != null) {
        DeenCard(onClick = { actions.onOpenQuran(lastRead.surah, lastRead.ayah) }, modifier = Modifier.enter(2)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(DeenIcons.Quran)
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
        DeenCard(onClick = { actions.onOpenQuran(1, 1) }, modifier = Modifier.enter(2)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(DeenIcons.Quran)
                Text(
                    stringResource(R.string.feature_today_start_quran),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    extras.hadith?.let { DailyHadithCard(it, onClick = { actions.onOpenHadith(it.number) }, modifier = Modifier.enter(3)) }
    extras.fact?.let { DailyFactCard(it, onClick = actions.onOpenFacts, modifier = Modifier.enter(4)) }
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
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    )
    Spacer(Modifier.height(16.dp))
}

/** A small rounded square in the primary colour holding an icon, at the start of a card. */
@Composable
private fun IconTile(icon: Int) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

/** This week at a glance: a ring per day filling with each prayer ticked off, and the streak. */
@Composable
private fun TrackerCard(prayed: Map<LocalDate, Set<Prayer>>, today: LocalDate, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val streak = remember(prayed, today) { PrayerTracker.streak(prayed, today) }
    val todayCount = PrayerTracker.count(prayed, today)
    DeenCard(onClick = onOpen, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(DeenIcons.Ok)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feature_today_tracker_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (streak > 0) {
                        pluralStringResource(R.plurals.feature_today_streak, streak, localDigits(streak))
                    } else {
                        stringResource(R.string.feature_today_tracker_hint)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                stringResource(R.string.feature_today_tracker_today, localDigits(todayCount), localDigits(PrayerTracker.prayers.size)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        WeekStrip(prayed, today, Modifier.padding(top = 14.dp))
    }
}

@Composable
internal fun WeekStrip(prayed: Map<LocalDate, Set<Prayer>>, today: LocalDate, modifier: Modifier = Modifier) {
    val locale = currentLocale
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (6 downTo 0).forEach { back ->
            val day = today.minusDays(back.toLong())
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (back == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (back == 0) FontWeight.SemiBold else FontWeight.Normal
                )
                DayRing(PrayerTracker.count(prayed, day), PrayerTracker.prayers.size, highlight = back == 0)
            }
        }
    }
}

/** A ring in five segments, one per prayer; a full day becomes a filled disc with a tick. */
@Composable
internal fun DayRing(count: Int, of: Int, highlight: Boolean, size: androidx.compose.ui.unit.Dp = 30.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val shown by animateFloatAsState(count.toFloat(), if (DeenTheme.look.motion) DeenMotion.snappy() else snap(), label = "dayRing")
    Canvas(Modifier.size(size)) {
        val stroke = 3.5.dp.toPx()
        val inset = stroke / 2
        val arc = Size(this.size.width - stroke, this.size.height - stroke)
        val gap = 14f
        val sweep = 360f / of
        if (count >= of) {
            drawCircle(primary)
            val tick = Path().apply {
                moveTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.52f)
                lineTo(this@Canvas.size.width * 0.45f, this@Canvas.size.height * 0.66f)
                lineTo(this@Canvas.size.width * 0.71f, this@Canvas.size.height * 0.38f)
            }
            drawPath(tick, onPrimary, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            return@Canvas
        }
        repeat(of) { i ->
            val fill = (shown - i).coerceIn(0f, 1f)
            val start = -90f + i * sweep + gap / 2
            drawArc(track, start, sweep - gap, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (fill > 0f) {
                drawArc(
                    primary,
                    start,
                    (sweep - gap) * fill,
                    false,
                    Offset(inset, inset),
                    arc,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        if (highlight) drawCircle(primary, radius = 2.dp.toPx())
    }
}

/** The next day of the Islamic year (or the white days), with how far off it is. */
@Composable
private fun UpcomingCard(event: UpcomingEvent, today: LocalDate, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val dates = rememberShortDateFormatter()
    DeenCard(onClick = onOpen, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(DeenIcons.Calendar)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feature_today_upcoming),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(event.event.labelRes()), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${dates.format(event.date)} · ${hijriDateText(event.hijri)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                eventWhen(event.daysAway, started = event.date.isBefore(today)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

/** The khatmah plan: today's portion and the way through the whole Quran. */
@Composable
private fun KhatmahCard(progress: KhatmahProgress, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    DeenCard(onClick = onOpen, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(DeenIcons.Quran)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        R.string.feature_today_khatmah_day,
                        localDigits(progress.day.coerceAtMost(progress.plan.days)),
                        localDigits(progress.plan.days)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    when {
                        progress.complete -> stringResource(R.string.feature_today_khatmah_complete)

                        progress.pagesLeftToday == 0 -> stringResource(R.string.feature_today_khatmah_done_today)

                        else -> stringResource(
                            R.string.feature_today_khatmah_today,
                            localDigits(progress.todayFrom),
                            localDigits(progress.todayTo)
                        )
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (!progress.complete && progress.pagesLeftToday > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.feature_today_khatmah_left,
                            progress.pagesLeftToday,
                            localDigits(progress.pagesLeftToday)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${localDigits((progress.fraction * 100).toInt())}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ThinProgress(progress.fraction, Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PrayerList(state: TodayUiState.Ready, prayed: Set<Prayer>, isFriday: Boolean, rows: PrayerRowActions) {
    val schedule = state.schedule
    val now = state.now
    val timeFormatter = rememberTimeFormatter()
    val zone = schedule.location.zoneId
    val ramadan = schedule.hijriDate.isRamadan
    Prayer.entries.forEach { prayer ->
        val at = schedule.today[prayer]
        val isNext = state.next.prayer == prayer && state.next.at == at
        val name = stringResource(prayer.labelRes(isFriday && prayer == Prayer.DHUHR))
        PrayerRow(
            name = name,
            time = timeFormatter.format(at.atZone(zone)),
            caption = when {
                ramadan && prayer == Prayer.FAJR -> stringResource(R.string.feature_today_suhoor_ends)
                ramadan && prayer == Prayer.MAGHRIB -> stringResource(R.string.feature_today_iftar)
                else -> null
            },
            isNext = isNext,
            isMuted = (at.isBefore(now) && !isNext) || prayer == Prayer.SUNRISE,
            mode = state.alerts.modeFor(prayer),
            onCycleAlert = { rows.onCycleAlert(prayer) },
            prayed = if (prayer.isPrayer) prayer in prayed else null,
            // A prayer can be ticked once its time has come.
            canTick = !at.isAfter(now),
            onTogglePrayed = { rows.onTogglePrayed(prayer, prayer !in prayed) }
        )
    }
    NightLine(remember(schedule.timeline, now.epochSecond / 60) { NightTimes.of(schedule.timeline, now) }, zone)
}

/** Under the times: the middle of the night and the start of its last third. */
@Composable
private fun NightLine(night: NightTimes, zone: ZoneId) {
    val timeFormatter = rememberTimeFormatter()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painterResource(DeenIcons.Night),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            stringResource(
                R.string.feature_today_night_line,
                timeFormatter.format(night.midnight.atZone(zone)),
                timeFormatter.format(night.lastThird.atZone(zone))
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrayerRow(
    name: String,
    time: String,
    caption: String?,
    isNext: Boolean,
    isMuted: Boolean,
    mode: AlertMode,
    onCycleAlert: () -> Unit,
    prayed: Boolean?,
    canTick: Boolean,
    onTogglePrayed: () -> Unit
) {
    val sleek = DeenTheme.look.sleek
    val gradient = DeenTheme.gradient
    // On the gradient pill (Sleek's next prayer) everything takes the gradient's own text colour.
    val onPill = isNext && sleek
    val color = when {
        onPill -> gradient.content
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
            .padding(start = 2.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prayed != null) {
            PrayedCheck(
                checked = prayed,
                enabled = canTick || prayed,
                color = if (onPill) gradient.content else MaterialTheme.colorScheme.primary,
                checkColor = if (onPill) gradient.start else MaterialTheme.colorScheme.onPrimary,
                description = stringResource(if (prayed) R.string.feature_today_prayed_undo else R.string.feature_today_prayed_mark, name),
                onToggle = onTogglePrayed
            )
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = weight, color = shownColor)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = shownColor.copy(alpha = 0.8f))
            }
        }
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = weight, color = shownColor, textAlign = TextAlign.End)
        val modeLabel = stringResource(mode.labelRes())
        val description = stringResource(R.string.feature_today_alert_for, name, modeLabel)
        IconButton(onClick = onCycleAlert, modifier = Modifier.semantics { contentDescription = description }) {
            Icon(
                painter = painterResource(mode.iconRes()),
                contentDescription = null,
                tint = when {
                    onPill -> gradient.content
                    mode == AlertMode.SILENT -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** A round tick-box: an outline until the prayer is marked, then it fills and the tick draws in. */
@Composable
private fun PrayedCheck(checked: Boolean, enabled: Boolean, color: Color, checkColor: Color, description: String, onToggle: () -> Unit) {
    val motion = DeenTheme.look.motion
    val haptics = LocalHapticFeedback.current
    val fill by animateFloatAsState(if (checked) 1f else 0f, if (motion) DeenMotion.bouncy() else snap(), label = "prayed")
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox) {
                if (motion) haptics.performHapticFeedback(if (it) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick)
                onToggle()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val ring = 1.6.dp.toPx()
            val radius = size.minDimension / 2
            drawCircle(color.copy(alpha = if (enabled) 0.6f else 0.25f), radius = radius - ring / 2, style = Stroke(ring))
            if (fill <= 0f) return@Canvas
            drawCircle(color, radius = radius * fill.coerceAtMost(1.08f))
            val tick = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.53f)
                lineTo(size.width * 0.44f, size.height * 0.68f)
                lineTo(size.width * 0.73f, size.height * 0.36f)
            }
            drawPath(
                tick,
                checkColor.copy(alpha = fill.coerceIn(0f, 1f)),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
