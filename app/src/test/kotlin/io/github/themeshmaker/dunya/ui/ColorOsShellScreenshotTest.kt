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
import androidx.compose.material3.Surface
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
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.DeenSwitch
import io.github.themeshmaker.dunya.core.designsystem.component.DeenTabScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.LocalFloatingBarInset
import io.github.themeshmaker.dunya.core.designsystem.component.MenuRow
import io.github.themeshmaker.dunya.core.designsystem.component.PillChoice
import io.github.themeshmaker.dunya.core.designsystem.component.ProvideColorOsBlur
import io.github.themeshmaker.dunya.core.designsystem.component.SectionLabel
import io.github.themeshmaker.dunya.core.designsystem.component.colorOsBlurSource
import io.github.themeshmaker.dunya.core.designsystem.component.paddingExceptBottom
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.designsystem.theme.DeviceSkin
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import org.junit.After
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
 * The ColorOS style, to hold up against ColorOS 17's own apps: a pushed settings screen and a tab
 * screen with the floating tab bar. Only runs with -Pscreenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w351dp-h805dp-xxhdpi", application = Application::class)
class ColorOsShellScreenshotTest {
    @get:Rule(order = 0)
    val hostActivity = object : ExternalResource() {
        override fun before() {
            val app = RuntimeEnvironment.getApplication()
            Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
            DeviceSkin.colorOsOverride = true
        }
    }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @After
    fun reset() {
        DeviceSkin.colorOsOverride = null
    }

    @Composable
    private fun Themed(dark: Boolean, content: @Composable () -> Unit) {
        DeenTheme(style = ThemeStyle.EMERALD, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, uiStyle = UiStyle.COLOROS) {
            Surface(color = DeenTheme.look.page, contentColor = MaterialTheme.colorScheme.onBackground) {
                Box(Modifier.size(351.dp, 805.dp)) { content() }
            }
        }
    }

    @Composable
    private fun SettingsSample() {
        DeenScaffold(title = "Notifications", onBack = {}, backLabel = "Back") { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    MenuRow("Adhan at prayer times", subtitle = "Plays the full adhan", trailing = { DeenSwitch(true, {}) })
                    MenuRow("Reminder before prayer", trailing = { DeenSwitch(false, {}) })
                    MenuRow("Adhan voice", subtitle = "Makkah", onClick = {})
                }
                SectionLabel("Appearance")
                DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    MenuRow("Theme", onClick = {})
                    MenuRow("Language", subtitle = "English", onClick = {})
                    MenuRow("Show Hijri date", trailing = { DeenSwitch(true, {}) })
                }
                PillChoice(listOf("Light", "Dark", "System"), "Dark", { it }, {}, Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun TabSample() = ProvideColorOsBlur {
        CompositionLocalProvider(LocalFloatingBarInset provides 70.dp) {
            Box(Modifier.fillMaxSize().colorOsBlurSource()) { TabContent() }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { FusionTabBar(current = null, onSelect = {}) }
    }

    @Composable
    private fun TabContent() {
        run {
            DeenTabScaffold(title = "More") { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .paddingExceptBottom(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = padding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DeenCard(onClick = {}) {
                        Text("Khatmah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Today: pages 81–100", style = MaterialTheme.typography.titleMedium)
                    }
                    DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        MenuRow("Qibla", icon = DeenIcons.Qibla, onClick = {})
                        MenuRow("Prayer tracker", icon = DeenIcons.Ok, subtitle = "Tick off your prayers", onClick = {})
                        MenuRow("Islamic calendar", icon = DeenIcons.History, onClick = {})
                    }
                    DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        MenuRow("Settings", icon = DeenIcons.Settings, onClick = {})
                        MenuRow("About Deen", icon = DeenIcons.Info, onClick = {})
                    }
                    repeat(3) {
                        DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                            MenuRow("Adhkar of the morning", subtitle = "After Fajr until sunrise", onClick = {})
                            MenuRow("Adhkar of the evening", subtitle = "After ʿAsr until Maghrib", onClick = {})
                        }
                    }
                }
            }
        }
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent(content)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/coloros-$name.png")
    }

    @Test fun settingsDark() = shoot("settings-dark") { Themed(dark = true) { SettingsSample() } }

    @Test fun settingsLight() = shoot("settings-light") { Themed(dark = false) { SettingsSample() } }

    @Test fun tabDark() = shoot("tab-dark") { Themed(dark = true) { TabSample() } }

    @Test fun tabLight() = shoot("tab-light") { Themed(dark = false) { TabSample() } }
}
