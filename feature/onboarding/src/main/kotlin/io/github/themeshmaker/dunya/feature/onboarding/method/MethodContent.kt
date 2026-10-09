package io.github.themeshmaker.dunya.feature.onboarding.method

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.regionRes
import io.github.themeshmaker.dunya.feature.onboarding.R

/** Calculation method + Asr school. The caller provides the save button via [footer]. */
@Composable
internal fun MethodContent(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    viewModel: MethodViewModel = hiltViewModel(),
    footer: @Composable (save: () -> Unit, enabled: Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.settingsSaved.collect { onSaved() } }

    Column(modifier) {
        LazyColumn(Modifier.weight(1f).selectableGroup()) {
            item { header() }
            items(CalculationMethod.entries) { method ->
                ChoiceRow(
                    selected = state.method == method,
                    title = stringResource(method.labelRes()),
                    subtitle = stringResource(method.regionRes()),
                    badge = if (method == state.suggestedMethod) stringResource(R.string.feature_onboarding_method_suggested) else null,
                    onClick = { viewModel.selectMethod(method) }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.feature_onboarding_method_asr_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                )
                Text(
                    text = stringResource(R.string.feature_onboarding_method_asr_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            items(AsrMethod.entries) { asr ->
                ChoiceRow(
                    selected = state.asrMethod == asr,
                    title = stringResource(asr.labelRes()),
                    subtitle = null,
                    badge = null,
                    onClick = { viewModel.selectAsr(asr) }
                )
            }
        }
        footer(viewModel::save, state.loaded)
    }
}

@Composable
private fun ChoiceRow(selected: Boolean, title: String, subtitle: String?, badge: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
