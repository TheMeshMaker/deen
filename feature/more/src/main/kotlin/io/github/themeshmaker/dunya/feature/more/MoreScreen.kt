package io.github.themeshmaker.dunya.feature.more

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.MenuRow
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.ProjectLinks

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
internal fun MoreScreenRoute(actions: MoreActions, viewModel: MoreViewModel = hiltViewModel()) {
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
            SupportCard(canDonate = viewModel.canDonate)
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

/**
 * Support Deen: a donation link (FOSS build; Google Play allows only Play Billing) and free ways
 * to help. Deen shows no ads, so this is the only ask, and it never pops up by itself.
 */
@Composable
private fun SupportCard(canDonate: Boolean) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val gradient = DeenTheme.gradient
    val shareText = stringResource(R.string.feature_more_share_text, ProjectLinks.RELEASES)
    val shareTitle = stringResource(R.string.feature_more_share)
    DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(gradient.brush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(DeenIcons.Support), contentDescription = null, tint = gradient.content)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_more_support), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.feature_more_support_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (canDonate) {
                Button(
                    onClick = { runCatching { uriHandler.openUri(ProjectLinks.DONATE) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Icon(painterResource(DeenIcons.Support), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.feature_more_donate), modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    stringResource(R.string.feature_more_donate_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }
        MenuRow(
            stringResource(R.string.feature_more_share),
            icon = DeenIcons.Share,
            subtitle = stringResource(R.string.feature_more_share_body),
            onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                runCatching { context.startActivity(Intent.createChooser(send, shareTitle)) }
            }
        )
        MenuRow(
            stringResource(R.string.feature_more_star),
            icon = DeenIcons.Reward,
            subtitle = stringResource(R.string.feature_more_star_body),
            onClick = { runCatching { uriHandler.openUri(ProjectLinks.REPOSITORY) } }
        )
        MenuRow(
            stringResource(R.string.feature_more_report),
            icon = DeenIcons.Problem,
            subtitle = stringResource(R.string.feature_more_report_body),
            onClick = { runCatching { uriHandler.openUri(ProjectLinks.ISSUES) } }
        )
        Text(
            stringResource(R.string.feature_more_dua),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
