package io.github.themeshmaker.dunya.feature.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.GradientHero
import io.github.themeshmaker.dunya.core.designsystem.component.PillChoice
import io.github.themeshmaker.dunya.core.designsystem.component.StarBadge
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.Revelation
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits

@Composable
internal fun QuranHomeScreenRoute(onOpen: (surah: Int, ayah: Int) -> Unit, viewModel: QuranHomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuranHomeScreen(state, onOpen, viewModel::setQuery, viewModel::setTab)
}

@Composable
private fun QuranHomeScreen(state: QuranHomeUiState, onOpen: (Int, Int) -> Unit, onQuery: (String) -> Unit, onTab: (QuranTab) -> Unit) {
    val look = DeenTheme.look
    val entrance = rememberEntrance()
    // Filtered rows glide into place (Sleek) instead of jumping.
    val placement: @Composable androidx.compose.foundation.lazy.LazyItemScope.() -> Modifier = {
        if (look.motion) Modifier.animateItem() else Modifier
    }
    DeenTabScaffold(title = stringResource(R.string.feature_quran_title)) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (look.sleek) 10.dp else 8.dp)
        ) {
            state.lastRead?.let { position ->
                state.surah(position.surah)?.let { surah ->
                    item(key = "continue") { Box(Modifier.rise(entrance, 0)) { ContinueCard(surah, position, onOpen) } }
                }
            }
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.feature_quran_search)) },
                    leadingIcon = { Icon(painterResource(DeenIcons.Search), contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQuery("") }) {
                                Icon(painterResource(DeenIcons.Reset), contentDescription = stringResource(UiR.string.core_ui_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = look.card,
                        focusedContainerColor = look.card,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
            item(key = "tabs") {
                PillChoice(
                    options = QuranTab.entries,
                    selected = state.tab,
                    label = {
                        stringResource(
                            when (it) {
                                QuranTab.SURAHS -> R.string.feature_quran_tab_surahs
                                QuranTab.JUZ -> R.string.feature_quran_tab_juz
                                QuranTab.BOOKMARKS -> R.string.feature_quran_tab_bookmarks
                            }
                        )
                    },
                    onSelect = onTab,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            when (state.tab) {
                QuranTab.SURAHS -> itemsIndexed(state.filtered, key = { _, s -> "s${s.number}" }) { index, surah ->
                    SurahRow(surah, onClick = { onOpen(surah.number, 1) }, modifier = placement().rise(entrance, index + 1))
                }

                QuranTab.JUZ -> itemsIndexed(state.juz, key = { _, j -> "j${j.juz}" }) { index, juz ->
                    val surah = state.surah(juz.surah)
                    DeenCard(
                        onClick = { onOpen(juz.surah, juz.ayah) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = placement().rise(entrance, index)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StarBadge(localDigits(juz.juz))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.feature_quran_juz, localDigits(juz.juz)),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    surah?.let {
                                        stringResource(R.string.feature_quran_position, surahTitle(it), localDigits(juz.ayah))
                                    }.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                QuranTab.BOOKMARKS -> {
                    if (state.bookmarks.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                stringResource(R.string.feature_quran_bookmarks_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(state.bookmarks, key = { "b${it.surah}:${it.ayah}" }) { mark ->
                        val surah = state.surah(mark.surah) ?: return@items
                        DeenCard(onClick = {
                            onOpen(mark.surah, mark.ayah)
                        }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = placement()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(
                                    painterResource(DeenIcons.BookmarkFilled),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.feature_quran_position, surahTitle(surah), localDigits(mark.ayah)),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(surah: Surah, position: QuranPosition, onOpen: (Int, Int) -> Unit) {
    val gradient = DeenTheme.gradient
    GradientHero(
        shape = DeenTheme.look.cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feature_quran_continue),
                    style = MaterialTheme.typography.labelLarge,
                    color = gradient.content.copy(alpha = 0.85f)
                )
                Text(
                    surah.arabicName,
                    style = DeenTextStyles.arabicBody.copy(fontSize = DeenTextStyles.arabicBody.fontSize * 1.2f),
                    color = gradient.content
                )
                Text(
                    stringResource(R.string.feature_quran_verse, localDigits(position.ayah)) + " · " + surahTitle(surah),
                    style = MaterialTheme.typography.bodyMedium,
                    color = gradient.content.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = { onOpen(surah.number, position.ayah) },
                colors = ButtonDefaults.buttonColors(containerColor = gradient.content, contentColor = gradient.start)
            ) {
                Text(stringResource(R.string.feature_quran_continue_button))
            }
        }
    }
}

@Composable
internal fun surahTitle(surah: Surah): String = if (isArabicUi) surah.arabicName else surah.transliteration

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DeenCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StarBadge(localDigits(surah.number))
            Column(Modifier.weight(1f)) {
                Text(surahTitle(surah), style = MaterialTheme.typography.titleMedium)
                if (!isArabicUi) {
                    Text(
                        surah.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    stringResource(
                        if (surah.revelation ==
                            Revelation.MECCAN
                        ) {
                            R.string.feature_quran_meccan
                        } else {
                            R.string.feature_quran_medinan
                        }
                    ) +
                        " · " + pluralStringResource(R.plurals.feature_quran_verses, surah.ayahCount, localDigits(surah.ayahCount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (!isArabicUi) {
                Text(surah.arabicName, style = DeenTextStyles.arabicBody, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
