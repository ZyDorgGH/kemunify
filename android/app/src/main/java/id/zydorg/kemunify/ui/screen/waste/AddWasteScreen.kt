package id.zydorg.kemunify.ui.screen.waste

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.zydorg.kemunify.data.converter.isWeightValidDecimal
import id.zydorg.kemunify.data.converter.toWeightBigDecimalOrZero
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.ui.common.UiState
import id.zydorg.kemunify.ui.theme.DarkGreen
import id.zydorg.kemunify.ui.theme.LightGreen40
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWasteScreen(
    onNavigateUp: () -> Unit,
    viewModel: WasteViewModel =  hiltViewModel(),
){

    var customerName by remember { mutableStateOf("") }
    val weights = remember { mutableStateMapOf<String, String>() }
    val weightErrors = remember { mutableStateMapOf<String, String>() }
    val coroutineScope = rememberCoroutineScope()
    var wasteData by remember { mutableStateOf<List<WasteEntity>>(emptyList()) }
    val context = LocalContext.current

    viewModel.wasteUiState.collectAsState(initial = UiState.Loading).value.let { uiState ->
        when (uiState) {
            is UiState.Loading -> {
                viewModel.fetchWaste()
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                wasteData = uiState.data
            }

            is UiState.Error -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Gagal Memuat Data", color = Color.DarkGray)
                }
            }
        }
    }

    val wasteTypes = wasteData.map { it.wasteName }
    LaunchedEffect(wasteTypes) {
        if (wasteTypes.isNotEmpty()){
            wasteTypes.forEach { wasteType ->
                weights[wasteType] = "0.00"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manajemen Sampah",
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(
                        onClick = {  },
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Hapus Semua Nasabah",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            )
        }
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Input Nama Nasabah
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Nama Nasabah") },
                modifier = Modifier
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth().semantics { contentDescription = "Input Customer" },
                isError = customerName.isBlank()
            )

            if (customerName.isBlank()) {
                Text(
                    "Nama nasabah harus diisi",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }

            // Input Berat untuk Setiap Jenis Sampah
            wasteTypes.forEach { wasteType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "$wasteType:",
                        modifier = Modifier.width(150.dp),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = weights[wasteType] ?: "0.00",
                        onValueChange = {
                            weights[wasteType] = it
                            if (it.isNotBlank() && !isWeightValidDecimal(it)) {
                                weightErrors[wasteType] = "Harus angka (0.00)"
                            } else {
                                weightErrors.remove(wasteType)
                            }
                        },
                        modifier = Modifier.weight(1f).semantics { contentDescription = wasteType },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = weightErrors.containsKey(wasteType)
                    )
                }

                weightErrors[wasteType]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 158.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

            }

            Row(
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f, true),
                    onClick = onNavigateUp
                ) {
                    Text(text = "Cancel")
                }

                FilledTonalButton(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f, true)
                        .testTag("Save Button"),
                    onClick = {
                        val bigDecimalWeights = weights.mapValues { (_, value) ->
                            value.toWeightBigDecimalOrZero()
                        }
                        val formatter = SimpleDateFormat("dd/MM/yyyy HH.mm", Locale.getDefault())
                        val formattedTime = formatter.format(Date())
                        coroutineScope.launch {
                            viewModel.addCustomerWithWeights(
                                name = customerName,
                                weights = bigDecimalWeights,
                                date = formattedTime,
                                context = context
                            )
                            Log.e("Weights", "Error initializing weights: $weights")
                        }
                        onNavigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = DarkGreen,
                        containerColor = LightGreen40
                    )
                ) {
                    Text(text = "Save")
                }
            }
        }

    }
}