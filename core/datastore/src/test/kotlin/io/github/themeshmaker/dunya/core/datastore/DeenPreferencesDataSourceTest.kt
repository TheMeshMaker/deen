package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
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
}
