/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.adhkar

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.themeshmaker.dunya.core.designsystem.component.LiquidBackdrop
import io.github.themeshmaker.dunya.core.designsystem.component.ProvideLiquidGlass
import io.github.themeshmaker.dunya.core.designsystem.component.liquidSource
import io.github.themeshmaker.dunya.core.designsystem.component.pageBackground
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.Dhikr
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.LocalizedText
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.QuranReading
import io.github.themeshmaker.dunya.core.model.Revelation
import io.github.themeshmaker.dunya.core.model.SunnahAct
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.model.TasbihDhikr
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The daily challenges card in a few styles, as PNGs in build/outputs/roborazzi. Only runs with
 * -Pscreenshots: `./gradlew :feature:adhkar:testDebugUnitTest -Pscreenshots`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h700dp-xxhdpi")
class AdhkarChallengesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun noAnimations() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private val dua = Dhikr(
        id = 5,
        preface = null,
        text = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
        quran = emptyList(),
        count = 1,
        instruction = null,
        translation = "O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, " +
            "and to You is the resurrection.",
        virtue = null,
        reference = LocalizedText("الترمذي (3391)", "at-Tirmidhi 3391")
    )

    private val morning = AdhkarCategory(AdhkarCategory.MORNING, LocalizedText("أذكار الصباح", "Morning"), List(25) { dua.copy(id = it) })

    /** Every kind of challenge, to see them all (a real day has three). */
    private val everything = listOf(
        ChallengeView.Dua(dua, dua.text.orEmpty(), morning),
        ChallengeView.Quran(
            QuranReading.Passage(67),
            Surah(67, 30, "الملك", "Al-Mulk", "The Sovereignty", Revelation.MECCAN),
            QuranPosition(67, 1)
        ),
        ChallengeView.Tasbih(TasbihDhikr.SUBHANALLAHI_WA_BIHAMDIHI, 100),
        ChallengeView.Adhkar(morning, 18),
        ChallengeView.Prayers(3),
        ChallengeView.HadithOfTheDay(
            Hadith(1, LocalizedText("إنما الأعمال بالنيات", "Actions are by intentions"), "", "", LocalizedText("", ""))
        ),
        ChallengeView.Sunnah(SunnahAct.DUHA)
    )

    private fun state(items: List<ChallengeView>, done: Set<ChallengeKind>, streak: Int) = ChallengesUiState(
        date = LocalDate.of(2026, 10, 14),
        items = items,
        done = done,
        streak = streak,
        best = 9,
        showTranslation = true,
        haptics = false
    )

    @Composable
    private fun Card(style: UiStyle, dark: Boolean, items: List<ChallengeView>, done: Set<ChallengeKind>, streak: Int) {
        DeenTheme(style = ThemeStyle.EMERALD, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = style) {
            Box(Modifier.fillMaxSize()) {
                val content = @Composable {
                    ChallengesCard(state(items, done, streak), onToggle = { _, _ -> }, links = links, modifier = Modifier.padding(16.dp))
                }
                if (style == UiStyle.LIQUID) {
                    ProvideLiquidGlass(dark) {
                        LiquidBackdrop(vivid = false, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().liquidSource(1f)) { content() }
                    }
                } else {
                    Box(Modifier.fillMaxSize().pageBackground(DeenTheme.look)) { content() }
                }
            }
        }
    }

    private val links = ChallengeLinks({ _, _ -> }, {}, {}, {}, {})

    private fun shoot(
        name: String,
        style: UiStyle,
        dark: Boolean = false,
        items: List<ChallengeView> = everything.take(3),
        done: Set<ChallengeKind> = setOf(ChallengeKind.DUA),
        streak: Int = 4
    ) {
        compose.setContent { Card(style, dark, items, done, streak) }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/challenges-$name.png")
    }

    @Test fun sleek() = shoot("sleek", UiStyle.SLEEK)

    @Test
    @Config(qualifiers = "w412dp-h1300dp-xxhdpi")
    fun everyKind() = shoot("every-kind", UiStyle.SLEEK, items = everything, done = setOf(ChallengeKind.PRAYERS, ChallengeKind.SUNNAH))

    @Test fun liquid() = shoot("liquid", UiStyle.LIQUID)

    @Test fun classicDark() = shoot("classic-dark", UiStyle.CLASSIC, dark = true, done = ChallengeKind.entries.toSet(), streak = 5)

    @Test
    @Config(qualifiers = "ar-w412dp-h700dp-xxhdpi")
    fun arabic() = shoot("arabic", UiStyle.SLEEK, items = everything.drop(3).take(3), done = emptySet(), streak = 0)

    /** The whole Adhkar tab in Liquid, for the trailer and the store listing. */
    private fun tab(name: String, theme: ThemeStyle, dark: Boolean) {
        val categories = listOf(
            AdhkarCategory.MORNING to LocalizedText("أذكار الصباح", "Morning") to 25,
            AdhkarCategory.EVENING to LocalizedText("أذكار المساء", "Evening") to 23,
            AdhkarCategory.AFTER_PRAYER to LocalizedText("أذكار بعد الصلاة", "After prayer") to 10,
            AdhkarCategory.SLEEP to LocalizedText("أذكار النوم", "Before sleep") to 12,
            "waking" to LocalizedText("أذكار الاستيقاظ", "Waking up") to 3,
            AdhkarCategory.ADHAN to LocalizedText("عند الأذان", "With the adhan") to 4,
            AdhkarCategory.MOSQUE to LocalizedText("المسجد", "The mosque") to 4,
            "wudu" to LocalizedText("الوضوء", "Wudu") to 4,
            "home" to LocalizedText("الخروج من المنزل", "Leaving home") to 3
        ).map { (idTitle, size) -> AdhkarCategory(idTitle.first, idTitle.second, List(size) { dua.copy(id = it) }) }
        val state = AdhkarHomeUiState(
            categories = categories,
            done = mapOf(AdhkarCategory.MORNING to 25, AdhkarCategory.EVENING to 6, AdhkarCategory.AFTER_PRAYER to 10),
            suggested = AdhkarCategory.EVENING,
            tasbih = io.github.themeshmaker.dunya.core.model.TasbihState(todayTotal = 233, day = LocalDate.of(2026, 10, 14)),
            today = LocalDate.of(2026, 10, 14),
            challenges = state(everything.take(3), setOf(ChallengeKind.DUA, ChallengeKind.QURAN), 12)
        )
        compose.setContent {
            DeenTheme(style = theme, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = UiStyle.LIQUID) {
                ProvideLiquidGlass(dark) {
                    Box(Modifier.fillMaxSize()) {
                        LiquidBackdrop(vivid = false, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().liquidSource(1f)) {
                            AdhkarHomeScreen(state, onToggleChallenge = { _, _ -> }, onOpenCategory = {}, onOpenTasbih = {}, links = links)
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/adhkar-tab-$name.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi")
    fun tabLiquid() = tab("liquid", ThemeStyle.EMERALD, dark = false)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun tabLiquidArabicDark() = tab("liquid-ar-dark", ThemeStyle.KISWAH, dark = true)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun tabLiquidArabic() = tab("liquid-ar", ThemeStyle.EMERALD, dark = false)
}
