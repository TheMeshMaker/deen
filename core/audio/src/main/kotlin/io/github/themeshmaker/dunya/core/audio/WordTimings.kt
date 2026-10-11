package io.github.themeshmaker.dunya.core.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.model.Reciter
import io.github.themeshmaker.dunya.core.model.WordTiming
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Word-by-word timings for each reciter (quran-align, CC BY 4.0), bundled as
 * `assets/audio/timings/<reciter>.tsv`. Word numbers count the words of a verse as written in the
 * Tanzil text, skipping pause marks; verse 1 never includes the Bismillah (its own recording, 1:1).
 */
@Singleton
class WordTimings @Inject constructor(@ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private val loaded = HashMap<Reciter, Map<Int, List<WordTiming>>>()

    suspend fun forVerse(reciter: Reciter, surah: Int, ayah: Int): List<WordTiming> = table(reciter)[key(surah, ayah)].orEmpty()

    private suspend fun table(reciter: Reciter): Map<Int, List<WordTiming>> = mutex.withLock {
        loaded.getOrPut(reciter) {
            withContext(Dispatchers.IO) {
                context.assets.open("audio/timings/${reciter.id}.tsv").bufferedReader().useLines { parseTimings(it) }
            }
        }
    }
}

private fun key(surah: Int, ayah: Int) = surah * 1000 + ayah

/** `surah<TAB>ayah<TAB>first,end,startMs,endMs;…` per line. */
internal fun parseTimings(lines: Sequence<String>): Map<Int, List<WordTiming>> {
    val table = HashMap<Int, List<WordTiming>>(6_300)
    for (line in lines) {
        val parts = line.split('\t')
        if (parts.size < 3) continue
        val surah = parts[0].toIntOrNull() ?: continue
        val ayah = parts[1].toIntOrNull() ?: continue
        table[key(surah, ayah)] = parts[2].split(';').mapNotNull { segment ->
            val n = segment.split(',')
            if (n.size != 4) return@mapNotNull null
            WordTiming(n[0].toInt(), n[1].toInt(), n[2].toInt(), n[3].toInt())
        }
    }
    return table
}

/**
 * The word being recited at [positionMs], or null before the first word. Between two words the
 * previous one stays lit, so the highlight never flickers off mid-verse.
 */
fun List<WordTiming>.wordAt(positionMs: Long): Int? {
    var current: Int? = null
    for (timing in this) {
        if (timing.startMs > positionMs) break
        current = timing.firstWord
    }
    return current
}
