package io.github.themeshmaker.dunya.feature.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.audio.QuranAudioState
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.Tag
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.Reciter
import io.github.themeshmaker.dunya.core.model.Revelation
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.model.TafsirSource
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuranReaderScreenRoute(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenTafsir: (surah: Int, ayah: Int) -> Unit,
    viewModel: QuranReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerMode by viewModel.playerMode.collectAsStateWithLifecycle()
    val audio by viewModel.audioState.collectAsStateWithLifecycle()
    val highlight by viewModel.highlight.collectAsStateWithLifecycle()
    val tafsir by viewModel.tafsir.collectAsStateWithLifecycle()
    val surah = state.surah
    val arabicUi = isArabicUi

    var menuOpen by remember { mutableStateOf(false) }
    var actionsFor by rememberSaveable { mutableIntStateOf(0) }
    var tafsirFor by rememberSaveable { mutableIntStateOf(0) }
    var choosingReciter by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val playingHere = audio.active && audio.surah == surah?.number
    val reciter = Reciter.byId(state.reading.reciterId)

    DeenScaffold(
        title = surah?.let { surahTitle(it) }.orEmpty(),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back),
        largeTitle = false,
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(painterResource(DeenIcons.MoreVert), contentDescription = stringResource(R.string.feature_quran_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(20.dp)) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_quran_player_mode)) },
                    leadingIcon = { Icon(painterResource(DeenIcons.Player), contentDescription = null) },
                    trailingIcon = { Switch(checked = playerMode, onCheckedChange = null) },
                    onClick = {
                        if (playerMode) viewModel.closePlayer() else viewModel.playerMode.value = true
                        menuOpen = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_quran_surah_tafsir)) },
                    leadingIcon = { Icon(painterResource(DeenIcons.Tafsir), contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        surah?.let { onOpenTafsir(it.number, listState.firstVisibleItemIndex.coerceAtLeast(1)) }
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_quran_reciter_named, reciter.name(arabicUi))) },
                    leadingIcon = { Icon(painterResource(DeenIcons.Reciter), contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        choosingReciter = true
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = playerMode && surah != null,
                enter = slideInVertically(DeenMotion.snappy()) { it } + fadeIn(),
                exit = slideOutVertically(DeenMotion.snappy()) { it } + fadeOut()
            ) {
                PlayerBar(
                    audio = if (playingHere) audio else QuranAudioState(reciter = reciter),
                    reciterName = reciter.name(arabicUi),
                    onPlay = {
                        if (playingHere) {
                            viewModel.togglePlay()
                        } else {
                            val first = listState.firstVisibleItemIndex.coerceIn(1, state.ayahs.size.coerceAtLeast(1))
                            surah?.let { viewModel.playFrom(first, it.arabicName, arabicUi) }
                        }
                    },
                    onNext = viewModel::next,
                    onPrevious = viewModel::previous,
                    onReciter = { choosingReciter = true },
                    onClose = viewModel::closePlayer
                )
            }
        }
    ) { padding ->
        if (!state.loaded || surah == null) {
            CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            return@DeenScaffold
        }
        // Jump to the requested verse once, then start remembering what the reader looks at.
        var positioned by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(state.loaded) {
            if (!positioned) {
                if (state.startAyah > 1) listState.scrollToItem(state.startAyah.coerceAtMost(state.ayahs.size))
                positioned = true
            }
            snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { index ->
                if (positioned) viewModel.onVisible(index.coerceIn(1, state.ayahs.size))
            }
        }
        // Follow the recitation, unless the reader is scrolling on their own.
        val followAyah = highlight?.ayah
        LaunchedEffect(followAyah, playerMode) {
            if (followAyah != null && playerMode && !listState.isScrollInProgress) {
                val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
                val target = followAyah.coerceAtLeast(0)
                if (target !in visible.drop(1).dropLast(1)) listState.animateScrollToItem(target)
            }
        }
        val look = DeenTheme.look
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 32.dp)
        ) {
            item(key = "header") {
                SurahHeader(surah, state.ayahs.first().bismillah, state.reading.arabicScale, highlight?.takeIf { it.ayah == 0 }?.word)
            }
            itemsIndexed(state.ayahs, key = { _, ayah -> ayah.number }) { _, ayah ->
                AyahItem(
                    ayah = ayah,
                    juz = state.juzStarts[ayah.number],
                    scale = state.reading.arabicScale,
                    bookmarked = ayah.number in state.bookmarks,
                    playing = highlight?.ayah == ayah.number,
                    word = highlight?.takeIf { it.ayah == ayah.number }?.word,
                    onLongPress = { actionsFor = ayah.number }
                )
            }
            item(key = "footer") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (surah.number > 1) {
                        OutlinedButton(onClick = { onOpenSurah(surah.number - 1) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.feature_quran_previous))
                        }
                    }
                    if (surah.number < 114) {
                        OutlinedButton(onClick = { onOpenSurah(surah.number + 1) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.feature_quran_next))
                        }
                    }
                }
            }
        }
    }

    val context = LocalContext.current
    if (actionsFor > 0 && surah != null) {
        val ayah = state.ayahs.getOrNull(actionsFor - 1)
        if (ayah != null) {
            AyahActionsSheet(
                surah = surah,
                ayah = ayah,
                bookmarked = ayah.number in state.bookmarks,
                onDismiss = { actionsFor = 0 },
                onPlay = {
                    viewModel.playFrom(ayah.number, surah.arabicName, arabicUi)
                    actionsFor = 0
                },
                onTafsir = {
                    tafsirFor = ayah.number
                    viewModel.loadTafsir()
                    actionsFor = 0
                },
                onBookmark = {
                    viewModel.toggleBookmark(ayah.number)
                    actionsFor = 0
                },
                onCopy = {
                    context.copy(ayah.text, surah, ayah.number)
                    actionsFor = 0
                },
                onShare = {
                    context.shareAyah(ayah.text, surah, ayah.number)
                    actionsFor = 0
                }
            )
        }
    }
    if (tafsirFor > 0 && surah != null) {
        val ayah = state.ayahs.getOrNull(tafsirFor - 1)
        if (ayah != null) {
            TafsirSheet(
                surah = surah,
                ayah = ayah,
                state = tafsir,
                onSource = { viewModel.loadTafsir(it) },
                onRetry = { viewModel.loadTafsir(tafsir.source) },
                onOpenFull = {
                    tafsirFor = 0
                    onOpenTafsir(surah.number, ayah.number)
                },
                onDismiss = { tafsirFor = 0 }
            )
        }
    }
    if (choosingReciter) {
        ReciterDialog(selected = reciter, onSelect = {
            viewModel.setReciter(it)
            choosingReciter = false
        }, onDismiss = { choosingReciter = false })
    }
}

@Composable
private fun SurahHeader(surah: Surah, bismillah: String?, scale: Float, highlightWord: Int?) {
    DeenCard(modifier = Modifier.padding(vertical = 8.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.feature_quran_surah_name, surah.arabicName),
                style = DeenTextStyles.arabicDisplay.copy(fontSize = DeenTextStyles.arabicDisplay.fontSize * 0.75f),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                listOfNotNull(
                    surah.translation.takeIf { !isArabicUi },
                    stringResource(
                        if (surah.revelation ==
                            Revelation.MECCAN
                        ) {
                            R.string.feature_quran_meccan
                        } else {
                            R.string.feature_quran_medinan
                        }
                    ),
                    pluralStringResource(R.plurals.feature_quran_verses, surah.ayahCount, localDigits(surah.ayahCount))
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (bismillah != null) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(
                    highlightWords(bismillah, highlightWord, DeenTheme.recitation),
                    style = DeenTextStyles.quran.scaled(scale),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AyahItem(ayah: Ayah, juz: Int?, scale: Float, bookmarked: Boolean, playing: Boolean, word: Int?, onLongPress: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val green = DeenTheme.recitation
    val tint by animateColorAsState(if (playing) green.copy(alpha = 0.08f) else Color.Transparent, tween(400), label = "playing")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tint)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
                onLongClickLabel = stringResource(R.string.feature_quran_verse_options)
            )
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (juz != null &&
                ayah.number > 1
            ) {
                Tag(stringResource(R.string.feature_quran_juz, localDigits(juz)), MaterialTheme.colorScheme.primary)
            }
            if (bookmarked) {
                Icon(
                    painterResource(DeenIcons.BookmarkFilled),
                    contentDescription = stringResource(R.string.feature_quran_bookmarked),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            highlightWords(ayah.text, word, green).let { text ->
                buildAnnotatedString {
                    append(text)
                    append(" " + endOfAyah(ayah.number))
                }
            },
            style = DeenTextStyles.quran.scaled(scale),
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
    HorizontalDivider(Modifier.padding(horizontal = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

/**
 * The verse with the word being recited coloured green. Words are counted the way the timing
 * data counts them: tokens of the Tanzil text that contain letters (pause marks don't count).
 */
private fun highlightWords(text: String, word: Int?, green: Color): AnnotatedString = buildAnnotatedString {
    var index = 0
    text.split(' ').forEachIndexed { i, token ->
        if (i > 0) append(' ')
        val isWord = token.any { it.isLetter() }
        if (isWord && index == word) {
            withStyle(SpanStyle(color = green, background = green.copy(alpha = 0.14f), fontWeight = FontWeight.Bold)) { append(token) }
        } else {
            append(token)
        }
        if (isWord) index++
    }
}

/** Floating bar of player mode: reciter, verse, previous / play-pause / next, close. */
@Composable
private fun PlayerBar(
    audio: QuranAudioState,
    reciterName: String,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: () -> Unit,
    onClose: () -> Unit
) {
    val look = DeenTheme.look
    val gradient = DeenTheme.gradient
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = look.card,
        border = look.cardBorder,
        shadowElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onReciter)
                        .padding(4.dp)
                ) {
                    Text(reciterName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    val verse = audio.ayah
                    Text(
                        when {
                            audio.error -> stringResource(R.string.feature_quran_player_offline)
                            verse == 0 -> stringResource(R.string.feature_quran_bismillah)
                            verse != null -> stringResource(R.string.feature_quran_verse, localDigits(verse))
                            else -> stringResource(R.string.feature_quran_player_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (audio.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                IconButton(onClick = onPrevious, enabled = audio.active) {
                    Icon(
                        painterResource(DeenIcons.SkipPrevious),
                        contentDescription = stringResource(R.string.feature_quran_previous_verse)
                    )
                }
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(gradient.brush)
                        .clickable(role = Role.Button, onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    if (audio.buffering) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = gradient.content, strokeWidth = 2.5.dp)
                    } else {
                        Icon(
                            painterResource(if (audio.playing) DeenIcons.Pause else DeenIcons.Play),
                            contentDescription = stringResource(
                                if (audio.playing) R.string.feature_quran_pause else R.string.feature_quran_play
                            ),
                            tint = gradient.content
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = audio.active) {
                    Icon(painterResource(DeenIcons.SkipNext), contentDescription = stringResource(R.string.feature_quran_next_verse))
                }
                IconButton(onClick = onClose) {
                    Icon(painterResource(DeenIcons.Stop), contentDescription = stringResource(R.string.feature_quran_close_player))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahActionsSheet(
    surah: Surah,
    ayah: Ayah,
    bookmarked: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onTafsir: () -> Unit,
    onBookmark: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DeenTheme.look.sheetColor, shape = DeenTheme.look.sheetShape) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text(
                stringResource(R.string.feature_quran_position, surahTitle(surah), localDigits(ayah.number)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                ayah.text,
                style = DeenTextStyles.quran.copy(
                    fontSize = DeenTextStyles.quran.fontSize * 0.8f,
                    lineHeight =
                        DeenTextStyles.quran.lineHeight * 0.8f
                ),
                textAlign = TextAlign.Right,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            SheetAction(DeenIcons.Play, stringResource(R.string.feature_quran_play_from_here), onPlay)
            SheetAction(DeenIcons.Tafsir, stringResource(R.string.feature_quran_tafsir), onTafsir)
            SheetAction(
                if (bookmarked) DeenIcons.BookmarkFilled else DeenIcons.Bookmark,
                stringResource(if (bookmarked) R.string.feature_quran_unbookmark else R.string.feature_quran_bookmark),
                onBookmark
            )
            SheetAction(DeenIcons.Copy, stringResource(UiR.string.core_ui_copy), onCopy)
            SheetAction(DeenIcons.Share, stringResource(UiR.string.core_ui_share), onShare)
        }
    }
}

@Composable
private fun SheetAction(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TafsirSheet(
    surah: Surah,
    ayah: Ayah,
    state: TafsirState,
    onSource: (TafsirSource) -> Unit,
    onRetry: () -> Unit,
    onOpenFull: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DeenTheme.look.sheetColor, shape = DeenTheme.look.sheetShape) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.feature_quran_tafsir_of, surahTitle(surah), localDigits(ayah.number)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(ayah.text, style = DeenTextStyles.quran, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            TafsirBody(ayah.number, state, onSource, onRetry)
            TextButton(onClick = onOpenFull, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.feature_quran_tafsir_full))
            }
        }
    }
}

/** Source chips, the English meaning (English UI) and the tafsir of one verse. Shared with the surah screen. */
@Composable
internal fun TafsirBody(
    ayahNumber: Int,
    state: TafsirState,
    onSource: (TafsirSource) -> Unit,
    onRetry: () -> Unit,
    showChips: Boolean = true
) {
    val arabicUi = isArabicUi
    if (showChips) TafsirChips(state.source, onSource)
    state.meaning.getOrNull(ayahNumber - 1)?.takeIf { !arabicUi && it.isNotBlank() }?.let { meaning ->
        Text(
            stringResource(R.string.feature_quran_meaning),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(meaning, style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.feature_quran_meaning_credit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    when {
        state.loading -> CircularProgressIndicator(Modifier.padding(16.dp))

        state.unavailable -> {
            Text(
                stringResource(R.string.feature_quran_tafsir_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.feature_quran_retry)) }
        }

        else -> state.entries?.getOrNull(ayahNumber - 1)?.let { entry ->
            Text(state.source.name(arabicUi), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (entry.coveredBy != null) {
                Text(
                    stringResource(R.string.feature_quran_tafsir_together, localDigits(entry.coveredBy!!)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(entry.text, style = tafsirStyle(), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun TafsirChips(selected: TafsirSource, onSelect: (TafsirSource) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TafsirSource.entries.forEach { source ->
            FilterChip(
                selected = source == selected,
                onClick = { onSelect(source) },
                label = { Text(source.name(isArabicUi)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
internal fun tafsirStyle(): TextStyle = DeenTextStyles.arabicBody.copy(
    fontSize = DeenTextStyles.arabicBody.fontSize * 0.95f,
    textDirection = TextDirection.Rtl
)

@Composable
private fun ReciterDialog(selected: Reciter, onSelect: (Reciter) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_quran_reciter)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Reciter.entries.forEach { reciter ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { onSelect(reciter) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reciter == selected, onClick = null, modifier = Modifier.padding(8.dp))
                        Text(reciter.name(isArabicUi), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text(
                    stringResource(R.string.feature_quran_reciter_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } }
    )
}

/** U+06DD ARABIC END OF AYAH followed by the verse number in Arabic-Indic digits; Amiri Quran draws them as one ornament. */
internal fun endOfAyah(number: Int): String = "۝" + number.toString().map { '٠' + (it - '0') }.joinToString("")

internal fun TextStyle.scaled(scale: Float) = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

private fun citation(surah: Surah, ayah: Int) = "﴿${surah.arabicName} ${ayah.toString().map { '٠' + (it - '0') }.joinToString("")}﴾"

private fun Context.copy(text: String, surah: Surah, ayah: Int) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(surah.arabicName, "$text ${citation(surah, ayah)}"))
    Toast.makeText(this, UiR.string.core_ui_copied, Toast.LENGTH_SHORT).show()
}

private fun Context.shareAyah(text: String, surah: Surah, ayah: Int) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$text ${citation(surah, ayah)}")
    startActivity(Intent.createChooser(send, null))
}
