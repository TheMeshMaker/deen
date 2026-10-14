package io.github.themeshmaker.dunya.core.data

import io.github.themeshmaker.dunya.core.datastore.ActivityDataSource
import io.github.themeshmaker.dunya.core.domain.ActivityRepository
import io.github.themeshmaker.dunya.core.domain.DailyChallengePicker
import io.github.themeshmaker.dunya.core.domain.PrayerTracker
import io.github.themeshmaker.dunya.core.model.ActivityData
import io.github.themeshmaker.dunya.core.model.ChallengeKind
import io.github.themeshmaker.dunya.core.model.KhatmahPlan
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.QuranPosition
import io.github.themeshmaker.dunya.core.model.TasbihState
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class OfflineActivityRepository @Inject constructor(private val source: ActivityDataSource) : ActivityRepository {
    override val activity: Flow<ActivityData> = source.activity

    override suspend fun setLastRead(position: QuranPosition) = source.setLastRead(position)

    override suspend fun toggleBookmark(position: QuranPosition) = source.toggleBookmark(position)

    override suspend fun setAdhkarCount(day: LocalDate, categoryId: String, dhikrId: Int, count: Int) =
        source.setAdhkarCount(day, "$categoryId/$dhikrId", count)

    override suspend fun resetAdhkar(day: LocalDate, categoryId: String) = source.resetAdhkar(day, categoryId)

    override suspend fun setTasbih(state: TasbihState) = source.setTasbih(state)

    override suspend fun setPrayed(day: LocalDate, prayer: Prayer, prayed: Boolean) {
        source.setPrayed(day, prayer, prayed)
        // The day's "all five prayers" challenge follows the tracker.
        if (ChallengeKind.PRAYERS in DailyChallengePicker.kindsFor(day)) {
            val all = source.activity.first().prayedOn(day).size >= PrayerTracker.prayers.size
            source.setChallengeDone(day, ChallengeKind.PRAYERS, all)
        }
    }

    override suspend fun setKhatmah(plan: KhatmahPlan?) = source.setKhatmah(plan)

    override suspend fun setChallengeDone(day: LocalDate, kind: ChallengeKind, done: Boolean) = source.setChallengeDone(day, kind, done)
}
