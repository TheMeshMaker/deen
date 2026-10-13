package io.github.themeshmaker.dunya.feature.adhkar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.GradientHero
import io.github.themeshmaker.dunya.core.designsystem.component.RollingText
import io.github.themeshmaker.dunya.core.designsystem.component.SectionLabel
import io.github.themeshmaker.dunya.core.designsystem.component.ThinProgress
import io.github.themeshmaker.dunya.core.designsystem.component.paddingExceptBottom
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.TasbihDhikr
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberHaptics
import io.github.themeshmaker.dunya.core.ui.text
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------- Tab

@Composable
internal fun AdhkarHomeScreenRoute(
    onOpenCategory: (String) -> Unit,
    onOpenTasbih: () -> Unit,
    viewModel: AdhkarHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val look = DeenTheme.look
    val entrance = rememberEntrance()
    DeenTabScaffold(title = stringResource(R.string.feature_adhkar_title)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .paddingExceptBottom(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter)
                .padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            Box(Modifier.rise(entrance, 0)) { TasbihCard(state, onOpenTasbih) }
            val suggested = state.categories.firstOrNull { it.id == state.suggested }
            if (suggested != null) {
                SectionLabel(stringResource(R.string.feature_adhkar_now))
                CategoryCard(suggested, state.done[suggested.id] ?: 0, onClick = {
                    onOpenCategory(suggested.id)
                }, modifier = Modifier.rise(entrance, 1))
            }
            SectionLabel(stringResource(R.string.feature_adhkar_all))
            state.categories.filter { it.id != state.suggested }.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(look.gap), modifier = Modifier.rise(entrance, 2 + rowIndex)) {
                    row.forEach { category ->
                        CategoryCard(category, state.done[category.id] ?: 0, onClick = {
                            onOpenCategory(category.id)
                        }, modifier = Modifier.weight(1f), compact = true)
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(
                stringResource(R.string.feature_adhkar_review_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TasbihCard(state: AdhkarHomeUiState, onOpen: () -> Unit) {
    val gradient = DeenTheme.gradient
    val todayTotal = if (state.tasbih.day == state.today) state.tasbih.todayTotal else 0
    GradientHero(shape = DeenTheme.look.cardShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_tasbih_title), style = MaterialTheme.typography.titleLarge, color = gradient.content)
                Text(
                    stringResource(R.string.feature_tasbih_today, localDigits(todayTotal)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = gradient.content.copy(alpha = 0.85f)
                )
            }
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = gradient.content, contentColor = gradient.start)
            ) {
                Text(stringResource(R.string.feature_tasbih_open))
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: AdhkarCategory,
    done: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val total = category.items.size
    DeenCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(if (compact) 36.dp else 44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(category.icon()), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            if (!compact) {
                Column(Modifier.weight(1f)) {
                    Text(category.title.text(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.feature_adhkar_progress, localDigits(done), localDigits(total)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (compact) {
            Text(category.title.text(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            Text(
                stringResource(R.string.feature_adhkar_progress, localDigits(done), localDigits(total)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ThinProgress(if (total == 0) 0f else done / total.toFloat(), Modifier.padding(top = 10.dp))
    }
}

internal fun AdhkarCategory.icon(): Int = when (id) {
    AdhkarCategory.MORNING -> DeenIcons.Twilight
    AdhkarCategory.EVENING -> DeenIcons.Evening
    AdhkarCategory.AFTER_PRAYER -> DeenIcons.Adhkar
    AdhkarCategory.SLEEP -> DeenIcons.Bed
    "waking" -> DeenIcons.Sun
    AdhkarCategory.ADHAN -> DeenIcons.Adhan
    AdhkarCategory.MOSQUE -> DeenIcons.Mosque
    "wudu" -> DeenIcons.Water
    "home" -> DeenIcons.Home
    else -> DeenIcons.Adhkar
}

// ---------------------------------------------------------------------------------------------- Reader

@Composable
internal fun AdhkarReaderScreenRoute(onBack: () -> Unit, viewModel: AdhkarReaderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(state.reading.haptics)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val category = state.category
    DeenScaffold(
        title = category?.title?.text().orEmpty(),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back),
        actions = {
            IconButton(onClick = { confirmReset = true }) {
                Icon(painterResource(DeenIcons.Reset), contentDescription = stringResource(R.string.feature_adhkar_reset))
            }
        }
    ) { padding ->
        if (category == null) {
            CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            return@DeenScaffold
        }
        val look = DeenTheme.look
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            item(key = "progress") {
                Column {
                    Text(
                        stringResource(R.string.feature_adhkar_progress, localDigits(state.doneCount), localDigits(state.items.size)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ThinProgress(
                        if (state.items.isEmpty()) 0f else state.doneCount / state.items.size.toFloat(),
                        Modifier.padding(top = 6.dp)
                    )
                    Text(
                        stringResource(R.string.feature_adhkar_tap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            itemsIndexed(state.items, key = { _, item -> item.dhikr.id }) { index, item ->
                DhikrCard(
                    item = item,
                    scale = state.reading.arabicScale,
                    showTranslation = state.reading.showTranslation && !isArabicUi,
                    onCount = {
                        val completed = viewModel.count(item)
                        if (completed) {
                            haptics.done()
                            // Move on to the next one that still needs saying.
                            val next = state.items.withIndex().firstOrNull { (i, v) -> i > index && v.count < v.dhikr.count }?.index
                            if (next != null) scope.launch { listState.animateScrollToItem(next + 1) }
                        } else if (item.count < item.dhikr.count) {
                            haptics.tick()
                        }
                    }
                )
            }
            if (state.items.isNotEmpty() && state.doneCount == state.items.size) {
                item(key = "done") {
                    Text(
                        stringResource(R.string.feature_adhkar_all_done),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.feature_adhkar_reset)) },
            text = { Text(stringResource(R.string.feature_adhkar_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    confirmReset = false
                }) { Text(stringResource(R.string.feature_adhkar_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }
}

@Composable
private fun DhikrCard(item: DhikrView, scale: Float, showTranslation: Boolean, onCount: () -> Unit) {
    val dhikr = item.dhikr
    val done = item.count >= dhikr.count
    var showDetails by rememberSaveable(dhikr.id) { mutableStateOf(false) }
    val remaining = (dhikr.count - item.count).coerceAtLeast(0)
    val countLabel = stringResource(R.string.feature_adhkar_count_label, localDigits(item.count), localDigits(dhikr.count))
    val fade by animateFloatAsState(if (done) 0.6f else 1f, if (DeenTheme.look.motion) tween(450) else snap(), label = "done")
    DeenCard(
        onClick = onCount,
        modifier = Modifier
            .alpha(fade)
            .animateContentSize()
            .semantics { contentDescription = countLabel }
    ) {
        dhikr.instruction?.let {
            Text(
                it.text(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        val base = if (dhikr.isQuran) DeenTextStyles.quran else DeenTextStyles.arabicBody
        Text(
            item.arabic,
            style = base.copy(fontSize = base.fontSize * scale, lineHeight = base.lineHeight * scale, textDirection = TextDirection.Rtl),
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        if (showTranslation) {
            Text(dhikr.translation, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        val reward = dhikr.virtue ?: dhikr.sunnah
        if (reward != null) RewardNote(reward.text(), stated = dhikr.virtue != null, Modifier.padding(top = 12.dp))
        if (showDetails) {
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Text(dhikr.reference.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(stringResource(if (showDetails) R.string.feature_adhkar_less else R.string.feature_adhkar_source))
            }
            Spacer(Modifier.weight(1f))
            CountBubble(done = done, remaining = remaining, total = dhikr.count)
        }
    }
}

/** What saying it earns ([stated] in the hadith), or, when no reward is stated, how the Prophet ﷺ used it. */
@Composable
private fun RewardNote(text: String, stated: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tint = if (stated) colors.primary else colors.onSurfaceVariant
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = if (stated) 0.08f else 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painterResource(if (stated) DeenIcons.Reward else DeenIcons.Hadith),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column {
            Text(
                stringResource(if (stated) R.string.feature_adhkar_reward else R.string.feature_adhkar_sunnah),
                style = MaterialTheme.typography.labelLarge,
                color = tint
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

@Composable
private fun CountBubble(done: Boolean, remaining: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    val motion = DeenTheme.look.motion
    val fill by animateColorAsState(
        if (done) colors.primary else colors.primary.copy(alpha = 0.12f),
        if (motion) tween(300) else snap(),
        label = "bubble"
    )
    // A soft pulse on every count.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(remaining) {
        if (motion && remaining < total) {
            pulse.snapTo(1.14f)
            pulse.animateTo(1f, DeenMotion.bouncy())
        }
    }
    Box(
        Modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = done,
            transitionSpec = {
                if (motion) {
                    (scaleIn(DeenMotion.bouncy(), initialScale = 0.3f) + fadeIn(tween(150))) togetherWith
                        (scaleOut(tween(150), targetScale = 0.6f) + fadeOut(tween(120)))
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            label = "check"
        ) { isDone ->
            if (isDone) {
                Icon(painterResource(DeenIcons.Check), contentDescription = null, tint = colors.onPrimary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RollingText(
                        localDigits(remaining),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                        down = true
                    )
                    if (total > 1) Text("/" + localDigits(total), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
            }
        }
    }
}

/** Phrases for the tasbih picker: Arabic always, English meaning in the English UI. */
@Composable
internal fun TasbihDhikr.label(): String = if (isArabicUi) arabic else english
