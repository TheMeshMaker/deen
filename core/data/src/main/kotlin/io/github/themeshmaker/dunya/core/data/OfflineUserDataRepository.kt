/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.data

import io.github.themeshmaker.dunya.core.datastore.DeenPreferencesDataSource
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.CustomColors
import io.github.themeshmaker.dunya.core.model.LiquidSettings
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.model.UserData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class OfflineUserDataRepository @Inject constructor(private val preferences: DeenPreferencesDataSource) : UserDataRepository {
    override val userData: Flow<UserData> = preferences.userData

    override suspend fun setLocation(location: SavedLocation) = preferences.setLocation(location)

    override suspend fun setPrayerSettings(settings: PrayerSettings) = preferences.setPrayerSettings(settings)

    override suspend fun setThemeStyle(style: ThemeStyle) = preferences.setThemeStyle(style)

    override suspend fun setCustomColors(colors: CustomColors) = preferences.setCustomColors(colors)

    override suspend fun setAlertSettings(settings: AlertSettings) = preferences.setAlertSettings(settings)

    override suspend fun setReliabilityStepConfirmed(step: String, confirmed: Boolean) =
        preferences.setReliabilityStepConfirmed(step, confirmed)

    override suspend fun setOnboardingComplete() = preferences.setOnboardingComplete()

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    override suspend fun setUiStyle(style: UiStyle) = preferences.setUiStyle(style)

    override suspend fun setSecretsUnlocked(unlocked: Boolean) = preferences.setSecretsUnlocked(unlocked)

    override suspend fun setTimetableCalendar(calendar: CalendarType) = preferences.setTimetableCalendar(calendar)

    override suspend fun setReadingSettings(settings: ReadingSettings) = preferences.setReadingSettings(settings)

    override suspend fun setLiquidSettings(settings: LiquidSettings) = preferences.setLiquidSettings(settings)
}
