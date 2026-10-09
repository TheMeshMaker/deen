package io.github.themeshmaker.dunya.core.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.model.Coordinates
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Location from Android itself, no Google Play services needed. Used by the FOSS build, and by
 * the Play build when Google's location doesn't answer (common on Chinese ROMs).
 *
 * A recent last-known fix is returned straight away (prayer times only need city accuracy);
 * otherwise every enabled provider is asked at once and the first answer wins.
 */
class PlatformLocationSource @Inject constructor(@ApplicationContext private val context: Context) : DeviceLocationSource {

    override suspend fun currentLocation(): Coordinates? {
        if (!context.hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        val lastKnown = lastKnown(manager, providers)
        if (lastKnown != null && lastKnown.ageMillis() <= FRESH_ENOUGH_MS) return lastKnown.toCoordinates()

        val current = withTimeoutOrNull(TIMEOUT_MS) { firstFix(manager, providers) }
        return (current ?: lastKnown)?.toCoordinates()
    }

    /** Races all providers; the losers are cancelled as soon as one answers. */
    private suspend fun firstFix(manager: LocationManager, providers: List<String>): Location? = channelFlow {
        providers.forEach { provider -> launch { send(currentFrom(manager, provider)) } }
        awaitClose()
    }.filterNotNull().firstOrNull()

    private suspend fun currentFrom(manager: LocationManager, provider: String): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                continuation.resume(location)
            }
        } catch (_: SecurityException) {
            // e.g. GPS with only coarse permission before Android 12
            continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            continuation.resume(null)
        }
    }

    private fun lastKnown(manager: LocationManager, providers: List<String>): Location? = providers
        .mapNotNull { provider ->
            try {
                manager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            }
        }
        .minByOrNull { it.ageMillis() }

    private companion object {
        const val TIMEOUT_MS = 20_000L
        const val FRESH_ENOUGH_MS = 30 * 60 * 1000L
    }
}

/** Age from the monotonic clock, so it is immune to wall-clock changes. */
fun Location.ageMillis(): Long = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000
