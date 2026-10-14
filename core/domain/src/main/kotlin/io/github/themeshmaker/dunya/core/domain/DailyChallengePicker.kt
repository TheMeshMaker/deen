package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.Challenge
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.DailyChallenges
import io.github.themeshmaker.dunya.core.model.DuaRef
import io.github.themeshmaker.dunya.core.model.PassageName
import io.github.themeshmaker.dunya.core.model.QuranReading
import io.github.themeshmaker.dunya.core.model.SunnahAct
import io.github.themeshmaker.dunya.core.model.TasbihDhikr
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Picks each day's challenges at random: which three kinds (from duas, Quran, tasbih, a set of adhkar,
 * the five prayers, the hadith of the day and sunnah acts), in what order, and what each asks for.
 * The date is the seed, so a day's challenges stay put when the app is opened again; the next day
 * brings a new draw.
 */
object DailyChallengePicker {
    const val PER_DAY = 3

    /** How often each kind comes up, relative to the others. */
    private val weights = linkedMapOf(
        ChallengeKind.DUA to 3,
        ChallengeKind.QURAN to 3,
        ChallengeKind.TASBIH to 3,
        ChallengeKind.ADHKAR to 2,
        ChallengeKind.SUNNAH to 2,
        ChallengeKind.PRAYERS to 1,
        ChallengeKind.HADITH to 1
    )

    /** Adhkar categories whose duas make a good challenge (said by anyone, any day). */
    private val duaCategories = listOf(
        AdhkarCategory.MORNING,
        AdhkarCategory.EVENING,
        AdhkarCategory.AFTER_PRAYER,
        AdhkarCategory.SLEEP,
        "waking",
        "home",
        AdhkarCategory.MOSQUE,
        "wudu"
    )

    /** Sets of adhkar to finish. */
    val adhkarSets: List<String> = listOf(AdhkarCategory.MORNING, AdhkarCategory.EVENING, AdhkarCategory.AFTER_PRAYER, AdhkarCategory.SLEEP)

    /** Passages and amounts for the Quran challenge (Fridays always have Al-Kahf). */
    val readings: List<QuranReading> = listOf(
        QuranReading.Passage(67),
        QuranReading.Passage(32),
        QuranReading.Passage(36),
        QuranReading.Passage(55),
        QuranReading.Passage(56),
        QuranReading.Passage(78),
        QuranReading.Passage(87),
        QuranReading.Passage(76),
        QuranReading.Passage(73),
        QuranReading.Passage(2, 255, 255, PassageName.AYAT_AL_KURSI),
        QuranReading.Passage(2, 285, 286, PassageName.END_OF_AL_BAQARAH),
        QuranReading.Passage(18, 1, 10, PassageName.FIRST_TEN_OF_AL_KAHF),
        QuranReading.Passage(112, name = PassageName.THREE_QULS),
        QuranReading.Pages(2),
        QuranReading.Pages(4),
        QuranReading.Pages(5)
    )

    val kahf: QuranReading = QuranReading.Passage(18)

    val tasbihCounts: List<Int> = listOf(33, 100)

    /** Which kinds of challenge [date] has, in order (needs no content, so any screen can check it). */
    fun kindsFor(date: LocalDate): List<ChallengeKind> {
        val random = random(date, SALT_KINDS)
        val left = LinkedHashMap(weights)
        return List(PER_DAY) {
            var roll = random.nextInt(left.values.sum())
            val kind = left.entries.first { (_, weight) -> (roll < weight).also { roll -= weight } }.key
            left.remove(kind)
            kind
        }
    }

    fun pick(date: LocalDate, adhkar: List<AdhkarCategory>): DailyChallenges = DailyChallenges(
        date,
        kindsFor(date).mapNotNull { kind ->
            when (kind) {
                ChallengeKind.DUA -> duaFor(date, adhkar)?.let { Challenge.Dua(it) }

                ChallengeKind.QURAN -> Challenge.Quran(quranFor(date))

                ChallengeKind.TASBIH -> tasbihChallenge(date)

                ChallengeKind.ADHKAR -> adhkarChallenge(date)

                ChallengeKind.PRAYERS -> Challenge.Prayers

                ChallengeKind.HADITH -> Challenge.Hadith

                ChallengeKind.SUNNAH -> Challenge.Sunnah(
                    SunnahAct.entries[random(date, ChallengeKind.SUNNAH.ordinal).nextInt(SunnahAct.entries.size)]
                )
            }
        }
    )

    /** [date]'s tasbih challenge, if it has one. */
    fun tasbihFor(date: LocalDate): Challenge.Tasbih? = if (ChallengeKind.TASBIH in kindsFor(date)) tasbihChallenge(date) else null

    /** [date]'s set of adhkar to finish, if it has one. */
    fun adhkarFor(date: LocalDate): Challenge.Adhkar? = if (ChallengeKind.ADHKAR in kindsFor(date)) adhkarChallenge(date) else null

    fun quranFor(date: LocalDate): QuranReading =
        if (date.dayOfWeek == DayOfWeek.FRIDAY) kahf else readings[random(date, ChallengeKind.QURAN.ordinal).nextInt(readings.size)]

    /** A short dua (said at most three times) from the everyday adhkar. */
    fun duaFor(date: LocalDate, adhkar: List<AdhkarCategory>): DuaRef? {
        val pool = adhkar.filter { it.id in duaCategories }.flatMap { category ->
            category.items.filter { it.text != null && it.quran.isEmpty() && it.count <= 3 }.map { DuaRef(category.id, it.id) }
        }
        if (pool.isEmpty()) return null
        return pool[random(date, ChallengeKind.DUA.ordinal).nextInt(pool.size)]
    }

    private fun tasbihChallenge(date: LocalDate): Challenge.Tasbih {
        val random = random(date, ChallengeKind.TASBIH.ordinal)
        return Challenge.Tasbih(
            TasbihDhikr.entries[random.nextInt(TasbihDhikr.entries.size)],
            tasbihCounts[random.nextInt(tasbihCounts.size)]
        )
    }

    private fun adhkarChallenge(date: LocalDate) =
        Challenge.Adhkar(adhkarSets[random(date, ChallengeKind.ADHKAR.ordinal).nextInt(adhkarSets.size)])

    private fun random(date: LocalDate, salt: Int) = Random(date.toEpochDay() * 7_919 + salt * 104_729)

    private const val SALT_KINDS = 101
}

/** The daily challenges' streak: days on which all of that day's challenges were done. Everything stays on the phone. */
object ChallengeStreak {
    /** Only a day's own challenges can be ticked, so three done means the day is complete. */
    fun complete(done: Map<LocalDate, Set<ChallengeKind>>, day: LocalDate): Boolean =
        done[day].orEmpty().size >= DailyChallengePicker.PER_DAY

    /**
     * Days in a row with every challenge done, ending today, or yesterday while today is still in
     * progress (so the streak doesn't drop to zero every morning).
     */
    fun current(done: Map<LocalDate, Set<ChallengeKind>>, today: LocalDate): Int {
        var day = if (complete(done, today)) today else today.minusDays(1)
        var count = 0
        while (complete(done, day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** The longest run of complete days ever recorded. */
    fun best(done: Map<LocalDate, Set<ChallengeKind>>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        done.keys.filter { complete(done, it) }.sorted().forEach { day ->
            run = if (previous == day.minusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }
}
