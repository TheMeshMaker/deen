/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.DeenLinks
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.QuranPosition
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTrackerTest {

    private val today = LocalDate.of(2026, 10, 13)
    private val all = Prayer.prayers.toSet()

    @Test
    fun aStreakCountsWholeDaysAndWaitsForToday() {
        val prayed = mapOf(
            today.minusDays(1) to all,
            today.minusDays(2) to all,
            today.minusDays(3) to all - Prayer.ASR,
            today to setOf(Prayer.FAJR)
        )
        assertEquals(2, PrayerTracker.streak(prayed, today))
        assertEquals(3, PrayerTracker.streak(prayed + (today to all), today))
    }

    @Test
    fun sunriseNeverCounts() {
        val prayed = mapOf(today to setOf(Prayer.SUNRISE, Prayer.FAJR))
        assertEquals(1, PrayerTracker.count(prayed, today))
        assertFalse(PrayerTracker.complete(prayed, today))
    }

    @Test
    fun theBestStreakIsTheLongestRun() {
        val prayed = (1L..3L).associate { today.minusDays(it + 10) to all } + (0L..4L).associate { today.minusDays(it) to all }
        assertEquals(5, PrayerTracker.bestStreak(prayed))
    }

    @Test
    fun monthTotalsStopAtToday() {
        val prayed = mapOf(today to all, today.minusDays(1) to setOf(Prayer.FAJR))
        val (done, due) = PrayerTracker.monthTotals(prayed, YearMonth.from(today), today)
        assertEquals(6, done)
        assertEquals(today.dayOfMonth * 5, due)
        assertEquals(0 to 0, PrayerTracker.monthTotals(prayed, YearMonth.from(today).plusMonths(1), today))
    }
}

class KhatmahTest {

    /** Ten pages, two verses each, all in surah 2 (enough to exercise the arithmetic). */
    private val pages = QuranPages((0 until 10).map { QuranPosition(2, it * 2 + 1) })
    private val start = LocalDate.of(2026, 10, 1)

    @Test
    fun pageLookupFindsThePageAVerseIsOn() {
        assertEquals(1, pages.pageOf(QuranPosition(2, 1)))
        assertEquals(1, pages.pageOf(QuranPosition(2, 2)))
        assertEquals(2, pages.pageOf(QuranPosition(2, 3)))
        assertEquals(10, pages.pageOf(QuranPosition(3, 1)))
        assertEquals(QuranPosition(2, 5), pages.start(3))
    }

    @Test
    fun todaysPortionGrowsWithTheDays() {
        val plan = KhatmahPlan(start, days = 5)
        val first = KhatmahProgress.of(plan, null, pages, start)
        assertEquals(1, first.day)
        assertEquals(1, first.todayFrom)
        assertEquals(2, first.todayTo)
        assertEquals(2, first.pagesLeftToday)

        val third = KhatmahProgress.of(plan, QuranPosition(2, 5), pages, start.plusDays(2))
        assertEquals(2, third.pagesRead)
        assertEquals(3, third.todayFrom)
        assertEquals(6, third.todayTo)
        assertEquals(4, third.pagesLeftToday)
        assertEquals(-4, third.ahead)
    }

    @Test
    fun readingAheadLeavesNothingForToday() {
        val progress = KhatmahProgress.of(KhatmahPlan(start, days = 5), QuranPosition(2, 11), pages, start)
        assertEquals(5, progress.pagesRead)
        assertEquals(0, progress.pagesLeftToday)
        assertTrue(progress.ahead > 0)
    }

    @Test
    fun aPlanCanStartPartWay() {
        val progress = KhatmahProgress.of(KhatmahPlan(start, days = 2, startPage = 7), QuranPosition(2, 1), pages, start)
        assertEquals(4, progress.totalPages)
        assertEquals(0, progress.pagesRead)
        assertEquals(7, progress.todayFrom)
        assertEquals(8, progress.todayTo)
    }

    @Test
    fun finishingAnNasCompletesIt() {
        val progress = KhatmahProgress.of(KhatmahPlan(start, days = 5), QuranPosition(114, 6), pages, start.plusDays(3))
        assertTrue(progress.complete)
        assertEquals(1f, progress.fraction)
    }
}

class DeenLinksTest {
    @Test
    fun quranLinksRoundTrip() {
        assertEquals(QuranPosition(18, 1), DeenLinks.quranPosition(DeenLinks.quran(18)))
        assertEquals(QuranPosition(2, 255), DeenLinks.quranPosition(DeenLinks.quran(2, 255)))
        assertNull(DeenLinks.quranPosition(DeenLinks.adhkar("morning")))
        assertNull(DeenLinks.quranPosition("quran/200/1"))
    }
}
