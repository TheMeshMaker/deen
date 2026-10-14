/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.GradientHero
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.ProphetFact

// The "of the day" cards, shared by Today and the Hadith tab.

@Composable
fun DailyHadithCard(hadith: Hadith, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gradient = DeenTheme.gradient
    GradientHero(shape = DeenTheme.look.cardShape, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.core_ui_daily_hadith),
                style = MaterialTheme.typography.labelLarge,
                color = gradient.content.copy(alpha = 0.85f)
            )
            Text(hadith.title.text(), style = MaterialTheme.typography.titleLarge, color = gradient.content)
            Text(
                if (isArabicUi) hadith.arabic else hadith.english,
                style = if (isArabicUi) {
                    DeenTextStyles.arabicBody.copy(
                        fontSize = DeenTextStyles.arabicBody.fontSize * 0.9f
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = gradient.content,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.core_ui_read_more), color = gradient.content)
            }
        }
    }
}

@Composable
fun DailyFactCard(fact: ProphetFact, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DeenCard(onClick = onClick, modifier = modifier) {
        Text(
            stringResource(R.string.core_ui_daily_fact),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(fact.text.text(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
        Text(
            fact.source.text(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
