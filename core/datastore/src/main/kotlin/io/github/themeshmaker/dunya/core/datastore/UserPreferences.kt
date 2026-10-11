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
    val themeStyle: String = "EMERALD",
    val customStart: Int? = null,
    val customEnd: Int? = null,
    val alerts: AlertPreferences? = null,
    val confirmedReliabilitySteps: Set<String> = emptySet(),
    val themeMode: String = "SYSTEM",
    val uiStyle: String = "CLASSIC",
    val secretsUnlocked: Boolean = false,
    val timetableCalendar: String = "GREGORIAN",
    val reading: ReadingPreferences = ReadingPreferences()
)

@Serializable
data class ReadingPreferences(
    val arabicScale: Float = 1f,
    val showTranslation: Boolean = true,
    val haptics: Boolean = true,
    val reciterId: String = "Alafasy_128kbps",
    val tafsirId: String = "jalalayn"
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

@Serializable
data class AlertPreferences(
    val modes: Map<String, String> = emptyMap(),
    val reminderMinutes: Int = 0,
    val afterAdhan: Map<String, Int> = emptyMap(),
    val respectSilentMode: Boolean = true,
    val useAlarmClock: Boolean = true,
    val voice: String = "default",
    val fajrVoice: String? = null,
    val masjid: MasjidPreferences? = null
)

@Serializable
data class MasjidPreferences(val enabled: Boolean = false, val minutesBefore: Int = 20, val prayers: List<String> = emptyList())
