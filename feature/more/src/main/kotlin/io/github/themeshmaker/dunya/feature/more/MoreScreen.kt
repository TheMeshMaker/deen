package io.github.themeshmaker.dunya.feature.more

import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.screen
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import io.github.themeshmaker.dunya.core.model.DistributionChannel

@Composable
internal fun MoreScreenRoute(viewModel: MoreViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoreScreen(uiState)
}

private data class MoreEntry(@StringRes val title: Int, @StringRes val milestone: Int)

private val entries = listOf(
    MoreEntry(R.string.feature_more_settings, R.string.feature_more_milestone_5),
    MoreEntry(R.string.feature_more_reliability, R.string.feature_more_milestone_2),
    MoreEntry(R.string.feature_more_support, R.string.feature_more_milestone_5),
    MoreEntry(R.string.feature_more_about, R.string.feature_more_milestone_5)
)

@Composable
internal fun MoreScreen(uiState: MoreUiState, modifier: Modifier = Modifier) {
    Column(
        modifier
            .windowInsetsPadding(WindowInsets.screen)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.feature_more_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
        )
        entries.forEach { entry ->
            ListItem(
                headlineContent = { Text(stringResource(entry.title)) },
                supportingContent = { Text(stringResource(entry.milestone)) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
        Text(
            text = buildLine(uiState.buildInfo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}

@Composable
private fun buildLine(info: AppBuildInfo): String {
    val channel = stringResource(
        when (info.channel) {
            DistributionChannel.PLAY -> R.string.feature_more_channel_play
            DistributionChannel.FOSS -> R.string.feature_more_channel_foss
        }
    )
    // Isolate the version as left-to-right, otherwise "0.1.0-debug" renders as "debug-0.1.0" in Arabic.
    val version = BidiFormatter.getInstance().unicodeWrap(info.versionName, TextDirectionHeuristics.LTR)
    return stringResource(R.string.feature_more_build_line, version, channel)
}

@Preview
@Composable
private fun MoreScreenPreview() {
    DeenTheme {
        MoreScreen(MoreUiState(AppBuildInfo("0.1.0", DistributionChannel.FOSS, isDebug = true)))
    }
}
