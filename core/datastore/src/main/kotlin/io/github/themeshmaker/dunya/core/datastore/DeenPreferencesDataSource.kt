package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.DataStore
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
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
            confirmedReliabilitySteps = prefs.confirmedReliabilitySteps,
            themeMode = enumOrNull<ThemeMode>(prefs.themeMode) ?: ThemeMode.SYSTEM,
            uiStyle = enumOrNull<UiStyle>(prefs.uiStyle) ?: UiStyle.CLASSIC,
            secretsUnlocked = prefs.secretsUnlocked,
            timetableCalendar = enumOrNull<CalendarType>(prefs.timetableCalendar) ?: CalendarType.GREGORIAN,
            reading = ReadingSettings(
                arabicScale = prefs.reading.arabicScale.coerceIn(ReadingSettings.SCALE_RANGE),
                showTranslation = prefs.reading.showTranslation,
                haptics = prefs.reading.haptics
            )
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

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { it.copy(themeMode = mode.name) }
    }

    suspend fun setUiStyle(style: UiStyle) {
        dataStore.updateData { it.copy(uiStyle = style.name) }
    }

    suspend fun setSecretsUnlocked(unlocked: Boolean) {
        dataStore.updateData { it.copy(secretsUnlocked = unlocked) }
    }

    suspend fun setTimetableCalendar(calendar: CalendarType) {
        dataStore.updateData { it.copy(timetableCalendar = calendar.name) }
    }

    suspend fun setReadingSettings(settings: ReadingSettings) {
        dataStore.updateData {
            it.copy(reading = ReadingPreferences(settings.arabicScale, settings.showTranslation, settings.haptics))
        }
    }

    suspend fun setOnboardingComplete() {
        dataStore.updateData { it.copy(onboardingComplete = true) }
    }
}
