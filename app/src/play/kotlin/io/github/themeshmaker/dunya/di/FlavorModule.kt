package io.github.themeshmaker.dunya.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.location.DeviceLocationSource
import io.github.themeshmaker.dunya.core.location.FallbackLocationSource
import io.github.themeshmaker.dunya.core.location.PlatformLocationSource
import io.github.themeshmaker.dunya.core.location.gms.FusedLocationSource
import io.github.themeshmaker.dunya.core.model.DistributionChannel

/** Bindings for the Google Play build. Play Billing tips join here in M5. */
@Module
@InstallIn(SingletonComponent::class)
object FlavorModule {
    @Provides
    fun provideDistributionChannel(): DistributionChannel = DistributionChannel.PLAY

    /** Google's fused location first; Android's own providers when Play services don't answer. */
    @Provides
    fun provideDeviceLocationSource(fused: FusedLocationSource, platform: PlatformLocationSource): DeviceLocationSource =
        FallbackLocationSource(primary = fused, secondary = platform)
}
