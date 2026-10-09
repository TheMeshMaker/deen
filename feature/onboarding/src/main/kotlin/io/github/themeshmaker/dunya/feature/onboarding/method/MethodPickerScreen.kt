package io.github.themeshmaker.dunya.feature.onboarding.method

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.feature.onboarding.BackButton
import io.github.themeshmaker.dunya.feature.onboarding.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MethodPickerScreen(onDone: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_onboarding_method_title)) },
                navigationIcon = { BackButton(onDone) }
            )
        }
    ) { padding ->
        MethodContent(onSaved = onDone, modifier = Modifier.fillMaxSize().padding(padding)) { save, enabled ->
            Button(onClick = save, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.feature_onboarding_save))
            }
        }
    }
}
