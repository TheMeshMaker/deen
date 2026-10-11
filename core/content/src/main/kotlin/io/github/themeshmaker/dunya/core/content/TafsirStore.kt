package io.github.themeshmaker.dunya.core.content

import android.content.Context
import android.util.Log
import io.github.themeshmaker.dunya.core.model.TafsirEntry
import io.github.themeshmaker.dunya.core.model.TafsirSource
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Tafsir texts. Al-Jalalayn and Pickthall's English are bundled; the longer tafsirs come from
 * spa5k/tafsir_api (MIT, via the jsDelivr CDN) the first time a surah is opened, and are then kept
 * in the app's files so they work offline.
 */
internal class TafsirStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Downloaded tafsir of one surah, from the phone if it was fetched before. Null if offline. */
    suspend fun remote(source: TafsirSource, surah: Int): Map<Int, String>? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "tafsir/${source.id}/$surah.json")
        val body = if (file.exists()) {
            file.readText()
        } else {
            try {
                download("https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/${source.id}/$surah.json").also {
                    file.parentFile?.mkdirs()
                    file.writeText(it)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Couldn't fetch ${source.id} $surah", e)
                return@withContext null
            }
        }
        try {
            json.decodeFromString<List<RemoteAyah>>(body).associate { it.ayah to cleanHtml(it.text) }
        } catch (e: SerializationException) {
            file.delete()
            null
        } catch (e: IllegalArgumentException) {
            file.delete()
            null
        }
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "Deen")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    @Serializable
    private data class RemoteAyah(val ayah: Int, val text: String = "")

    private companion object {
        const val TAG = "TafsirStore"
        const val TIMEOUT_MS = 15_000
    }
}

/** A bundled verse table (`surah<TAB>ayah<TAB>text`, newlines written as `\n`), grouped by surah. */
internal fun parseVerseTsv(lines: Sequence<String>): Map<Int, Map<Int, String>> {
    val bySurah = HashMap<Int, HashMap<Int, String>>()
    for (line in lines) {
        val first = line.indexOf('\t')
        val second = line.indexOf('\t', first + 1)
        if (first < 0 || second < 0) continue
        val surah = line.substring(0, first).toIntOrNull() ?: continue
        val ayah = line.substring(first + 1, second).toIntOrNull() ?: continue
        bySurah.getOrPut(surah) { HashMap() }[ayah] = line.substring(second + 1).replace("\\n", "\n")
    }
    return bySurah
}

private val tags = Regex("<[^>]+>")
private val blankLines = Regex("\n{3,}")

internal fun cleanHtml(text: String): String = tags.replace(text.replace("<br>", "\n").replace("</p>", "\n\n"), "")
    .replace("&nbsp;", " ")
    .replace(blankLines, "\n\n")
    .trim()

/**
 * Verse-by-verse tafsir for a surah of [ayahCount] verses. A verse missing from [texts] was
 * explained together with a neighbour, which it then points to: the passage before it, or for an
 * opening verse (e.g. 42:1, حم) the first passage after it.
 */
internal fun tafsirEntries(texts: Map<Int, String>, ayahCount: Int): List<TafsirEntry> {
    val explained = (1..ayahCount).filter { !texts[it].isNullOrBlank() }
    return (1..ayahCount).map { ayah ->
        if (ayah in explained) {
            TafsirEntry(ayah, texts.getValue(ayah))
        } else {
            val covering = explained.lastOrNull { it < ayah } ?: explained.firstOrNull { it > ayah }
            TafsirEntry(ayah, covering?.let { texts.getValue(it) }.orEmpty(), coveredBy = covering)
        }
    }
}
