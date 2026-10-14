/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.BuildConfig
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import io.github.themeshmaker.dunya.core.model.DistributionChannel
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** [DistributionChannel] comes from the flavor-specific FlavorModule (src/play or src/foss). */
    @Provides
    fun provideAppBuildInfo(channel: DistributionChannel): AppBuildInfo = AppBuildInfo(
        versionName = BuildConfig.VERSION_NAME,
        channel = channel,
        isDebug = BuildConfig.DEBUG
    )

    /** UTC instants; every place that shows a time converts with an explicit ZoneId. */
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
