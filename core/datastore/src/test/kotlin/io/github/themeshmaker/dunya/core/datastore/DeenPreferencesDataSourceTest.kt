/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import io.github.themeshmaker.dunya.core.model.AdhanVoice
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LiquidSettings
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.MasjidReminder
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.UiStyle
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeenPreferencesDataSourceTest {

    private class InMemoryDataStore(initial: UserPreferences = UserPreferences()) : DataStore<UserPreferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<UserPreferences> = state
        override suspend fun updateData(transform: suspend (t: UserPreferences) -> UserPreferences): UserPreferences =
            transform(state.value).also { state.value = it }
    }

    private val serializer = UserPreferencesSerializer()

    private suspend fun decode(json: String) = serializer.readFrom(json.byteInputStream())

    @Test
    fun defaultsBeforeOnboarding() = runTest {
        val data = DeenPreferencesDataSource(InMemoryDataStore()).userData.first()
        assertFalse(data.onboardingComplete)
        assertNull(data.location)
        assertNull(data.prayerSettings)
    }

    @Test
    fun roundTripsLocationAndSettingsThroughJson() = runTest {
        val store = InMemoryDataStore()
        val source = DeenPreferencesDataSource(store)
        val location =
            SavedLocation("Riyadh", "الرياض", "SA", Coordinates(24.6877, 46.7219), ZoneId.of("Asia/Riyadh"), LocationSource.MANUAL)
        val settings = PrayerSettings(
            method = CalculationMethod.UMM_AL_QURA,
            asrMethod = AsrMethod.HANAFI,
            offsetMinutes = mapOf(Prayer.ISHA to 5),
            hijriOffsetDays = -1
        )
        source.setLocation(location)
        source.setPrayerSettings(settings)
        source.setOnboardingComplete()

        // Through the real on-disk format and back.
        val bytes = ByteArrayOutputStream().also { serializer.writeTo(store.data.first(), it) }.toByteArray()
        val reloaded = DeenPreferencesDataSource(InMemoryDataStore(decode(bytes.decodeToString()))).userData.first()

        assertTrue(reloaded.onboardingComplete)
        assertEquals(location, reloaded.location)
        assertEquals(settings, reloaded.prayerSettings)
    }

    @Test
    fun unknownEnumNamesFallBackInsteadOfCrashing() = runTest {
        val prefs = decode(
            """{"onboardingComplete":true,"prayer":{"method":"REMOVED_METHOD","asrMethod":"HANAFI","offsetMinutes":{"NOT_A_PRAYER":3}},"futureField":1}"""
        )
        val settings = DeenPreferencesDataSource(InMemoryDataStore(prefs)).userData.first().prayerSettings!!
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, settings.method)
        assertEquals(AsrMethod.HANAFI, settings.asrMethod)
        assertTrue(settings.offsetMinutes.isEmpty())
    }

    @Test(expected = CorruptionException::class)
    fun garbageIsReportedAsCorruption() = runTest {
        decode("{not json")
    }

    @Test
    fun newSettingsRoundTripAndOldFilesGetDefaults() = runTest {
        val old = DeenPreferencesDataSource(
            InMemoryDataStore(decode("""{"onboardingComplete":true,"alerts":{"reminderMinutes":10}}"""))
        ).userData.first()
        assertEquals(UiStyle.CLASSIC, old.uiStyle)
        assertEquals(ThemeMode.SYSTEM, old.themeMode)
        assertEquals(AdhanVoice.DEFAULT, old.alertSettings.voice)
        assertFalse(old.alertSettings.masjid.enabled)
        assertEquals(10, old.alertSettings.reminderMinutes)

        val store = InMemoryDataStore()
        val source = DeenPreferencesDataSource(store)
        val alerts = AlertSettings(
            voice = AdhanVoice.TRADITIONAL,
            fajrVoice = AdhanVoice.CUSTOM,
            masjid = MasjidReminder(enabled = true, minutesBefore = 35, prayers = setOf(Prayer.FAJR, Prayer.ISHA))
        )
        source.setAlertSettings(alerts)
        source.setUiStyle(UiStyle.SLEEK)
        source.setThemeMode(ThemeMode.OLED_BLACK)
        source.setTimetableCalendar(CalendarType.HIJRI)
        source.setReadingSettings(ReadingSettings(arabicScale = 1.3f, showTranslation = false, haptics = false))
        source.setSecretsUnlocked(true)
        val bytes = ByteArrayOutputStream().also { serializer.writeTo(store.data.first(), it) }.toByteArray()
        val reloaded = DeenPreferencesDataSource(InMemoryDataStore(decode(bytes.decodeToString()))).userData.first()
        assertEquals(alerts, reloaded.alertSettings)
        assertEquals(UiStyle.SLEEK, reloaded.uiStyle)
        assertEquals(ThemeMode.OLED_BLACK, reloaded.themeMode)
        assertEquals(CalendarType.HIJRI, reloaded.timetableCalendar)
        assertEquals(ReadingSettings(1.3f, showTranslation = false, haptics = false), reloaded.reading)
        assertTrue(reloaded.secretsUnlocked)
    }

    @Test
    fun liquidSettingsRoundTripAndBadValuesFallBack() = runTest {
        val store = InMemoryDataStore()
        val source = DeenPreferencesDataSource(store)
        assertEquals(LiquidSettings(), source.userData.first().liquid)
        val tuned = LiquidSettings(transparency = 0.9f, lensWidth = 1f, edge = 0.2f, tiltLight = false)
        source.setLiquidSettings(tuned)
        val bytes = ByteArrayOutputStream().also { serializer.writeTo(store.data.first(), it) }.toByteArray()
        assertEquals(tuned, DeenPreferencesDataSource(InMemoryDataStore(decode(bytes.decodeToString()))).userData.first().liquid)

        val odd = decode("""{"liquid":{"transparency":7.0,"lensWidth":-1.0,"edge":0.3}}""")
        assertEquals(LiquidSettings(edge = 0.3f), DeenPreferencesDataSource(InMemoryDataStore(odd)).userData.first().liquid)
    }

    @Test
    fun theRemovedColorOsStyleBecomesSleek() = runTest {
        val prefs = decode("""{"uiStyle":"COLOROS"}""")
        assertEquals(UiStyle.SLEEK, DeenPreferencesDataSource(InMemoryDataStore(prefs)).userData.first().uiStyle)
    }
}
