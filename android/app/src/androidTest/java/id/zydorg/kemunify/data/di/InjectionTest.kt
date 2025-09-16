package id.zydorg.kemunify.data.di

import android.content.Context
import androidx.room.Room
import id.zydorg.kemunify.data.database.KemunifyDatabase
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.preference.userDataStore
import id.zydorg.kemunify.data.repository.DefaultKemunifyRepository
import id.zydorg.kemunify.data.repository.KemunifyRepository

class InjectionTest (
    context: Context
) {
    val database: KemunifyDatabase = Room.inMemoryDatabaseBuilder(
        context.applicationContext,
        KemunifyDatabase::class.java
    )
        .allowMainThreadQueries() // Safe untuk testing
        .build()

    val userPreferences = UserPreferences.getInstance(context.userDataStore)

    val userRepository: KemunifyRepository = DefaultKemunifyRepository(
        database = database,
        userPreferences = userPreferences
    )
}