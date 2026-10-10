package io.github.themeshmaker.dunya.core.model

import java.time.ZoneId

data class Coordinates(val latitude: Double, val longitude: Double)

/** A populated place from the bundled offline city database (GeoNames). */
data class City(
    val id: Int,
    val name: String,
    /** Arabic name when GeoNames has one. */
    val arabicName: String?,
    /** State / province / region, in English. */
    val region: String,
    /** ISO 3166-1 alpha-2, e.g. "SA". */
    val countryCode: String,
    val coordinates: Coordinates,
    val zoneId: ZoneId,
    val population: Int
)

enum class LocationSource {
    DEVICE,
    MANUAL
}

/** The place prayer times are calculated for. */
data class SavedLocation(
    val name: String,
    val arabicName: String?,
    val countryCode: String?,
    val coordinates: Coordinates,
    val zoneId: ZoneId,
    val source: LocationSource
)

data class UserData(
    val onboardingComplete: Boolean,
    val location: SavedLocation?,
    /** Null until the user has confirmed a calculation method. */
    val prayerSettings: PrayerSettings?,
    val themeStyle: ThemeStyle = ThemeStyle.EMERALD,
    val alertSettings: AlertSettings = AlertSettings(),
    /** Reliability steps Android can't detect, confirmed by the user (e.g. OEM auto-start). */
    val confirmedReliabilitySteps: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val uiStyle: UiStyle = UiStyle.CLASSIC,
    /** Hidden options were unlocked (About → tap the version seven times). */
    val secretsUnlocked: Boolean = false,
    val timetableCalendar: CalendarType = CalendarType.GREGORIAN,
    val reading: ReadingSettings = ReadingSettings()
)

/** How Quran, hadith and adhkar text is shown. */
data class ReadingSettings(
    /** Multiplies the Arabic text size; 0.8–1.6. */
    val arabicScale: Float = 1f,
    /** Show translations under Arabic adhkar and hadith. */
    val showTranslation: Boolean = true,
    /** Vibrate on each count in the tasbih and adhkar. */
    val haptics: Boolean = true
) {
    companion object {
        val SCALE_RANGE: ClosedFloatingPointRange<Float> = 0.8f..1.6f
    }
}
