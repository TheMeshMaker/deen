package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.Challenge
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.Dhikr
import io.github.themeshmaker.dunya.core.model.LocalizedText
import io.github.themeshmaker.dunya.core.model.QuranRange
import io.github.themeshmaker.dunya.core.model.QuranReading
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengesTest {

    private val today = LocalDate.of(2026, 10, 14)
    private val days = (0L until 120L).map { today.plusDays(it) }

    private fun dhikr(id: Int, text: String? = "دعاء", count: Int = 1, quran: List<QuranRange> = emptyList()) = Dhikr(
        id = id,
        preface = null,
        text = text,
        quran = quran,
        count = count,
        instruction = null,
        translation = "",
        virtue = null,
        reference = LocalizedText("", "")
    )

    private val adhkar = listOf(
        AdhkarCategory(
            AdhkarCategory.MORNING,
            LocalizedText("أذكار الصباح", "Morning"),
            listOf(dhikr(1), dhikr(2, count = 100), dhikr(3, text = null, quran = listOf(QuranRange(112, 1, 4))), dhikr(4, count = 3))
        ),
        AdhkarCategory(AdhkarCategory.ADHAN, LocalizedText("عند الأذان", "With the adhan"), listOf(dhikr(1))),
        AdhkarCategory(AdhkarCategory.SLEEP, LocalizedText("أذكار النوم", "Before sleep"), listOf(dhikr(7)))
    )

    @Test
    fun aDayKeepsItsChallengesWhileTheKindsAndOrderChangeFromDayToDay() {
        assertEquals(DailyChallengePicker.pick(today, adhkar), DailyChallengePicker.pick(today, adhkar))
        val kinds = days.map { DailyChallengePicker.kindsFor(it) }
        kinds.forEach { day ->
            assertEquals(DailyChallengePicker.PER_DAY, day.size)
            assertEquals(day.size, day.toSet().size)
        }
        // Every kind comes up, in different combinations and orders, and not the same set every day.
        assertEquals(ChallengeKind.entries.toSet(), kinds.flatten().toSet())
        assertTrue(kinds.map { it.toSet() }.distinct().size > 10)
        assertTrue(kinds.distinct().size > 20)
        assertTrue(kinds.zipWithNext().count { (a, b) -> a == b } < 10)
        days.forEach { assertEquals(DailyChallengePicker.kindsFor(it), DailyChallengePicker.pick(it, adhkar).items.map { c -> c.kind }) }
    }

    @Test
    fun theDuaIsAShortSupplicationFromTheEverydayAdhkar() {
        val picked = days.mapNotNull { DailyChallengePicker.duaFor(it, adhkar) }.map { it.categoryId to it.dhikrId }.toSet()
        // Not the 100-times dhikr, not a Quran passage, not the adhan's (said only with the adhan).
        assertEquals(setOf(AdhkarCategory.MORNING to 1, AdhkarCategory.MORNING to 4, AdhkarCategory.SLEEP to 7), picked)
        assertNull(DailyChallengePicker.duaFor(today, emptyList()))
    }

    @Test
    fun fridaysReadAlKahf() {
        val friday = days.first { it.dayOfWeek == DayOfWeek.FRIDAY }
        assertEquals(QuranReading.Passage(18), DailyChallengePicker.quranFor(friday))
        assertEquals(QuranReading.Passage(18), DailyChallengePicker.quranFor(friday.plusWeeks(1)))
    }

    @Test
    fun otherScreensSeeTheSameTasbihAndAdhkarChallenges() {
        days.forEach { day ->
            val items = DailyChallengePicker.pick(day, adhkar).items
            assertEquals(items.filterIsInstance<Challenge.Tasbih>().firstOrNull(), DailyChallengePicker.tasbihFor(day))
            assertEquals(items.filterIsInstance<Challenge.Adhkar>().firstOrNull(), DailyChallengePicker.adhkarFor(day))
        }
        val tasbih = days.mapNotNull { DailyChallengePicker.tasbihFor(it) }
        assertEquals(setOf(33, 100), tasbih.map { it.count }.toSet())
        assertTrue(tasbih.map { it.dhikr }.distinct().size > 3)
    }

    @Test
    fun theStreakCountsDaysWithAllThreeAndWaitsForToday() {
        val three = setOf(ChallengeKind.DUA, ChallengeKind.SUNNAH, ChallengeKind.PRAYERS)
        val done = mapOf(
            today.minusDays(1) to three,
            today.minusDays(2) to setOf(ChallengeKind.QURAN, ChallengeKind.TASBIH, ChallengeKind.HADITH),
            today.minusDays(3) to setOf(ChallengeKind.DUA, ChallengeKind.QURAN),
            today.minusDays(4) to three,
            today to setOf(ChallengeKind.DUA)
        )
        assertEquals(2, ChallengeStreak.current(done, today))
        assertEquals(3, ChallengeStreak.current(done + (today to three), today))
        assertEquals(0, ChallengeStreak.current(done, today.plusDays(2)))
        assertEquals(2, ChallengeStreak.best(done))
        assertEquals(3, ChallengeStreak.best(done + (today to three)))
    }
}
