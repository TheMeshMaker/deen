package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userData: Flow<UserData>

    suspend fun setLocation(location: SavedLocation)

    suspend fun setPrayerSettings(settings: PrayerSettings)

    suspend fun setThemeStyle(style: ThemeStyle)

    suspend fun setOnboardingComplete()
}

sealed interface DeviceLocationResult {
    data class Found(val location: SavedLocation) : DeviceLocationResult

    /** No permission, location off, or no provider answered in time. */
    data object Unavailable : DeviceLocationResult
}

interface LocationRepository {
    /** One-shot device location, named after the nearest city in the offline database. */
    suspend fun detectDeviceLocation(): DeviceLocationResult

    suspend fun searchCities(query: String, limit: Int = 30): List<City>
}
