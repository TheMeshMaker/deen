package io.github.themeshmaker.dunya.core.content

import io.github.themeshmaker.dunya.core.model.GradeKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses the real bundled assets, so a broken content build fails here and not on a phone. */
class ContentAssetsTest {

    private fun asset(path: String) = File("src/main/assets/$path")

    private val quran by lazy { asset("quran/quran-uthmani.txt").useLines { parseQuranText(it) } }
    private val meta by lazy { parseQuranMeta(asset("quran/quran-meta.json").readText()) }

    @Test
    fun quranHasEverySurahAndVerse() {
        assertEquals(114, quran.size)
        assertEquals(6236, quran.values.sumOf { it.size })
        meta.surahs.forEach { surah -> assertEquals("surah ${surah.number}", surah.ayahCount, quran.getValue(surah.number).size) }
        assertEquals(30, meta.juz.size)
    }

    @Test
    fun bismillahIsSplitOffExceptForAlFatihahAndAtTawbah() {
        assertNull(quran.getValue(1).first().bismillah)
        assertNull(quran.getValue(9).first().bismillah)
        val baqarah = quran.getValue(2).first()
        assertEquals(quran.getValue(1).first().text, baqarah.bismillah)
        assertEquals("الٓمٓ", baqarah.text)
        // At-Tin is written with an extra shadda on the first letter; it must still be recognised.
        assertNotNull(quran.getValue(95).first().bismillah)
        (2..114).filter { it != 9 }.forEach { assertNotNull("surah $it", quran.getValue(it).first().bismillah) }
    }

    @Test
    fun adhkarParseAndEveryQuranReferenceExists() {
        val categories = parseAdhkar(asset("adhkar.json").readText())
        assertTrue(categories.size >= 9)
        assertTrue(categories.first { it.id == "morning" }.items.size > 20)
        categories.flatMap { it.items }.forEach { dhikr ->
            assertTrue("dhikr ${dhikr.id} has nothing to recite", dhikr.text != null || dhikr.isQuran)
            assertTrue(dhikr.translation.isNotBlank())
            assertTrue(dhikr.reference.ar!!.isNotBlank() && dhikr.reference.en!!.isNotBlank())
            dhikr.quran.forEach { range ->
                val verses = quran.getValue(range.surah)
                assertTrue("${range.surah}:${range.to}", range.from >= 1 && range.to <= verses.size && range.from <= range.to)
            }
        }
    }

    @Test
    fun nawawiAndFactsAreComplete() {
        val nawawi = parseNawawi(asset("hadith/nawawi40.json").readText())
        assertEquals((1..42).toList(), nawawi.map { it.number })
        nawawi.forEach { assertTrue(it.arabic.isNotBlank() && it.english.isNotBlank()) }

        val facts = parseFacts(asset("prophet-facts.json").readText())
        assertTrue(facts.size >= 60)
        facts.forEach { assertTrue(it.text.ar!!.contains("ﷺ") && it.text.en!!.contains("ﷺ")) }
    }

    @Test
    fun hadithCollectionsIndexMatchesTheBookFiles() {
        val index = parseHadithIndex(asset("hadith/collections.json").readText())
        assertEquals(
            listOf("bukhari", "muslim", "abudawud", "tirmidhi", "nasai", "ibnmajah", "malik", "qudsi"),
            index.collections.map {
                it.id
            }
        )
        index.collections.forEach { collection ->
            var total = 0
            collection.books.forEach { book ->
                val entries = asset("hadith/${collection.id}/${book.number}.tsv").readLines().mapNotNull {
                    parseHadithLine(it, collection.id, book.number, index)
                }
                assertEquals("${collection.id}/${book.number}", book.count, entries.size)
                total += entries.size
            }
            assertEquals(collection.id, collection.count, total)
        }
    }

    @Test
    fun gradesResolve() {
        val index = parseHadithIndex(asset("hadith/collections.json").readText())
        val first = parseHadithLine(asset("hadith/abudawud/1.tsv").readLines().first(), "abudawud", 1, index)!!
        assertEquals("1", first.number)
        assertEquals("Al-Albani", first.grade!!.grader.en)
        assertEquals(GradeKind.AUTHENTIC, first.grade!!.kind)
        val bukhari = parseHadithLine(asset("hadith/bukhari/1.tsv").readLines().first(), "bukhari", 1, index)!!
        assertNull(bukhari.grade)
        assertTrue(ArabicText.normalize(bukhari.text).contains("انما الاعمال بالنيات"))
    }

    @Test
    fun arabicSearchIgnoresDiacriticsAndSpellingVariants() {
        assertEquals("انما الاعمال بالنيات", ArabicText.normalize("إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ"))
        assertTrue(ArabicText.normalize("صَلاَةٌ").contains(ArabicText.normalize("صلاة")))
    }
}
