package io.github.themeshmaker.dunya.core.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.SoftReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads the bundled city list (assets/cities.tsv, ~64k places) on first use. The index takes
 * ~15 MB, so it is held softly: the system can reclaim it once location setup is done, and it
 * is never loaded by background work such as alarms.
 */
@Singleton
class CityDatabase @Inject constructor(@ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private var cached: SoftReference<CityIndex>? = null

    suspend fun index(): CityIndex = mutex.withLock {
        cached?.get() ?: withContext(Dispatchers.IO) {
            context.assets.open(ASSET).use { CityIndex.parse(it) }
        }.also { cached = SoftReference(it) }
    }

    private companion object {
        const val ASSET = "cities.tsv"
    }
}
