package io.github.themeshmaker.dunya.feature.adhkar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.ThinProgress
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.PassageName
import io.github.themeshmaker.dunya.core.model.QuranReading
import io.github.themeshmaker.dunya.core.model.SunnahAct
import io.github.themeshmaker.dunya.core.ui.isArabicUi
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.core.ui.rememberHaptics
import io.github.themeshmaker.dunya.core.ui.text

/** Callbacks for the challenges' "go and do it" buttons. */
internal class ChallengeLinks(
    val onOpenQuran: (surah: Int, ayah: Int) -> Unit,
    val onStartTasbih: (TasbihRoute) -> Unit,
    val onOpenCategory: (String) -> Unit,
    val onOpenTracker: () -> Unit,
    val onOpenHadith: (number: Int) -> Unit
)

/**
 * Today's challenges (three, drawn at random each day), each ticked off here or ticking itself when
 * done elsewhere (a tasbih round, a set of adhkar, the five prayers), with the streak of days all of
 * them were done.
 */
@Composable
internal fun ChallengesCard(
    state: ChallengesUiState,
    onToggle: (ChallengeKind, Boolean) -> Unit,
    links: ChallengeLinks,
    modifier: Modifier = Modifier
) {
    val total = state.items.size
    val doneCount = state.items.count { it.kind in state.done }
    val haptics = rememberHaptics(state.haptics)
    val toggle: (ChallengeKind, Boolean) -> Unit = { kind, done ->
        if (done) haptics.done() else haptics.tick()
        onToggle(kind, done)
    }
    DeenCard(modifier = modifier, contentPadding = PaddingValues(vertical = 14.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_adhkar_challenges), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.allDone) {
                        stringResource(R.string.feature_adhkar_challenges_all_done)
                    } else {
                        stringResource(R.string.feature_adhkar_challenges_progress, localDigits(doneCount), localDigits(total))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StreakBadge(state.streak)
        }
        ThinProgress(
            if (total == 0) 0f else doneCount / total.toFloat(),
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        state.items.forEach { item -> ChallengeItem(item, state, toggle, links) }
        Text(
            buildString {
                append(stringResource(R.string.feature_adhkar_challenge_streak_note))
                if (state.best > 0) append(" · ").append(stringResource(R.string.feature_adhkar_challenge_best, localDigits(state.best)))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
        )
    }
}

@Composable
private fun ChallengeItem(
    item: ChallengeView,
    state: ChallengesUiState,
    onToggle: (ChallengeKind, Boolean) -> Unit,
    links: ChallengeLinks
) {
    val done = item.kind in state.done
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun Open(onClick: () -> Unit) = TextButton(onClick = onClick) { Text(stringResource(R.string.feature_adhkar_challenge_open)) }

    when (item) {
        is ChallengeView.Dua -> ChallengeRow(item.kind, stringResource(R.string.feature_adhkar_challenge_dua_label), done, onToggle) {
            DuaChallenge(item, state)
        }

        is ChallengeView.Quran -> ChallengeRow(
            item.kind,
            stringResource(R.string.feature_adhkar_challenge_quran_label),
            done,
            onToggle,
            action = { Open { links.onOpenQuran(item.start.surah, item.start.ayah) } }
        ) {
            Text(quranTitle(item), style = MaterialTheme.typography.titleSmall)
            val reading = item.reading
            if (!isArabicUi && item.surah != null && reading is QuranReading.Passage && reading.name == PassageName.SURAH) {
                Text(item.surah.translation, style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }

        is ChallengeView.Tasbih -> ChallengeRow(
            item.kind,
            stringResource(R.string.feature_adhkar_challenge_tasbih_label),
            done,
            onToggle,
            action = {
                if (!done) {
                    TextButton(onClick = { links.onStartTasbih(TasbihRoute(item.dhikr.id, item.count)) }) {
                        Text(stringResource(R.string.feature_adhkar_challenge_start))
                    }
                }
            }
        ) {
            Text(
                item.dhikr.arabic,
                style = DeenTextStyles.arabicBody.copy(fontSize = 20.sp, lineHeight = 34.sp, textDirection = TextDirection.Rtl)
            )
            Text(
                pluralStringResource(R.plurals.feature_adhkar_challenge_times, item.count, localDigits(item.count)),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }

        is ChallengeView.Adhkar -> ChallengeRow(
            item.kind,
            stringResource(R.string.feature_adhkar_challenge_adhkar_label),
            done,
            onToggle,
            action = { Open { links.onOpenCategory(item.category.id) } }
        ) {
            Text(
                stringResource(R.string.feature_adhkar_challenge_finish_adhkar, item.category.title.text()),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(
                    R.string.feature_adhkar_challenge_progress_of,
                    localDigits(item.done),
                    localDigits(item.category.items.size)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }

        is ChallengeView.Prayers -> ChallengeRow(
            item.kind,
            stringResource(R.string.feature_adhkar_challenge_prayers_label),
            done,
            onToggle,
            action = { Open(links.onOpenTracker) }
        ) {
            Text(stringResource(R.string.feature_adhkar_challenge_prayers), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.feature_adhkar_challenge_progress_of, localDigits(item.ticked), localDigits(5)),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }

        is ChallengeView.HadithOfTheDay -> ChallengeRow(
            item.kind,
            stringResource(R.string.feature_adhkar_challenge_hadith_label),
            done,
            onToggle,
            action = item.hadith?.let { hadith -> { Open { links.onOpenHadith(hadith.number) } } }
        ) {
            Text(stringResource(R.string.feature_adhkar_challenge_hadith), style = MaterialTheme.typography.titleSmall)
            item.hadith?.let {
                Text(
                    it.title.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        is ChallengeView.Sunnah -> ChallengeRow(item.kind, stringResource(R.string.feature_adhkar_challenge_sunnah_label), done, onToggle) {
            Text(stringResource(item.act.title()), style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun SunnahAct.title(): Int = when (this) {
    SunnahAct.DUHA -> R.string.feature_adhkar_sunnah_duha
    SunnahAct.WITR -> R.string.feature_adhkar_sunnah_witr
    SunnahAct.FAJR_SUNNAH -> R.string.feature_adhkar_sunnah_fajr
    SunnahAct.CHARITY -> R.string.feature_adhkar_sunnah_charity
    SunnahAct.KINSHIP -> R.string.feature_adhkar_sunnah_kinship
    SunnahAct.KIND_WORD -> R.string.feature_adhkar_sunnah_kind_word
    SunnahAct.DUA_FOR_OTHERS -> R.string.feature_adhkar_sunnah_dua_for_others
}

/** The streak so far, as a small pill (the number with a reward mark), lit once there is one. */
@Composable
private fun StreakBadge(streak: Int) {
    val colors = MaterialTheme.colorScheme
    val label = pluralStringResource(R.plurals.feature_adhkar_challenge_streak, streak, localDigits(streak))
    val lit = streak > 0
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (lit) colors.primary else colors.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painterResource(DeenIcons.Reward),
            contentDescription = null,
            tint = if (lit) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            localDigits(streak),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (lit) colors.onPrimary else colors.onSurfaceVariant
        )
    }
}

/** One challenge: a tick circle, what to do, and (optionally) a button to go and do it. */
@Composable
private fun ChallengeRow(
    kind: ChallengeKind,
    label: String,
    done: Boolean,
    onToggle: (ChallengeKind, Boolean) -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        TickCircle(done, label, onChange = { onToggle(kind, it) })
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 4.dp)
                .graphicsLayer { alpha = if (done) 0.6f else 1f }
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
        action?.invoke()
    }
}

@Composable
private fun TickCircle(done: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = DeenTheme.look.motion
    val fill by animateColorAsState(
        if (done) colors.primary else colors.surface.copy(alpha = 0f),
        if (motion) tween(200) else snap(),
        label = "tick"
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done && motion) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, DeenMotion.bouncy())
        }
    }
    val state = stringResource(if (done) R.string.feature_adhkar_challenge_done else R.string.feature_adhkar_challenge_not_done)
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(28.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, if (done) colors.primary else colors.outline, CircleShape)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = onChange)
            .semantics {
                contentDescription = label
                stateDescription = state
            },
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(
                painterResource(DeenIcons.Check),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** The dua in Arabic (a few lines; all of it, with its meaning, on tap), where it comes from and how many times. */
@Composable
private fun DuaChallenge(item: ChallengeView.Dua, state: ChallengesUiState) {
    val dua = item.dhikr
    var expanded by rememberSaveable(state.date) { mutableStateOf(false) }
    Text(
        buildString {
            append(stringResource(R.string.feature_adhkar_challenge_say_dua))
            if (dua.count >
                1
            ) {
                append(" · ").append(pluralStringResource(R.plurals.feature_adhkar_challenge_times, dua.count, localDigits(dua.count)))
            }
        },
        style = MaterialTheme.typography.titleSmall
    )
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Text(
            item.arabic,
            style = DeenTextStyles.arabicBody.copy(fontSize = 19.sp, lineHeight = 34.sp, textDirection = TextDirection.Rtl),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
        if (expanded && state.showTranslation && dua.translation.isNotBlank()) {
            Text(
                dua.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            stringResource(if (expanded) R.string.feature_adhkar_challenge_show_less else R.string.feature_adhkar_challenge_show_all),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
    val from = listOfNotNull(
        stringResource(R.string.feature_adhkar_challenge_from, item.category.title.text()),
        dua.reference.text().takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    if (from.isNotEmpty()) {
        Text(from, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun quranTitle(item: ChallengeView.Quran): String = when (val reading = item.reading) {
    is QuranReading.Pages -> pluralStringResource(R.plurals.feature_adhkar_challenge_read_pages, reading.pages, localDigits(reading.pages))

    is QuranReading.Passage -> when (reading.name) {
        PassageName.AYAT_AL_KURSI -> stringResource(R.string.feature_adhkar_challenge_read_kursi)

        PassageName.END_OF_AL_BAQARAH -> stringResource(R.string.feature_adhkar_challenge_read_baqarah_end)

        PassageName.FIRST_TEN_OF_AL_KAHF -> stringResource(R.string.feature_adhkar_challenge_read_kahf_ten)

        PassageName.THREE_QULS -> stringResource(R.string.feature_adhkar_challenge_read_quls)

        PassageName.SURAH -> {
            val surah = item.surah
            val name = when {
                surah == null -> localDigits(reading.surah)
                isArabicUi -> surah.arabicName
                else -> surah.transliteration
            }
            stringResource(R.string.feature_adhkar_challenge_read_surah, name)
        }
    }
}
