package io.github.themeshmaker.dunya.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.HijriDate
import io.github.themeshmaker.dunya.core.model.SavedLocation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

/** The app's current UI locale (follows the per-app language setting). */
val currentLocale: Locale
    @Composable
    @ReadOnlyComposable
    get() = LocalConfiguration.current.locales[0]

private val Locale.isArabic: Boolean get() = language == "ar"

/**
 * Prayer-time formatter: 12/24 h as set on the phone, AM/PM words and digits of the UI
 * language (Arabic gets Arabic-Indic digits and ص/م).
 */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val locale = currentLocale
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale).withDecimalStyle(DecimalStyle.of(locale))
    }
}

/** e.g. "Friday, 9 October" / "الجمعة، ٩ أكتوبر". */
@Composable
fun rememberDateFormatter(): DateTimeFormatter {
    val locale = currentLocale
    return remember(locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale))
    }
}

/** e.g. "27 Rabīʿ al-Ākhir 1448 AH" / "٢٧ ربيع الآخر ١٤٤٨ هـ". */
@Composable
fun hijriDateText(date: HijriDate): String {
    val months = stringArrayResource(R.array.core_ui_hijri_months)
    return stringResource(R.string.core_ui_hijri_date, date.day, months[date.month - 1], date.year)
}

@Composable
@ReadOnlyComposable
fun SavedLocation.displayName(): String = if (currentLocale.isArabic) arabicName ?: name else name

@Composable
@ReadOnlyComposable
fun City.displayName(): String = if (currentLocale.isArabic) arabicName ?: name else name

/** "Riyadh Region, Saudi Arabia" in the UI language (country names come from Android). */
@Composable
@ReadOnlyComposable
fun City.displayRegion(): String {
    val country = Locale.Builder().setRegion(countryCode).build().getDisplayCountry(currentLocale)
    return if (region.isBlank() || currentLocale.isArabic) country else "$region, $country"
}
