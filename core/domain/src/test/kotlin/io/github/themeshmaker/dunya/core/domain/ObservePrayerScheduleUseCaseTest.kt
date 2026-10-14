package io.github.themeshmaker.dunya.core.domain

import app.cash.turbine.test
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.CalendarType
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LiquidSettings
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.model.UserData
import io.github.themeshmaker.dunya.core.prayer.HijriCalendar
import io.github.themeshmaker.dunya.core.prayer.PrayerTimeCalculator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservePrayerScheduleUseCaseTest {

    private val riyadh = SavedLocation(
        name = "Riyadh",
        arabicName = "الرياض",
        countryCode = "SA",
        coordinates = Coordinates(24.6877, 46.7219),
        zoneId = ZoneId.of("Asia/Riyadh"),
        source = LocationSource.MANUAL
    )
    private val settings = PrayerSettings(CalculationMethod.UMM_AL_QURA)

    private class FakeUserDataRepository(initial: UserData) : UserDataRepository {
        val state = MutableStateFlow(initial)
        override val userData = state
        override suspend fun setLocation(location: SavedLocation) = state.update { it.copy(location = location) }
        override suspend fun setPrayerSettings(settings: PrayerSettings) = state.update { it.copy(prayerSettings = settings) }
        override suspend fun setOnboardingComplete() = state.update { it.copy(onboardingComplete = true) }
        override suspend fun setCustomColors(colors: io.github.themeshmaker.dunya.core.model.CustomColors) = Unit

        override suspend fun setThemeStyle(style: ThemeStyle) = state.update { it.copy(themeStyle = style) }
        override suspend fun setAlertSettings(settings: AlertSettings) = state.update { it.copy(alertSettings = settings) }
        override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
        override suspend fun setUiStyle(style: UiStyle) = state.update { it.copy(uiStyle = style) }
        override suspend fun setSecretsUnlocked(unlocked: Boolean) = state.update { it.copy(secretsUnlocked = unlocked) }
        override suspend fun setTimetableCalendar(calendar: CalendarType) = state.update { it.copy(timetableCalendar = calendar) }
        override suspend fun setReadingSettings(settings: ReadingSettings) = state.update { it.copy(reading = settings) }
        override suspend fun setLiquidSettings(settings: LiquidSettings) = state.update { it.copy(liquid = settings) }
        override suspend fun setReliabilityStepConfirmed(step: String, confirmed: Boolean) = Unit
        private inline fun MutableStateFlow<UserData>.update(f: (UserData) -> UserData) {
            value = f(value)
        }
    }

    /** A clock driven by the coroutine test scheduler, so `delay` and time move together. */
    private class SchedulerClock(private val scope: TestScope, private val start: Instant) : Clock() {
        override fun instant(): Instant = start.plusMillis(scope.testScheduler.currentTime)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }

    private fun TestScope.useCase(repository: UserDataRepository, start: Instant) =
        ObservePrayerScheduleUseCase(repository, PrayerTimeCalculator(HijriCalendar()), HijriCalendar(), SchedulerClock(this, start))

    @Test
    fun emitsNullUntilLocationAndMethodAreSet() = runTest {
        val repository = FakeUserDataRepository(UserData(false, null, null))
        useCase(repository, Instant.parse("2026-10-09T09:00:00Z"))().test {
            assertNull(awaitItem())
            repository.setLocation(riyadh)
            expectNoEvents() // still no method
            repository.setPrayerSettings(settings)
            assertEquals(LocalDate.of(2026, 10, 9), awaitItem()!!.date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recalculatesAfterLocalMidnight() = runTest {
        val repository = FakeUserDataRepository(UserData(true, riyadh, settings))
        // 23:59:30 in Riyadh (UTC+3)
        useCase(repository, Instant.parse("2026-10-09T20:59:30Z"))().test {
            assertEquals(LocalDate.of(2026, 10, 9), awaitItem()!!.date)
            testScheduler.advanceTimeBy(31_000)
            assertEquals(LocalDate.of(2026, 10, 10), awaitItem()!!.date)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
