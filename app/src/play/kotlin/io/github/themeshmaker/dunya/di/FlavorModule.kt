package io.github.themeshmaker.dunya.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.model.DistributionChannel

/**
 * Bindings for the Google Play build. Later milestones bind the Play-only
 * implementations here (Fused location, Play Billing tips).
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorModule {
    @Provides
    fun provideDistributionChannel(): DistributionChannel = DistributionChannel.PLAY
}
