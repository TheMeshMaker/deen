/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.hadith

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.GradientHero
import io.github.themeshmaker.dunya.core.designsystem.component.SectionLabel
import io.github.themeshmaker.dunya.core.designsystem.component.StarBadge
import io.github.themeshmaker.dunya.core.designsystem.component.Tag
import io.github.themeshmaker.dunya.core.designsystem.component.paddingExceptBottom
import io.github.themeshmaker.dunya.core.designsystem.component.rememberEntrance
import io.github.themeshmaker.dunya.core.designsystem.component.rise
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.GradeKind
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.HadithCollection
import io.github.themeshmaker.dunya.core.model.HadithEntry
import io.github.themeshmaker.dunya.core.model.HadithGrade
import io.github.themeshmaker.dunya.core.model.ProphetFact
import io.github.themeshmaker.dunya.core.ui.DailyFactCard
import io.github.themeshmaker.dunya.core.ui.DailyHadithCard
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.text

// ---------------------------------------------------------------------------------------------- Tab

@Composable
internal fun HadithHomeScreenRoute(
    onOpenNawawi: (Int) -> Unit,
    onOpenFacts: () -> Unit,
    onOpenCollection: (String) -> Unit,
    viewModel: HadithHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val look = DeenTheme.look
    val entrance = rememberEntrance()
    DeenTabScaffold(title = stringResource(R.string.feature_hadith_title)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .paddingExceptBottom(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter)
                .padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            state.dailyHadith?.let { hadith ->
                DailyHadithCard(hadith, onClick = { onOpenNawawi(hadith.number) }, modifier = Modifier.rise(entrance, 0))
            }
            state.dailyFact?.let { fact -> DailyFactCard(fact, onClick = onOpenFacts, modifier = Modifier.rise(entrance, 1)) }

            SectionLabel(stringResource(R.string.feature_hadith_collections))
            CollectionCard(
                title = stringResource(R.string.feature_hadith_nawawi),
                subtitle = stringResource(R.string.feature_hadith_nawawi_body),
                badge = localDigits(state.nawawiCount),
                onClick = { onOpenNawawi(1) },
                modifier = Modifier.rise(entrance, 2)
            )
            CollectionCard(
                title = stringResource(R.string.feature_hadith_facts),
                subtitle = stringResource(R.string.feature_hadith_facts_body),
                badge = localDigits(state.factCount),
                onClick = onOpenFacts,
                modifier = Modifier.rise(entrance, 3)
            )
            state.collections.forEachIndexed { index, collection ->
                CollectionCard(
                    modifier = Modifier.rise(entrance, 4 + index),
                    title = collection.name.text(),
                    subtitle = listOfNotNull(
                        collection.author?.text(),
                        pluralStringResource(R.plurals.feature_hadith_count, collection.count, localDigits(collection.count))
                    ).joinToString(" · "),
                    badge = null,
                    onClick = { onOpenCollection(collection.id) }
                )
            }
            Text(
                stringResource(R.string.feature_hadith_arabic_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CollectionCard(title: String, subtitle: String, badge: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DeenCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (badge != null) {
                StarBadge(badge, size = 44.dp)
            } else {
                Icon(
                    painterResource(DeenIcons.Hadith),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(DeenIcons.ChevronEnd), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------------------------- Nawawi

@Composable
internal fun NawawiScreenRoute(onBack: () -> Unit, viewModel: NawawiViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeenScaffold(
        title = stringResource(R.string.feature_hadith_nawawi),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        if (state.hadiths.isEmpty()) return@DeenScaffold
        val pager = rememberPagerState(initialPage = (state.start - 1).coerceIn(0, state.hadiths.lastIndex)) { state.hadiths.size }
        val context = LocalContext.current
        HorizontalPager(pager, modifier = Modifier.padding(padding), beyondViewportPageCount = 1) { page ->
            val hadith = state.hadiths[page]
            val look = DeenTheme.look
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = look.gutter, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(look.gap)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StarBadge(localDigits(hadith.number), size = 44.dp)
                    Text(hadith.title.text(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                DeenCard {
                    ArabicText(hadith.arabic, state.reading.arabicScale)
                    if (!isArabicUi && state.reading.showTranslation) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text(hadith.english, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        hadith.source.text(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            context.copyText(hadith.arabic + "\n" + hadith.source.ar)
                        }) { Text(stringResource(UiR.string.core_ui_copy)) }
                        TextButton(onClick = {
                            context.shareText(
                                listOfNotNull(
                                    hadith.arabic,
                                    hadith.english.takeIf {
                                        !isArabicUiStatic(context)
                                    },
                                    hadith.source.en
                                ).joinToString("\n\n")
                            )
                        }) { Text(stringResource(UiR.string.core_ui_share)) }
                    }
                }
                Text(
                    stringResource(R.string.feature_hadith_swipe, localDigits(page + 1), localDigits(state.hadiths.size)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Facts

@Composable
internal fun FactsScreenRoute(onBack: () -> Unit, viewModel: FactsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeenScaffold(
        title = stringResource(R.string.feature_hadith_facts),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        val look = DeenTheme.look
        val list = rememberLazyListState()
        LaunchedEffect(state.todayId) {
            val index = state.facts.indexOfFirst { it.id == state.todayId }
            if (index > 0) list.scrollToItem(index + 1)
        }
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            item {
                Text(
                    stringResource(R.string.feature_hadith_facts_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.facts, key = { it.id }) { fact ->
                val today = fact.id == state.todayId
                DeenCard(containerColor = if (today) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else DeenTheme.look.card) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StarBadge(localDigits(fact.id), size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            if (today) {
                                Text(
                                    stringResource(R.string.feature_hadith_today),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(fact.text.text(), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                fact.source.text(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Collection

@Composable
internal fun HadithCollectionScreenRoute(
    onBack: () -> Unit,
    onOpenBook: (book: Int, highlight: String?) -> Unit,
    viewModel: HadithCollectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val collection = state.collection
    DeenScaffold(
        title = collection?.name?.text().orEmpty(),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        if (collection == null) return@DeenScaffold
        val look = DeenTheme.look
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (look.sleek) 10.dp else 8.dp)
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.feature_hadith_search)) },
                    leadingIcon = { Icon(painterResource(DeenIcons.Search), contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::search) {
                            Icon(painterResource(DeenIcons.ChevronEnd), contentDescription = stringResource(UiR.string.core_ui_search))
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = look.card,
                        focusedContainerColor = look.card,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
            item(key = "about") {
                Text(
                    stringResource(if (collection.allAuthentic) R.string.feature_hadith_all_authentic else R.string.feature_hadith_graded),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            when {
                state.searching -> item(key = "progress") { CircularProgressIndicator(Modifier.padding(24.dp)) }

                state.results != null -> {
                    val results = state.results.orEmpty()
                    item(key = "count") {
                        Text(
                            pluralStringResource(R.plurals.feature_hadith_results, results.size, localDigits(results.size)),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    items(results, key = { "r${it.book}/${it.number}" }) { entry ->
                        val book = collection.books.firstOrNull { it.number == entry.book }
                        DeenCard(onClick = { onOpenBook(entry.book, entry.number) }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.feature_hadith_number, localDigits(entry.number)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                book?.let {
                                    Text(
                                        it.title.text(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                entry.text,
                                style = DeenTextStyles.arabicBody.copy(fontSize = DeenTextStyles.arabicBody.fontSize * 0.85f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                else -> items(collection.books, key = { "b${it.number}" }) { book ->
                    DeenCard(onClick = {
                        onOpenBook(book.number, null)
                    }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StarBadge(localDigits(book.number))
                            Column(Modifier.weight(1f)) {
                                Text(book.title.text(), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    pluralStringResource(R.plurals.feature_hadith_count, book.count, localDigits(book.count)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Book

@Composable
internal fun HadithBookScreenRoute(onBack: () -> Unit, viewModel: HadithBookViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val collection = state.collection
    val book = collection?.books?.firstOrNull { b -> state.entries.firstOrNull()?.book == b.number }
    DeenScaffold(
        title = book?.title?.text() ?: collection?.name?.text().orEmpty(),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        if (collection == null || state.entries.isEmpty()) {
            CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            return@DeenScaffold
        }
        val look = DeenTheme.look
        val list = rememberLazyListState()
        LaunchedEffect(state.highlight, state.entries.size) {
            val index = state.entries.indexOfFirst { it.number == state.highlight }
            if (index > 0) list.scrollToItem(index)
        }
        val context = LocalContext.current
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            items(state.entries, key = { it.number }) { entry ->
                HadithEntryCard(
                    entry = entry,
                    collection = collection,
                    scale = state.reading.arabicScale,
                    highlighted = entry.number == state.highlight,
                    onCopy = { context.copyText(entry.text + "\n" + citation(collection, entry, arabic = true)) },
                    onShare = { context.shareText(entry.text + "\n\n" + citation(collection, entry, arabic = true)) }
                )
            }
        }
    }
}

@Composable
private fun HadithEntryCard(
    entry: HadithEntry,
    collection: HadithCollection,
    scale: Float,
    highlighted: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    DeenCard(containerColor = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else DeenTheme.look.card) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.feature_hadith_number, localDigits(entry.number)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            when {
                entry.grade != null -> GradeTag(entry.grade!!)
                collection.allAuthentic -> Tag(stringResource(R.string.feature_hadith_sahih), gradeColor(GradeKind.AUTHENTIC))
            }
        }
        ArabicText(entry.text, scale, Modifier.padding(top = 8.dp))
        entry.grade?.let {
            Text(
                stringResource(R.string.feature_hadith_graded_by, it.grader.text()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCopy) { Text(stringResource(UiR.string.core_ui_copy)) }
            TextButton(onClick = onShare) { Text(stringResource(UiR.string.core_ui_share)) }
        }
    }
}

@Composable
private fun GradeTag(grade: HadithGrade) {
    Tag(grade.label.text(), gradeColor(grade.kind))
}

@Composable
private fun gradeColor(kind: GradeKind): Color = when (kind) {
    GradeKind.AUTHENTIC -> MaterialTheme.colorScheme.primary
    GradeKind.GOOD -> MaterialTheme.colorScheme.tertiary
    GradeKind.WEAK -> Color(0xFFB26A00)
    GradeKind.FABRICATED -> MaterialTheme.colorScheme.error
    GradeKind.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Arabic reading text in Amiri, right-aligned even in the English UI. */
@Composable
internal fun ArabicText(text: String, scale: Float, modifier: Modifier = Modifier) {
    val style = DeenTextStyles.arabicBody
    Text(
        text,
        style = style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale, textDirection = TextDirection.Rtl),
        textAlign = TextAlign.Right,
        modifier = modifier.fillMaxWidth()
    )
}

private fun citation(collection: HadithCollection, entry: HadithEntry, arabic: Boolean): String =
    "— ${if (arabic) collection.name.ar else collection.name.en} (${entry.number})"

private fun isArabicUiStatic(context: Context): Boolean = context.resources.configuration.locales[0].language == "ar"

private fun Context.copyText(text: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("hadith", text))
    Toast.makeText(this, UiR.string.core_ui_copied, Toast.LENGTH_SHORT).show()
}

private fun Context.shareText(text: String) {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
}
