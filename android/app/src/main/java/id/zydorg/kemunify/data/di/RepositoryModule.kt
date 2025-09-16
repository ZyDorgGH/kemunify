package id.zydorg.kemunify.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.zydorg.kemunify.data.repository.DefaultKemunifyRepository
import id.zydorg.kemunify.data.repository.KemunifyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindKemunnifyRepository(
        kemunifyRepository: DefaultKemunifyRepository
    ): KemunifyRepository
}