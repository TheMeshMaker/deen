/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import io.github.themeshmaker.dunya.core.model.Coordinates

/** One-shot, coarse device location. Returns null when unavailable; never throws. */
fun interface DeviceLocationSource {
    suspend fun currentLocation(): Coordinates?
}

/** Tries [primary] (e.g. Google's fused location) and falls back to [secondary]. */
class FallbackLocationSource(private val primary: DeviceLocationSource, private val secondary: DeviceLocationSource) :
    DeviceLocationSource {
    override suspend fun currentLocation(): Coordinates? = primary.currentLocation() ?: secondary.currentLocation()
}

fun Location.toCoordinates() = Coordinates(latitude, longitude)

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
