package id.zydorg.kemunify.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waste_table")
data class WasteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val wasteName: String,
    val weightsJson: String
)

@Entity(tableName = "customer_table")
data class CustomerEntity(
    @PrimaryKey
    val customerName: String,
    val date: String
)