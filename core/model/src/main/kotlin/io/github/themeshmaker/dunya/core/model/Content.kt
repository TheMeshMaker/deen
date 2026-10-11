package io.github.themeshmaker.dunya.core.model

/** Text shipped in both app languages. Either side may be missing for some fields. */
data class LocalizedText(val ar: String?, val en: String?) {
    /** The text for the current UI language, falling back to the other one. */
    fun pick(arabic: Boolean): String = (if (arabic) ar ?: en else en ?: ar).orEmpty()
}

/** A passage of the Quran: verses [from]..[to] of [surah], inclusive. */
data class QuranRange(val surah: Int, val from: Int, val to: Int)

/** One remembrance: Arabic to recite, how many times, and where it comes from. */
data class Dhikr(
    val id: Int,
    /** Said before a Quran passage, e.g. seeking refuge before Ayat al-Kursi. */
    val preface: String?,
    /** Arabic text of a supplication; null when the dhikr is only [quran]. */
    val text: String?,
    /** Quranic passages, shown from the bundled Tanzil text. */
    val quran: List<QuranRange>,
    val count: Int,
    /** When or how to say it, e.g. "After wudu". */
    val instruction: LocalizedText?,
    val translation: String,
    /** The reward promised for it in the hadith. */
    val virtue: LocalizedText?,
    /** When no reward is stated: how the Prophet ﷺ said or taught it. */
    val sunnah: LocalizedText? = null,
    val reference: LocalizedText
) {
    val isQuran: Boolean get() = quran.isNotEmpty()
}

data class AdhkarCategory(val id: String, val title: LocalizedText, val items: List<Dhikr>) {
    companion object {
        const val MORNING = "morning"
        const val EVENING = "evening"
        const val AFTER_PRAYER = "after_prayer"
        const val SLEEP = "sleep"
        const val MOSQUE = "mosque"
        const val ADHAN = "adhan"
    }
}

data class Hadith(val number: Int, val title: LocalizedText, val arabic: String, val english: String, val source: LocalizedText)

enum class Revelation {
    MECCAN,
    MEDINAN
}

data class Surah(
    val number: Int,
    val ayahCount: Int,
    val arabicName: String,
    val transliteration: String,
    val translation: String,
    val revelation: Revelation
)

/** One verse, verbatim from the Tanzil text. [bismillah] is set for verse 1 of surahs that open with it. */
data class Ayah(val surah: Int, val number: Int, val text: String, val bismillah: String? = null)

data class JuzStart(val juz: Int, val surah: Int, val ayah: Int)

/** A short, sourced fact about the Prophet ﷺ, shown one a day. */
data class ProphetFact(val id: Int, val text: LocalizedText, val source: LocalizedText)

/** How a scholar graded a hadith, simplified for colour coding. */
enum class GradeKind {
    AUTHENTIC,
    GOOD,
    WEAK,
    FABRICATED,
    OTHER
}

data class HadithGrade(val grader: LocalizedText, val label: LocalizedText, val kind: GradeKind)

/** One "book" (kitab) of a collection; [first] and [last] are hadith numbers. */
data class HadithBook(val number: Int, val title: LocalizedText, val count: Int, val first: String, val last: String)

data class HadithCollection(
    val id: String,
    val name: LocalizedText,
    val author: LocalizedText?,
    /** Al-Bukhari and Muslim: every hadith is authentic by scholarly consensus, so none carries a grade. */
    val allAuthentic: Boolean,
    val count: Int,
    val books: List<HadithBook>
)

/** One hadith of a full collection (Arabic only). [number] is the collection's own numbering. */
data class HadithEntry(
    val collectionId: String,
    val book: Int,
    val number: String,
    val numberInBook: Int,
    val text: String,
    val grade: HadithGrade?
)
