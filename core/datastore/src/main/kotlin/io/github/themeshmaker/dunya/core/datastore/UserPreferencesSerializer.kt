/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class UserPreferencesSerializer @Inject constructor() : Serializer<UserPreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences = try {
        json.decodeFromString(UserPreferences.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable user preferences", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Unreadable user preferences", e)
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        output.write(json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray())
    }
}
