package io.github.themeshmaker.dunya.core.content

import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.Dhikr
import io.github.themeshmaker.dunya.core.model.GradeKind
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.HadithBook
import io.github.themeshmaker.dunya.core.model.HadithCollection
import io.github.themeshmaker.dunya.core.model.HadithEntry
import io.github.themeshmaker.dunya.core.model.HadithGrade
import io.github.themeshmaker.dunya.core.model.JuzStart
import io.github.themeshmaker.dunya.core.model.LocalizedText
import io.github.themeshmaker.dunya.core.model.ProphetFact
import io.github.themeshmaker.dunya.core.model.QuranRange
import io.github.themeshmaker.dunya.core.model.Revelation
import io.github.themeshmaker.dunya.core.model.Surah
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Parsers for the bundled assets (tools/content builds them). Pure Kotlin so unit tests can run
// them against the real files.

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class TextDto(val ar: String? = null, val en: String? = null) {
    fun toModel() = LocalizedText(ar, en)
}

// ---- Quran -------------------------------------------------------------------------------------

/** Tanzil text, `surah|ayah|text` per line; `#` lines are the licence block. */
internal fun parseQuranText(lines: Sequence<String>): Map<Int, List<Ayah>> {
    val bySurah = LinkedHashMap<Int, MutableList<Ayah>>()
    for (line in lines) {
        if (line.isBlank() || line.startsWith("#")) continue
        val first = line.indexOf('|')
        val second = line.indexOf('|', first + 1)
        if (first < 0 || second < 0) continue
        val surah = line.substring(0, first).toInt()
        val number = line.substring(first + 1, second).toInt()
        val text = line.substring(second + 1)
        bySurah.getOrPut(surah) { ArrayList() } += splitBismillah(surah, number, text)
    }
    return bySurah
}

/**
 * Tanzil prefixes the Bismillah to verse 1 of every surah except al-Fatihah (where it *is*
 * verse 1) and at-Tawbah (which has none). The reader shows it as a header, so it is split off
 * here: the first four words, exactly as written (95:1 and 97:1 carry an extra shadda).
 */
internal fun splitBismillah(surah: Int, number: Int, text: String): Ayah {
    if (number != 1 || surah == 1 || surah == 9) return Ayah(surah, number, text)
    var end = 0
    repeat(BISMILLAH_WORDS) {
        end = text.indexOf(' ', end + 1)
        if (end < 0) return Ayah(surah, number, text)
    }
    return Ayah(surah, number, text.substring(end + 1), bismillah = text.substring(0, end))
}

private const val BISMILLAH_WORDS = 4

@Serializable
private data class QuranMetaDto(val surahs: List<SurahDto>, val juz: List<JuzDto>)

@Serializable
private data class SurahDto(
    val number: Int,
    val ayahs: Int,
    val name: String,
    val transliteration: String,
    val translation: String,
    val revelation: String
)

@Serializable
private data class JuzDto(val juz: Int, val surah: Int, val ayah: Int)

internal class QuranMeta(val surahs: List<Surah>, val juz: List<JuzStart>)

internal fun parseQuranMeta(text: String): QuranMeta {
    val dto = json.decodeFromString(QuranMetaDto.serializer(), text)
    return QuranMeta(
        surahs = dto.surahs.map {
            Surah(
                it.number,
                it.ayahs,
                it.name,
                it.transliteration,
                it.translation,
                if (it.revelation ==
                    "MEDINAN"
                ) {
                    Revelation.MEDINAN
                } else {
                    Revelation.MECCAN
                }
            )
        },
        juz = dto.juz.map { JuzStart(it.juz, it.surah, it.ayah) }
    )
}

// ---- Adhkar ------------------------------------------------------------------------------------

@Serializable
private data class AdhkarDto(val categories: List<CategoryDto>)

@Serializable
private data class CategoryDto(val id: String, val title: TextDto, val items: List<DhikrDto>)

@Serializable
private data class DhikrDto(
    val id: Int,
    val preface: String? = null,
    val text: String? = null,
    val quran: List<List<Int>> = emptyList(),
    val count: Int = 1,
    val instruction: TextDto? = null,
    val translation: String,
    val virtue: TextDto? = null,
    val reference: TextDto
)

internal fun parseAdhkar(text: String): List<AdhkarCategory> =
    json.decodeFromString(AdhkarDto.serializer(), text).categories.map { category ->
        AdhkarCategory(
            id = category.id,
            title = category.title.toModel(),
            items = category.items.map { item ->
                Dhikr(
                    id = item.id,
                    preface = item.preface,
                    text = item.text,
                    quran = item.quran.map { (surah, from, to) -> QuranRange(surah, from, to) },
                    count = item.count.coerceAtLeast(1),
                    instruction = item.instruction?.toModel(),
                    translation = item.translation,
                    virtue = item.virtue?.toModel(),
                    reference = item.reference.toModel()
                )
            }
        )
    }

// ---- Hadith ------------------------------------------------------------------------------------

@Serializable
private data class NawawiDto(val hadiths: List<NawawiHadithDto>)

@Serializable
private data class NawawiHadithDto(val number: Int, val title: TextDto, val arabic: String, val english: String, val source: TextDto)

internal fun parseNawawi(text: String): List<Hadith> = json.decodeFromString(NawawiDto.serializer(), text).hadiths.map {
    Hadith(it.number, it.title.toModel(), it.arabic, it.english, it.source.toModel())
}

@Serializable
private data class FactsDto(val facts: List<FactDto>)

@Serializable
private data class FactDto(val id: Int, val text: TextDto, val source: TextDto)

internal fun parseFacts(text: String): List<ProphetFact> = json.decodeFromString(FactsDto.serializer(), text).facts.map {
    ProphetFact(it.id, it.text.toModel(), it.source.toModel())
}

@Serializable
private data class CollectionsDto(val graders: List<TextDto>, val grades: List<GradeDto>, val collections: List<CollectionDto>)

@Serializable
private data class GradeDto(val en: String, val ar: String, val `class`: String)

@Serializable
private data class CollectionDto(
    val id: String,
    val name: TextDto,
    val author: TextDto? = null,
    val allAuthentic: Boolean = false,
    val count: Int,
    val books: List<BookDto>
)

@Serializable
private data class BookDto(val book: Int, val title: TextDto, val count: Int, val first: String, val last: String)

/** The collection index plus the grade tables the per-book files point into. */
internal class HadithIndex(
    val collections: List<HadithCollection>,
    private val graders: List<LocalizedText>,
    private val grades: List<HadithGradeLabel>
) {
    internal class HadithGradeLabel(val label: LocalizedText, val kind: GradeKind)

    /** "grader:label" index pair from a TSV line, or null. */
    fun grade(code: String): HadithGrade? {
        if (code.isEmpty()) return null
        val colon = code.indexOf(':')
        if (colon < 0) return null
        val grader = graders.getOrNull(code.substring(0, colon).toIntOrNull() ?: return null) ?: return null
        val label = grades.getOrNull(code.substring(colon + 1).toIntOrNull() ?: return null) ?: return null
        return HadithGrade(grader, label.label, label.kind)
    }
}

internal fun parseHadithIndex(text: String): HadithIndex {
    val dto = json.decodeFromString(CollectionsDto.serializer(), text)
    return HadithIndex(
        collections = dto.collections.map { c ->
            HadithCollection(
                id = c.id,
                name = c.name.toModel(),
                author = c.author?.toModel(),
                allAuthentic = c.allAuthentic,
                count = c.count,
                books = c.books.map { HadithBook(it.book, it.title.toModel(), it.count, it.first, it.last) }
            )
        },
        graders = dto.graders.map { it.toModel() },
        grades = dto.grades.map { g ->
            HadithIndex.HadithGradeLabel(
                LocalizedText(g.ar, g.en),
                GradeKind.entries.firstOrNull { it.name == g.`class` } ?: GradeKind.OTHER
            )
        }
    )
}

/** One line of `hadith/<collection>/<book>.tsv`: number, number in book, grade code, text. */
internal fun parseHadithLine(line: String, collectionId: String, book: Int, index: HadithIndex): HadithEntry? {
    val a = line.indexOf('\t')
    val b = line.indexOf('\t', a + 1)
    val c = line.indexOf('\t', b + 1)
    if (a < 0 || b < 0 || c < 0) return null
    return HadithEntry(
        collectionId = collectionId,
        book = book,
        number = line.substring(0, a),
        numberInBook = line.substring(a + 1, b).toIntOrNull() ?: 0,
        text = line.substring(c + 1),
        grade = index.grade(line.substring(b + 1, c))
    )
}
