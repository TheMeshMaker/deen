package io.github.themeshmaker.dunya.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.MenuRow
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme

/** Destinations reachable from the More tab. */
class MoreActions(
    val onOpenSettings: () -> Unit,
    val onOpenQibla: () -> Unit,
    val onOpenTimetable: () -> Unit,
    val onOpenTasbih: () -> Unit,
    val onOpenReliability: () -> Unit,
    val onOpenSources: () -> Unit
)

@Composable
internal fun MoreScreenRoute(actions: MoreActions) {
    val look = DeenTheme.look
    DeenTabScaffold(title = stringResource(R.string.feature_more_title)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                MenuRow(
                    stringResource(R.string.feature_more_qibla),
                    icon = DeenIcons.Qibla,
                    subtitle = stringResource(R.string.feature_more_qibla_body),
                    onClick = actions.onOpenQibla
                )
                MenuRow(
                    stringResource(R.string.feature_more_timetable),
                    icon = DeenIcons.Calendar,
                    subtitle = stringResource(R.string.feature_more_timetable_body),
                    onClick = actions.onOpenTimetable
                )
                MenuRow(
                    stringResource(R.string.feature_more_tasbih),
                    icon = DeenIcons.Tasbih,
                    subtitle = stringResource(R.string.feature_more_tasbih_body),
                    onClick = actions.onOpenTasbih
                )
            }
            DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                MenuRow(
                    stringResource(R.string.feature_more_settings),
                    icon = DeenIcons.Settings,
                    subtitle = stringResource(R.string.feature_more_settings_body),
                    onClick = actions.onOpenSettings
                )
                MenuRow(
                    stringResource(R.string.feature_more_reliability),
                    icon = DeenIcons.Battery,
                    subtitle = stringResource(R.string.feature_more_reliability_body),
                    onClick = actions.onOpenReliability
                )
                MenuRow(
                    stringResource(R.string.feature_more_sources),
                    icon = DeenIcons.Info,
                    subtitle = stringResource(R.string.feature_more_sources_body),
                    onClick = actions.onOpenSources
                )
            }
            DeenCard {
                MenuRow(
                    stringResource(R.string.feature_more_support),
                    icon = DeenIcons.Support,
                    subtitle = stringResource(R.string.feature_more_support_body)
                )
            }
            Text(
                stringResource(R.string.feature_more_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
