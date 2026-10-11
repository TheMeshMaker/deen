package io.github.themeshmaker.dunya.core.content

import android.content.Context
import android.content.res.AssetManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.domain.ContentRepository
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.HadithCollection
import io.github.themeshmaker.dunya.core.model.HadithEntry
import io.github.themeshmaker.dunya.core.model.JuzStart
import io.github.themeshmaker.dunya.core.model.ProphetFact
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.model.TafsirEntry
import io.github.themeshmaker.dunya.core.model.TafsirSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads the bundled texts from assets on first use and keeps the small ones in memory. Hadith
 * collections are one file per book, so only the book being read is loaded.
 */
@Singleton
internal class AssetContentRepository @Inject constructor(@ApplicationContext context: Context) : ContentRepository {
    private val assets: AssetManager = context.assets
    private val mutex = Mutex()

    private var quran: Map<Int, List<Ayah>>? = null
    private var quranMeta: QuranMeta? = null
    private var adhkar: List<AdhkarCategory>? = null
    private var nawawi: List<Hadith>? = null
    private var facts: List<ProphetFact>? = null
    private var hadithIndex: HadithIndex? = null

    private val tafsirStore = TafsirStore(context)
    private var jalalayn: Map<Int, Map<Int, String>>? = null
    private var pickthall: Map<Int, Map<Int, String>>? = null

    /** The last book opened, so paging back and forth in a reader doesn't re-read it. */
    private var lastBook: Pair<String, List<HadithEntry>>? = null

    override suspend fun adhkar(): List<AdhkarCategory> = cached({ adhkar }, { adhkar = it }) { parseAdhkar(read("adhkar.json")) }

    override suspend fun surahs(): List<Surah> = meta().surahs

    override suspend fun juzStarts(): List<JuzStart> = meta().juz

    override suspend fun ayahs(surah: Int): List<Ayah> = quranText()[surah].orEmpty()

    override suspend fun nawawi(): List<Hadith> = cached({ nawawi }, { nawawi = it }) { parseNawawi(read("hadith/nawawi40.json")) }

    override suspend fun prophetFacts(): List<ProphetFact> = cached({ facts }, { facts = it }) { parseFacts(read("prophet-facts.json")) }

    override suspend fun hadithCollections(): List<HadithCollection> = index().collections

    override suspend fun hadithBook(collectionId: String, book: Int): List<HadithEntry> {
        val key = "$collectionId/$book"
        mutex.withLock { lastBook?.takeIf { it.first == key }?.let { return it.second } }
        val index = index()
        val entries = withContext(Dispatchers.IO) {
            lines("hadith/$collectionId/$book.tsv").mapNotNull { parseHadithLine(it, collectionId, book, index) }.toList()
        }
        mutex.withLock { lastBook = key to entries }
        return entries
    }

    override suspend fun searchHadith(collectionId: String, query: String, limit: Int): List<HadithEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val index = index()
        val collection = index.collections.firstOrNull { it.id == collectionId } ?: return emptyList()
        val needle = ArabicText.normalize(trimmed)
        val byNumber = trimmed.all { it.isDigit() }
        return withContext(Dispatchers.Default) {
            val results = ArrayList<HadithEntry>()
            for (book in collection.books) {
                ensureActive()
                for (line in lines("hadith/$collectionId/${book.number}.tsv")) {
                    val entry = parseHadithLine(line, collectionId, book.number, index) ?: continue
                    val hit = if (byNumber) entry.number == trimmed else ArabicText.normalize(entry.text).contains(needle)
                    if (hit) {
                        results += entry
                        if (results.size >= limit) return@withContext results
                    }
                }
            }
            results
        }
    }

    override suspend fun tafsir(source: TafsirSource, surah: Int): List<TafsirEntry>? {
        val ayahCount = meta().surahs.getOrNull(surah - 1)?.ayahCount ?: return null
        val texts = if (source.bundled) {
            cached({
                jalalayn
            }, { jalalayn = it }) { assets.open("tafsir/jalalayn.tsv").bufferedReader().useLines { parseVerseTsv(it) } }[surah]
        } else {
            tafsirStore.remote(source, surah)
        } ?: return null
        return tafsirEntries(texts, ayahCount)
    }

    override suspend fun meaning(surah: Int): List<String> {
        val ayahCount = meta().surahs.getOrNull(surah - 1)?.ayahCount ?: return emptyList()
        val texts = cached({
            pickthall
        }, { pickthall = it }) { assets.open("quran/pickthall.tsv").bufferedReader().useLines { parseVerseTsv(it) } }[surah]
        return (1..ayahCount).map { texts?.get(it).orEmpty() }
    }

    private suspend fun meta(): QuranMeta = cached({ quranMeta }, { quranMeta = it }) { parseQuranMeta(read("quran/quran-meta.json")) }

    private suspend fun quranText(): Map<Int, List<Ayah>> =
        cached({ quran }, { quran = it }) { assets.open("quran/quran-uthmani.txt").bufferedReader().useLines { parseQuranText(it) } }

    private suspend fun index(): HadithIndex = cached({
        hadithIndex
    }, { hadithIndex = it }) { parseHadithIndex(read("hadith/collections.json")) }

    private suspend fun <T> cached(get: () -> T?, set: (T) -> Unit, load: () -> T): T = mutex.withLock {
        get() ?: withContext(Dispatchers.IO) { load() }.also(set)
    }

    private fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    /** Lines of a text asset; the stream is read fully, so callers never leak it. */
    private fun lines(path: String): Sequence<String> = assets.open(path).bufferedReader().use { it.readLines() }.asSequence()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ContentModule {
    @Binds
    abstract fun bindContentRepository(impl: AssetContentRepository): ContentRepository
}
