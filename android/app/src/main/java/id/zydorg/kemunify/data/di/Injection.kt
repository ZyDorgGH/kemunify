package id.zydorg.kemunify.data.di

import android.content.Context
import id.zydorg.kemunify.data.database.KemunifyDatabase
import id.zydorg.kemunify.data.repository.DefaultKemunifyRepository
import id.zydorg.kemunify.data.repository.KemunifyRepository

class Injection(
    context: Context
){
    private val database = KemunifyDatabase.getDatabase(context)

    val userRepository: KemunifyRepository = DefaultKemunifyRepository(
        database = database,
    )
}