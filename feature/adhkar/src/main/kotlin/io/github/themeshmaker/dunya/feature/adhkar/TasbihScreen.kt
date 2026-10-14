package io.github.themeshmaker.dunya.feature.adhkar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.RollingText
import io.github.themeshmaker.dunya.core.designsystem.component.rememberPatternDrift
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.TasbihDhikr
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberHaptics
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val TARGETS = listOf(33, 99, 100, 0)

@Composable
internal fun TasbihScreenRoute(onBack: () -> Unit, viewModel: TasbihViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reading by viewModel.reading.collectAsStateWithLifecycle()
    val challenge by viewModel.challenge.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(reading.haptics)
    DeenScaffold(
        title = stringResource(R.string.feature_tasbih_title),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back),
        largeTitle = false,
        actions = {
            IconButton(onClick = viewModel::reset) {
                Icon(painterResource(DeenIcons.Reset), contentDescription = stringResource(R.string.feature_tasbih_reset))
            }
        }
    ) { padding ->
        val tasbih = state ?: return@DeenScaffold
        val dhikr = TasbihDhikr.byId(tasbih.dhikrId)
        val look = DeenTheme.look
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            challenge?.let { goal ->
                ChallengeBanner(
                    goal,
                    set = tasbih.dhikrId == goal.dhikr.id && tasbih.target == goal.count,
                    onTake = viewModel::takeChallenge
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(TasbihDhikr.entries, key = { it.id }) { option ->
                    FilterChip(
                        selected = option == dhikr,
                        onClick = { viewModel.setDhikr(option.id) },
                        label = { Text(option.arabic, style = DeenTextStyles.arabicBody.copy(fontSize = 16.sp, lineHeight = 26.sp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Text(dhikr.arabic, style = DeenTextStyles.arabicBody.copy(fontSize = 30.sp, lineHeight = 52.sp), textAlign = TextAlign.Center)
            Text(dhikr.english, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            CounterRing(
                count = tasbih.count,
                target = tasbih.target,
                onTap = {
                    val completed = viewModel.count()
                    if (completed) haptics.done() else haptics.tick()
                    completed
                },
                label = stringResource(R.string.feature_tasbih_tap, localDigits(tasbih.count))
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TARGETS.forEach { target ->
                    FilterChip(
                        selected = tasbih.target == target,
                        onClick = { viewModel.setTarget(target) },
                        label = { Text(if (target == 0) "∞" else localDigits(target)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            DeenCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat(stringResource(R.string.feature_tasbih_today_label), localDigits(tasbih.todayTotal))
                    Stat(stringResource(R.string.feature_tasbih_all_time), localDigits(tasbih.allTimeTotal))
                }
            }
        }
    }
}

/** Today's tasbih challenge: what to say and how many times, done or (if not set up yet) a button to set it up. */
@Composable
private fun ChallengeBanner(goal: TasbihChallenge, set: Boolean, onTake: () -> Unit) {
    DeenCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(if (goal.done) DeenIcons.Check else DeenIcons.Reward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    stringResource(if (goal.done) R.string.feature_tasbih_challenge_done else R.string.feature_tasbih_challenge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${goal.dhikr.arabic} · ${pluralStringResource(
                        R.plurals.feature_adhkar_challenge_times,
                        goal.count,
                        localDigits(goal.count)
                    )}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!goal.done && !set) {
                TextButton(onClick = onTake) { Text(stringResource(R.string.feature_tasbih_challenge_take)) }
            }
        }
    }
}

/** The big tap target: a ring that fills towards the target, the count in the middle. */
@Composable
private fun CounterRing(count: Int, target: Int, onTap: () -> Boolean, label: String) {
    val gradient = DeenTheme.gradient
    val colors = MaterialTheme.colorScheme
    val motion = DeenTheme.look.motion
    val progress by animateFloatAsState(
        if (target >
            0
        ) {
            count / target.toFloat()
        } else {
            0f
        },
        if (motion) DeenMotion.snappy() else spring(),
        label = "tasbih"
    )
    val scope = rememberCoroutineScope()
    // Each tap: the bead presses in and springs back, and a ring ripples outwards.
    val press = remember { Animatable(1f) }
    val ripples = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }
    // A completed round: a wider ring of light and a burst of small beads.
    val burst = remember { Animatable(0f) }
    Box(
        Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                val completed = onTap()
                if (!motion) return@clickable
                scope.launch {
                    press.snapTo(0.94f)
                    press.animateTo(1f, DeenMotion.bouncy())
                }
                val ripple = Animatable(0f)
                if (ripples.size >= MAX_RIPPLES) ripples.removeAt(0)
                ripples += ripple
                scope.launch {
                    ripple.animateTo(1f, tween(750, easing = DeenMotion.EaseOutSoft))
                    ripples.remove(ripple)
                }
                if (completed) {
                    scope.launch {
                        burst.snapTo(0f)
                        burst.animateTo(1f, tween(1_000, easing = DeenMotion.EaseOutSoft))
                        burst.snapTo(0f)
                    }
                }
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val base = size.minDimension / 2f - 18.dp.toPx()
            ripples.forEach { r ->
                val v = r.value
                drawCircle(
                    colors.primary.copy(alpha = (1f - v) * 0.35f),
                    radius = base * (1f + 0.22f * v),
                    style = Stroke((1f - v) * 6.dp.toPx() + 1f)
                )
            }
            val b = burst.value
            if (b > 0f) {
                drawCircle(
                    gradient.end.copy(alpha = (1f - b) * 0.5f),
                    radius = base * (1f + 0.35f * b),
                    style = Stroke(10.dp.toPx() * (1f - b) + 1f)
                )
                for (i in 0 until BURST_BEADS) {
                    val angle = Math.toRadians(i * 360.0 / BURST_BEADS - 90.0)
                    val distance = base * (1.02f + 0.3f * b)
                    drawCircle(
                        colors.primary.copy(alpha = 1f - b),
                        radius = 5.dp.toPx() * (1f - b * 0.6f),
                        center = Offset(center.x + distance * cos(angle).toFloat(), center.y + distance * sin(angle).toFloat())
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(18.dp)
                .graphicsLayer {
                    val s = press.value * (1f + 0.05f * sin(burst.value * Math.PI).toFloat())
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(gradient.brush)
        ) {
            GeometricPattern(
                color = gradient.pattern,
                modifier = Modifier.matchParentSize(),
                cell = 36.dp,
                offsetX = rememberPatternDrift(36.dp, 30_000)
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = colors.primary.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke)
            )
            if (target > 0) {
                drawArc(
                    color = colors.primary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(
                localDigits(count),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                color = gradient.content,
                fontWeight = FontWeight.Light
            )
            if (target > 0) {
                Text("/ " + localDigits(target), style = MaterialTheme.typography.titleMedium, color = gradient.content.copy(alpha = 0.8f))
            }
        }
    }
}

private const val MAX_RIPPLES = 4
private const val BURST_BEADS = 12

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
