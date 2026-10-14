/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.model

/** In-app destinations that notifications and the widget can open (an Intent extra on the launch intent). */
object DeenLinks {
    const val EXTRA_OPEN = "io.github.themeshmaker.dunya.extra.OPEN"

    private const val ADHKAR_PREFIX = "adhkar/"
    private const val QURAN_PREFIX = "quran/"

    fun adhkar(categoryId: String): String = ADHKAR_PREFIX + categoryId

    /** The adhkar category in [link], if it is an adhkar link. */
    fun adhkarCategory(link: String?): String? = link?.takeIf { it.startsWith(ADHKAR_PREFIX) }?.removePrefix(ADHKAR_PREFIX)

    fun quran(surah: Int, ayah: Int = 1): String = "$QURAN_PREFIX$surah/$ayah"

    /** The Quran position in [link], if it is a Quran link. */
    fun quranPosition(link: String?): QuranPosition? {
        val parts = link?.takeIf { it.startsWith(QURAN_PREFIX) }?.removePrefix(QURAN_PREFIX)?.split('/') ?: return null
        val surah = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..114 } ?: return null
        return QuranPosition(surah, parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
    }
}
