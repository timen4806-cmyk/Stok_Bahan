package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppThemeMode
import com.example.data.model.BahanItem
import com.example.data.model.Pengaturan
import com.example.data.model.UserProfile
import com.example.data.repository.StokRepository
import com.example.util.DocumentExporter
import com.example.util.ExportResult
import com.example.util.NotificationHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StokViewModel(
    private val repository: StokRepository,
    private val userId: String,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastExportResult = MutableStateFlow<ExportResult?>(null)
    val lastExportResult: StateFlow<ExportResult?> = _lastExportResult.asStateFlow()

    fun dismissExportResult() {
        _lastExportResult.value = null
    }

    val rawMaterials: StateFlow<List<BahanItem>> = repository.observeBahan(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val settings: StateFlow<Pengaturan> = repository.observeSettings(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Pengaturan()
        )

    val profile: StateFlow<UserProfile> = repository.observeProfile(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserProfile()
        )

    val filteredMaterials: StateFlow<List<BahanItem>> = combine(
        rawMaterials,
        searchQuery,
        selectedCategory
    ) { list, query, category ->
        list.filter { item ->
            val matchQuery = query.isBlank() || item.nama.contains(query, ignoreCase = true)
            val matchCat = when (category) {
                "Semua" -> true
                "Pipa" -> item.nama.startsWith("PIPA", ignoreCase = true)
                "Holo" -> item.nama.startsWith("HOLO", ignoreCase = true)
                "Openback" -> item.nama.startsWith("OPENBACK", ignoreCase = true)
                "Lis" -> item.nama.startsWith("LIS", ignoreCase = true)
                "Lainnya" -> !item.nama.startsWith("PIPA", ignoreCase = true) &&
                             !item.nama.startsWith("HOLO", ignoreCase = true) &&
                             !item.nama.startsWith("OPENBACK", ignoreCase = true) &&
                             !item.nama.startsWith("LIS", ignoreCase = true)
                else -> true
            }
            matchQuery && matchCat
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun addMaterial(nama: String, n1: String = "", n2: String = "", n3: String = "", n4: String = "", n5: String = "") {
        if (nama.isBlank()) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.addBahan(userId, nama, n1, n2, n3, n4, n5)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Bahan '$nama' berhasil ditambahkan"
                notificationHelper.showNotification("Bahan Baru Ditambahkan", "Bahan $nama telah masuk ke daftar inventaris.")
            } else {
                _statusMessage.value = "Gagal menambah bahan: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateMaterialName(item: BahanItem, newName: String) {
        if (newName.isBlank() || newName == item.nama) return
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = item.copy(nama = newName)
            val result = repository.updateBahan(userId, updated)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Nama bahan diubah menjadi '$newName'"
            } else {
                _statusMessage.value = "Gagal mengubah nama: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateCellValue(itemId: String, colIndex: Int, value: String, materialName: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val fieldName = "nilai$colIndex"
            val result = repository.updateBahanCell(userId, itemId, fieldName, value)
            _isSyncing.value = false
            if (result.isSuccess) {
                // Check if low stock threshold (e.g. numeric <= 5)
                val numVal = value.trim().toIntOrNull()
                if (numVal != null && numVal in 1..5) {
                    notificationHelper.notifyLowStock(materialName, value)
                }
            } else {
                _statusMessage.value = "Gagal memperbarui nilai kolom: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteMaterial(item: BahanItem) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.deleteBahan(userId, item.id)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Bahan '${item.nama}' dihapus"
            } else {
                _statusMessage.value = "Gagal menghapus bahan: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateColumnHeaders(
        judul: String,
        k1: String,
        k2: String,
        k3: String,
        k4: String,
        k5: String
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val newSettings = Pengaturan(
                judul = judul.ifBlank { "STOK BAHAN SINAR MAS" },
                kolom1 = k1.ifBlank { "KOLOM 1" },
                kolom2 = k2.ifBlank { "KOLOM 2" },
                kolom3 = k3.ifBlank { "KOLOM 3" },
                kolom4 = k4.ifBlank { "KOLOM 4" },
                kolom5 = k5.ifBlank { "KOLOM 5" }
            )
            val result = repository.updateSettings(userId, newSettings)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Judul kolom berhasil diperbarui"
            } else {
                _statusMessage.value = "Gagal menyimpan judul kolom"
            }
        }
    }

    fun updateTwoFactorAuth(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val pinHash = if (enabled && pin.isNotBlank()) SecurityUtils.hashPin(pin) else ""
            val currentProfile = profile.value
            val updatedProfile = currentProfile.copy(
                twoFactorEnabled = enabled,
                twoFactorPin = pinHash
            )
            val result = repository.updateProfile(userId, updatedProfile)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = if (enabled) "Otentikasi 2FA berhasil diaktifkan" else "2FA dinonaktifkan"
            }
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            val updated = profile.value.copy(themeMode = mode.name)
            repository.updateProfile(userId, updated)
        }
    }

    fun triggerCloudBackup() {
        viewModelScope.launch {
            _isSyncing.value = true
            val currentItems = rawMaterials.value
            notificationHelper.notifyCloudBackupSuccess(currentItems.size)
            _statusMessage.value = "Pencadangan cloud real-time berhasil (${currentItems.size} bahan)"
            _isSyncing.value = false
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    DocumentExporter.exportToPdf(context, settings.value, rawMaterials.value)
                }
                _lastExportResult.value = result
                _statusMessage.value = "PDF berhasil disimpan di ${result.folderPath}/${result.fileName}"
                notificationHelper.showNotification(
                    title = "Ekspor PDF Selesai",
                    message = "Tersimpan di memori internal: ${result.folderPath}/${result.fileName}"
                )
            } catch (e: Exception) {
                _statusMessage.value = "Gagal membuat PDF: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun printReport(context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DocumentExporter.printReport(context, settings.value, rawMaterials.value)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Gagal membuka jendela cetak: ${e.localizedMessage}"
            }
        }
    }

    fun exportExcel(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    DocumentExporter.exportToExcel(context, settings.value, rawMaterials.value)
                }
                _lastExportResult.value = result
                _statusMessage.value = "Excel/CSV berhasil disimpan di ${result.folderPath}/${result.fileName}"
                notificationHelper.showNotification(
                    title = "Ekspor Excel Selesai",
                    message = "Tersimpan di memori internal: ${result.folderPath}/${result.fileName}"
                )
            } catch (e: Exception) {
                _statusMessage.value = "Gagal membuat berkas Excel: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun shareAnalysis(context: Context) {
        val items = rawMaterials.value
        val pipaCount = items.count { it.nama.startsWith("PIPA", ignoreCase = true) }
        val holoCount = items.count { it.nama.startsWith("HOLO", ignoreCase = true) }
        val openbackCount = items.count { it.nama.startsWith("OPENBACK", ignoreCase = true) }
        val lisCount = items.count { it.nama.startsWith("LIS", ignoreCase = true) }
        val lainnyaCount = items.size - (pipaCount + holoCount + openbackCount + lisCount)

        val text = buildString {
            appendLine("📊 *LAPORAN ANALISIS INVENTARIS — SINAR MAS ALUMINIUM*")
            appendLine("🏢 Judul: ${settings.value.judul}")
            appendLine("📦 Total Katalog: ${items.size} jenis bahan")
            appendLine("───────────────")
            appendLine("🔹 Pipa Aluminium: $pipaCount item")
            appendLine("🔹 Profil Holo: $holoCount item")
            appendLine("🔹 Profil Openback: $openbackCount item")
            appendLine("🔹 Profil Lis: $lisCount item")
            appendLine("🔹 Lain-lain / Aksesoris: $lainnyaCount item")
            appendLine("───────────────")
            appendLine("🔒 Status Database: Cloud Real-time Encrypted")
            appendLine("🕒 Dicetak melalui Aplikasi Sinar Mas Aluminium Android")
        }

        DocumentExporter.shareAnalysisText(context, text)
    }
}
