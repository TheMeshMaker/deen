/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.ui.R as UiR

private class Source(@StringRes val title: Int, @StringRes val body: Int, val url: String?)

/** Where every text, recording and font comes from (mirrors CONTENT_SOURCES.md). */
private val sources = listOf(
    Source(R.string.feature_settings_src_quran, R.string.feature_settings_src_quran_body, "https://tanzil.net"),
    Source(R.string.feature_settings_src_hadith, R.string.feature_settings_src_hadith_body, "https://github.com/fawazahmed0/hadith-api"),
    Source(
        R.string.feature_settings_src_adhkar,
        R.string.feature_settings_src_adhkar_body,
        "https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB"
    ),
    Source(R.string.feature_settings_src_tafsir, R.string.feature_settings_src_tafsir_body, "https://github.com/spa5k/tafsir_api"),
    Source(
        R.string.feature_settings_src_recitation,
        R.string.feature_settings_src_recitation_body,
        "https://github.com/cpfair/quran-align"
    ),
    Source(R.string.feature_settings_src_english, R.string.feature_settings_src_english_body, null),
    Source(R.string.feature_settings_src_adhan, R.string.feature_settings_src_adhan_body, "https://commons.wikimedia.org"),
    Source(R.string.feature_settings_src_times, R.string.feature_settings_src_times_body, "https://github.com/batoulapps/adhan-kotlin"),
    Source(R.string.feature_settings_src_cities, R.string.feature_settings_src_cities_body, "https://www.geonames.org"),
    Source(R.string.feature_settings_src_fonts, R.string.feature_settings_src_fonts_body, null),
    Source(R.string.feature_settings_src_app, R.string.feature_settings_src_app_body, SOURCE_URL)
)

@Composable
internal fun AboutScreenRoute(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    DeenScaffold(
        title = stringResource(R.string.feature_settings_sources),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        val look = DeenTheme.look
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            Text(
                stringResource(R.string.feature_settings_sources_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LicenceCard(onOpen = { url -> runCatching { uriHandler.openUri(url) } })
            sources.forEach { source ->
                DeenCard {
                    Text(stringResource(source.title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(source.body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    source.url?.let { url ->
                        TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) {
                            Text(url.removePrefix("https://"))
                        }
                    }
                }
            }
        }
    }
}

/**
 * The app's legal notice (the GPL's "Appropriate Legal Notices"): the copyright, the licence and its
 * additional terms in plain words, the rule that the Quran text is never changed, and where the
 * official app comes from.
 */
@Composable
private fun LicenceCard(onOpen: (String) -> Unit) {
    DeenCard {
        Text(stringResource(R.string.feature_settings_legal_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feature_settings_legal_copyright),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
        listOf(
            R.string.feature_settings_legal_body,
            R.string.feature_settings_legal_quran,
            R.string.feature_settings_legal_official
        ).forEach {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Row {
            TextButton(onClick = { onOpen(NOTICE_URL) }) { Text(stringResource(R.string.feature_settings_legal_read)) }
            TextButton(onClick = { onOpen(REPORT_URL) }) { Text(stringResource(R.string.feature_settings_legal_report)) }
        }
    }
}

private const val NOTICE_URL = "$SOURCE_URL/blob/main/NOTICE"
private const val REPORT_URL = "$SOURCE_URL/issues/new"
