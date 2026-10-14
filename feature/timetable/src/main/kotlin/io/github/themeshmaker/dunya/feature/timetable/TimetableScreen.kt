/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.timetable

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.PillChoice
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.currentLocale
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.localDigits
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.TextStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimetableScreenRoute(onBack: () -> Unit, viewModel: TimetableViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeenScaffold(
        title = stringResource(R.string.feature_timetable_title),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        val look = DeenTheme.look
        val locale = currentLocale
        val is24 = DateFormat.is24HourFormat(LocalContext.current)
        val timeFormat = remember(locale, is24) {
            DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm", locale).withDecimalStyle(DecimalStyle.of(locale))
        }
        val hijriMonths = stringArrayResource(UiR.array.core_ui_hijri_months)
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = look.gutter, end = look.gutter, bottom = 24.dp)
        ) {
            item(key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    PillChoice(
                        options = CalendarType.entries,
                        selected = state.calendar,
                        label = {
                            stringResource(
                                if (it ==
                                    CalendarType.GREGORIAN
                                ) {
                                    R.string.feature_timetable_gregorian
                                } else {
                                    R.string.feature_timetable_hijri
                                }
                            )
                        },
                        onSelect = viewModel::setCalendar,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::previous) {
                            Icon(
                                painterResource(DeenIcons.ChevronStart),
                                contentDescription = stringResource(R.string.feature_timetable_previous)
                            )
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (val month = state.month) {
                                    is TimetableMonth.Gregorian -> month.month.month.getDisplayName(
                                        TextStyle.FULL_STANDALONE,
                                        locale
                                    ) + " " +
                                        localDigits(month.month.year)

                                    is TimetableMonth.Hijri -> stringResource(
                                        R.string.feature_timetable_hijri_month,
                                        hijriMonths[
                                            month.month -
                                                1
                                        ],
                                        localDigits(month.year)
                                    )

                                    null -> ""
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            state.location?.let {
                                Text(
                                    it.displayName(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = viewModel::next) {
                            Icon(
                                painterResource(DeenIcons.ChevronEnd),
                                contentDescription = stringResource(R.string.feature_timetable_next)
                            )
                        }
                    }
                    if (state.rows.none { it.isToday }) {
                        TextButton(onClick = viewModel::thisMonth, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(R.string.feature_timetable_this_month))
                        }
                    }
                }
            }
            stickyHeader(key = "header") {
                HeaderRow(Modifier.background(look.page))
            }
            items(state.rows, key = { it.date.toEpochDay() }) { row ->
                val colors = MaterialTheme.colorScheme
                val background = when {
                    row.isToday -> colors.primary
                    row.isFriday -> colors.primary.copy(alpha = 0.07f)
                    else -> Color.Transparent
                }
                val content = if (row.isToday) colors.onPrimary else colors.onSurface
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(background)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.width(DAY_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
                        val main = if (state.calendar == CalendarType.HIJRI) row.hijri.day else row.date.dayOfMonth
                        Text(
                            localDigits(main),
                            style = MaterialTheme.typography.titleSmall,
                            color = content,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (state.calendar == CalendarType.HIJRI) {
                                localDigits(row.date.dayOfMonth) + " " + row.date.month.getDisplayName(TextStyle.SHORT, locale)
                            } else {
                                row.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                    Prayer.entries.forEach { prayer ->
                        Text(
                            row.times[prayer]?.atZone(state.zone)?.format(timeFormat).orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (prayer == Prayer.SUNRISE) content.copy(alpha = 0.6f) else content,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private val DAY_WIDTH = 52.dp

@Composable
private fun HeaderRow(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.feature_timetable_day),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(DAY_WIDTH)
        )
        Prayer.entries.forEach { prayer ->
            Text(
                stringResource(prayer.shortLabelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Column headers have room for about five letters. */
private fun Prayer.shortLabelRes(): Int = when (this) {
    Prayer.FAJR -> R.string.feature_timetable_short_fajr
    Prayer.SUNRISE -> R.string.feature_timetable_short_sunrise
    Prayer.DHUHR -> R.string.feature_timetable_short_dhuhr
    Prayer.ASR -> R.string.feature_timetable_short_asr
    Prayer.MAGHRIB -> R.string.feature_timetable_short_maghrib
    Prayer.ISHA -> R.string.feature_timetable_short_isha
}
