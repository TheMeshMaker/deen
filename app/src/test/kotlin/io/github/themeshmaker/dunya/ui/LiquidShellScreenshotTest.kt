/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.ui

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.LiquidBackdrop
import io.github.themeshmaker.dunya.core.designsystem.component.LocalFloatingBarInset
import io.github.themeshmaker.dunya.core.designsystem.component.MenuRow
import io.github.themeshmaker.dunya.core.designsystem.component.PillChoice
import io.github.themeshmaker.dunya.core.designsystem.component.ProvideLiquidGlass
import io.github.themeshmaker.dunya.core.designsystem.component.liquidSource
import io.github.themeshmaker.dunya.core.designsystem.component.paddingExceptBottom
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.LiquidSettings
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Liquid shell: a tab screen of glass cards over the light backdrop, with the glass tab bar
 * floating over it. Only runs with -Pscreenshots: `./gradlew :app:testFossDebugUnitTest -Pscreenshots`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi", application = Application::class)
class LiquidShellScreenshotTest {
    /** The app's merged manifest doesn't declare the compose test host activity: register it first. */
    @get:Rule(order = 0)
    val hostActivity = object : ExternalResource() {
        override fun before() {
            val app = RuntimeEnvironment.getApplication()
            Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
        }
    }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Composable
    private fun Shell(dark: Boolean, theme: ThemeStyle, liquid: LiquidSettings) {
        DeenTheme(style = theme, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = UiStyle.LIQUID, liquid = liquid) {
            ProvideLiquidGlass(dark) {
                Box(Modifier.size(412.dp, 915.dp)) {
                    LiquidBackdrop(vivid = false, modifier = Modifier.fillMaxSize())
                    CompositionLocalProvider(LocalFloatingBarInset provides 84.dp) {
                        Box(Modifier.fillMaxSize().liquidSource(1f)) { SampleTab() }
                    }
                    Box(Modifier.align(Alignment.BottomCenter)) { LiquidTabBar(current = null, onSelect = {}) }
                }
            }
        }
    }

    @Composable
    private fun SampleTab() {
        DeenTabScaffold(title = "Quran") { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .paddingExceptBottom(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = padding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillChoice(listOf("Surahs", "Juz", "Bookmarks"), "Surahs", { it }, {}, Modifier.fillMaxWidth())
                DeenCard(onClick = {}) {
                    Text("Khatmah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Today: pages 81–100", style = MaterialTheme.typography.titleMedium)
                }
                DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    listOf(
                        "Al-Faatiha" to "The Opening",
                        "Al-Baqara" to "The Cow",
                        "Aal-i-Imraan" to "The Family of Imraan",
                        "An-Nisaa" to "The Women"
                    )
                        .forEach { (name, meaning) -> MenuRow(name, icon = DeenIcons.Quran, subtitle = meaning, onClick = {}) }
                }
                DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    MenuRow("Prayer tracker", icon = DeenIcons.Ok, subtitle = "Tick off your prayers and see your streak", onClick = {})
                    MenuRow("Islamic calendar", icon = DeenIcons.History, subtitle = "Ramadan, Eid, ʿArafah and the white days", onClick = {
                    })
                }
            }
        }
    }

    private fun shoot(name: String, dark: Boolean, theme: ThemeStyle = ThemeStyle.EMERALD, liquid: LiquidSettings = LiquidSettings()) {
        compose.setContent { Shell(dark, theme, liquid) }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/liquid-$name.png")
    }

    @Test fun light() = shoot("light", dark = false)

    @Test fun dark() = shoot("dark", dark = true)

    @Test fun andalus() = shoot("andalus", dark = false, theme = ThemeStyle.ANDALUS)

    @Test fun clearWideDeep() = shoot(
        "clear-wide-deep",
        dark = false,
        liquid = LiquidSettings(transparency = 1f, lensWidth = 1f, edge = 1f)
    )

    @Test fun frostedNarrowThin() =
        shoot("frosted-narrow-thin", dark = false, liquid = LiquidSettings(transparency = 0f, lensWidth = 0f, edge = 0f))
}
