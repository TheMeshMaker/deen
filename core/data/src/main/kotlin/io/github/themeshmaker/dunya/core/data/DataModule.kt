package io.github.themeshmaker.dunya.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.themeshmaker.dunya.core.domain.LocationRepository
import io.github.themeshmaker.dunya.core.domain.UserDataRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindUserDataRepository(impl: OfflineUserDataRepository): UserDataRepository

    @Binds
    abstract fun bindLocationRepository(impl: OfflineLocationRepository): LocationRepository
}
