package io.github.themeshmaker.dunya.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
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

/** The Sleek style: lighter, airier headings on the same font. */
internal val SleekTypography: Typography = DeenTypography.run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Light),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Light),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Light),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.25).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Medium)
    )
}

/**
 * The Heritage style: headings and titles in Amiri, like a printed manuscript; body text stays in
 * Readex Pro for legibility. Amiri's tall marks need more line height than Material's defaults.
 */
internal val HeritageTypography: Typography = DeenTypography.run {
    fun TextStyle.amiri(weight: FontWeight = FontWeight.Bold) =
        copy(fontFamily = AmiriFontFamily, fontWeight = weight, lineHeight = fontSize * 1.45f, letterSpacing = 0.sp)
    copy(
        displayLarge = displayLarge.amiri(FontWeight.Normal),
        displayMedium = displayMedium.amiri(FontWeight.Normal),
        displaySmall = displaySmall.amiri(FontWeight.Normal),
        headlineLarge = headlineLarge.amiri(),
        headlineMedium = headlineMedium.amiri(),
        headlineSmall = headlineSmall.amiri(),
        titleLarge = titleLarge.amiri(),
        titleMedium = titleMedium.amiri()
    )
}

/**
 * Readex Pro has no Urdu-only letters (ٹ ڈ ڑ ے ھ…), and mixing two fonts inside a word breaks the
 * joins between letters. For Urdu the whole interface uses the phone's own font (Android picks a
 * Nastaliq or Naskh face for Urdu), with that font's natural line height for its tall letters.
 */
internal fun Typography.withPhoneFont(): Typography {
    fun TextStyle.phone() = copy(fontFamily = FontFamily.Default, lineHeight = TextUnit.Unspecified)
    return copy(
        displayLarge = displayLarge.phone(),
        displayMedium = displayMedium.phone(),
        displaySmall = displaySmall.phone(),
        headlineLarge = headlineLarge.phone(),
        headlineMedium = headlineMedium.phone(),
        headlineSmall = headlineSmall.phone(),
        titleLarge = titleLarge.phone(),
        titleMedium = titleMedium.phone(),
        titleSmall = titleSmall.phone(),
        bodyLarge = bodyLarge.phone(),
        bodyMedium = bodyMedium.phone(),
        bodySmall = bodySmall.phone(),
        labelLarge = labelLarge.phone(),
        labelMedium = labelMedium.phone(),
        labelSmall = labelSmall.phone()
    )
}

/** Languages whose letters Readex Pro lacks and that join across letters (see [withPhoneFont]). */
internal val PhoneFontLanguages = setOf("ur")

/** Amiri (SIL OFL 1.1), used for religious text: adhkar, hadith, the app name. */
val AmiriFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

/**
 * Amiri Quran (SIL OFL 1.1): Amiri's companion face made for the Uthmani Quran text, with the
 * small high letters and pause marks positioned for reading.
 */
val QuranFontFamily = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

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

    /** Quran text: Amiri Quran, generous line height for the marks above and below. */
    val quran = TextStyle(
        fontFamily = QuranFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 54.sp,
        textDirection = TextDirection.Rtl
    )
}
