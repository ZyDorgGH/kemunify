package id.zydorg.kemunify.ui.screen.waste

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.ui.common.UiState
import id.zydorg.kemunify.ui.theme.DarkGreen
import id.zydorg.kemunify.ui.theme.KemunifyTheme
import id.zydorg.kemunify.ui.theme.LightGreen40
import id.zydorg.kemunify.ui.theme.WhiteSmoke
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteDetail(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    viewModel: WasteViewModel = hiltViewModel(),
) {
    var wasteData by remember { mutableStateOf<List<WasteEntity>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingWasteName by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var fabHeight by remember { mutableIntStateOf(0) }

    val heightInDp = with(LocalDensity.current) { fabHeight.toDp() }


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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "List Jenis Sampah",
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
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                containerColor = LightGreen40,
                contentColor = DarkGreen,
                onClick = { showEditDialog = true },
                modifier = Modifier.padding(16.dp).semantics { contentDescription = "add list waste" }.onGloballyPositioned {
                    fabHeight = it.size.height
                },
            ) {
                Icon(Icons.Default.Add, "Tambah List Sampah")
            }

        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            WasteDetailContent(wastes = wasteData, heightInDp)
        }
    }
    if (showEditDialog) {
        EditWasteDialog(
            wasteName = "",
            onDismiss = {
                showEditDialog = false
                editingWasteName = null
            },
            onSave = { newName ->
                coroutineScope.launch {
                    viewModel.insertNewWasteName(newName)
                    Log.d("save newName", "menyimpan $editingWasteName")
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun WasteDetailContent(
    wastes: List<WasteEntity>,
    heightInDp: Dp,
    modifier: Modifier = Modifier,
){
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
        ,
        contentPadding = PaddingValues(bottom = heightInDp + 16.dp)
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
                        modifier = Modifier.weight(5f)
                    )
                    Text(
                        "Hapus",
                        modifier = Modifier.weight(3f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        " ",
                        modifier = Modifier.weight(2f),
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
                            WasteItem(waste = waste)
                        }
                    }
                }


            }
        }
    }
}

@Composable
private fun WasteItem(
    waste: WasteEntity,
    modifier: Modifier = Modifier,
    viewModel: WasteViewModel = hiltViewModel(),
){
    var confirmDelete by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingWasteName by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            waste.wasteName,
            modifier = Modifier
                .padding(16.dp)
                .weight(5f)
        )

        IconButton(
            onClick = {
                confirmDelete = true
            },
            modifier = Modifier
                .padding(16.dp)
                .weight(3f),
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                tint = Color.Red,
                contentDescription = null
            )
        }
        Text(
            "Edit",
            modifier = Modifier
                .weight(2f)
                .clickable {
                    editingWasteName = waste.wasteName
                    Log.d("data in editingWeightData", "There is $editingWasteName")
                    showEditDialog = true
                },
            color = Color.Blue.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    if (confirmDelete){
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus Jenis Sampah") },
            text = { Text("Apakah Anda yakin ingin menghapus Jenis Sampah ${waste.wasteName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteWasteName(waste.wasteName)
                        }
                        confirmDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                Button(onClick = { confirmDelete = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditDialog && editingWasteName != null) {
        EditWasteDialog(
            wasteName = waste.wasteName,
            onDismiss = {
                showEditDialog = false
                editingWasteName = null
            },
            onSave = { newName ->
                coroutineScope.launch {
                    viewModel.updateWasteName(waste.wasteName, newName)
                }
                showEditDialog = false
            }
        )
    }

}

@Composable
private fun EditWasteDialog(
    wasteName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var wasteNameInput by remember { mutableStateOf(wasteName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Jenis Sampah") },
        text = {
            Column {
                OutlinedTextField(
                    value = wasteNameInput,
                    onValueChange = {
                        wasteNameInput = it
                    },
                    label = { Text("Nama Jenis Sampah") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = errorMessage != null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (errorMessage.isNullOrEmpty() && wasteNameInput.isNotBlank()) {
                        onSave(wasteNameInput)
                    }
                },
                enabled = errorMessage.isNullOrEmpty() && wasteNameInput.isNotBlank()
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

@Preview(showBackground = true)
@Composable
fun WasteListPreview() {
    KemunifyTheme {
        val dummyWasteList: List<WasteEntity> = listOf(
            WasteEntity(
                wasteName = "Plastik",
                weightsJson = "1"
            ),
            WasteEntity(
                wasteName = "Kertas",
                weightsJson = "1"
            ),
            WasteEntity(
                wasteName = "Logam",
                weightsJson = "1"
            ),
            WasteEntity(
                wasteName = "Organik",
                weightsJson = "1"
            ),
            WasteEntity(
                wasteName = "Kaca",
                weightsJson = "1"
            )
        )


    }
}