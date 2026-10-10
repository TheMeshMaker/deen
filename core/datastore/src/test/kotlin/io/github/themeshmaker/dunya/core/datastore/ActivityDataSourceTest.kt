package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.DataStore
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.TasbihState
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityDataSourceTest {

    private class InMemoryDataStore(initial: ActivityPreferences = ActivityPreferences()) : DataStore<ActivityPreferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<ActivityPreferences> = state
        override suspend fun updateData(transform: suspend (t: ActivityPreferences) -> ActivityPreferences): ActivityPreferences =
            transform(state.value).also { state.value = it }
    }

    private val day = LocalDate.of(2026, 10, 10)

    @Test
    fun adhkarCountsBelongToOneDay() = runTest {
        val source = ActivityDataSource(InMemoryDataStore())
        source.setAdhkarCount(day, "morning/3", 2)
        source.setAdhkarCount(day, "evening/1", 1)
        assertEquals(mapOf("morning/3" to 2, "evening/1" to 1), source.activity.first().adhkarCountsOn(day))
        assertTrue(source.activity.first().adhkarCountsOn(day.plusDays(1)).isEmpty())

        // The first count of a new day drops yesterday's.
        source.setAdhkarCount(day.plusDays(1), "morning/1", 1)
        assertEquals(mapOf("morning/1" to 1), source.activity.first().adhkarCountsOn(day.plusDays(1)))

        source.resetAdhkar(day.plusDays(1), "morning")
        assertTrue(source.activity.first().adhkarCountsOn(day.plusDays(1)).isEmpty())
    }

    @Test
    fun bookmarksToggleAndReadingPositionAndTasbihSurviveTheFileFormat() = runTest {
        val store = InMemoryDataStore()
        val source = ActivityDataSource(store)
        source.setLastRead(QuranPosition(18, 23))
        source.toggleBookmark(QuranPosition(2, 255))
        source.toggleBookmark(QuranPosition(36, 1))
        source.toggleBookmark(QuranPosition(36, 1))
        source.setTasbih(TasbihState(dhikrId = "istighfar", count = 12, target = 100, day = day, todayTotal = 40, allTimeTotal = 1_000))

        val serializer = ActivityPreferencesSerializer()
        val bytes = ByteArrayOutputStream().also { serializer.writeTo(store.data.first(), it) }.toByteArray()
        val reloaded = ActivityDataSource(InMemoryDataStore(serializer.readFrom(bytes.inputStream()))).activity.first()
        assertEquals(QuranPosition(18, 23), reloaded.lastRead)
        assertEquals(listOf(QuranPosition(2, 255)), reloaded.bookmarks)
        assertEquals(TasbihState("istighfar", 12, 100, day, 40, 1_000), reloaded.tasbih)
    }
}
