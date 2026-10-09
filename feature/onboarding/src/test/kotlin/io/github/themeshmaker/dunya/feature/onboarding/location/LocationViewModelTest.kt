package io.github.themeshmaker.dunya.feature.onboarding.location

import io.github.themeshmaker.dunya.core.domain.DeviceLocationResult
import io.github.themeshmaker.dunya.core.domain.LocationRepository
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UserData
import java.io.FileNotFoundException
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val riyadh = SavedLocation("Riyadh", "الرياض", "SA", Coordinates(24.69, 46.72), ZoneId.of("Asia/Riyadh"), LocationSource.DEVICE)

    private class FakeUserData : UserDataRepository {
        val state = MutableStateFlow(UserData(false, null, null))
        override val userData = state
        override suspend fun setLocation(location: SavedLocation) {
            state.value = state.value.copy(location = location)
        }
        override suspend fun setPrayerSettings(settings: PrayerSettings) = Unit
        override suspend fun setOnboardingComplete() = Unit
        override suspend fun setThemeStyle(style: ThemeStyle) = Unit
    }

    private class FakeLocations(
        val detect: suspend () -> DeviceLocationResult,
        val search: suspend (String) -> List<City> = { emptyList() }
    ) : LocationRepository {
        override suspend fun detectDeviceLocation() = detect()
        override suspend fun searchCities(query: String, limit: Int) = search(query)
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun detectedLocationIsSaved() = runTest {
        val userData = FakeUserData()
        val viewModel = LocationViewModel(FakeLocations(detect = { DeviceLocationResult.Found(riyadh) }), userData)
        val saved = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.locationSaved.first() }

        viewModel.detect()
        advanceUntilIdle()

        assertEquals(riyadh, userData.state.value.location)
        assertTrue(saved.isCompleted)
    }

    @Test
    fun brokenLocationLookupShowsFailureInsteadOfCrashing() = runTest {
        val viewModel = LocationViewModel(FakeLocations(detect = { throw FileNotFoundException("cities.tsv") }), FakeUserData())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.detect()
        advanceUntilIdle()

        assertEquals(Detection.FAILED, viewModel.uiState.value.detection)
    }

    @Test
    fun brokenSearchShowsNoResultsInsteadOfCrashing() = runTest {
        val viewModel = LocationViewModel(
            FakeLocations(detect = { DeviceLocationResult.Unavailable }, search = { throw FileNotFoundException("cities.tsv") }),
            FakeUserData()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("riy")
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals("riy", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.results.isEmpty())
    }
}
