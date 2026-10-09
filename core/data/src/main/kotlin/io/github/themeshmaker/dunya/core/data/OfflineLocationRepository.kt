package io.github.themeshmaker.dunya.core.data

import io.github.themeshmaker.dunya.core.domain.DeviceLocationResult
import io.github.themeshmaker.dunya.core.domain.LocationRepository
import io.github.themeshmaker.dunya.core.location.CityDatabase
import io.github.themeshmaker.dunya.core.location.DeviceLocationSource
import io.github.themeshmaker.dunya.core.model.City
import io.github.themeshmaker.dunya.core.model.Coordinates
import io.github.themeshmaker.dunya.core.model.LocationSource
import io.github.themeshmaker.dunya.core.model.SavedLocation
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/** Device location named via the offline city database; nothing is sent anywhere. */
internal class OfflineLocationRepository @Inject constructor(
    private val deviceLocation: DeviceLocationSource,
    private val cities: CityDatabase
) : LocationRepository {

    override suspend fun detectDeviceLocation(): DeviceLocationResult {
        val coordinates = deviceLocation.currentLocation() ?: return DeviceLocationResult.Unavailable
        val nearest = cities.index().nearest(coordinates)
        return DeviceLocationResult.Found(savedLocationFor(coordinates, nearest?.first, nearest?.second))
    }

    override suspend fun searchCities(query: String, limit: Int): List<City> = cities.index().search(query, limit)

    companion object {
        /** Within this distance the device location is shown as the city's name. */
        const val NAME_RADIUS_KM = 50.0

        /** Within this distance we trust the city's country and time zone (deserts, coasts). */
        const val REGION_RADIUS_KM = 400.0

        internal fun savedLocationFor(coordinates: Coordinates, nearest: City?, distanceKm: Double?): SavedLocation {
            val named = nearest?.takeIf { distanceKm != null && distanceKm <= NAME_RADIUS_KM }
            val regional = nearest?.takeIf { distanceKm != null && distanceKm <= REGION_RADIUS_KM }
            return SavedLocation(
                name = named?.name ?: formatCoordinates(coordinates),
                arabicName = named?.arabicName,
                countryCode = regional?.countryCode,
                coordinates = coordinates,
                zoneId = regional?.zoneId ?: ZoneId.systemDefault(),
                source = LocationSource.DEVICE
            )
        }

        private fun formatCoordinates(c: Coordinates) = String.format(Locale.ROOT, "%.3f, %.3f", c.latitude, c.longitude)
    }
}
