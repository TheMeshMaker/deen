package io.github.themeshmaker.dunya.core.datastore

import io.github.themeshmaker.dunya.core.model.AlertMode
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.HighLatitudeRule
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.MasjidReminder
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import java.time.DateTimeException
import java.time.ZoneId

// Model <-> on-disk mappings. Public because :core:alarm keeps a direct-boot copy of the same
// preferences (see AlarmState). Unknown enum names fall back instead of crashing.

inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = enumValues<T>().firstOrNull { it.name == name }

fun LocationPreferences.toModel() = SavedLocation(
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

fun SavedLocation.toPreferences() = LocationPreferences(
    name = name,
    arabicName = arabicName,
    countryCode = countryCode,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    zoneId = zoneId.id,
    source = source.name
)

fun PrayerPreferences.toModel() = PrayerSettings(
    method = enumOrNull<CalculationMethod>(method) ?: CalculationMethod.MUSLIM_WORLD_LEAGUE,
    asrMethod = enumOrNull<AsrMethod>(asrMethod) ?: AsrMethod.STANDARD,
    highLatitudeRule = enumOrNull<HighLatitudeRule>(highLatitudeRule) ?: HighLatitudeRule.AUTO,
    offsetMinutes = offsetMinutes.mapNotNull { (key, minutes) -> enumOrNull<Prayer>(key)?.let { it to minutes } }.toMap(),
    ramadanIshaExtension = ramadanIshaExtension,
    hijriOffsetDays = hijriOffsetDays
)

fun PrayerSettings.toPreferences() = PrayerPreferences(
    method = method.name,
    asrMethod = asrMethod.name,
    highLatitudeRule = highLatitudeRule.name,
    offsetMinutes = offsetMinutes.mapKeys { it.key.name },
    ramadanIshaExtension = ramadanIshaExtension,
    hijriOffsetDays = hijriOffsetDays
)

fun AlertPreferences.toModel() = AlertSettings(
    modes = AlertSettings.DEFAULT_MODES + modes.mapNotNull { (prayer, mode) ->
        val p = enumOrNull<Prayer>(prayer)
        val m = enumOrNull<AlertMode>(mode)
        if (p != null && m != null) p to m else null
    },
    reminderMinutes = reminderMinutes.coerceIn(0, 60),
    afterAdhanMinutes = afterAdhan.mapNotNull { (prayer, minutes) ->
        enumOrNull<Prayer>(prayer)?.takeIf { it.isPrayer && minutes > 0 }?.let { it to minutes.coerceIn(1, 60) }
    }.toMap(),
    respectSilentMode = respectSilentMode,
    useAlarmClock = useAlarmClock,
    voice = voice,
    fajrVoice = fajrVoice,
    masjid = masjid?.toModel() ?: MasjidReminder(),
    kahfReminder = kahf
)

fun AlertSettings.toPreferences() = AlertPreferences(
    modes = modes.entries.associate { (prayer, mode) -> prayer.name to mode.name },
    reminderMinutes = reminderMinutes,
    afterAdhan = afterAdhanMinutes.filterValues { it > 0 }.mapKeys { it.key.name },
    respectSilentMode = respectSilentMode,
    useAlarmClock = useAlarmClock,
    voice = voice,
    fajrVoice = fajrVoice,
    masjid = masjid.toPreferences(),
    kahf = kahfReminder
)

fun MasjidPreferences.toModel() = MasjidReminder(
    enabled = enabled,
    minutesBefore = minutesBefore.coerceIn(MasjidReminder.MINUTES_RANGE),
    prayers = prayers.mapNotNull { enumOrNull<Prayer>(it) }.filter { it.isPrayer }.toSet().ifEmpty { Prayer.prayers.toSet() }
)

fun MasjidReminder.toPreferences() = MasjidPreferences(
    enabled = enabled,
    minutesBefore = minutesBefore,
    prayers = Prayer.entries.filter { it in prayers }.map { it.name }
)
