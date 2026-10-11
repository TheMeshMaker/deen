package io.github.themeshmaker.dunya.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeFidelity

/**
 * A full Material colour scheme from one seed colour, the same way the bundled themes in
 * Color.kt were made (Material Color Utilities, SchemeFidelity, so the seed stays vivid). Used for
 * the newer themes and for the user's custom colours, where it has to happen on the phone.
 */
internal fun seededScheme(seed: Color, dark: Boolean): ColorScheme {
    val s = SchemeFidelity(Hct.fromInt(seed.toArgb()), isDark = dark, contrastLevel = 0.0)
    return if (dark) s.toDark() else s.toLight()
}

/** The more colourful of two colours (a near-black or near-white seed would give a grey scheme). */
internal fun moreColourful(a: Color, b: Color): Color = if (Hct.fromInt(a.toArgb()).chroma >= Hct.fromInt(b.toArgb()).chroma) a else b

private fun Int.c() = Color(this)

private fun DynamicScheme.toLight(): ColorScheme = lightColorScheme(
    primary = primary.c(),
    onPrimary = onPrimary.c(),
    primaryContainer = primaryContainer.c(),
    onPrimaryContainer = onPrimaryContainer.c(),
    inversePrimary = inversePrimary.c(),
    secondary = secondary.c(),
    onSecondary = onSecondary.c(),
    secondaryContainer = secondaryContainer.c(),
    onSecondaryContainer = onSecondaryContainer.c(),
    tertiary = tertiary.c(),
    onTertiary = onTertiary.c(),
    tertiaryContainer = tertiaryContainer.c(),
    onTertiaryContainer = onTertiaryContainer.c(),
    background = background.c(),
    onBackground = onBackground.c(),
    surface = surface.c(),
    onSurface = onSurface.c(),
    surfaceVariant = surfaceVariant.c(),
    onSurfaceVariant = onSurfaceVariant.c(),
    surfaceTint = surfaceTint.c(),
    inverseSurface = inverseSurface.c(),
    inverseOnSurface = inverseOnSurface.c(),
    error = error.c(),
    onError = onError.c(),
    errorContainer = errorContainer.c(),
    onErrorContainer = onErrorContainer.c(),
    outline = outline.c(),
    outlineVariant = outlineVariant.c(),
    scrim = scrim.c(),
    surfaceBright = surfaceBright.c(),
    surfaceContainer = surfaceContainer.c(),
    surfaceContainerHigh = surfaceContainerHigh.c(),
    surfaceContainerHighest = surfaceContainerHighest.c(),
    surfaceContainerLow = surfaceContainerLow.c(),
    surfaceContainerLowest = surfaceContainerLowest.c(),
    surfaceDim = surfaceDim.c()
)

private fun DynamicScheme.toDark(): ColorScheme = darkColorScheme(
    primary = primary.c(),
    onPrimary = onPrimary.c(),
    primaryContainer = primaryContainer.c(),
    onPrimaryContainer = onPrimaryContainer.c(),
    inversePrimary = inversePrimary.c(),
    secondary = secondary.c(),
    onSecondary = onSecondary.c(),
    secondaryContainer = secondaryContainer.c(),
    onSecondaryContainer = onSecondaryContainer.c(),
    tertiary = tertiary.c(),
    onTertiary = onTertiary.c(),
    tertiaryContainer = tertiaryContainer.c(),
    onTertiaryContainer = onTertiaryContainer.c(),
    background = background.c(),
    onBackground = onBackground.c(),
    surface = surface.c(),
    onSurface = onSurface.c(),
    surfaceVariant = surfaceVariant.c(),
    onSurfaceVariant = onSurfaceVariant.c(),
    surfaceTint = surfaceTint.c(),
    inverseSurface = inverseSurface.c(),
    inverseOnSurface = inverseOnSurface.c(),
    error = error.c(),
    onError = onError.c(),
    errorContainer = errorContainer.c(),
    onErrorContainer = onErrorContainer.c(),
    outline = outline.c(),
    outlineVariant = outlineVariant.c(),
    scrim = scrim.c(),
    surfaceBright = surfaceBright.c(),
    surfaceContainer = surfaceContainer.c(),
    surfaceContainerHigh = surfaceContainerHigh.c(),
    surfaceContainerHighest = surfaceContainerHighest.c(),
    surfaceContainerLow = surfaceContainerLow.c(),
    surfaceContainerLowest = surfaceContainerLowest.c(),
    surfaceDim = surfaceDim.c()
)
