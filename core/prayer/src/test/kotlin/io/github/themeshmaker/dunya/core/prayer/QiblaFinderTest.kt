package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaFinderTest {
    private val qibla = QiblaFinder()

    /** Reference bearings from the adhan library's own test suite. */
    @Test
    fun bearingsMatchKnownValues() {
        assertEquals(58.4817635, qibla.bearing(Coordinates(40.7128, -74.0059)), 0.01) // New York
        assertEquals(118.9872189, qibla.bearing(Coordinates(51.5074, -0.1278)), 0.01) // London
        assertEquals(277.4996044, qibla.bearing(Coordinates(-33.8688, 151.2093)), 0.01) // Sydney
    }

    @Test
    fun distanceFromRiyadhIsAbout790KmAsTheCrowFlies() {
        assertEquals(790.0, qibla.distanceKm(Coordinates(24.7136, 46.6753)), 10.0)
    }
}
