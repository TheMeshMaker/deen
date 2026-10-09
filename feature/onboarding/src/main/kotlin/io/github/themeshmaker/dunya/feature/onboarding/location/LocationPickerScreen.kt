package io.github.themeshmaker.dunya.feature.onboarding.location

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.themeshmaker.dunya.feature.onboarding.BackButton
import io.github.themeshmaker.dunya.feature.onboarding.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerScreen(onDone: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_onboarding_location_picker_title)) },
                navigationIcon = { BackButton(onDone) }
            )
        }
    ) { padding ->
        LocationContent(onSaved = onDone, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
