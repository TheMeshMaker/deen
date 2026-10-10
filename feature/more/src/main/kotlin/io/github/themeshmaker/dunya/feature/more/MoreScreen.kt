package io.github.themeshmaker.dunya.feature.more

import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.screen
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.designsystem.theme.gradient
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import io.github.themeshmaker.dunya.core.model.DistributionChannel
import io.github.themeshmaker.dunya.core.model.ThemeStyle

@Composable
internal fun MoreScreenRoute(onOpenReliability: () -> Unit, viewModel: MoreViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoreScreen(uiState, onSelectTheme = viewModel::selectTheme, onOpenReliability = onOpenReliability)
}

private data class MoreEntry(@StringRes val title: Int, @StringRes val milestone: Int)

private val entries = listOf(
    MoreEntry(R.string.feature_more_settings, R.string.feature_more_milestone_5),
    MoreEntry(R.string.feature_more_support, R.string.feature_more_milestone_5),
    MoreEntry(R.string.feature_more_about, R.string.feature_more_milestone_5)
)

@Composable
internal fun MoreScreen(
    uiState: MoreUiState,
    onSelectTheme: (ThemeStyle) -> Unit,
    onOpenReliability: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        ThemePicker(selected = uiState.themeStyle, onSelect = onSelectTheme)
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_more_reliability)) },
            supportingContent = { Text(stringResource(R.string.feature_more_reliability_body)) },
            modifier = Modifier.clickable(onClick = onOpenReliability)
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
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

private fun ThemeStyle.nameRes(): Int = when (this) {
    ThemeStyle.EMERALD -> R.string.feature_more_theme_emerald
    ThemeStyle.KISWAH -> R.string.feature_more_theme_kiswah
    ThemeStyle.DESERT -> R.string.feature_more_theme_desert
}

/** Colour themes as gradient swatches. All free. */
@Composable
private fun ThemePicker(selected: ThemeStyle, onSelect: (ThemeStyle) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.feature_more_themes_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feature_more_themes_free),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeStyle.entries.forEach { style ->
                ThemeSwatch(style, isSelected = style == selected, onClick = { onSelect(style) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeSwatch(style: ThemeStyle, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gradient = style.gradient()
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    }
                )
                .padding(if (isSelected) 5.dp else 0.dp)
                .clip(shape)
                .background(gradient.brush),
            contentAlignment = Alignment.Center
        ) {
            GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize(), cell = 28.dp)
            if (isSelected) {
                Text("✓", color = gradient.content, style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            stringResource(style.nameRes()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
        MoreScreen(MoreUiState(AppBuildInfo("0.1.0", DistributionChannel.FOSS, isDebug = true)), onSelectTheme = {}, onOpenReliability = {})
    }
}
