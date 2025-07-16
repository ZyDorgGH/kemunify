package id.zydorg.kemunify.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore


object Preference {
    val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "user"
    )
}