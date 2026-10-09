package io.github.themeshmaker.dunya.core.prayer

import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.Coordinates as AdhanCoordinates
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.Madhab as AdhanMadhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.HighLatitudeRule
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerDay
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.toJavaInstant

/** Offline prayer times via adhan2 (docs/BLUEPRINT.md §7). Pure function of its inputs. */
class PrayerTimeCalculator @Inject constructor(private val hijriCalendar: HijriCalendar) {

    fun prayerDay(date: LocalDate, coordinates: Coordinates, settings: PrayerSettings): PrayerDay {
        val adhanCoordinates = AdhanCoordinates(coordinates.latitude, coordinates.longitude)
        val parameters = settings.method.toAdhan().parameters.copy(
            madhab = settings.asrMethod.toAdhan(),
            highLatitudeRule = settings.highLatitudeRule.toAdhan(coordinates),
            prayerAdjustments = adjustmentsFor(date, settings)
        )
        val times = PrayerTimes(
            adhanCoordinates,
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters
        )
        return PrayerDay(
            date = date,
            times = mapOf(
                Prayer.FAJR to times.fajr.toJavaInstant(),
                Prayer.SUNRISE to times.sunrise.toJavaInstant(),
                Prayer.DHUHR to times.dhuhr.toJavaInstant(),
                Prayer.ASR to times.asr.toJavaInstant(),
                Prayer.MAGHRIB to times.maghrib.toJavaInstant(),
                Prayer.ISHA to times.isha.toJavaInstant()
            )
        )
    }

    private fun adjustmentsFor(date: LocalDate, settings: PrayerSettings): PrayerAdjustments {
        val offsets = settings.offsetMinutes
        val ramadanExtra = if (
            settings.method == CalculationMethod.UMM_AL_QURA &&
            settings.ramadanIshaExtension &&
            hijriCalendar.toHijri(date, settings.hijriOffsetDays).isRamadan
        ) {
            RAMADAN_ISHA_EXTENSION_MINUTES
        } else {
            0
        }
        return PrayerAdjustments(
            fajr = offsets[Prayer.FAJR] ?: 0,
            sunrise = offsets[Prayer.SUNRISE] ?: 0,
            dhuhr = offsets[Prayer.DHUHR] ?: 0,
            asr = offsets[Prayer.ASR] ?: 0,
            maghrib = offsets[Prayer.MAGHRIB] ?: 0,
            isha = (offsets[Prayer.ISHA] ?: 0) + ramadanExtra
        )
    }

    private companion object {
        const val RAMADAN_ISHA_EXTENSION_MINUTES = 30
        const val HIGH_LATITUDE_THRESHOLD = 48.0
    }

    private fun CalculationMethod.toAdhan(): AdhanMethod = when (this) {
        CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA
        CalculationMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE
        CalculationMethod.NORTH_AMERICA -> AdhanMethod.NORTH_AMERICA
        CalculationMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN
        CalculationMethod.KARACHI -> AdhanMethod.KARACHI
        CalculationMethod.DUBAI -> AdhanMethod.DUBAI
        CalculationMethod.KUWAIT -> AdhanMethod.KUWAIT
        CalculationMethod.QATAR -> AdhanMethod.QATAR
        CalculationMethod.SINGAPORE -> AdhanMethod.SINGAPORE
        CalculationMethod.MOONSIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE
        CalculationMethod.TURKEY -> AdhanMethod.TURKEY
    }

    private fun AsrMethod.toAdhan(): AdhanMadhab = when (this) {
        AsrMethod.STANDARD -> AdhanMadhab.SHAFI
        AsrMethod.HANAFI -> AdhanMadhab.HANAFI
    }

    /** adhan2's own "recommended" rule only looks at northern latitudes, so AUTO is resolved here. */
    private fun HighLatitudeRule.toAdhan(coordinates: Coordinates): AdhanHighLatitudeRule = when (this) {
        HighLatitudeRule.AUTO ->
            if (abs(coordinates.latitude) > HIGH_LATITUDE_THRESHOLD) {
                AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
            } else {
                AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
            }

        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT

        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT

        HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
    }
}
