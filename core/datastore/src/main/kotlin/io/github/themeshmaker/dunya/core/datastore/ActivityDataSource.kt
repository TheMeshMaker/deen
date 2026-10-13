package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import io.github.themeshmaker.dunya.core.model.ActivityData
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.TasbihState
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * On-disk shape of [ActivityData], in its own file (`activity.json`) so frequent writes from the
 * tasbih and the Quran reader never touch the settings file.
 */
@Serializable
data class ActivityPreferences(
    val lastRead: PositionPreferences? = null,
    val bookmarks: List<PositionPreferences> = emptyList(),
    val adhkarDay: String? = null,
    val adhkarCounts: Map<String, Int> = emptyMap(),
    val tasbih: TasbihPreferences = TasbihPreferences(),
    /** "2026-10-13" → ["FAJR", "DHUHR", …]. */
    val prayed: Map<String, List<String>> = emptyMap(),
    val khatmah: KhatmahPreferences? = null
)

@Serializable
data class KhatmahPreferences(val startedOn: String, val days: Int, val startPage: Int = 1)

@Serializable
data class PositionPreferences(val surah: Int, val ayah: Int)

@Serializable
data class TasbihPreferences(
    val dhikrId: String = "subhanallah",
    val count: Int = 0,
    val target: Int = 33,
    val day: String? = null,
    val todayTotal: Int = 0,
    val allTimeTotal: Long = 0
)

class ActivityDataSource @Inject constructor(@ActivityStore private val dataStore: DataStore<ActivityPreferences>) {

    val activity: Flow<ActivityData> = dataStore.data.map { prefs ->
        ActivityData(
            lastRead = prefs.lastRead?.toModel(),
            bookmarks = prefs.bookmarks.mapNotNull { it.toModel() },
            adhkarDay = prefs.adhkarDay?.toDate(),
            adhkarCounts = prefs.adhkarCounts,
            tasbih = prefs.tasbih.let {
                TasbihState(
                    it.dhikrId,
                    it.count.coerceAtLeast(0),
                    it.target.coerceAtLeast(0),
                    it.day?.toDate(),
                    it.todayTotal,
                    it.allTimeTotal
                )
            },
            prayed = prefs.prayed.mapNotNull { (day, names) ->
                val date = day.toDate() ?: return@mapNotNull null
                date to names.mapNotNull { name -> Prayer.entries.firstOrNull { it.name == name }?.takeIf { it.isPrayer } }.toSet()
            }.toMap(),
            khatmah = prefs.khatmah?.let { k ->
                k.startedOn.toDate()?.let { KhatmahPlan(it, k.days.coerceIn(1, 365), k.startPage.coerceIn(1, KhatmahPlan.PAGES)) }
            }
        )
    }

    /** Ticks a prayer of [day] on or off; history older than about a year and a half is dropped. */
    suspend fun setPrayed(day: LocalDate, prayer: Prayer, prayed: Boolean) {
        if (!prayer.isPrayer) return
        dataStore.updateData { prefs ->
            val key = day.toString()
            val current = prefs.prayed[key].orEmpty().toSet()
            val next = if (prayed) current + prayer.name else current - prayer.name
            val oldest = day.minusDays(KEEP_PRAYED_DAYS).toString()
            val kept = (prefs.prayed + (key to next.sortedBy { name -> Prayer.entries.indexOfFirst { it.name == name } }))
                .filter { (k, v) -> v.isNotEmpty() && k >= oldest }
            prefs.copy(prayed = kept)
        }
    }

    suspend fun setKhatmah(plan: KhatmahPlan?) {
        dataStore.updateData { prefs ->
            prefs.copy(khatmah = plan?.let { KhatmahPreferences(it.startedOn.toString(), it.days, it.startPage) })
        }
    }

    suspend fun setLastRead(position: QuranPosition) {
        dataStore.updateData { it.copy(lastRead = PositionPreferences(position.surah, position.ayah)) }
    }

    suspend fun toggleBookmark(position: QuranPosition) {
        dataStore.updateData { prefs ->
            val entry = PositionPreferences(position.surah, position.ayah)
            prefs.copy(bookmarks = if (entry in prefs.bookmarks) prefs.bookmarks - entry else listOf(entry) + prefs.bookmarks)
        }
    }

    /** Sets one adhkar count for [day]; counts from an earlier day are dropped first. */
    suspend fun setAdhkarCount(day: LocalDate, key: String, count: Int) {
        dataStore.updateData { prefs ->
            val counts = if (prefs.adhkarDay == day.toString()) prefs.adhkarCounts else emptyMap()
            prefs.copy(adhkarDay = day.toString(), adhkarCounts = counts + (key to count))
        }
    }

    /** Clears today's counts for one category (keys start with "category/"). */
    suspend fun resetAdhkar(day: LocalDate, categoryId: String) {
        dataStore.updateData { prefs ->
            val counts = if (prefs.adhkarDay == day.toString()) prefs.adhkarCounts else emptyMap()
            prefs.copy(adhkarDay = day.toString(), adhkarCounts = counts.filterKeys { !it.startsWith("$categoryId/") })
        }
    }

    suspend fun setTasbih(state: TasbihState) {
        dataStore.updateData {
            it.copy(
                tasbih = TasbihPreferences(
                    dhikrId = state.dhikrId,
                    count = state.count,
                    target = state.target,
                    day = state.day?.toString(),
                    todayTotal = state.todayTotal,
                    allTimeTotal = state.allTimeTotal
                )
            )
        }
    }
}

private const val KEEP_PRAYED_DAYS = 550L

private fun PositionPreferences.toModel(): QuranPosition? = QuranPosition(surah, ayah).takeIf { surah in 1..114 && ayah >= 1 }

private fun String.toDate(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
}

class ActivityPreferencesSerializer @Inject constructor() : Serializer<ActivityPreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue = ActivityPreferences()

    override suspend fun readFrom(input: InputStream): ActivityPreferences = try {
        json.decodeFromString(ActivityPreferences.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable activity", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Unreadable activity", e)
    }

    override suspend fun writeTo(t: ActivityPreferences, output: OutputStream) {
        output.write(json.encodeToString(ActivityPreferences.serializer(), t).encodeToByteArray())
    }
}
