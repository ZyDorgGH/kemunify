package id.zydorg.kemunify.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.KemunifyDatabase
import id.zydorg.kemunify.data.database.WasteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode

class DefaultKemunifyRepository(
    private val database: KemunifyDatabase
) : KemunifyRepository {
    override suspend fun insertWaste(waste: WasteEntity) {
        database.wasteDao().insert(waste)
    }

    override suspend fun updateWaste(waste: WasteEntity) {
        database.wasteDao().update(waste)
    }

    override suspend fun updateWasteWeight(
        wasteName: String,
        customerName: String,
        newWeight: BigDecimal
    ) {
        val waste = database.wasteDao().getWasteByName(wasteName) ?: return

        val weights = Gson().fromJson<MutableMap<String, String>>(
            waste.weightsJson,
            object : TypeToken<MutableMap<String, String>>() {}.type
        ) ?: mutableMapOf()

        weights[customerName] = newWeight.setScale(2, RoundingMode.HALF_UP).toString()

        database.wasteDao().update(
            waste.copy(weightsJson = Gson().toJson(weights))
        )
    }

    override fun getAllWaste(): Flow<List<WasteEntity>> = database.wasteDao().getAllWaste()

    override suspend fun deleteWaste(id: Int) {
        database.wasteDao().delete(id)
    }

    override suspend fun insertCustomer(customer: CustomerEntity) {
        database.customerDao().insert(customer)
    }

    override fun getAllCustomer(): Flow<List<CustomerEntity>> =
        database.customerDao().getAllCustomers()

    override suspend fun delete(customerName: String) {
        database.customerDao().delete(customerName)

        val allWaste = database.wasteDao().getAllWaste().first()
        allWaste.forEach { waste ->
            val weights = Gson().fromJson<MutableMap<String, String>>(
                waste.weightsJson,
                object : TypeToken<MutableMap<String, String>>() {}.type
            ) ?: mutableMapOf()

            weights.remove(customerName)

            database.wasteDao().update(
                waste.copy(weightsJson = Gson().toJson(weights))
            )
        }
    }

    override suspend fun deleteAllCustomers() {
        database.customerDao().deleteAllCustomers()

        val allWaste = database.wasteDao().getAllWaste().first()
        allWaste.forEach { waste ->
            database.wasteDao().update(
                waste.copy(weightsJson = "{}")
            )
        }
    }
}