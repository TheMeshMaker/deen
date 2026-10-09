package io.github.themeshmaker.dunya.core.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Insets for a top-level screen: clear of the status bar, display cutout and (in landscape)
 * the system navigation bar. The bottom edge is handled by the app's navigation bar/rail.
 * Use with `Modifier.windowInsetsPadding(WindowInsets.screen)`. Screens with a TopAppBar get
 * this from Scaffold instead.
 */
val WindowInsets.Companion.screen: WindowInsets
    @Composable get() = safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
