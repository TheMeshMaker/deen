package io.github.themeshmaker.dunya.core.data

import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.Coordinates
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineLocationRepositoryTest {

    private val riyadh = City(
        id = 108410,
        name = "Riyadh",
        arabicName = "الرياض",
        region = "Riyadh Region",
        countryCode = "SA",
        coordinates = Coordinates(24.6877, 46.7219),
        zoneId = ZoneId.of("Asia/Riyadh"),
        population = 4_205_961
    )
    private val here = Coordinates(24.70, 46.80)

    @Test
    fun nearbyCityGivesNameCountryAndZone() {
        val saved = OfflineLocationRepository.savedLocationFor(here, riyadh, 8.0)
        assertEquals("Riyadh", saved.name)
        assertEquals("الرياض", saved.arabicName)
        assertEquals("SA", saved.countryCode)
        assertEquals(ZoneId.of("Asia/Riyadh"), saved.zoneId)
        assertEquals(here, saved.coordinates) // prayer times use the real position, not the city centre
    }

    @Test
    fun remoteSpotKeepsCountryButShowsCoordinates() {
        val saved = OfflineLocationRepository.savedLocationFor(here, riyadh, 200.0)
        assertEquals("24.700, 46.800", saved.name)
        assertNull(saved.arabicName)
        assertEquals("SA", saved.countryCode)
    }

    @Test
    fun farFromAnyCityFallsBackToDeviceZone() {
        val saved = OfflineLocationRepository.savedLocationFor(here, riyadh, 2_000.0)
        assertNull(saved.countryCode)
        assertEquals(ZoneId.systemDefault(), saved.zoneId)
    }
}
