/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.timetable

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.themeshmaker.dunya.core.designsystem.component.LiquidBackdrop
import io.github.themeshmaker.dunya.core.designsystem.component.ProvideLiquidGlass
import io.github.themeshmaker.dunya.core.designsystem.component.liquidSource
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.prayer.IslamicEvents
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Islamic calendar, as a PNG in build/outputs/roborazzi. Only runs with -Pscreenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class CalendarScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun noAnimations() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun shoot(name: String, uiStyle: UiStyle, dark: Boolean) {
        val today = LocalDate.of(2026, 10, 13)
        val state = IslamicCalendarUiState(today, IslamicEvents.upcoming(today))
        compose.setContent {
            DeenTheme(style = ThemeStyle.EMERALD, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = uiStyle) {
                Box(Modifier.size(412.dp, 915.dp)) {
                    if (uiStyle == UiStyle.LIQUID) {
                        ProvideLiquidGlass(dark) {
                            LiquidBackdrop(vivid = false, modifier = Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().liquidSource(1f)) { IslamicCalendarScreen(state, onBack = {}) }
                        }
                    } else {
                        IslamicCalendarScreen(state, onBack = {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/calendar-$name.png")
    }

    @Test fun liquidLight() = shoot("liquid-light", UiStyle.LIQUID, dark = false)

    @Test
    @Config(qualifiers = "ar-w412dp-h915dp-xxhdpi")
    fun liquidLightArabic() = shoot("liquid-light-ar", UiStyle.LIQUID, dark = false)

    @Test fun sleekDark() = shoot("sleek-dark", UiStyle.SLEEK, dark = true)
}
