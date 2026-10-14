/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.HijriDate
import io.github.themeshmaker.dunya.core.model.LocalizedText
import io.github.themeshmaker.dunya.core.model.SavedLocation
import java.text.DecimalFormatSymbols
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

/** True when the app is showing Arabic. */
val isArabicUi: Boolean
    @Composable
    @ReadOnlyComposable
    get() = currentLocale.isArabic

/** The text in the current UI language, falling back to the other one. */
@Composable
@ReadOnlyComposable
fun LocalizedText.text(): String = pick(currentLocale.isArabic)

/** Numbers in the UI language's digits (Arabic-Indic in Arabic). */
@Composable
@ReadOnlyComposable
fun localDigits(value: Any): String = value.toString().withDigitsOf(currentLocale)

/** [text] with [number] (as it appears in it) drawn in [numberStyle], e.g. a big count before its unit: "**5** days". */
fun emphasizeNumber(text: String, number: String, numberStyle: SpanStyle): AnnotatedString {
    val at = text.indexOf(number)
    return buildAnnotatedString {
        append(text)
        if (at >= 0) addStyle(numberStyle, at, at + number.length)
    }
}

fun String.withDigitsOf(locale: Locale): String {
    val zero = DecimalFormatSymbols.getInstance(locale).zeroDigit
    if (zero == '0') return this
    return buildString(length) { this@withDigitsOf.forEach { append(if (it in '0'..'9') zero + (it - '0') else it) } }
}
