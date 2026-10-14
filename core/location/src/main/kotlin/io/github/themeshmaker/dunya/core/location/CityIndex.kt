/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.location

import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.Coordinates
import java.io.InputStream
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * In-memory index over the bundled GeoNames cities (see tools/geonames/build_cities.py for the
 * file format). Rows are sorted by population, so results come out biggest-city-first.
 */
class CityIndex private constructor(private val cities: List<City>, private val keys: Array<String>) {

    val size: Int get() = cities.size

    /** Word-prefix matches first ("riy" → Riyadh), then matches anywhere in the name. */
    fun search(query: String, limit: Int = 30): List<City> {
        val needle = TextNormalizer.normalize(query)
        if (needle.isEmpty()) return emptyList()
        val prefixMatches = ArrayList<City>()
        val otherMatches = ArrayList<City>()
        for (i in cities.indices) {
            val key = keys[i]
            val at = key.indexOf(needle)
            if (at < 0) continue
            if (at == 0 || key[at - 1] == ' ') {
                prefixMatches += cities[i]
                if (prefixMatches.size == limit) break
            } else if (otherMatches.size < limit) {
                otherMatches += cities[i]
            }
        }
        return (prefixMatches + otherMatches).take(limit)
    }

    /** Closest city and its distance in km, or null if the index is empty. */
    fun nearest(coordinates: Coordinates): Pair<City, Double>? {
        var best: City? = null
        var bestDistance = Double.MAX_VALUE
        for (city in cities) {
            val d = distanceKm(coordinates, city.coordinates)
            if (d < bestDistance) {
                bestDistance = d
                best = city
            }
        }
        return best?.let { it to bestDistance }
    }

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0

        /** Parses the (already un-gzipped) TSV. */
        fun parse(input: InputStream): CityIndex {
            val zones = HashMap<String, ZoneId>()
            val strings = HashMap<String, String>() // de-duplicates regions and country codes
            val cities = ArrayList<City>(70_000)
            val keys = ArrayList<String>(70_000)
            input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                val c = line.split('\t')
                if (c.size < 9) return@forEachLine
                val zone = zones.getOrPut(c[8]) { runCatching { ZoneId.of(c[8]) }.getOrNull() ?: return@forEachLine }
                val arabic = c[2].ifEmpty { null }
                cities += City(
                    id = c[0].toInt(),
                    name = c[1],
                    arabicName = arabic,
                    countryCode = strings.getOrPut(c[3]) { c[3] },
                    region = strings.getOrPut(c[4]) { c[4] },
                    coordinates = Coordinates(c[5].toDouble(), c[6].toDouble()),
                    population = c[7].toInt(),
                    zoneId = zone
                )
                keys += TextNormalizer.searchKey(c[1], arabic)
            }
            return CityIndex(cities, keys.toTypedArray())
        }

        /** Equirectangular approximation: accurate to well under 1% at city distances. */
        fun distanceKm(a: Coordinates, b: Coordinates): Double {
            val meanLat = Math.toRadians((a.latitude + b.latitude) / 2)
            var dLon = b.longitude - a.longitude
            if (dLon > 180) dLon -= 360
            if (dLon < -180) dLon += 360
            val x = Math.toRadians(dLon) * cos(meanLat)
            val y = Math.toRadians(b.latitude - a.latitude)
            return EARTH_RADIUS_KM * sqrt(x * x + y * y)
        }
    }
}
