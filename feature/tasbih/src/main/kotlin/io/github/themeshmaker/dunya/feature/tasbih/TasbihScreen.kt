package io.github.themeshmaker.dunya.feature.tasbih

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.themeshmaker.dunya.core.designsystem.component.ComingSoon
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme

@Composable
internal fun TasbihScreen(modifier: Modifier = Modifier) {
    ComingSoon(
        icon = painterResource(DeenIcons.Tasbih),
        title = stringResource(R.string.feature_tasbih_title),
        message = stringResource(R.string.feature_tasbih_coming_soon),
        modifier = modifier
    )
}

@Preview
@Composable
private fun TasbihScreenPreview() {
    DeenTheme { TasbihScreen() }
}
