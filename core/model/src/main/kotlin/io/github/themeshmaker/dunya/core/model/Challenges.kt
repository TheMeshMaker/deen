/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.model

import java.time.LocalDate

/** The kinds of daily challenge; each day draws three different ones at random. */
enum class ChallengeKind {
    DUA,
    QURAN,
    TASBIH,
    ADHKAR,
    PRAYERS,
    HADITH,
    SUNNAH
}

/** One daily challenge. */
sealed interface Challenge {
    val kind: ChallengeKind

    /** Say a dua from the adhkar. */
    data class Dua(val ref: DuaRef) : Challenge {
        override val kind get() = ChallengeKind.DUA
    }

    /** Read a passage or a few pages. */
    data class Quran(val reading: QuranReading) : Challenge {
        override val kind get() = ChallengeKind.QURAN
    }

    /** A round of [dhikr] on the tasbih, [count] times (ticks itself when counted). */
    data class Tasbih(val dhikr: TasbihDhikr, val count: Int) : Challenge {
        override val kind get() = ChallengeKind.TASBIH
    }

    /** Finish a whole set of adhkar (ticks itself when the last one is counted). */
    data class Adhkar(val categoryId: String) : Challenge {
        override val kind get() = ChallengeKind.ADHKAR
    }

    /** Tick off all five prayers in the tracker (ticks itself). */
    data object Prayers : Challenge {
        override val kind get() = ChallengeKind.PRAYERS
    }

    /** Read the hadith of the day. */
    data object Hadith : Challenge {
        override val kind get() = ChallengeKind.HADITH
    }

    /** A sunnah act during the day. */
    data class Sunnah(val act: SunnahAct) : Challenge {
        override val kind get() = ChallengeKind.SUNNAH
    }
}

/** Everyday sunnah acts for the [Challenge.Sunnah] challenge. */
enum class SunnahAct {
    DUHA,
    WITR,
    FAJR_SUNNAH,
    CHARITY,
    KINSHIP,
    KIND_WORD,
    DUA_FOR_OTHERS
}

/** What the daily Quran challenge asks to read. */
sealed interface QuranReading {
    /** Verses [from]..[to] of [surah] ([to] null: to the end of the surah), named as [name] says. */
    data class Passage(val surah: Int, val from: Int = 1, val to: Int? = null, val name: PassageName = PassageName.SURAH) : QuranReading

    /** [pages] Madani pages, carrying on from where the reader last stopped. */
    data class Pages(val pages: Int) : QuranReading
}

/** How a [QuranReading.Passage] is named: by its surah, or by the passage's own name. */
enum class PassageName {
    SURAH,
    AYAT_AL_KURSI,
    END_OF_AL_BAQARAH,
    FIRST_TEN_OF_AL_KAHF,

    /** Al-Ikhlas, Al-Falaq and An-Nas (112–114). */
    THREE_QULS
}

/** A dua from the adhkar: category [categoryId], dhikr [dhikrId]. */
data class DuaRef(val categoryId: String, val dhikrId: Int)

/** One day's challenges, in the order they are shown. */
data class DailyChallenges(val date: LocalDate, val items: List<Challenge>)
