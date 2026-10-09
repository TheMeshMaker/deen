package io.github.themeshmaker.dunya.core.model

/** Calculation methods (docs/BLUEPRINT.md §7). Parameters live in :core:prayer. */
enum class CalculationMethod {
    UMM_AL_QURA,
    MUSLIM_WORLD_LEAGUE,
    NORTH_AMERICA,
    EGYPTIAN,
    KARACHI,
    DUBAI,
    KUWAIT,
    QATAR,
    SINGAPORE,
    MOONSIGHTING_COMMITTEE,
    TURKEY
}

/** Asr time: Standard (Shafi'i, Maliki, Hanbali; shadow = 1x) or Hanafi (shadow = 2x). */
enum class AsrMethod {
    STANDARD,
    HANAFI
}

/** How Fajr and Isha are found where twilight never fully ends in summer. */
enum class HighLatitudeRule {
    /** Seventh of the night above 48° latitude (north or south), otherwise middle of the night. */
    AUTO,
    MIDDLE_OF_THE_NIGHT,
    SEVENTH_OF_THE_NIGHT,
    TWILIGHT_ANGLE
}

data class PrayerSettings(
    val method: CalculationMethod,
    val asrMethod: AsrMethod = AsrMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.AUTO,
    /** Per-prayer corrections in minutes, e.g. to match the local mosque. */
    val offsetMinutes: Map<Prayer, Int> = emptyMap(),
    /** Umm al-Qura only: Isha is 120 instead of 90 minutes after Maghrib during Ramadan. */
    val ramadanIshaExtension: Boolean = true,
    /** Shifts the Hijri date to match local moon sighting. */
    val hijriOffsetDays: Int = 0
)
