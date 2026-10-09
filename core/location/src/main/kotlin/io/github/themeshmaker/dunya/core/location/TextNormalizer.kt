package io.github.themeshmaker.dunya.core.location

import java.text.Normalizer
import java.util.Locale

/**
 * Folds text for forgiving city search in Latin and Arabic script:
 * case, accents, Arabic diacritics (tashkeel), hamza seats (أ إ آ → ا), ة/ه, ى/ي,
 * tatweel and punctuation are all ignored.
 */
object TextNormalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val separators = Regex("[\\s\\-_'’ʻʼʿʾ`.,()]+")

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return combiningMarks.replace(decomposed, "")
            .replace('ٱ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace("ـ", "")
            .replace(separators, " ")
            .trim()
    }

    /** One searchable string per city; Arabic words are also indexed without the article "ال". */
    fun searchKey(name: String, arabicName: String?): String = buildString {
        append(normalize(name))
        if (!arabicName.isNullOrEmpty()) {
            val arabic = normalize(arabicName)
            append(' ').append(arabic)
            arabic.split(' ').filter { it.startsWith("ال") && it.length > 3 }.forEach {
                append(' ').append(it.removePrefix("ال"))
            }
        }
    }
}
