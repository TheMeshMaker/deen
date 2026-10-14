/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.today

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.domain.PrayerSchedule
import io.github.themeshmaker.dunya.core.domain.QuranPages
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.prayer.HijriCalendar
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeline
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today in each style, light and dark, as PNGs in build/outputs/roborazzi. Only runs with
 * -Pscreenshots (see build.gradle.kts): `./gradlew :feature:today:testDebugUnitTest -Pscreenshots`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class TodayScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val calculator = PrayerTimeCalculator(HijriCalendar())
    private val riyadh =
        SavedLocation("Riyadh", "الرياض", "SA", Coordinates(24.6877, 46.7219), ZoneId.of("Asia/Riyadh"), LocationSource.MANUAL)
    private val settings = PrayerSettings(CalculationMethod.UMM_AL_QURA)
    private val date = LocalDate.of(2026, 10, 13)

    @Before
    fun noAnimations() {
        // Entrances would otherwise be caught half way; the screens are the same without them.
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun state(): TodayUiState.Ready {
        fun day(d: LocalDate) = calculator.prayerDay(d, riyadh.coordinates, settings)
        val timeline = PrayerTimeline(day(date.minusDays(1)), day(date), day(date.plusDays(1)))
        val schedule = PrayerSchedule(riyadh, settings, date, HijriCalendar().toHijri(date), timeline)
        val now = timeline.today[Prayer.ASR].minusSeconds(47 * 60 + 13)
        return TodayUiState.Ready(schedule, timeline.next(now), now, AlertSettings(), alarmSetupNeeded = false)
    }

    private val extras = TodayExtras(
        lastRead = QuranPosition(2, 142),
        prayed = mapOf(
            date to setOf(Prayer.FAJR, Prayer.DHUHR),
            date.minusDays(1) to Prayer.prayers.toSet(),
            date.minusDays(2) to Prayer.prayers.toSet(),
            date.minusDays(3) to setOf(Prayer.FAJR, Prayer.MAGHRIB, Prayer.ISHA),
            date.minusDays(5) to Prayer.prayers.toSet()
        ),
        khatmah = KhatmahPlan(date.minusDays(4), 30),
        pages = QuranPages((0 until 604).map { QuranPosition(1 + it * 113 / 604, 1 + it % 7) }.distinct())
    )

    private val actions = TodayActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
    private val rows = PrayerRowActions({}, { _, _ -> })

    @Composable
    private fun Screen(uiStyle: UiStyle, dark: Boolean, theme: ThemeStyle, height: Int) {
        DeenTheme(style = theme, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = uiStyle) {
            Box(Modifier.size(412.dp, height.dp)) {
                when (uiStyle) {
                    UiStyle.LIQUID -> ProvideLiquidGlass(dark) {
                        LiquidBackdrop(vivid = true, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().liquidSource(1f)) { LiquidToday(state(), extras, actions, rows) }
                    }

                    UiStyle.CLASSIC, UiStyle.HERITAGE -> ClassicToday(state(), extras, actions, rows)

                    else -> SleekToday(state(), extras, actions, rows)
                }
            }
        }
    }

    private fun shoot(name: String, uiStyle: UiStyle, dark: Boolean, theme: ThemeStyle = ThemeStyle.EMERALD, height: Int = 915) {
        compose.setContent { Screen(uiStyle, dark, theme, height) }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/today-$name.png")
    }

    @Test fun liquidLight() = shoot("liquid-light", UiStyle.LIQUID, dark = false)

    @Test
    @Config(qualifiers = "w412dp-h2300dp-xxhdpi")
    fun liquidLightWhole() = shoot("liquid-light-whole", UiStyle.LIQUID, dark = false, height = 2300)

    @Test fun liquidDark() = shoot("liquid-dark", UiStyle.LIQUID, dark = true)

    @Test fun liquidRose() = shoot("liquid-rose", UiStyle.LIQUID, dark = false, theme = ThemeStyle.ROSE)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidLightArabic() = shoot("liquid-light-ar", UiStyle.LIQUID, dark = false)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidKiswahArabic() = shoot("liquid-kiswah-ar", UiStyle.LIQUID, dark = true, theme = ThemeStyle.KISWAH)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidMidnightArabic() = shoot("liquid-midnight-ar", UiStyle.LIQUID, dark = true, theme = ThemeStyle.MIDNIGHT)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidDesertArabic() = shoot("liquid-desert-ar", UiStyle.LIQUID, dark = false, theme = ThemeStyle.DESERT)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidRoseArabic() = shoot("liquid-rose-ar", UiStyle.LIQUID, dark = false, theme = ThemeStyle.ROSE)

    @Test fun liquidKiswah() = shoot("liquid-kiswah", UiStyle.LIQUID, dark = true, theme = ThemeStyle.KISWAH)

    @Test fun liquidMidnight() = shoot("liquid-midnight", UiStyle.LIQUID, dark = true, theme = ThemeStyle.MIDNIGHT)

    @Test fun liquidDesert() = shoot("liquid-desert", UiStyle.LIQUID, dark = false, theme = ThemeStyle.DESERT)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidAndalusArabic() = shoot("liquid-andalus-ar", UiStyle.LIQUID, dark = false, theme = ThemeStyle.ANDALUS)

    @Test fun sleekLight() = shoot("sleek-light", UiStyle.SLEEK, dark = false)

    @Test fun classicLight() = shoot("classic-light", UiStyle.CLASSIC, dark = false)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun trackerLiquidArabic() = trackerLiquid("tracker-liquid-ar")

    @Test
    fun trackerLiquid() = trackerLiquid("tracker-liquid")

    private fun trackerLiquid(name: String) {
        compose.setContent {
            DeenTheme(style = ThemeStyle.EMERALD, themeMode = ThemeMode.LIGHT, uiStyle = UiStyle.LIQUID) {
                ProvideLiquidGlass(false) {
                    Box(Modifier.size(412.dp, 915.dp)) {
                        LiquidBackdrop(vivid = false, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().liquidSource(1f)) {
                            TrackerScreen(TrackerUiState(date, state().schedule, extras.prayed), onBack = {}, onSet = { _, _, _ -> })
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
