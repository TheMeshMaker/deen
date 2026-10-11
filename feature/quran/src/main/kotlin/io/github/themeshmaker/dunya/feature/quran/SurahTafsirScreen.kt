package io.github.themeshmaker.dunya.feature.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.StarBadge
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits

/** The tafsir of a whole surah, verse by verse, with a choice of tafsir. */
@Composable
internal fun SurahTafsirScreenRoute(onBack: () -> Unit, viewModel: SurahTafsirViewModel = hiltViewModel()) {
    val loaded by viewModel.surah.collectAsStateWithLifecycle()
    val tafsir by viewModel.tafsir.collectAsStateWithLifecycle()
    val surah = loaded?.first
    DeenScaffold(
        title = surah?.let { stringResource(R.string.feature_quran_tafsir_title, surahTitle(it)) }.orEmpty(),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        val ayahs = loaded?.second.orEmpty()
        val look = DeenTheme.look
        val list = rememberLazyListState()
        LaunchedEffect(tafsir.entries != null) {
            if (tafsir.entries != null && viewModel.startAyah > 1) list.scrollToItem(viewModel.startAyah)
        }
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            item(key = "chips") { TafsirChips(tafsir.source, viewModel::select) }
            when {
                tafsir.loading -> item(key = "loading") { CircularProgressIndicator(Modifier.padding(24.dp)) }

                tafsir.unavailable -> item(key = "offline") {
                    DeenCard { TafsirBody(1, tafsir, viewModel::select, { viewModel.select(tafsir.source) }, showChips = false) }
                }

                else -> items(ayahs, key = { it.number }) { ayah ->
                    val entry = tafsir.entries?.getOrNull(ayah.number - 1)
                    DeenCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StarBadge(localDigits(ayah.number), size = 34.dp)
                            Text(
                                ayah.text,
                                style = DeenTextStyles.quran.scaled(0.85f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        tafsir.meaning.getOrNull(ayah.number - 1)?.takeIf { !isArabicUi && it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                        }
                        when {
                            entry == null -> Unit

                            // Explained together with an earlier verse: point to it instead of repeating it.
                            entry.coveredBy != null -> Text(
                                stringResource(R.string.feature_quran_tafsir_together, localDigits(entry.coveredBy!!)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            else -> Text(
                                entry.text,
                                style = tafsirStyle(),
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
