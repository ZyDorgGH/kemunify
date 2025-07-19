package id.zydorg.kemunify.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.model.User
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface KemunifyRepository {
    suspend fun insertWaste(waste: WasteEntity)

    suspend fun updateWaste(waste: WasteEntity)

    suspend fun updateWasteWeight(wasteName: String, customerName: String, newWeight: BigDecimal)

    fun getAllWaste(): Flow<List<WasteEntity>>

    suspend fun deleteWaste(id: Int)

    suspend fun insertCustomer(customer: CustomerEntity)

    fun getAllCustomer(): Flow<List<CustomerEntity>>

    suspend fun delete(customerName: String)

    suspend fun deleteAllCustomers()

    suspend fun signInWithGoogle(context: Context, credential: CredentialManager): Flow<User>

    fun handleSignIn(result: GetCredentialResponse): User?
}