package id.zydorg.kemunify.ui.screen.detail

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.MainApplication
import id.zydorg.kemunify.data.converter.toBigDecimalOrZero
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.factory.ViewModelFactory
import id.zydorg.kemunify.ui.theme.WhiteSmoke
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    customerId: String,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(
        factory = ViewModelFactory(MainApplication.injection)
    ),
) {
    val wasteState by viewModel.wasteUiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var expandedListMore by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var customerToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rekap Data $customerId",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        navigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = {
                            expandedListMore = true
                            customerToDelete = customerId
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Home",
                                tint = Color.DarkGray,
                            )
                        }
                        DropdownMenu(expanded = expandedListMore, onDismissRequest = { expandedListMore = false }) {
                            DropdownMenuItem(
                                text = { Text("Hapus Tabel $customerId", color = Color.Red) },
                                onClick = {
                                    expanded = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        tint = Color.Red,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            val totalWeight = remember(wasteState.wastes, customerId) {
                wasteState.wastes.fold(BigDecimal.ZERO) { acc, waste ->
                    val weights = Gson().fromJson<Map<String, String>>(
                        waste.weightsJson,
                        object : TypeToken<Map<String, String>>() {}.type
                    ) ?: emptyMap()

                    val weightStr = weights[customerId] ?: "0.00"
                    try {
                        acc + BigDecimal(weightStr.replace(",", "."))
                    } catch (e: Exception) {
                        acc
                    }
                }.setScale(2, RoundingMode.HALF_UP)
            }

            DetailContent(wastes = wasteState.wastes, customerName = customerId)

            Text(
                "Total Berat Sampah: $totalWeight kg",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Hapus Nasabah") },
            text = { Text("Apakah Anda yakin ingin menghapus nasabah $customerId?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerToDelete != null){
                            coroutineScope.launch {
                                viewModel.deleteCustomer(customerToDelete!!)
                            }
                        }
                        expanded = false
                        navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                Button(onClick = { expanded = false }) {
                    Text("Batal")
                }
            }
        )
    }

}

@Composable
private fun DetailContent(
    wastes: List<WasteEntity>,
    customerName: String,
    modifier: Modifier = Modifier,
){
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = modifier
                        .padding(16.dp)
                        .background(WhiteSmoke.copy(alpha = 0.7f))
                        .fillMaxWidth()
                ) {
                    Text(
                        "Nama Sampah",
                        modifier = Modifier.weight(3f)
                    )
                    Text(
                        "Berat",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        " ",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
                Divider()
                Column {
                    wastes.forEachIndexed { index, waste ->
                        val backgroundColor = if (index % 2 == 0) {
                            WhiteSmoke.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }

                        Box(
                            modifier = Modifier
                                .background(backgroundColor)
                                .fillMaxWidth()
                        ) {
                            DetailItem(waste = waste, customerName = customerName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    waste: WasteEntity,
    customerName:String,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel(
        factory = ViewModelFactory(MainApplication.injection)
    ),
){
    data class EditWeightData(
        val wasteName: String,
        val customerName: String,
        val currentWeight: String
    )

    var showEditDialog by remember { mutableStateOf(false) }
    var editingWeightData by remember { mutableStateOf<EditWeightData?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val weights = Gson().fromJson<Map<String, String>>(
        waste.weightsJson,
        object : TypeToken<Map<String, String>>() {}.type
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        val weightValue = weights[customerName] ?: "0.00"
        Text(
            waste.wasteName,
            modifier = Modifier
                .padding(16.dp)
                .weight(3f)
        )
        Text(
            weightValue,
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            "Edit",
            modifier = Modifier
                .weight(1f)
                .clickable {
                    editingWeightData = EditWeightData(
                        wasteName = waste.wasteName,
                        customerName = customerName,
                        currentWeight = weightValue
                    )
                    Log.d("data in editingWeightData", "There is $editingWeightData")
                    showEditDialog = true
                },
            color = Color.Blue.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    if (showEditDialog && editingWeightData != null) {
        EditWeightDialog(
            currentWeight = editingWeightData!!.currentWeight,
            onDismiss = {
                showEditDialog = false
                editingWeightData = null
            },
            onSave = { newWeight ->
                coroutineScope.launch {
                    if (editingWeightData != null){
                        viewModel.updateWasteWeight(
                            wasteName = editingWeightData!!.wasteName,
                            customerName = editingWeightData!!.customerName,
                            newWeight = newWeight.toBigDecimalOrZero()
                        )
                    } else {
                        Log.d("data in editingWeightData", "data kosong $editingWeightData")
                    }

                }
                showEditDialog = false
            }
        )
    }

}

@Composable
fun EditWeightDialog(
    currentWeight: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var weightInput by remember { mutableStateOf(currentWeight) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Berat Sampah") },
        text = {
            Column {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        weightInput = it
                        // Validasi input
                        if (it.isNotBlank() && it.toBigDecimalOrNull() == null) {
                            errorMessage = "Format angka salah (contoh: 1,25)"
                        } else {
                            errorMessage = null
                        }
                    },
                    label = { Text("Berat (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (errorMessage == null && weightInput.isNotBlank()) {
                        onSave(weightInput)
                    }
                },
                enabled = errorMessage == null && weightInput.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

