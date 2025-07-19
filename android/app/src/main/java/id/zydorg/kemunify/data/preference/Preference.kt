package id.zydorg.kemunify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.zydorg.kemunify.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user")
class UserPreferences private constructor(private val dataStore: DataStore<Preferences>){
    private val FULL_NAME = stringPreferencesKey("full_name")
    private val EMAIL = stringPreferencesKey("email")
    private val PROFILE = stringPreferencesKey("profile")
    private val IS_LOGIN_KEY = booleanPreferencesKey("isLogin")

    fun getUserSession(): Flow<User> {
        return dataStore.data.map { preferences->
            User(
                preferences[FULL_NAME] ?: "",
                preferences[EMAIL] ?: "",
                preferences[PROFILE] ?: "",
                preferences[IS_LOGIN_KEY] ?: false
            )
        }
    }

    suspend fun saveUserSession(user: User){
        dataStore.edit { preferences ->
            preferences[FULL_NAME] = user.fullName
            preferences[EMAIL] = user.email
            preferences[PROFILE] = user.profile
            preferences[IS_LOGIN_KEY] = true
        }
    }

    suspend fun logout(){
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(dataStore: DataStore<Preferences>): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(dataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}