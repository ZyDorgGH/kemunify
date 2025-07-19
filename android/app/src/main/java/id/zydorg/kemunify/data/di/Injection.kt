package id.zydorg.kemunify.data.di

import android.content.Context
import id.zydorg.kemunify.data.database.KemunifyDatabase
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.preference.userDataStore
import id.zydorg.kemunify.data.repository.DefaultKemunifyRepository
import id.zydorg.kemunify.data.repository.KemunifyRepository


class Injection(
    context: Context
){
    private val database = KemunifyDatabase.getDatabase(context)

    val userPreferences = UserPreferences.getInstance(context.userDataStore)

    val userRepository: KemunifyRepository = DefaultKemunifyRepository(
        database = database,
        userPreferences = userPreferences
    )

}