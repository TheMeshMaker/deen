/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.domain

import io.github.themeshmaker.dunya.core.model.ActivityData
import io.github.themeshmaker.dunya.core.model.AdhkarCategory
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.Ayah
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.CustomColors
import io.github.themeshmaker.dunya.core.model.Hadith
import io.github.themeshmaker.dunya.core.model.HadithCollection
import io.github.themeshmaker.dunya.core.model.HadithEntry
import io.github.themeshmaker.dunya.core.model.JuzStart
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.LiquidSettings
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.ProphetFact
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.Surah
import io.github.themeshmaker.dunya.core.model.TafsirEntry
import io.github.themeshmaker.dunya.core.model.TafsirSource
import io.github.themeshmaker.dunya.core.model.TasbihState
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.model.UserData
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userData: Flow<UserData>

    suspend fun setLocation(location: SavedLocation)

    suspend fun setPrayerSettings(settings: PrayerSettings)

    suspend fun setThemeStyle(style: ThemeStyle)

    suspend fun setCustomColors(colors: CustomColors)

    suspend fun setAlertSettings(settings: AlertSettings)

    /** Reliability steps Android can't detect (e.g. OEM auto-start), confirmed by the user. */
    suspend fun setReliabilityStepConfirmed(step: String, confirmed: Boolean)

    suspend fun setOnboardingComplete()

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUiStyle(style: UiStyle)

    suspend fun setSecretsUnlocked(unlocked: Boolean)

    suspend fun setTimetableCalendar(calendar: CalendarType)

    suspend fun setReadingSettings(settings: ReadingSettings)

    /** How the Liquid style's glass is tuned. */
    suspend fun setLiquidSettings(settings: LiquidSettings)
}

/** What the user has been doing: reading position, bookmarks, adhkar counts, tasbih, prayers, khatmah. */
interface ActivityRepository {
    val activity: Flow<ActivityData>

    suspend fun setLastRead(position: QuranPosition)

    suspend fun toggleBookmark(position: QuranPosition)

    suspend fun setAdhkarCount(day: LocalDate, categoryId: String, dhikrId: Int, count: Int)

    suspend fun resetAdhkar(day: LocalDate, categoryId: String)

    suspend fun setTasbih(state: TasbihState)

    /** Marks one of the five prayers of [day] as prayed or not. */
    suspend fun setPrayed(day: LocalDate, prayer: Prayer, prayed: Boolean)

    /** Starts (or replaces) the khatmah plan; null ends it. */
    suspend fun setKhatmah(plan: KhatmahPlan?)

    /** Marks one of [day]'s daily challenges done or not. */
    suspend fun setChallengeDone(day: LocalDate, kind: ChallengeKind, done: Boolean)
}

/** The bundled, offline religious texts (core:content). Everything is read lazily and cached. */
interface ContentRepository {
    suspend fun adhkar(): List<AdhkarCategory>

    suspend fun surahs(): List<Surah>

    /** Verbatim Tanzil text of one surah. */
    suspend fun ayahs(surah: Int): List<Ayah>

    suspend fun juzStarts(): List<JuzStart>

    /** Where each of the 604 Madani pages starts. */
    suspend fun pageStarts(): List<QuranPosition>

    /** An-Nawawi's Forty, with English. */
    suspend fun nawawi(): List<Hadith>

    suspend fun prophetFacts(): List<ProphetFact>

    suspend fun hadithCollections(): List<HadithCollection>

    suspend fun hadithBook(collectionId: String, book: Int): List<HadithEntry>

    /** Hadith whose Arabic contains [query] (diacritics ignored), or whose number is [query]. */
    suspend fun searchHadith(collectionId: String, query: String, limit: Int = 200): List<HadithEntry>

    /**
     * The tafsir of every verse of [surah], or null when it isn't on the phone and can't be fetched
     * (the longer tafsirs are downloaded once per surah).
     */
    suspend fun tafsir(source: TafsirSource, surah: Int): List<TafsirEntry>?

    /** Pickthall's English meaning of every verse of [surah] (1930, public domain). */
    suspend fun meaning(surah: Int): List<String>
}

sealed interface DeviceLocationResult {
    data class Found(val location: SavedLocation) : DeviceLocationResult

    /** No permission, location off, or no provider answered in time. */
    data object Unavailable : DeviceLocationResult
}

interface LocationRepository {
    /** One-shot device location, named after the nearest city in the offline database. */
    suspend fun detectDeviceLocation(): DeviceLocationResult

    suspend fun searchCities(query: String, limit: Int = 30): List<City>
}
