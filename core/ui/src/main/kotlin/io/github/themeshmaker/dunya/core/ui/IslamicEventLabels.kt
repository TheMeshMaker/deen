/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.ui

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.themeshmaker.dunya.core.model.IslamicEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle

@StringRes
fun IslamicEvent.labelRes(): Int = when (this) {
    IslamicEvent.NEW_YEAR -> R.string.core_ui_event_new_year
    IslamicEvent.ASHURA -> R.string.core_ui_event_ashura
    IslamicEvent.RAMADAN -> R.string.core_ui_event_ramadan
    IslamicEvent.LAST_TEN_NIGHTS -> R.string.core_ui_event_last_ten
    IslamicEvent.EID_AL_FITR -> R.string.core_ui_event_eid_fitr
    IslamicEvent.ARAFAH -> R.string.core_ui_event_arafah
    IslamicEvent.EID_AL_ADHA -> R.string.core_ui_event_eid_adha
    IslamicEvent.WHITE_DAYS -> R.string.core_ui_event_white_days
}

/** A line about the day: what is recommended on it. */
@StringRes
fun IslamicEvent.detailRes(): Int = when (this) {
    IslamicEvent.NEW_YEAR -> R.string.core_ui_event_new_year_detail
    IslamicEvent.ASHURA -> R.string.core_ui_event_ashura_detail
    IslamicEvent.RAMADAN -> R.string.core_ui_event_ramadan_detail
    IslamicEvent.LAST_TEN_NIGHTS -> R.string.core_ui_event_last_ten_detail
    IslamicEvent.EID_AL_FITR -> R.string.core_ui_event_eid_fitr_detail
    IslamicEvent.ARAFAH -> R.string.core_ui_event_arafah_detail
    IslamicEvent.EID_AL_ADHA -> R.string.core_ui_event_eid_adha_detail
    IslamicEvent.WHITE_DAYS -> R.string.core_ui_event_white_days_detail
}

/** "Today", "Now" (a span that has begun), "Tomorrow" or "In 12 days". */
@Composable
@ReadOnlyComposable
fun eventWhen(daysAway: Long, started: Boolean): String = when {
    daysAway == 0L && started -> stringResource(R.string.core_ui_event_now)
    daysAway == 0L -> stringResource(R.string.core_ui_event_today)
    daysAway == 1L -> stringResource(R.string.core_ui_event_tomorrow)
    else -> pluralStringResource(R.plurals.core_ui_event_in_days, daysAway.toInt(), daysAway.toInt())
}

/** e.g. "Thu 18 Feb" / "الخميس ١٨ فبراير". */
@Composable
fun rememberShortDateFormatter(): DateTimeFormatter {
    val locale = currentLocale
    return remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).withDecimalStyle(DecimalStyle.of(locale))
    }
}

/** Whether [date] is in the past relative to [today] (a span that already began). */
fun startedBefore(date: LocalDate, today: LocalDate): Boolean = date.isBefore(today)
