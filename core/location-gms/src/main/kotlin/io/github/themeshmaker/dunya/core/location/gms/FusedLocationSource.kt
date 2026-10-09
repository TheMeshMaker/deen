package io.github.themeshmaker.dunya.core.location.gms

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.location.DeviceLocationSource
import io.github.themeshmaker.dunya.core.location.ageMillis
import io.github.themeshmaker.dunya.core.location.hasLocationPermission
import io.github.themeshmaker.dunya.core.location.toCoordinates
import io.github.themeshmaker.dunya.core.model.Coordinates
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Google's fused location. Returns null (so the platform source is tried) if Play services are missing or slow. */
class FusedLocationSource @Inject constructor(@ApplicationContext private val context: Context) : DeviceLocationSource {

    override suspend fun currentLocation(): Coordinates? {
        if (!context.hasLocationPermission()) return null
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (availability != ConnectionResult.SUCCESS) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val last = client.lastLocation.await()
            if (last != null && last.ageMillis() <= FRESH_ENOUGH_MS) return last.toCoordinates()

            val tokenSource = CancellationTokenSource()
            val current = try {
                withTimeoutOrNull(TIMEOUT_MS) {
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).await()
                }
            } finally {
                tokenSource.cancel()
            }
            (current ?: last)?.toCoordinates()
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            // Permission revoked between the check above and the call.
            null
        } catch (_: Exception) {
            // ApiException, SecurityException, broken Play services on some ROMs: fall back.
            null
        }
    }

    private companion object {
        // Short: the platform source still gets its turn if Google doesn't answer.
        const val TIMEOUT_MS = 10_000L
        const val FRESH_ENOUGH_MS = 30 * 60 * 1000L
    }
}
