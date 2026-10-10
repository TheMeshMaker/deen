package io.github.themeshmaker.dunya.core.data

import io.github.themeshmaker.dunya.core.datastore.DeenPreferencesDataSource
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UserData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class OfflineUserDataRepository @Inject constructor(private val preferences: DeenPreferencesDataSource) : UserDataRepository {
    override val userData: Flow<UserData> = preferences.userData

    override suspend fun setLocation(location: SavedLocation) = preferences.setLocation(location)

    override suspend fun setPrayerSettings(settings: PrayerSettings) = preferences.setPrayerSettings(settings)

    override suspend fun setThemeStyle(style: ThemeStyle) = preferences.setThemeStyle(style)

    override suspend fun setAlertSettings(settings: AlertSettings) = preferences.setAlertSettings(settings)

    override suspend fun setReliabilityStepConfirmed(step: String, confirmed: Boolean) =
        preferences.setReliabilityStepConfirmed(step, confirmed)

    override suspend fun setOnboardingComplete() = preferences.setOnboardingComplete()
}
