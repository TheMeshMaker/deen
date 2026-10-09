package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.DataStore
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.HighLatitudeRule
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UserData
import java.time.DateTimeException
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeenPreferencesDataSource @Inject constructor(private val dataStore: DataStore<UserPreferences>) {

    val userData: Flow<UserData> = dataStore.data.map { prefs ->
        UserData(
            onboardingComplete = prefs.onboardingComplete,
            location = prefs.location?.toModel(),
            prayerSettings = prefs.prayer?.toModel(),
            themeStyle = enumOrNull<ThemeStyle>(prefs.themeStyle) ?: ThemeStyle.EMERALD
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

    suspend fun setOnboardingComplete() {
        dataStore.updateData { it.copy(onboardingComplete = true) }
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = enumValues<T>().firstOrNull { it.name == name }

private fun LocationPreferences.toModel() = SavedLocation(
    name = name,
    arabicName = arabicName,
    countryCode = countryCode,
    coordinates = Coordinates(latitude, longitude),
    zoneId = try {
        ZoneId.of(zoneId)
    } catch (_: DateTimeException) {
        ZoneId.systemDefault()
    },
    source = enumOrNull<LocationSource>(source) ?: LocationSource.MANUAL
)

private fun SavedLocation.toPreferences() = LocationPreferences(
    name = name,
    arabicName = arabicName,
    countryCode = countryCode,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    zoneId = zoneId.id,
    source = source.name
)

private fun PrayerPreferences.toModel() = PrayerSettings(
    method = enumOrNull<CalculationMethod>(method) ?: CalculationMethod.MUSLIM_WORLD_LEAGUE,
    asrMethod = enumOrNull<AsrMethod>(asrMethod) ?: AsrMethod.STANDARD,
    highLatitudeRule = enumOrNull<HighLatitudeRule>(highLatitudeRule) ?: HighLatitudeRule.AUTO,
    offsetMinutes = offsetMinutes.mapNotNull { (key, minutes) -> enumOrNull<Prayer>(key)?.let { it to minutes } }.toMap(),
    ramadanIshaExtension = ramadanIshaExtension,
    hijriOffsetDays = hijriOffsetDays
)

private fun PrayerSettings.toPreferences() = PrayerPreferences(
    method = method.name,
    asrMethod = asrMethod.name,
    highLatitudeRule = highLatitudeRule.name,
    offsetMinutes = offsetMinutes.mapKeys { it.key.name },
    ramadanIshaExtension = ramadanIshaExtension,
    hijriOffsetDays = hijriOffsetDays
)
