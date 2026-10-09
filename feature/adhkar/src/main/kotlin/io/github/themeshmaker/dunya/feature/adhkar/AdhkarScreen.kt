package io.github.themeshmaker.dunya.feature.adhkar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.themeshmaker.dunya.core.designsystem.component.ComingSoon
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme

@Composable
internal fun AdhkarScreen(modifier: Modifier = Modifier) {
    ComingSoon(
        icon = painterResource(DeenIcons.Adhkar),
        title = stringResource(R.string.feature_adhkar_title),
        message = stringResource(R.string.feature_adhkar_coming_soon),
        modifier = modifier
    )
}

@Preview
@Composable
private fun AdhkarScreenPreview() {
    DeenTheme { AdhkarScreen() }
}
