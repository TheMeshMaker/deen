package io.github.themeshmaker.dunya.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
