package io.github.themeshmaker.dunya.core.datastore

import kotlinx.serialization.Serializable

/**
 * On-disk shape of the user's settings (JSON via DataStore). Enum values are stored by name and
 * every field has a default, so older files keep loading as fields are added.
 */
@Serializable
data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val location: LocationPreferences? = null,
    val prayer: PrayerPreferences? = null,
    val themeStyle: String = "EMERALD"
)

@Serializable
data class LocationPreferences(
    val name: String,
    val arabicName: String? = null,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val source: String
)

@Serializable
data class PrayerPreferences(
    val method: String,
    val asrMethod: String = "STANDARD",
    val highLatitudeRule: String = "AUTO",
    val offsetMinutes: Map<String, Int> = emptyMap(),
    val ramadanIshaExtension: Boolean = true,
    val hijriOffsetDays: Int = 0
)
