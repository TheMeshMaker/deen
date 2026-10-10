package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.DataStore
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UserData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeenPreferencesDataSource @Inject constructor(private val dataStore: DataStore<UserPreferences>) {

    val userData: Flow<UserData> = dataStore.data.map { prefs ->
        UserData(
            onboardingComplete = prefs.onboardingComplete,
            location = prefs.location?.toModel(),
            prayerSettings = prefs.prayer?.toModel(),
            themeStyle = enumOrNull<ThemeStyle>(prefs.themeStyle) ?: ThemeStyle.EMERALD,
            alertSettings = prefs.alerts?.toModel() ?: AlertSettings(),
            confirmedReliabilitySteps = prefs.confirmedReliabilitySteps
        )
    }

    suspend fun setLocation(location: SavedLocation) {
        dataStore.updateData { it.copy(location = location.toPreferences()) }
    }

    suspend fun setPrayerSettings(settings: PrayerSettings) {
        dataStore.updateData { it.copy(prayer = settings.toPreferences()) }
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        dataStore.updateData { it.copy(themeStyle = style.name) }
    }

    suspend fun setAlertSettings(settings: AlertSettings) {
        dataStore.updateData { it.copy(alerts = settings.toPreferences()) }
    }

    suspend fun setReliabilityStepConfirmed(step: String, confirmed: Boolean) {
        dataStore.updateData {
            it.copy(confirmedReliabilitySteps = if (confirmed) it.confirmedReliabilitySteps + step else it.confirmedReliabilitySteps - step)
        }
    }

    suspend fun setOnboardingComplete() {
        dataStore.updateData { it.copy(onboardingComplete = true) }
    }
}
