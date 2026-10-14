/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        serializer: UserPreferencesSerializer
    ): DataStore<UserPreferences> = DataStoreFactory.create(
        serializer = serializer,
        // A corrupt file must never brick the app: start over with defaults (onboarding again).
        corruptionHandler = ReplaceFileCorruptionHandler { serializer.defaultValue },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.dataStoreFile("user_preferences.json") }
    )

    @Provides
    @Singleton
    @ActivityStore
    fun provideActivityDataStore(
        @ApplicationContext context: Context,
        serializer: ActivityPreferencesSerializer
    ): DataStore<ActivityPreferences> = DataStoreFactory.create(
        serializer = serializer,
        // Losing a tasbih count or reading position is a nuisance, never a reason to crash.
        corruptionHandler = ReplaceFileCorruptionHandler { serializer.defaultValue },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.dataStoreFile("activity.json") }
    )
}

/** The activity DataStore (Quran position, adhkar counts, tasbih), as opposed to user settings. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ActivityStore
