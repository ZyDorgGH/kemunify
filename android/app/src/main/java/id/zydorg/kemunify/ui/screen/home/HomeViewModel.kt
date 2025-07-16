package id.zydorg.kemunify.ui.screen.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.CustomerUiState
import id.zydorg.kemunify.ui.screen.waste.WasteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HomeViewModel(
    private val wasteRepository: KemunifyRepository,
    private val userPreferencesDataStore: DataStore<Preferences>
): ViewModel() {

    val customerUiState : StateFlow<CustomerUiState> =
        wasteRepository.getAllCustomer()
            .map { CustomerUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WasteViewModel.MILLIS),
                initialValue = CustomerUiState()
            )

    suspend fun updateWaste(waste: WasteEntity){
        viewModelScope.launch {
            wasteRepository.updateWaste(waste)
        }
    }

    suspend fun deleteCustomer(customer: String){
        viewModelScope.launch {
            wasteRepository.delete(customer)
        }
    }

    suspend fun deleteAllCustomers(){
        viewModelScope.launch {
            wasteRepository.deleteAllCustomers()
        }
    }

    suspend fun exportToExcel(context: Context): File? {
        try {
            val workbook = XSSFWorkbook()
            val folder = File(context.filesDir, "exported_files")
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val formatter = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault())
            val formattedTime = formatter.format(Date())
            val fileName = "rekap_sampah_$formattedTime.xlsx"
            val file = File(folder, fileName)

            return try {
                withContext(Dispatchers.IO) {
                    // Isi data ke workbook
                    exportWaste(workbook)

                    // Simpan ke file
                    FileOutputStream(file).use { outputStream ->
                        workbook.write(outputStream)
                    }

                    // Dapatkan URI file
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    // intent untuk membuka/membagikan
                    createAndLaunchShareIntent(context, fileUri)
                    workbook.close()

                    file
                }

            } catch (e: Exception) {
                Log.e("ExcelExport", "Error during export", e)
                showErrorToast(context, "Export failed: ${e.message}")
                return null
            } finally {
                workbook.close()
            }
        } catch (e: Exception) {
            Log.e("ExcelExport", "General error", e)
            showErrorToast(context, "Export failed: ${e.message}")
            return null
        }
    }

    private fun createAndLaunchShareIntent(context: Context, fileUri: Uri) {
        try {
            // Intent untuk membuka file
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Intent untuk membagikan file
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Gabungkan keduanya dalam chooser
            val chooserIntent = Intent.createChooser(shareIntent, "Open or Share File").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Jalankan intent
            context.startActivity(chooserIntent)

        } catch (e: ActivityNotFoundException) {
            Log.e("ExcelExport", "No app to open Excel files", e)
            showErrorToast(context, "No app available to open Excel files")
        } catch (e: SecurityException) {
            Log.e("ExcelExport", "Permission denied", e)
            showErrorToast(context, "Permission denied to open file")
        } catch (e: Exception) {
            Log.e("ExcelExport", "Error sharing file", e)
            showErrorToast(context, "Failed to share file: ${e.message}")
        }
    }

    private fun showErrorToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun exportWaste(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("Data Sampah")
        val headerStyle = createHeaderStyle(workbook)
        val headerRow = sheet.createRow(0)

        val wasteData = wasteRepository.getAllWaste().first()
        val customers = wasteRepository.getAllCustomer().first()

        createHeaderCells(headerRow, headerStyle, customers)
        fillDataRows(sheet, wasteData, customers)
    }

    private fun createHeaderCells(
        headerRow: Row,
        headerStyle: CellStyle,
        customers: List<CustomerEntity>
    ) {
        // Kolom 1: No
        headerRow.createCell(0).apply {
            setCellValue("No")
            setCellStyle(headerStyle)
        }

        // Kolom 2: Nama Sampah
        headerRow.createCell(1).apply {
            setCellValue("Nama Sampah")
            setCellStyle(headerStyle)
        }

        // Kolom untuk setiap nasabah
        customers.forEachIndexed { index, customer ->
            headerRow.createCell(index + 2).apply {
                setCellValue(customer.customerName)
                setCellStyle(headerStyle)
            }
        }
    }

    private fun fillDataRows(
        sheet: Sheet,
        wasteData: List<WasteEntity>,
        customers: List<CustomerEntity>
    ) {
        val gson = Gson()
        wasteData.forEachIndexed { rowIndex, waste ->
            val row = sheet.createRow(rowIndex + 1)

            // Kolom 1: Nomor
            row.createCell(0).setCellValue((rowIndex + 1).toDouble())

            // Kolom 2: Nama Sampah
            row.createCell(1).setCellValue(waste.wasteName)

            // Parse weights JSON
            val weights = gson.fromJson<Map<String, String>>(
                waste.weightsJson,
                object : TypeToken<Map<String, String>>() {}.type
            ) ?: emptyMap()

            // Data berat untuk setiap nasabah
            customers.forEachIndexed { colIndex, customer ->
                val weight = weights[customer.customerName] ?: "0.00"
                row.createCell(colIndex + 2).setCellValue(weight)
            }
        }
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val headerFont: Font = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 12
            color = IndexedColors.WHITE.index
        }

        return workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.MEDIUM
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.MEDIUM
            borderRight = BorderStyle.MEDIUM
        }
    }

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold(""){str,it -> str +"%02x".format(it)}

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("225718562840-rker96c90cnt52llmv6qcs04ogfokpui.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                handleSignIn(result)
            } catch (e: GetCredentialException){
                Log.e("Credential Error", "Error getting credential", e)
            }
        }
    }


    private fun handleSignIn(result: GetCredentialResponse){
        when(val credential = result.credential){

            is PublicKeyCredential -> {
                val responseJson = credential.authenticationResponseJson
            }
            // Password Credential
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
            }

            //GoogleToken credential
            is CustomCredential -> {
                if(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                    try {

                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        val googleIdToken = googleIdTokenCredential.idToken
                        Log.i("answer", googleIdToken)
                        val personId = googleIdTokenCredential.id
                        Log.i("answer", personId)
                        val displayName = googleIdTokenCredential.displayName
                        Log.i("answer", displayName.toString())
                        val personPhoto = googleIdTokenCredential.profilePictureUri
                        Log.i("answer", personPhoto.toString())
                        viewModelScope.launch {
                            saveData("email", personId)
                        }

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("Error Token", "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e("Error Credential Type", "Unexpected type of credential")
                }
            }
            else ->{
                Log.e("Error Credential Type", "Unexpected type of credential")
            }
        }
    }

    fun requestDriveAuthorization(
        context: Context,
        authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val requestedScopes = listOf(Scope(DriveScopes.DRIVE_FILE))
        val authorizationRequest =
            AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .build()

        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .addOnSuccessListener{
                Log.d("DriveAuth", "Authorization success. Has resolution? ${it.hasResolution()}")
                if(it.hasResolution()){
                    val pendingIntent = it.pendingIntent
                    val intentSenderRequest = pendingIntent?.intentSender?.let { it1 ->
                        IntentSenderRequest.Builder(
                            it1
                        ).build()
                    }
                    intentSenderRequest?.let { it1 -> authorizationLauncher.launch(it1) }

                } else {
                    Toast.makeText(
                        context,
                        "Accses already granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener { Toast.makeText(context, "Failure: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    private suspend fun saveData(key: String, text: String){
        userPreferencesDataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = text
        }
    }

    suspend fun deleteData(){
        userPreferencesDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getData(key: String): String? {
        val value = userPreferencesDataStore.data
            .map {
                it[stringPreferencesKey(key)]
            }.firstOrNull()

        return value
    }
}