package io.github.themeshmaker.dunya.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.themeshmaker.dunya.core.alarm.R as AlarmR
import io.github.themeshmaker.dunya.core.designsystem.component.DeenCard
import io.github.themeshmaker.dunya.core.designsystem.component.DeenScaffold
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.MenuRow
import io.github.themeshmaker.dunya.core.designsystem.component.PillChoice
import io.github.themeshmaker.dunya.core.designsystem.component.SectionLabel
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.designsystem.theme.DeviceSkin
import io.github.themeshmaker.dunya.core.designsystem.theme.gradient
import io.github.themeshmaker.dunya.core.model.AdhanVoice
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import io.github.themeshmaker.dunya.core.model.CustomColors
import io.github.themeshmaker.dunya.core.model.DistributionChannel
import io.github.themeshmaker.dunya.core.model.MasjidReminder
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.model.UserData
import io.github.themeshmaker.dunya.core.ui.AppLocales
import io.github.themeshmaker.dunya.core.ui.LanguageSheet
import io.github.themeshmaker.dunya.core.ui.R as UiR
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.findActivity
import io.github.themeshmaker.dunya.core.ui.labelRes
import io.github.themeshmaker.dunya.core.ui.localDigits

const val SOURCE_URL = "https://github.com/TheMeshMaker/deen"

@Composable
internal fun SettingsScreenRoute(
    onBack: () -> Unit,
    onOpenVoice: (fajr: Boolean) -> Unit,
    onOpenReliability: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenMethod: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == SettingsEvent.SECRETS_UNLOCKED) {
                Toast.makeText(context, R.string.feature_settings_secrets_unlocked, Toast.LENGTH_LONG).show()
            }
        }
    }
    DeenScaffold(
        title = stringResource(R.string.feature_settings_title),
        onBack = onBack,
        backLabel = stringResource(UiR.string.core_ui_back)
    ) { padding ->
        val data = state.data ?: return@DeenScaffold
        SettingsContent(
            data = data,
            buildInfo = state.buildInfo,
            padding = padding,
            viewModel = viewModel,
            onOpenVoice = onOpenVoice,
            onOpenReliability = onOpenReliability,
            onOpenLocation = onOpenLocation,
            onOpenMethod = onOpenMethod,
            onOpenTimetable = onOpenTimetable,
            onOpenAbout = onOpenAbout
        )
    }
}

@Composable
private fun SettingsContent(
    data: UserData,
    buildInfo: AppBuildInfo,
    padding: PaddingValues,
    viewModel: SettingsViewModel,
    onOpenVoice: (Boolean) -> Unit,
    onOpenReliability: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenMethod: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val look = DeenTheme.look
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = look.gutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(look.gap)
    ) {
        // ---- Appearance
        SectionLabel(stringResource(R.string.feature_settings_appearance))
        DeenCard {
            Text(stringResource(R.string.feature_settings_colours), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feature_settings_colours_free),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            var editCustom by rememberSaveable { mutableStateOf(false) }
            ThemeSwatches(
                selected = data.themeStyle,
                custom = data.customColors,
                onSelect = viewModel::setThemeStyle,
                onEditCustom = { editCustom = true }
            )
            if (editCustom) {
                CustomColoursSheet(
                    initial = data.customColors,
                    onApply = { colours ->
                        viewModel.setCustomColors(colours)
                        editCustom = false
                    },
                    onDismiss = { editCustom = false }
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.feature_settings_style), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feature_settings_style_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StylePicker(selected = data.uiStyle, onSelect = viewModel::setUiStyle)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.feature_settings_mode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            PillChoice(
                options = ThemeMode.entries,
                selected = data.themeMode,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setThemeMode,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---- Language
        SectionLabel(stringResource(R.string.feature_settings_language))
        DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            val current = remember(context) { AppLocales.current(context) }
            var picking by rememberSaveable { mutableStateOf(false) }
            MenuRow(
                title = stringResource(R.string.feature_settings_language),
                subtitle = current.displayName(),
                icon = DeenIcons.Language,
                onClick = { picking = true }
            )
            if (picking) {
                LanguageSheet(
                    current = current,
                    onPick = { language ->
                        picking = false
                        if (language != current) context.findActivity()?.let { AppLocales.set(it, language) }
                    },
                    onDismiss = { picking = false }
                )
            }
        }

        // ---- Adhan
        SectionLabel(stringResource(R.string.feature_settings_adhan))
        DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            val alerts = data.alertSettings
            MenuRow(
                title = stringResource(R.string.feature_settings_voice),
                subtitle = voiceName(alerts.voice),
                icon = DeenIcons.Adhan,
                onClick = { onOpenVoice(false) }
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_fajr_voice),
                subtitle = alerts.fajrVoice?.let { voiceName(it) } ?: stringResource(R.string.feature_settings_fajr_same),
                icon = DeenIcons.Night,
                onClick = { onOpenVoice(true) }
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.feature_settings_reminder), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.feature_settings_reminder_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChipRow(
                    options = listOf(0, 5, 10, 15, 20, 30),
                    selected = alerts.reminderMinutes,
                    label = {
                        if (it ==
                            0
                        ) {
                            stringResource(R.string.feature_settings_off)
                        } else {
                            stringResource(R.string.feature_settings_minutes_short, localDigits(it))
                        }
                    },
                    onSelect = { minutes -> viewModel.setAlerts { a -> a.copy(reminderMinutes = minutes) } }
                )
            }
            SwitchRow(
                title = stringResource(R.string.feature_settings_after_adhan),
                subtitle = stringResource(R.string.feature_settings_after_adhan_body),
                checked = alerts.afterAdhanOn,
                onChange = { on ->
                    viewModel.setAlerts { a ->
                        a.copy(
                            afterAdhanMinutes = if (on) Prayer.prayers.associateWith { AlertSettings.AFTER_ADHAN_DEFAULT } else emptyMap()
                        )
                    }
                }
            )
            AnimatedVisibility(visible = alerts.afterAdhanOn) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    Prayer.prayers.forEach { prayer ->
                        Text(
                            stringResource(prayer.labelRes()),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        ChipRow(
                            options = AlertSettings.AFTER_ADHAN_CHOICES,
                            selected = alerts.afterAdhanFor(prayer),
                            label = {
                                if (it == 0) {
                                    stringResource(R.string.feature_settings_off)
                                } else {
                                    stringResource(R.string.feature_settings_minutes_short, localDigits(it))
                                }
                            },
                            onSelect = { minutes ->
                                viewModel.setAlerts { a -> a.copy(afterAdhanMinutes = a.afterAdhanMinutes + (prayer to minutes)) }
                            }
                        )
                    }
                }
            }
            SwitchRow(
                title = stringResource(R.string.feature_settings_kahf),
                subtitle = stringResource(R.string.feature_settings_kahf_body),
                checked = alerts.kahfReminder,
                onChange = { on -> viewModel.setAlerts { it.copy(kahfReminder = on) } }
            )
            SwitchRow(
                title = stringResource(R.string.feature_settings_respect_silent),
                subtitle = stringResource(R.string.feature_settings_respect_silent_body),
                checked = alerts.respectSilentMode,
                onChange = { on -> viewModel.setAlerts { it.copy(respectSilentMode = on) } }
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_reliability),
                subtitle = stringResource(R.string.feature_settings_reliability_body),
                icon = DeenIcons.Battery,
                onClick = onOpenReliability
            )
        }

        // ---- Prayer times
        SectionLabel(stringResource(R.string.feature_settings_prayer_times))
        DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            MenuRow(
                title = stringResource(R.string.feature_settings_location),
                subtitle = data.location?.displayName(),
                icon = DeenIcons.Location,
                onClick = onOpenLocation
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_method),
                subtitle = data.prayerSettings?.method?.let { stringResource(it.labelRes()) },
                icon = DeenIcons.Tune,
                onClick = onOpenMethod
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_timetable),
                subtitle = stringResource(R.string.feature_settings_timetable_body),
                icon = DeenIcons.Calendar,
                onClick = onOpenTimetable
            )
        }

        // ---- Reading
        SectionLabel(stringResource(R.string.feature_settings_reading))
        DeenCard {
            ReadingSection(data.reading, onChange = viewModel::setReading)
        }

        // ---- Hidden options
        if (data.secretsUnlocked) {
            SectionLabel(stringResource(R.string.feature_settings_secrets))
            DeenCard {
                MasjidSection(
                    masjid = data.alertSettings.masjid,
                    onToggle = { on -> viewModel.setMasjid { it.copy(enabled = on) } },
                    onMinutes = { minutes -> viewModel.setMasjid { it.copy(minutesBefore = minutes) } },
                    onTogglePrayer = viewModel::toggleMasjidPrayer
                )
                TextButton(onClick = viewModel::hideSecrets, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.feature_settings_secrets_hide))
                }
            }
        }

        // ---- About
        SectionLabel(stringResource(R.string.feature_settings_about))
        DeenCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            MenuRow(
                title = stringResource(R.string.feature_settings_version),
                subtitle = buildLine(buildInfo),
                icon = DeenIcons.Info,
                onClick = viewModel::tapVersion
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_sources),
                subtitle = stringResource(R.string.feature_settings_sources_body),
                icon = DeenIcons.Hadith,
                onClick = onOpenAbout
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_source_code),
                subtitle = stringResource(R.string.feature_settings_source_code_body),
                icon = DeenIcons.Code,
                onClick = { runCatching { uriHandler.openUri(SOURCE_URL) } }
            )
            MenuRow(
                title = stringResource(R.string.feature_settings_privacy),
                subtitle = stringResource(R.string.feature_settings_privacy_body),
                icon = DeenIcons.Privacy
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The colour themes as gradient swatches; the last one is the user's own (tap to edit it). */
@Composable
private fun ThemeSwatches(selected: ThemeStyle, custom: CustomColors, onSelect: (ThemeStyle) -> Unit, onEditCustom: () -> Unit) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ThemeStyle.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { style ->
                    val isSelected = style == selected
                    val gradient = style.gradient(custom)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.RadioButton) { if (style == ThemeStyle.CUSTOM) onEditCustom() else onSelect(style) }
                            .semantics { this.selected = isSelected }
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .padding(if (isSelected) 5.dp else 0.dp)
                                .clip(CircleShape)
                                .background(gradient.brush),
                            contentAlignment = Alignment.Center
                        ) {
                            GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize(), cell = 26.dp)
                            when {
                                isSelected -> Text("✓", color = gradient.content, style = MaterialTheme.typography.titleLarge)

                                style == ThemeStyle.CUSTOM -> Icon(
                                    painterResource(DeenIcons.Palette),
                                    contentDescription = null,
                                    tint = gradient.content,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            stringResource(style.nameRes()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Little mock screens, three to a row, so the difference between the styles is visible before choosing. */
@Composable
private fun StylePicker(selected: UiStyle, onSelect: (UiStyle) -> Unit) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // The ColorOS style is offered only on ColorOS 17 and newer.
        UiStyle.entries.filter { it != UiStyle.COLOROS || DeviceSkin.isColorOs17 }.chunked(STYLES_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { style ->
                    val isSelected = style == selected
                    val motion = DeenTheme.look.motion
                    val edge by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        if (motion) tween(250) else snap(),
                        label = "styleEdge"
                    )
                    val lift by animateFloatAsState(
                        if (isSelected ||
                            !motion
                        ) {
                            1f
                        } else {
                            0.96f
                        },
                        if (motion) DeenMotion.bouncy() else snap(),
                        label = "styleLift"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = lift
                                scaleY = lift
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .border(if (isSelected) 2.dp else 1.dp, edge, RoundedCornerShape(18.dp))
                            .clickable(role = Role.RadioButton) { onSelect(style) }
                            .semantics { this.selected = isSelected }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StyleMock(style)
                        Text(
                            stringResource(style.nameRes()),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                repeat(STYLES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val STYLES_PER_ROW = 3

@Composable
private fun StyleMock(style: UiStyle) {
    val gradient = DeenTheme.gradient
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val immersive = style.immersive
    val base = if (dark) colors.background else Color.White
    val gold = lerp(colors.primary, Color(0xFFB08D3C), 0.55f)
    val page: Brush = when (style) {
        UiStyle.LIQUID -> Brush.linearGradient(
            listOf(lerp(base, gradient.end, 0.45f), lerp(base, gradient.start, 0.30f), lerp(base, gradient.end, 0.20f))
        )

        UiStyle.CLASSIC -> SolidColor(colors.surface)

        UiStyle.COLOROS -> SolidColor(if (dark) Color.Black else Color(0xFFF2F3F5))

        UiStyle.SLEEK -> SolidColor(colors.surfaceContainerLow)

        UiStyle.GLASS -> Brush.verticalGradient(listOf(lerp(base, gradient.end, 0.26f), lerp(base, gradient.start, 0.12f)))

        UiStyle.HERITAGE -> SolidColor(if (dark) Color(0xFF14110B) else Color(0xFFF5EEDC))
    }
    val card = when (style) {
        UiStyle.LIQUID -> Color.White.copy(alpha = if (dark) 0.16f else 0.50f)
        UiStyle.CLASSIC -> colors.surfaceContainerLow
        UiStyle.COLOROS -> if (dark) Color(0xFF1C1C1E) else Color.White
        UiStyle.SLEEK -> if (dark) colors.surfaceContainerHighest else Color.White
        UiStyle.GLASS -> Color.White.copy(alpha = if (dark) 0.10f else 0.62f)
        UiStyle.HERITAGE -> if (dark) Color(0xFF1D1912) else Color(0xFFFFFBF1)
    }
    val cardShape: Shape = when (style) {
        UiStyle.CLASSIC -> RoundedCornerShape(4.dp)
        UiStyle.HERITAGE -> CutCornerShape(4.dp)
        UiStyle.LIQUID -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(9.dp)
    }
    val edge: Color? = when (style) {
        UiStyle.LIQUID -> Color.White.copy(alpha = if (dark) 0.45f else 0.95f)
        UiStyle.SLEEK -> colors.outlineVariant
        UiStyle.GLASS -> Color.White.copy(alpha = if (dark) 0.25f else 0.9f)
        UiStyle.HERITAGE -> gold
        UiStyle.CLASSIC, UiStyle.COLOROS -> null
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(12.dp))
            .background(page)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header: a card inside the page (Classic, Heritage) or a full-bleed band (Sleek, Glass).
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(if (immersive) 0.dp else 6.dp)
                    .height(if (immersive) 64.dp else 48.dp)
                    .clip(
                        when (style) {
                            UiStyle.HERITAGE -> CutCornerShape(6.dp)
                            UiStyle.CLASSIC -> RoundedCornerShape(10.dp)
                            else -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        }
                    )
                    // Liquid: the gradient is the page itself, so the header melts into it.
                    .background(if (style == UiStyle.LIQUID) SolidColor(Color.Transparent) else gradient.brush)
            )
            Column(
                Modifier.padding(horizontal = 6.dp, vertical = if (immersive) 0.dp else 2.dp),
                verticalArrangement = Arrangement.spacedBy(if (immersive) 6.dp else 4.dp)
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (immersive) 18.dp else 14.dp)
                            .clip(cardShape)
                            .background(card)
                            .then(
                                if (edge !=
                                    null
                                ) {
                                    Modifier.border(if (style == UiStyle.HERITAGE) 0.75.dp else 0.5.dp, edge, cardShape)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
        // Navigation: a floating pill (Sleek, Glass) or a plain bar.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(if (immersive) 6.dp else 0.dp)
                .fillMaxWidth()
                .height(if (immersive) 14.dp else 12.dp)
                .clip(if (immersive) CircleShape else RoundedCornerShape(0.dp))
                .background(
                    when (style) {
                        UiStyle.LIQUID -> Color.White.copy(alpha = if (dark) 0.22f else 0.65f)
                        UiStyle.GLASS -> Color.White.copy(alpha = if (dark) 0.14f else 0.75f)
                        UiStyle.SLEEK -> colors.inverseSurface.copy(alpha = 0.85f)
                        UiStyle.HERITAGE -> gold.copy(alpha = 0.25f)
                        UiStyle.CLASSIC -> colors.surfaceContainer
                        UiStyle.COLOROS -> (if (dark) Color.White else Color.Black).copy(alpha = 0.10f)
                    }
                )
        )
    }
}

@Composable
private fun ReadingSection(reading: ReadingSettings, onChange: ((ReadingSettings) -> ReadingSettings) -> Unit) {
    Text(stringResource(R.string.feature_settings_arabic_size), style = MaterialTheme.typography.bodyLarge)
    Text(
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        style = DeenTextStyles.arabicBody.let {
            it.copy(
                fontSize = it.fontSize * reading.arabicScale,
                lineHeight =
                    it.lineHeight * reading.arabicScale
            )
        },
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
    Slider(
        value = reading.arabicScale,
        onValueChange = { value -> onChange { it.copy(arabicScale = (value * 10).toInt() / 10f) } },
        valueRange = ReadingSettings.SCALE_RANGE,
        steps = 7
    )
    SwitchRow(
        title = stringResource(R.string.feature_settings_translation),
        subtitle = stringResource(R.string.feature_settings_translation_body),
        checked = reading.showTranslation,
        onChange = { on -> onChange { it.copy(showTranslation = on) } },
        inset = false
    )
    SwitchRow(
        title = stringResource(R.string.feature_settings_haptics),
        subtitle = stringResource(R.string.feature_settings_haptics_body),
        checked = reading.haptics,
        onChange = { on -> onChange { it.copy(haptics = on) } },
        inset = false
    )
}

@Composable
private fun MasjidSection(masjid: MasjidReminder, onToggle: (Boolean) -> Unit, onMinutes: (Int) -> Unit, onTogglePrayer: (Prayer) -> Unit) {
    SwitchRow(
        title = stringResource(R.string.feature_settings_masjid),
        subtitle = stringResource(R.string.feature_settings_masjid_body),
        checked = masjid.enabled,
        onChange = onToggle,
        inset = false
    )
    if (!masjid.enabled) return
    Text(
        stringResource(R.string.feature_settings_masjid_minutes, localDigits(masjid.minutesBefore)),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp)
    )
    val range = MasjidReminder.MINUTES_RANGE
    Slider(
        value = masjid.minutesBefore.toFloat(),
        onValueChange = { onMinutes((it / 5).toInt() * 5) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (range.last - range.first) / 5 - 1
    )
    Text(stringResource(R.string.feature_settings_masjid_prayers), style = MaterialTheme.typography.bodyLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Prayer.prayers.forEach { prayer ->
            FilterChip(
                selected = prayer in masjid.prayers,
                onClick = { onTogglePrayer(prayer) },
                label = { Text(stringResource(prayer.labelRes())) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, inset: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = if (inset) 16.dp else 0.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun voiceName(voice: String): String = stringResource(
    when (voice) {
        AdhanVoice.TRADITIONAL -> AlarmR.string.core_alarm_voice_traditional
        AdhanVoice.AAQIB -> AlarmR.string.core_alarm_voice_aaqib
        AdhanVoice.CUSTOM -> AlarmR.string.core_alarm_voice_custom
        else -> AlarmR.string.core_alarm_voice_default
    }
)

@Composable
private fun buildLine(info: AppBuildInfo): String {
    val channel = stringResource(
        when (info.channel) {
            DistributionChannel.PLAY -> R.string.feature_settings_channel_play
            DistributionChannel.FOSS -> R.string.feature_settings_channel_foss
        }
    )
    // Isolate the version as left-to-right, otherwise "0.1.0-debug" renders as "debug-0.1.0" in Arabic.
    val version = BidiFormatter.getInstance().unicodeWrap(info.versionName, TextDirectionHeuristics.LTR)
    return stringResource(R.string.feature_settings_build_line, version, channel)
}

private fun ThemeStyle.nameRes(): Int = when (this) {
    ThemeStyle.EMERALD -> UiR.string.core_ui_theme_emerald
    ThemeStyle.KISWAH -> UiR.string.core_ui_theme_kiswah
    ThemeStyle.DESERT -> UiR.string.core_ui_theme_desert
    ThemeStyle.ANDALUS -> UiR.string.core_ui_theme_andalus
    ThemeStyle.ROSE -> UiR.string.core_ui_theme_rose
    ThemeStyle.MIDNIGHT -> UiR.string.core_ui_theme_midnight
    ThemeStyle.CUSTOM -> UiR.string.core_ui_theme_custom
}

private fun UiStyle.nameRes(): Int = when (this) {
    UiStyle.CLASSIC -> R.string.feature_settings_style_classic
    UiStyle.SLEEK -> R.string.feature_settings_style_sleek
    UiStyle.GLASS -> R.string.feature_settings_style_glass
    UiStyle.LIQUID -> R.string.feature_settings_style_liquid
    UiStyle.HERITAGE -> R.string.feature_settings_style_heritage
    UiStyle.COLOROS -> R.string.feature_settings_style_coloros
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.feature_settings_mode_system
    ThemeMode.LIGHT -> R.string.feature_settings_mode_light
    ThemeMode.DARK -> R.string.feature_settings_mode_dark
    ThemeMode.OLED_BLACK -> R.string.feature_settings_mode_black
}
