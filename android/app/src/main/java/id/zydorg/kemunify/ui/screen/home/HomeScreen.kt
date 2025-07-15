package id.zydorg.kemunify.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import id.zydorg.kemunify.MainApplication
import id.zydorg.kemunify.data.factory.ViewModelFactory
import id.zydorg.kemunify.ui.theme.DarkGreen
import id.zydorg.kemunify.ui.theme.LightGreen40
import id.zydorg.kemunify.ui.theme.WhiteSmoke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToDetail: (String) -> Unit,
    navigateToAddWaste: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(MainApplication.injection)
    ),
) {
    val context = LocalContext.current
    val customerState by viewModel.customerUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isCircularIndicatorShowing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<String?>(null) }
    var expandedActionMenu by remember { mutableStateOf(false) }
    var expandedListMore by remember { mutableStateOf(false) }

    val requestStoragePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                isCircularIndicatorShowing = true
                delay(3000)
                viewModel.exportToExcel(context)
                isCircularIndicatorShowing = false
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "This feature is unavailable because it requires access to the phone's storage",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manajemen Sampah",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                actions = {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { expandedActionMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Home",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        DropdownMenu(expanded = expandedActionMenu, onDismissRequest = { expandedActionMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Share Excel") },
                                enabled = customerState.customer.isNotEmpty(),
                                onClick = {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            ) -> {
                                                coroutineScope.launch {
                                                    isCircularIndicatorShowing = true
                                                    delay(3000)
                                                    viewModel.exportToExcel(context)
                                                }.invokeOnCompletion {
                                                    isCircularIndicatorShowing = false
                                                }
                                            }
                                            else -> {
                                                requestStoragePermission.launch(
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                )
                                            }

                                        }
                                    } else {
                                        coroutineScope.launch {
                                            isCircularIndicatorShowing = true
                                            delay(3000)
                                            viewModel.exportToExcel(context)
                                        }.invokeOnCompletion {
                                            isCircularIndicatorShowing = false
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Share,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Semua Tabel", color = Color.Red) },
                                onClick = {
                                    showDeleteAllDialog = true
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
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                containerColor = LightGreen40,
                contentColor = DarkGreen,
                onClick = { navigateToAddWaste() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Tambah Nasabah")
            }

        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isCircularIndicatorShowing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.5f),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Icon(
                    Icons.Outlined.Person,
                    tint = Color.Gray,

                    contentDescription = "Nasabah"
                )

                Text(
                    "Nasabah",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                horizontalAlignment = Alignment.Start
            ) {
                itemsIndexed(customerState.customer) { index, customer ->

                    val backgroundColor = if (index % 2 == 0) {
                        WhiteSmoke.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }

                    Row (
                        modifier = Modifier
                            .background(backgroundColor)
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                navigateToDetail(customer.customerName)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Icon(
                            Icons.Outlined.DateRange,
                            tint = Color.Black,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(38.dp),
                            contentDescription = "Rekap"
                        )

                        Column(modifier = Modifier
                            .weight(8f)
                            .padding(8.dp)) {
                            Text(
                                "Rekap Data ${customer.customerName}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                customer.date,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                        }

                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(onClick = {
                                expandedListMore = true
                                customerToDelete = customer.customerName
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Home",
                                    tint = Color.DarkGray,
                                )
                            }
                            DropdownMenu(expanded = expandedListMore, onDismissRequest = { expandedListMore = false }) {
                                DropdownMenuItem(
                                    text = { Text("Hapus Tabel ${customer.customerName}", color = Color.Red) },
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
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Hapus Semua Data") },
            text = { Text("Apakah Anda yakin ingin menghapus semua data nasabah?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteAllCustomers()
                        }
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteAllDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Hapus Nasabah") },
            text = { Text("Apakah Anda yakin ingin menghapus nasabah ${customerToDelete}?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerToDelete != null){
                            coroutineScope.launch {
                                viewModel.deleteCustomer(customerToDelete!!)
                            }
                        }
                        expanded = false
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