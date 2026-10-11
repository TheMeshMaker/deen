package io.github.themeshmaker.dunya.core.audio

import io.github.themeshmaker.dunya.core.model.Reciter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordTimingsTest {

    private val alafasy by lazy {
        File("../content/src/main/assets/audio/timings/${Reciter.ALAFASY.id}.tsv").useLines { parseTimings(it) }
    }

    @Test
    fun everyReciterHasTimingsForEveryVerse() {
        Reciter.entries.forEach { reciter ->
            val table = File("../content/src/main/assets/audio/timings/${reciter.id}.tsv").useLines { parseTimings(it) }
            assertEquals(reciter.id, 6236, table.size)
        }
    }

    @Test
    fun ayatAlKursiHasFiftyWordsInOrder() {
        val timings = alafasy.getValue(2 * 1000 + 255)
        assertEquals(50, timings.last().endWord)
        timings.zipWithNext().forEach { (a, b) -> assertTrue(a.startMs <= b.startMs) }
    }

    @Test
    fun wordAtFollowsTheRecitationAndHoldsBetweenWords() {
        val timings = alafasy.getValue(1 * 1000 + 1)
        assertNull(timings.wordAt(0))
        assertEquals(0, timings.wordAt(timings[0].startMs.toLong()))
        assertEquals(1, timings.wordAt(timings[1].startMs + 10L))
        // In the gap after a word ends, it stays lit until the next one starts.
        assertEquals(1, timings.wordAt(timings[2].startMs - 1L))
        assertEquals(timings.last().firstWord, timings.wordAt(10_000_000))
    }
}
