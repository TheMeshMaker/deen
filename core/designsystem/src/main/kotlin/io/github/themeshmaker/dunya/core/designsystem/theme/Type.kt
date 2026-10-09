package io.github.themeshmaker.dunya.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import io.github.themeshmaker.dunya.core.designsystem.R

/** UI text uses the system font (Roboto / system Arabic), so the stock M3 scale is kept. */
internal val DeenTypography = Typography()

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
