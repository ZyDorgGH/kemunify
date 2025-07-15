package id.zydorg.kemunify.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

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
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                initWasteData(context)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        private suspend fun initWasteData(context: Context) {
            val wasteDao = getDatabase(context).wasteDao()

            // Daftar jenis sampah
            val wasteTypes = listOf(
                "Gelas bersih",
                "Botol bersih",
                "Plastik rongsok",
                "Kardus",
                "Kardus rongsok",
                "Kertas Putih",
                "Buku",
                "Kaleng aluminium/pocari",
                "Kaleng rongsok",
                "Aluminium/panci",
                "Besi",
                "kaca bening",
                "kaca warna",
                "Tutup botol kecil",
                "Tutup botol galon"
            )

            // Cek apakah data sudah ada
            if (wasteDao.getCount() == 0) {
                // Insert data untuk setiap jenis sampah
                wasteTypes.forEach { wasteName ->
                    val wasteEntity = WasteEntity(
                        wasteName = wasteName,
                        weightsJson = "{}" // JSON kosong
                    )
                    wasteDao.insert(wasteEntity)
                }
            }
        }
    }
}