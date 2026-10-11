package io.github.themeshmaker.dunya.core.model

/**
 * A reciter whose verse-by-verse recordings are streamed from everyayah.com/data/[id]/ and for
 * whom word timings are bundled (quran-align), so the word being recited can be highlighted.
 */
enum class Reciter(val id: String, val arabicName: String, val englishName: String) {
    ALAFASY("Alafasy_128kbps", "مشاري راشد العفاسي", "Mishary Alafasy"),
    SUDAIS("Abdurrahmaan_As-Sudais_192kbps", "عبد الرحمن السديس", "Abdul Rahman Al-Sudais"),
    SHURAIM("Saood_ash-Shuraym_128kbps", "سعود الشريم", "Saud Al-Shuraim"),
    SHATRI("Abu_Bakr_Ash-Shaatree_128kbps", "أبو بكر الشاطري", "Abu Bakr Al-Shatri"),
    HUSARY("Husary_64kbps", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
    MINSHAWI("Minshawy_Murattal_128kbps", "محمد صديق المنشاوي", "Mohamed Siddiq Al-Minshawi"),
    ABDUL_BASIT("Abdul_Basit_Murattal_64kbps", "عبد الباسط عبد الصمد", "Abdul Basit Abdul Samad")
    ;

    fun name(arabic: Boolean): String = if (arabic) arabicName else englishName

    companion object {
        fun byId(id: String?): Reciter = entries.firstOrNull { it.id == id } ?: ALAFASY
    }
}

/**
 * Tafsir texts. Al-Jalalayn is bundled (works offline); the longer classical tafsirs are fetched
 * per surah on first use and kept on the phone. All are classical works in the public domain.
 */
enum class TafsirSource(val id: String, val arabicName: String, val englishName: String, val bundled: Boolean) {
    JALALAYN("jalalayn", "تفسير الجلالين", "Tafsir al-Jalalayn", bundled = true),
    IBN_KATHIR("ar-tafsir-ibn-kathir", "تفسير ابن كثير", "Tafsir Ibn Kathir", bundled = false),
    BAGHAWI("ar-tafsir-al-baghawi", "تفسير البغوي", "Tafsir al-Baghawi", bundled = false),
    QURTUBI("ar-tafseer-al-qurtubi", "تفسير القرطبي", "Tafsir al-Qurtubi", bundled = false)
    ;

    fun name(arabic: Boolean): String = if (arabic) arabicName else englishName

    companion object {
        fun byId(id: String?): TafsirSource = entries.firstOrNull { it.id == id } ?: JALALAYN
    }
}

/**
 * The tafsir of one verse. When a passage explains several verses together, the later verses
 * point back to it with [coveredBy].
 */
data class TafsirEntry(val ayah: Int, val text: String, val coveredBy: Int? = null)

/** One word's timing within a verse recording: words [firstWord]..[endWord) from [startMs] to [endMs]. */
data class WordTiming(val firstWord: Int, val endWord: Int, val startMs: Int, val endMs: Int)
