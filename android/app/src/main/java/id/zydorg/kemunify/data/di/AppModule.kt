package id.zydorg.kemunify.data.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.zydorg.kemunify.data.database.KemunifyDatabase
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.preference.userDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun kemunifyDatabase(app: Application): KemunifyDatabase{
        return KemunifyDatabase.getDatabase(app)
    }

    @Provides
    @Singleton
    fun userPreferences(app: Application) = UserPreferences.getInstance(app.userDataStore)

}