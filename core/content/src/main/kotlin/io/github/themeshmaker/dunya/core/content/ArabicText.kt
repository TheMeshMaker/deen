/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.content

/** Arabic text matching that ignores diacritics and the usual spelling variants. */
object ArabicText {
    private val marks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")

    fun normalize(text: String): String {
        val stripped = marks.replace(text, "")
        val out = StringBuilder(stripped.length)
        var lastSpace = true
        for (ch in stripped) {
            val mapped = when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                'ؤ' -> 'و'
                'ئ' -> 'ي'
                else -> if (ch.isLetterOrDigit()) ch.lowercaseChar() else ' '
            }
            if (mapped == ' ') {
                if (!lastSpace) out.append(' ')
                lastSpace = true
            } else {
                out.append(mapped)
                lastSpace = false
            }
        }
        return out.toString().trim()
    }
}
