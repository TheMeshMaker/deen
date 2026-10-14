/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.datastore.AlertPreferences
import io.github.themeshmaker.dunya.core.datastore.LocationPreferences
import io.github.themeshmaker.dunya.core.datastore.PrayerPreferences
import io.github.themeshmaker.dunya.core.datastore.enumOrNull
import io.github.themeshmaker.dunya.core.datastore.toModel
import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.AlertLogEntry
import io.github.themeshmaker.dunya.core.model.AlertOutcome
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.PlannedAlert
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.model.SavedLocation
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Everything the alarm pipeline needs, kept in *device-protected* storage so it is readable
 * before the user unlocks the phone after a reboot (direct boot). The app copies the user's
 * settings in here whenever they change (see [AlarmSync]).
 */
@Serializable
internal data class AlarmState(
    val location: LocationPreferences? = null,
    val prayer: PrayerPreferences? = null,
    val alerts: AlertPreferences? = null,
    val planned: List<StoredAlert> = emptyList(),
    val log: List<StoredLogEntry> = emptyList()
)

@Serializable
internal data class StoredAlert(val prayer: String, val kind: String, val at: Long, val prayerAt: Long)

@Serializable
internal data class StoredLogEntry(
    val prayer: String? = null,
    val kind: String,
    val scheduledAt: Long,
    val firedAt: Long,
    val outcome: String
)

/** Inputs for planning, decoded. Null until onboarding has produced a location and a method. */
data class AlarmInputs(val location: SavedLocation, val prayerSettings: PrayerSettings, val alertSettings: AlertSettings)

@Singleton
class AlarmStateStore @Inject constructor(@ApplicationContext context: Context) {

    private val store = DataStoreFactory.create(
        serializer = AlarmStateSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { AlarmState() },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.createDeviceProtectedStorageContext().dataStoreFile("alarm_state.json") }
    )

    suspend fun inputs(): AlarmInputs? {
        val state = store.data.first()
        val location = state.location?.toModel() ?: return null
        val prayer = state.prayer?.toModel() ?: return null
        return AlarmInputs(location, prayer, state.alerts?.toModel() ?: AlertSettings())
    }

    internal suspend fun setInputs(location: LocationPreferences?, prayer: PrayerPreferences?, alerts: AlertPreferences?) {
        store.updateData { it.copy(location = location, prayer = prayer, alerts = alerts) }
    }

    suspend fun planned(): List<PlannedAlert> = store.data.first().planned.mapNotNull { it.toModel() }

    internal suspend fun setPlanned(alerts: List<PlannedAlert>) {
        store.updateData { state ->
            state.copy(planned = alerts.map { StoredAlert(it.prayer.name, it.kind.name, it.at.toEpochMilli(), it.prayerAt.toEpochMilli()) })
        }
    }

    /** Newest first. */
    val log: Flow<List<AlertLogEntry>> = store.data.map { state -> state.log.mapNotNull { it.toModel() } }

    suspend fun appendLog(entry: AlertLogEntry) {
        store.updateData { state ->
            val stored = StoredLogEntry(
                prayer = entry.prayer?.name,
                kind = entry.kind.name,
                scheduledAt = entry.scheduledAt.toEpochMilli(),
                firedAt = entry.firedAt.toEpochMilli(),
                outcome = entry.outcome.name
            )
            state.copy(log = (listOf(stored) + state.log).take(MAX_LOG_ENTRIES))
        }
    }

    private companion object {
        const val MAX_LOG_ENTRIES = 100
    }
}

private fun StoredAlert.toModel(): PlannedAlert? {
    val prayer = enumOrNull<Prayer>(prayer) ?: return null
    val kind = enumOrNull<AlertKind>(kind) ?: return null
    return PlannedAlert(prayer, kind, Instant.ofEpochMilli(at), Instant.ofEpochMilli(prayerAt))
}

private fun StoredLogEntry.toModel(): AlertLogEntry? {
    val kind = enumOrNull<AlertKind>(kind) ?: return null
    val outcome = enumOrNull<AlertOutcome>(outcome) ?: return null
    return AlertLogEntry(
        prayer = prayer?.let { enumOrNull<Prayer>(it) },
        kind = kind,
        scheduledAt = Instant.ofEpochMilli(scheduledAt),
        firedAt = Instant.ofEpochMilli(firedAt),
        outcome = outcome
    )
}

private object AlarmStateSerializer : Serializer<AlarmState> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue = AlarmState()

    override suspend fun readFrom(input: InputStream): AlarmState = try {
        json.decodeFromString(AlarmState.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable alarm state", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Unreadable alarm state", e)
    }

    override suspend fun writeTo(t: AlarmState, output: OutputStream) {
        output.write(json.encodeToString(AlarmState.serializer(), t).encodeToByteArray())
    }
}
