package id.zydorg.kemunify.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WasteDao {
    @Insert
    suspend fun insert(waste: WasteEntity)

    @Query("SELECT COUNT(*) FROM waste_table")
    suspend fun getCount(): Int

    @Update
    suspend fun update(waste: WasteEntity)

    @Query("SELECT * FROM waste_table WHERE wasteName = :wasteName LIMIT 1")
    suspend fun getWasteByName(wasteName: String): WasteEntity?

    @Query("SELECT * FROM waste_table")
    fun getAllWaste(): Flow<List<WasteEntity>>

    @Query("DELETE FROM waste_table WHERE id = :id")
    suspend fun delete(id: Int)

}

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)

    @Query("SELECT * FROM customer_table")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("DELETE FROM customer_table WHERE customerName = :customerName")
    suspend fun delete(customerName: String)


    @Query("DELETE FROM customer_table")
    suspend fun deleteAllCustomers()

}