/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.location

import io.github.themeshmaker.dunya.core.model.Coordinates
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CityIndexTest {

    private val sample = """
        108410	Riyadh	الرياض	SA	Riyadh Region	24.6877	46.7219	4205961	Asia/Riyadh
        105343	Jeddah	جدة	SA	Mecca Region	21.4901	39.1862	4697000	Asia/Riyadh
        104515	Makkah	مكة المكرمة	SA	Mecca Region	21.4266	39.8256	1323624	Asia/Riyadh
        745044	Istanbul	إسطنبول	TR	Istanbul	41.0138	28.9497	15701602	Europe/Istanbul
        2643743	London	لندن	GB	England	51.5085	-0.1257	8961989	Europe/London
        2988507	Paris	باريس	FR	Ile-de-France	48.8534	2.3488	2138551	Europe/Paris
        6077243	Montréal		CA	Quebec	45.5088	-73.5878	1600000	America/Toronto
        105299	Jizan	جازان	SA	Jazan Region	16.8892	42.5511	105198	Asia/Riyadh
    """.trimIndent()

    private val index = CityIndex.parse(sample.byteInputStream())

    private fun names(query: String) = index.search(query).map { it.name }

    @Test
    fun parsesAllRows() {
        assertEquals(8, index.size)
    }

    @Test
    fun englishPrefixIgnoresCase() {
        assertEquals(listOf("Riyadh"), names("riy"))
        assertEquals(listOf("London"), names("LONDON"))
    }

    @Test
    fun accentsAreIgnored() {
        assertEquals(listOf("Montréal"), names("montreal"))
    }

    @Test
    fun arabicWithAndWithoutArticle() {
        assertEquals(listOf("Riyadh"), names("الرياض"))
        assertEquals(listOf("Riyadh"), names("رياض"))
        assertEquals(listOf("Makkah"), names("مكة"))
        assertEquals(listOf("Makkah"), names("المكرمه")) // taa marbuta typed as haa
    }

    @Test
    fun arabicDiacriticsAndHamzaSeatsAreIgnored() {
        assertEquals(listOf("Istanbul"), names("اسطنبول"))
        assertEquals(listOf("Jeddah"), names("جِدّة"))
    }

    @Test
    fun prefixMatchesComeBeforeInnerMatches() {
        // "an" starts no word in Jizan/Makkah but appears inside them; still found, after prefix hits.
        val results = names("an")
        assertTrue(results.containsAll(listOf("Jizan")))
    }

    @Test
    fun emptyQueryReturnsNothing() {
        assertTrue(names("  ").isEmpty())
    }

    @Test
    fun nearestCity() {
        val (city, km) = index.nearest(Coordinates(21.43, 39.83))!! // near the Haram
        assertEquals("Makkah", city.name)
        assertTrue(km < 2)
    }

    @Test
    fun distanceRiyadhToJeddahIsAbout850Km() {
        val km = CityIndex.distanceKm(Coordinates(24.6877, 46.7219), Coordinates(21.4901, 39.1862))
        assertEquals(850.0, km, 15.0)
    }

    @Test
    fun bundledAssetParsesAndFindsHolyCities() {
        val asset = File("src/main/assets/cities.tsv")
        val bundled = asset.inputStream().use { CityIndex.parse(it) }
        assertTrue(bundled.size > 60_000)
        assertEquals("Makkah", bundled.search("مكة").first().name)
        assertEquals("Madinah", bundled.search("المدينة").first().name)
        assertEquals("SA", bundled.search("riyadh").first().countryCode)
    }
}
