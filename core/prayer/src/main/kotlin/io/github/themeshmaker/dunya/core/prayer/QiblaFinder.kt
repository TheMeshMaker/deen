/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.prayer

import com.batoulapps.adhan2.Coordinates as AdhanCoordinates
import com.batoulapps.adhan2.Qibla
import io.github.themeshmaker.dunya.core.model.Coordinates
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Direction of the Kaaba (great-circle bearing from true north) and how far away it is. */
class QiblaFinder @Inject constructor() {
    /** Degrees clockwise from true north, 0..360. */
    fun bearing(from: Coordinates): Double = Qibla(AdhanCoordinates(from.latitude, from.longitude)).direction

    fun distanceKm(from: Coordinates): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(KAABA.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(KAABA.longitude - from.longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    companion object {
        val KAABA = Coordinates(21.4225241, 39.8261818)
        private const val EARTH_RADIUS_KM = 6371.0
    }
}
