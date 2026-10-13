package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.QuranPosition
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Madani page starts (604 of them, from the Tanzil metadata) and lookups by position. */
class QuranPages(private val starts: List<QuranPosition>) {
    val count: Int get() = starts.size

    /** The page [position] is on, 1-based. */
    fun pageOf(position: QuranPosition): Int {
        var low = 0
        var high = starts.lastIndex
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (starts[mid].isAfter(position)) high = mid - 1 else low = mid
        }
        return low + 1
    }

    /** The first verse on [page] (1-based). */
    fun start(page: Int): QuranPosition = starts[(page - 1).coerceIn(0, starts.lastIndex)]

    private fun QuranPosition.isAfter(other: QuranPosition) = surah > other.surah || (surah == other.surah && ayah > other.ayah)
}

/**
 * Where a khatmah plan stands today. Pages are Madani pages; [todayFrom]..[todayTo] is what is left
 * of today's portion (empty once it is read).
 */
data class KhatmahProgress(
    val plan: KhatmahPlan,
    /** 1-based day of the plan (can pass [KhatmahPlan.days] if it's running late). */
    val day: Int,
    val pagesRead: Int,
    val totalPages: Int,
    val todayFrom: Int,
    val todayTo: Int,
    /** Pages to read today to stay on schedule (0 once today's portion is done). */
    val pagesLeftToday: Int,
    /** Positive: pages ahead of schedule; negative: behind. */
    val ahead: Int,
    val complete: Boolean
) {
    val fraction: Float get() = if (totalPages == 0) 0f else pagesRead.toFloat() / totalPages

    companion object {
        /**
         * @param lastRead where the reader last stopped; pages before its page count as read.
         * Finishing An-Nas completes the plan.
         */
        fun of(plan: KhatmahPlan, lastRead: QuranPosition?, pages: QuranPages, today: LocalDate): KhatmahProgress {
            val total = pages.count - plan.startPage + 1
            val currentPage = lastRead?.let(pages::pageOf)?.coerceAtLeast(plan.startPage) ?: plan.startPage
            val complete = lastRead != null && lastRead.surah == LAST_SURAH && lastRead.ayah >= LAST_SURAH_AYAHS
            val read = if (complete) total else (currentPage - plan.startPage).coerceIn(0, total)
            val day = (ChronoUnit.DAYS.between(plan.startedOn, today) + 1).toInt().coerceAtLeast(1)
            val days = plan.days.coerceAtLeast(1)
            fun dueBy(d: Int) = ceil(total.toDouble() * d.coerceAtMost(days) / days).toInt()
            val dueToday = dueBy(day)
            return KhatmahProgress(
                plan = plan,
                day = day,
                pagesRead = read,
                totalPages = total,
                // From where the reader is (catching up if behind) to where today should end.
                todayFrom = (plan.startPage + read).coerceAtMost(pages.count),
                todayTo = (plan.startPage + dueToday - 1).coerceAtMost(pages.count),
                pagesLeftToday = (dueToday - read).coerceAtLeast(0),
                ahead = read - dueToday,
                complete = complete
            )
        }

        private const val LAST_SURAH = 114
        private const val LAST_SURAH_AYAHS = 6
    }
}
