package io.github.themeshmaker.dunya.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import io.github.themeshmaker.dunya.core.designsystem.R

private fun readex(weight: FontWeight) =
    Font(R.font.readex_pro, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/**
 * Readex Pro (SIL OFL 1.1): one modern family for Latin and Arabic UI text, so the app doesn't
 * look like stock Roboto. Variable font, so all weights come from one 270 KB file.
 */
val ReadexFontFamily = FontFamily(
    readex(FontWeight.Light),
    readex(FontWeight.Normal),
    readex(FontWeight.Medium),
    readex(FontWeight.SemiBold),
    readex(FontWeight.Bold)
)

private fun TextStyle.readex() = copy(fontFamily = ReadexFontFamily)

internal val DeenTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.readex(),
        displayMedium = displayMedium.readex(),
        displaySmall = displaySmall.readex(),
        headlineLarge = headlineLarge.readex().copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.readex().copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.readex().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.readex().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.readex(),
        titleSmall = titleSmall.readex(),
        bodyLarge = bodyLarge.readex(),
        bodyMedium = bodyMedium.readex(),
        bodySmall = bodySmall.readex(),
        labelLarge = labelLarge.readex(),
        labelMedium = labelMedium.readex(),
        labelSmall = labelSmall.readex()
    )
}

/** Amiri (SIL OFL 1.1), used for religious text: adhkar, hadith, the app name. */
val AmiriFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

/**
 * Text styles for Arabic reading text. Amiri needs generous line height for its
 * stacked diacritics (tashkeel), hence ~1.8x.
 */
object DeenTextStyles {
    val arabicDisplay = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 72.sp,
        textDirection = TextDirection.Rtl
    )

    val arabicBody = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 40.sp,
        textDirection = TextDirection.Rtl
    )
}
