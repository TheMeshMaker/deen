package io.github.themeshmaker.dunya.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.model.DistributionChannel

/**
 * Bindings for the FOSS build (F-Droid, GitHub Releases). Everything bound here must
 * be free software: platform location, external donation links.
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorModule {
    @Provides
    fun provideDistributionChannel(): DistributionChannel = DistributionChannel.FOSS
}
