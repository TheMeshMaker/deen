package io.github.themeshmaker.dunya.feature.onboarding.location

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.DeviceLocationResult
import io.github.themeshmaker.dunya.core.domain.LocationRepository
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.SavedLocation
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Detection {
    IDLE,
    DETECTING,
    FAILED,
    PERMISSION_DENIED
}

data class LocationUiState(val query: String = "", val results: List<City> = emptyList(), val detection: Detection = Detection.IDLE)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val detection = MutableStateFlow(Detection.IDLE)
    private val saved = Channel<Unit>(Channel.BUFFERED)

    /** Fires once the chosen location has been stored. */
    val locationSaved: Flow<Unit> = saved.receiveAsFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val results = query
        .debounce(SEARCH_DEBOUNCE_MS)
        .mapLatest { q -> if (q.isBlank()) emptyList() else safely(emptyList()) { locationRepository.searchCities(q) } }

    val uiState: StateFlow<LocationUiState> = combine(query, results, detection) { q, cities, d ->
        LocationUiState(query = q, results = if (q.isBlank()) emptyList() else cities, detection = d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onPermissionDenied() {
        detection.value = Detection.PERMISSION_DENIED
    }

    fun detect() {
        if (detection.value == Detection.DETECTING) return
        viewModelScope.launch {
            detection.value = Detection.DETECTING
            when (val result = safely(DeviceLocationResult.Unavailable) { locationRepository.detectDeviceLocation() }) {
                is DeviceLocationResult.Found -> {
                    detection.value = Detection.IDLE
                    save(result.location)
                }

                DeviceLocationResult.Unavailable -> detection.value = Detection.FAILED
            }
        }
    }

    fun select(city: City) {
        viewModelScope.launch {
            save(
                SavedLocation(
                    name = city.name,
                    arabicName = city.arabicName,
                    countryCode = city.countryCode,
                    coordinates = city.coordinates,
                    zoneId = city.zoneId,
                    source = LocationSource.MANUAL
                )
            )
        }
    }

    private suspend fun save(location: SavedLocation) {
        userDataRepository.setLocation(location)
        saved.send(Unit)
    }

    /** A failed lookup must show a message, never crash the app. */
    private suspend fun <T> safely(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Location lookup failed", e)
        fallback
    }

    private companion object {
        const val TAG = "LocationViewModel"
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}
