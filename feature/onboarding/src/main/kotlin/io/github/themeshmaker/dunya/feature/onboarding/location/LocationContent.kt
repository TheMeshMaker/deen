package io.github.themeshmaker.dunya.feature.onboarding.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.displayRegion
import io.github.themeshmaker.dunya.feature.onboarding.R

/**
 * "Use my location" plus offline city search. Shared by onboarding and the location picker.
 * [onSaved] runs once the choice has been stored.
 */
@Composable
internal fun LocationContent(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    viewModel: LocationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.detect() else viewModel.onPermissionDenied()
    }
    LaunchedEffect(viewModel) { viewModel.locationSaved.collect { onSaved() } }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { header() }
        item {
            DetectRow(
                detection = state.detection,
                onUseMyLocation = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.detect() else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            )
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(painterResource(DeenIcons.Search), contentDescription = null) },
                placeholder = { Text(stringResource(R.string.feature_onboarding_location_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        if (state.query.isNotBlank() && state.results.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.feature_onboarding_location_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(state.results, key = { it.id }) { city -> CityRow(city, onClick = { viewModel.select(city) }) }
    }
}

@Composable
private fun DetectRow(detection: Detection, onUseMyLocation: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onUseMyLocation,
            enabled = detection != Detection.DETECTING,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (detection == Detection.DETECTING) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            } else {
                Icon(painterResource(DeenIcons.MyLocation), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                text = stringResource(
                    if (detection == Detection.DETECTING) {
                        R.string.feature_onboarding_location_detecting
                    } else {
                        R.string.feature_onboarding_location_use_device
                    }
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        val message = when (detection) {
            Detection.FAILED -> R.string.feature_onboarding_location_failed
            Detection.PERMISSION_DENIED -> R.string.feature_onboarding_location_permission_denied
            else -> null
        }
        if (message != null) {
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CityRow(city: City, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(city.displayName()) },
        supportingContent = { Text(city.displayRegion()) },
        leadingContent = { Icon(painterResource(DeenIcons.Location), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
