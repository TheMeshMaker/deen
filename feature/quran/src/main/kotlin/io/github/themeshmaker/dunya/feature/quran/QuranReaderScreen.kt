package io.github.themeshmaker.dunya.feature.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.Tag
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.Revelation
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun QuranReaderScreenRoute(onBack: () -> Unit, onOpenSurah: (Int) -> Unit, viewModel: QuranReaderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuranReaderScreen(state, onBack, onOpenSurah, viewModel::onVisible, viewModel::toggleBookmark)
}

@Composable
private fun QuranReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onVisible: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit
) {
    val surah = state.surah
    DeenScaffold(
        title = surah?.let { surahTitle(it) }.orEmpty(),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        if (!state.loaded || surah == null) {
            CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            return@DeenScaffold
        }
        val listState = rememberLazyListState()
        // Jump to the requested verse once, then start remembering what the reader looks at.
        var positioned by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(state.loaded) {
            if (!positioned) {
                if (state.startAyah > 1) listState.scrollToItem(state.startAyah.coerceAtMost(state.ayahs.size))
                positioned = true
            }
            snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { index ->
                if (positioned) onVisible(index.coerceIn(1, state.ayahs.size))
            }
        }
        var selected by remember { mutableIntStateOf(0) }
        val look = DeenTheme.look
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 32.dp)
        ) {
            item(key = "header") { SurahHeader(surah, state.ayahs.first().bismillah, state.reading.arabicScale) }
            itemsIndexed(state.ayahs, key = { _, ayah -> ayah.number }) { _, ayah ->
                AyahItem(
                    surah = surah,
                    ayah = ayah,
                    juz = state.juzStarts[ayah.number],
                    scale = state.reading.arabicScale,
                    bookmarked = ayah.number in state.bookmarks,
                    selected = selected == ayah.number,
                    onClick = { selected = if (selected == ayah.number) 0 else ayah.number },
                    onToggleBookmark = { onToggleBookmark(ayah.number) }
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
}

@Composable
private fun SurahHeader(surah: Surah, bismillah: String?, scale: Float) {
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
                Text(bismillah, style = DeenTextStyles.quran.scaled(scale), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AyahItem(
    surah: Surah,
    ayah: Ayah,
    juz: Int?,
    scale: Float,
    bookmarked: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
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
            ayah.text + " " + endOfAyah(ayah.number),
            style = DeenTextStyles.quran.scaled(scale),
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        if (selected) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onToggleBookmark) {
                    Icon(painterResource(if (bookmarked) DeenIcons.BookmarkFilled else DeenIcons.Bookmark), contentDescription = null)
                    Text(
                        stringResource(if (bookmarked) R.string.feature_quran_unbookmark else R.string.feature_quran_bookmark),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                TextButton(onClick = { context.copy(ayah.text, surah, ayah.number) }) { Text(stringResource(UiR.string.core_ui_copy)) }
                TextButton(onClick = {
                    context.shareAyah(ayah.text, surah, ayah.number)
                }) { Text(stringResource(UiR.string.core_ui_share)) }
            }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

/** U+06DD ARABIC END OF AYAH followed by the verse number in Arabic-Indic digits; Amiri Quran draws them as one ornament. */
private fun endOfAyah(number: Int): String = "۝" + number.toString().map { '٠' + (it - '0') }.joinToString("")

private fun androidx.compose.ui.text.TextStyle.scaled(scale: Float) = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

private fun citation(surah: Surah, ayah: Int) = "﴿${surah.arabicName} ${ayah.toString().map { '٠' + (it - '0') }.joinToString("")}﴾"

private fun Context.copy(text: String, surah: Surah, ayah: Int) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(surah.arabicName, "$text ${citation(surah, ayah)}"))
    Toast.makeText(this, UiR.string.core_ui_copied, Toast.LENGTH_SHORT).show()
}

private fun Context.shareAyah(text: String, surah: Surah, ayah: Int) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$text ${citation(surah, ayah)}")
    startActivity(Intent.createChooser(send, null))
}
