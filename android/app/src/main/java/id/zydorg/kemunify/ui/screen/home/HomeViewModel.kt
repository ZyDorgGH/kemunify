package id.zydorg.kemunify.ui.screen.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val wasteRepository: KemunifyRepository,
    private val userPreferences: UserPreferences,
): ViewModel() {


    private val _customerUiState: MutableStateFlow<UiState<List<CustomerEntity>>> =
        MutableStateFlow(UiState.Loading)
    val customerUiState: StateFlow<UiState<List<CustomerEntity>>>
        get() = _customerUiState

    private val _userUiState: MutableStateFlow<UiState<User>> =
        MutableStateFlow(UiState.Loading)
    val userUiState: StateFlow<UiState<User>>
        get() = _userUiState

    fun fetchCustomer(){
        viewModelScope.launch {
            wasteRepository.getAllCustomer()
                .catch {e ->
                    _customerUiState.value = UiState.Error(e.message.toString())
                }
                .collect{customer ->
                    _customerUiState.value = UiState.Success(customer)
                }
        }
    }

    fun fetchUser(){
        viewModelScope.launch {
            userPreferences.getUserSession()
                .catch {e ->
                    _userUiState.value = UiState.Error(e.message.toString())
                }
                .collect{user ->
                    _userUiState.value = UiState.Success(user)
                }
        }
    }

    fun deleteCustomer(customer: String){
        viewModelScope.launch {
            wasteRepository.delete(customer)
        }
    }

    fun deleteAllCustomers(){
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

    fun logout() {
        viewModelScope.launch {
            userPreferences.logout()
        }
    }

}