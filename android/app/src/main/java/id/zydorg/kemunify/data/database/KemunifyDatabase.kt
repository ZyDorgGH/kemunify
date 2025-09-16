package id.zydorg.kemunify.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.InternalCoroutinesApi

@Database(entities = [WasteEntity::class, CustomerEntity::class], version = 1, exportSchema = false)
abstract class KemunifyDatabase : RoomDatabase(){

    abstract fun wasteDao(): WasteDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: KemunifyDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        @JvmStatic
        fun getDatabase(context: Context): KemunifyDatabase {
            return INSTANCE ?: kotlinx.coroutines.internal.synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KemunifyDatabase::class.java,
                    "kemunify_database"
                )
                    .fallbackToDestructiveMigration()
                    .createFromAsset("database/kemunify_database.db")
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}