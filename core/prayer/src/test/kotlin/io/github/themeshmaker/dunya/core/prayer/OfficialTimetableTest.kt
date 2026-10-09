package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.HighLatitudeRule
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compares our calculator with published timetables (Umm al-Qura, Moonsighting Committee,
 * Diyanet, MUIS, …), collected by the adhan project (MIT) in src/test/resources/times.
 * Each file allows the variance its source needs (official tables are rounded differently).
 */
class OfficialTimetableTest {

    private val calculator = PrayerTimeCalculator(HijriCalendar())
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    @Test
    fun allTimetablesMatch() {
        val files = File(javaClass.getResource("/times")!!.toURI()).listFiles()!!.filter { it.extension == "json" }
        assertTrue("no timetable fixtures found", files.size >= 7)

        val failures = files.flatMap { check(it) }
        assertTrue("Mismatches:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun check(file: File): List<String> {
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        val params = json.getValue("params").jsonObject
        val zone = ZoneId.of(params.string("timezone"))
        val variance = json["variance"]?.jsonPrimitive?.int ?: 0
        val coordinates = Coordinates(params.double("latitude"), params.double("longitude"))
        val settings = PrayerSettings(
            method = methodFor(params.string("method")),
            asrMethod = if (params.string("madhab") == "Hanafi") AsrMethod.HANAFI else AsrMethod.STANDARD,
            highLatitudeRule = when (params.string("highLatitudeRule")) {
                "SeventhOfTheNight" -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
                "TwilightAngle" -> HighLatitudeRule.TWILIGHT_ANGLE
                else -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            }
        )

        return json.getValue("times").jsonArray.flatMap { entry ->
            val day = entry.jsonObject
            val date = LocalDate.parse(day.string("date"))
            val computed = calculator.prayerDay(date, coordinates, settings)
            Prayer.entries.mapNotNull { prayer ->
                val expected = LocalTime.parse(day.string(prayer.name.lowercase()).uppercase(), timeFormat)
                val actual = computed[prayer].atZone(zone).toLocalTime()
                val diff = abs(Duration.between(expected, actual).toMinutes())
                if (diff > variance) "${file.name} $date $prayer expected $expected got $actual" else null
            }
        }
    }

    private fun methodFor(name: String) = when (name) {
        "UmmAlQura" -> CalculationMethod.UMM_AL_QURA
        "MoonsightingCommittee" -> CalculationMethod.MOONSIGHTING_COMMITTEE
        "Dubai" -> CalculationMethod.DUBAI
        "Kuwait" -> CalculationMethod.KUWAIT
        "Qatar" -> CalculationMethod.QATAR
        "Singapore" -> CalculationMethod.SINGAPORE
        "Turkey" -> CalculationMethod.TURKEY
        "MuslimWorldLeague" -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        "Egyptian" -> CalculationMethod.EGYPTIAN
        "Karachi" -> CalculationMethod.KARACHI
        "NorthAmerica" -> CalculationMethod.NORTH_AMERICA
        else -> error("Unknown method in fixture: $name")
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun JsonObject.double(key: String) = string(key).toDouble()
}
