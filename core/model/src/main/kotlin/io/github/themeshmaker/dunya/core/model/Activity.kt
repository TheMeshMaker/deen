package io.github.themeshmaker.dunya.core.model

import java.time.LocalDate

/** A place in the Quran. */
data class QuranPosition(val surah: Int, val ayah: Int)

/** The tasbih counter, kept across app restarts. [todayTotal] resets on a new [day]. */
data class TasbihState(
    val dhikrId: String = TasbihDhikr.SUBHANALLAH.id,
    val count: Int = 0,
    /** 0 = no target (counts forever). */
    val target: Int = 33,
    val day: LocalDate? = null,
    val todayTotal: Int = 0,
    val allTimeTotal: Long = 0
)

/** Built-in phrases for the tasbih. Arabic is shown as is; [english] is a short meaning. */
enum class TasbihDhikr(val id: String, val arabic: String, val english: String) {
    SUBHANALLAH("subhanallah", "سُبْحَانَ اللَّهِ", "Glory be to Allah"),
    ALHAMDULILLAH("alhamdulillah", "الْحَمْدُ لِلَّهِ", "Praise be to Allah"),
    ALLAHU_AKBAR("allahuakbar", "اللَّهُ أَكْبَرُ", "Allah is the Greatest"),
    LA_ILAHA_ILLALLAH("tahlil", "لَا إِلَهَ إِلَّا اللَّهُ", "There is no god but Allah"),
    ASTAGHFIRULLAH("istighfar", "أَسْتَغْفِرُ اللَّهَ", "I seek Allah's forgiveness"),
    SUBHANALLAHI_WA_BIHAMDIHI("subhanallahi_wabihamdihi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Glory and praise be to Allah"),
    HAWQALA("hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "No power or strength except through Allah"),
    SALAWAT("salawat", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "Blessings and peace upon our Prophet Muhammad")
    ;

    companion object {
        fun byId(id: String): TasbihDhikr = entries.firstOrNull { it.id == id } ?: SUBHANALLAH
    }
}

/**
 * What the user has been doing, kept apart from settings: last Quran position, bookmarks,
 * today's adhkar counts (key "category/id") and the tasbih.
 */
data class ActivityData(
    val lastRead: QuranPosition? = null,
    val bookmarks: List<QuranPosition> = emptyList(),
    val adhkarDay: LocalDate? = null,
    val adhkarCounts: Map<String, Int> = emptyMap(),
    val tasbih: TasbihState = TasbihState()
) {
    /** Today's counts, or none if they are from an earlier day. */
    fun adhkarCountsOn(day: LocalDate): Map<String, Int> = if (adhkarDay == day) adhkarCounts else emptyMap()
}
