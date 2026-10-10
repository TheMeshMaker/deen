package io.github.themeshmaker.dunya.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.AdhanVoice
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.localDigits

@Composable
internal fun AdhanVoiceScreenRoute(fajr: Boolean, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewing by viewModel.previewing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messages = mapOf(
        SettingsEvent.VOICE_IMPORTED to stringResource(R.string.feature_settings_voice_imported),
        SettingsEvent.VOICE_TOO_LARGE to stringResource(R.string.feature_settings_voice_too_large),
        SettingsEvent.VOICE_NOT_AUDIO to stringResource(R.string.feature_settings_voice_not_audio),
        SettingsEvent.VOICE_FAILED to stringResource(R.string.feature_settings_voice_failed)
    )
    LaunchedEffect(viewModel, messages) {
        viewModel.events.collect { event -> messages[event]?.let { snackbar.showSnackbar(it) } }
    }
    DisposableEffect(viewModel) { onDispose { viewModel.stopPreview() } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importVoice(uri, fajr)
    }

    DeenScaffold(
        title = stringResource(if (fajr) R.string.feature_settings_fajr_voice else R.string.feature_settings_voice),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        val alerts = state.data?.alertSettings ?: return@DeenScaffold
        val selected = if (fajr) alerts.fajrVoice else alerts.voice
        val look = DeenTheme.look
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = look.gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            if (fajr) {
                Text(
                    stringResource(R.string.feature_settings_fajr_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                if (fajr) {
                    VoiceRow(
                        title = stringResource(R.string.feature_settings_fajr_same),
                        subtitle = voiceName(alerts.voice),
                        selected = selected == null,
                        onSelect = viewModel::useSameVoiceForFajr
                    )
                }
                viewModel.bundledVoices.forEach { voice ->
                    VoiceRow(
                        title = stringResource(voice.name),
                        subtitle = stringResource(
                            R.string.feature_settings_voice_length,
                            localDigits(voice.seconds / 60),
                            localDigits("%02d".format(voice.seconds % 60))
                        ),
                        selected = selected == voice.id,
                        onSelect = { viewModel.setVoice(voice.id, fajr) },
                        playing = previewing == voice.id,
                        onPreview = { viewModel.togglePreview(voice.id, fajr) }
                    )
                }
                val custom = if (fajr) state.customFajrVoice else state.customVoice
                if (custom != null) {
                    VoiceRow(
                        title = stringResource(io.github.themeshmaker.dunya.core.alarm.R.string.core_alarm_voice_custom),
                        subtitle = custom.ifBlank { null },
                        selected = selected == AdhanVoice.CUSTOM,
                        onSelect = { viewModel.setVoice(AdhanVoice.CUSTOM, fajr) },
                        playing = previewing == AdhanVoice.CUSTOM,
                        onPreview = { viewModel.togglePreview(AdhanVoice.CUSTOM, fajr) }
                    )
                }
            }
            DeenCard {
                Text(stringResource(R.string.feature_settings_voice_pick_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.feature_settings_voice_pick_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                    Icon(painterResource(DeenIcons.AudioFile), contentDescription = null)
                    Text(stringResource(R.string.feature_settings_voice_pick), modifier = Modifier.padding(start = 8.dp))
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun VoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    playing: Boolean = false,
    onPreview: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onPreview != null) {
            IconButton(onClick = onPreview) {
                Icon(
                    painterResource(if (playing) DeenIcons.Stop else DeenIcons.Play),
                    contentDescription = stringResource(
                        if (playing) R.string.feature_settings_voice_stop else R.string.feature_settings_voice_play
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
