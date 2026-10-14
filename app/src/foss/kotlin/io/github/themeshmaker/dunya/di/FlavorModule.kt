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
import io.github.themeshmaker.dunya.core.location.DeviceLocationSource
import io.github.themeshmaker.dunya.core.location.PlatformLocationSource
import io.github.themeshmaker.dunya.core.model.DistributionChannel

/**
 * Bindings for the FOSS build (F-Droid, GitHub Releases). Everything bound here must
 * be free software.
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorModule {
    @Provides
    fun provideDistributionChannel(): DistributionChannel = DistributionChannel.FOSS

    @Provides
    fun provideDeviceLocationSource(platform: PlatformLocationSource): DeviceLocationSource = platform
}
