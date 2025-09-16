package id.zydorg.kemunify.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.KemunifyDatabase
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.data.preference.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class DefaultKemunifyRepository @Inject constructor (
    private val database: KemunifyDatabase,
    private val userPreferences: UserPreferences
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

    override suspend fun deleteWaste(wasteName: String) {
        database.wasteDao().delete(wasteName)
    }

    override suspend fun updateWasteName(newWaste: String, oldName: String) {
        database.wasteDao().updateWasteName(oldName = oldName, newName = newWaste)
    }

    override suspend fun insertWasteName(wasteName: String) {
        val wasteEntity = WasteEntity(
            wasteName = wasteName,
            weightsJson = "{}" // JSON kosong
        )
        database.wasteDao().insert(wasteEntity)
    }
    override suspend fun initWasteDataIfEmpty() {
        val existingWaste = database.wasteDao().getAllWaste().first()
        if (existingWaste.isEmpty()){
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
                "Tutup botol galon",
                "bubblewarp",
                "galon aqua",
                "galon lemineral",
                "plastik bening",
                "thinwall",
                "besi kopong",
                "karung",
                "CD / toples akrilik",
                "baterai",
                "setrika",
                "kawat"
            )

            wasteTypes.forEach { wasteName ->
                val wasteEntity = WasteEntity(
                    wasteName = wasteName,
                    weightsJson = "{}" // JSON kosong
                )
                database.wasteDao().insert(wasteEntity)
            }
        }
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

    override suspend fun signInWithGoogle(
        context: Context,
        credential: CredentialManager
    ): Flow<User> {
        return flow {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("225718562840-rker96c90cnt52llmv6qcs04ogfokpui.apps.googleusercontent.com")
                    .setAutoSelectEnabled(true)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credential.getCredential(
                    request = request,
                    context = context
                )

                val user = handleSignIn(result)

                if (user != null) {
                    userPreferences.saveUserSession(user)
                    emit(user)
                } else {
                    throw Exception("Credential is not a valid Google ID Token")
                }

            } catch (e: Exception) {
                Log.e("AuthRepository", "Google Sign-In failed", e)
                throw e
            }
        }
    }

    override fun handleSignIn(result: GetCredentialResponse): User? {
        return when (val credential = result.credential) {

            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        val personId = googleIdTokenCredential.id
                        val displayName = googleIdTokenCredential.displayName
                        val personPhoto = googleIdTokenCredential.profilePictureUri

                        return User(
                            fullName = displayName.toString(),
                            email = personId,
                            profile = personPhoto.toString(),
                            isLogin = true
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("AuthRepository", "Invalid Google ID token", e)
                        null
                    }
                } else null
            }

            else -> {
                Log.e("AuthRepository", "Unexpected credential type: ${credential::class.java}")
                null
            }
        }
    }
}