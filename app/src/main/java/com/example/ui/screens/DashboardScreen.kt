package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import com.example.util.DocumentExporter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppThemeMode
import com.example.data.model.BahanItem
import com.example.ui.dialogs.AddEditMaterialDialog
import com.example.ui.dialogs.ColumnHeaderDialog
import com.example.ui.dialogs.EditCellDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SinarMasBlue
import com.example.ui.theme.SinarMasGold
import com.example.ui.theme.SinarMasNavy
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.StokViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userEmail: String,
    stokViewModel: StokViewModel,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val rawMaterials by stokViewModel.rawMaterials.collectAsState()
    val filteredMaterials by stokViewModel.filteredMaterials.collectAsState()
    val settings by stokViewModel.settings.collectAsState()
    val profile by stokViewModel.profile.collectAsState()
    val searchQuery by stokViewModel.searchQuery.collectAsState()
    val selectedCategory by stokViewModel.selectedCategory.collectAsState()
    val isSyncing by stokViewModel.isSyncing.collectAsState()
    val statusMessage by stokViewModel.statusMessage.collectAsState()
    val lastExportResult by stokViewModel.lastExportResult.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEditName by remember { mutableStateOf<BahanItem?>(null) }
    var showHeaderDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<BahanItem?>(null) }

    // Cell editing state
    var editingCellInfo by remember {
        mutableStateOf<Triple<BahanItem, Int, String>?>(null) // Item, colIndex (1..5), columnTitle
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            stokViewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_sinar_mas),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SINAR MAS ALUMINIUM",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(SuccessGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncing) "Menyinkronkan…" else "Cloud Firestore Real-time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSyncing) SinarMasBlue else SuccessGreen
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp),
                            strokeWidth = 2.dp,
                            color = SinarMasBlue
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan & Keamanan",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Lainnya"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Atur Judul Kolom") },
                                leadingIcon = {
                                    Icon(Icons.Default.TableChart, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    showHeaderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cadangkan ke Cloud") },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudSync, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    stokViewModel.triggerCloudBackup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bagikan Analisis") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    stokViewModel.shareAnalysis(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Keluar Sesi (Logout)", color = DangerRed) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = DangerRed)
                                },
                                onClick = {
                                    showMenu = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SinarMasBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_material_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Bahan", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Title Header & Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = settings.judul,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Kelola daftar bahan dan catatan stok dalam satu aplikasi mobile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { stokViewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Cari nama bahan aluminium…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { stokViewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_material_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Bar: Buttons for Export PDF, Excel, Print, Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SinarMasBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showHeaderDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Judul Kolom", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { stokViewModel.exportPdf(context) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor PDF", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { stokViewModel.exportExcel(context) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_excel_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor Excel", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { stokViewModel.printReport(context) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak (Print)", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { stokViewModel.shareAnalysis(context) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan Analisis", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips
                val categories = listOf("Semua", "Pipa", "Holo", "Openback", "Lis", "Lainnya")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { stokViewModel.onCategorySelected(cat) },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SinarMasBlue,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Stats info strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${filteredMaterials.size} dari ${rawMaterials.size} bahan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ketuk angka/nilai untuk mengubah",
                    style = MaterialTheme.typography.labelSmall,
                    color = SinarMasBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            // Materials Table Container
            val scrollState = rememberScrollState()

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sticky Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .horizontalScroll(scrollState)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHeaderCell(text = "NO", width = 36.dp, alignCenter = true)
                        TableHeaderCell(text = "NAMA BAHAN", width = 170.dp)
                        TableHeaderCell(text = settings.kolom1.ifBlank { "KOLOM 1" }, width = 90.dp)
                        TableHeaderCell(text = settings.kolom2.ifBlank { "KOLOM 2" }, width = 90.dp)
                        TableHeaderCell(text = settings.kolom3.ifBlank { "KOLOM 3" }, width = 90.dp)
                        TableHeaderCell(text = settings.kolom4.ifBlank { "KOLOM 4" }, width = 90.dp)
                        TableHeaderCell(text = settings.kolom5.ifBlank { "KOLOM 5" }, width = 90.dp)
                        TableHeaderCell(text = "AKSI", width = 90.dp, alignCenter = true)
                    }

                    // Table Rows
                    if (filteredMaterials.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Tidak ditemukan bahan dengan nama '$searchQuery'" else "Daftar bahan masih kosong",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(filteredMaterials, key = { _, item -> item.id }) { index, item ->
                                val isEven = (index % 2 == 0)
                                val rowBg = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .horizontalScroll(scrollState)
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // NO
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(36.dp)
                                    )

                                    // NAMA BAHAN
                                    Text(
                                        text = item.nama,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .width(170.dp)
                                            .padding(end = 8.dp)
                                            .clickable { itemToEditName = item }
                                    )

                                    // KOLOM 1..5 (Interactive Cells)
                                    TableCell(
                                        value = item.nilai1,
                                        width = 90.dp,
                                        onClick = {
                                            editingCellInfo = Triple(item, 1, settings.kolom1.ifBlank { "KOLOM 1" })
                                        }
                                    )
                                    TableCell(
                                        value = item.nilai2,
                                        width = 90.dp,
                                        onClick = {
                                            editingCellInfo = Triple(item, 2, settings.kolom2.ifBlank { "KOLOM 2" })
                                        }
                                    )
                                    TableCell(
                                        value = item.nilai3,
                                        width = 90.dp,
                                        onClick = {
                                            editingCellInfo = Triple(item, 3, settings.kolom3.ifBlank { "KOLOM 3" })
                                        }
                                    )
                                    TableCell(
                                        value = item.nilai4,
                                        width = 90.dp,
                                        onClick = {
                                            editingCellInfo = Triple(item, 4, settings.kolom4.ifBlank { "KOLOM 4" })
                                        }
                                    )
                                    TableCell(
                                        value = item.nilai5,
                                        width = 90.dp,
                                        onClick = {
                                            editingCellInfo = Triple(item, 5, settings.kolom5.ifBlank { "KOLOM 5" })
                                        }
                                    )

                                    // ACTION BUTTONS (Ubah nama & Hapus)
                                    Row(
                                        modifier = Modifier.width(90.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = { itemToEditName = item },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Ubah nama",
                                                tint = SinarMasBlue,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { itemToDelete = item },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = DangerRed,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddDialog) {
        AddEditMaterialDialog(
            itemToEdit = null,
            onConfirm = { name ->
                stokViewModel.addMaterial(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (itemToEditName != null) {
        AddEditMaterialDialog(
            itemToEdit = itemToEditName,
            onConfirm = { newName ->
                stokViewModel.updateMaterialName(itemToEditName!!, newName)
                itemToEditName = null
            },
            onDismiss = { itemToEditName = null }
        )
    }

    if (showHeaderDialog) {
        ColumnHeaderDialog(
            currentSettings = settings,
            onSave = { judul, k1, k2, k3, k4, k5 ->
                stokViewModel.updateColumnHeaders(judul, k1, k2, k3, k4, k5)
                showHeaderDialog = false
            },
            onDismiss = { showHeaderDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            userEmail = userEmail,
            profile = profile,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onTwoFactorToggle = { enabled, pin ->
                stokViewModel.updateTwoFactorAuth(enabled, pin)
            },
            onBackupNow = {
                stokViewModel.triggerCloudBackup()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (editingCellInfo != null) {
        val (item, colIndex, title) = editingCellInfo!!
        val curVal = when (colIndex) {
            1 -> item.nilai1
            2 -> item.nilai2
            3 -> item.nilai3
            4 -> item.nilai4
            5 -> item.nilai5
            else -> ""
        }
        EditCellDialog(
            materialName = item.nama,
            columnTitle = title,
            currentValue = curVal,
            onSave = { newVal ->
                stokViewModel.updateCellValue(item.id, colIndex, newVal, item.nama)
                editingCellInfo = null
            },
            onDismiss = { editingCellInfo = null }
        )
    }

    if (itemToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Hapus bahan '${itemToDelete?.nama}' dari daftar stok inventaris?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { stokViewModel.deleteMaterial(it) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                ButtonDefaults.textButtonColors()
                androidx.compose.material3.TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (lastExportResult != null) {
        val exportInfo = lastExportResult!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { stokViewModel.dismissExportResult() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SinarMasGold,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Tersimpan di Memori Internal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Berkas telah dibuat dan disimpan langsung ke memori internal HP:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = SinarMasBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Folder: ${exportInfo.folderPath}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (exportInfo.mimeType.contains("pdf")) Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = if (exportInfo.mimeType.contains("pdf")) DangerRed else SinarMasBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = exportInfo.fileName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "💡 Berkas dapat langsung Anda temukan di aplikasi Pengelola Berkas / File Saya / Download di HP dalam folder 'SinarMasAluminium'.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        DocumentExporter.openFile(context, exportInfo)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SinarMasBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Berkas")
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            DocumentExporter.openDownloadsFolder(context)
                        }
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka Folder")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            DocumentExporter.shareFile(context, exportInfo)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    androidx.compose.material3.TextButton(onClick = { stokViewModel.dismissExportResult() }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    alignCenter: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
    )
}

@Composable
private fun TableCell(
    value: String,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (value.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 6.dp)
    ) {
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall,
            color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
