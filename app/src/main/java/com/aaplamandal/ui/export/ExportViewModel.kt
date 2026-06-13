package com.aaplamandal.ui.export

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.utils.ExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

sealed class ExportFormat {
    object JSON : ExportFormat()
    object ReceiptCSV : ExportFormat()          // renamed from TransactionCSV
    object ExpenseCSV : ExportFormat()
    object Database : ExportFormat()
    object CompleteBackup : ExportFormat()
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportedFiles: List<File> = emptyList(),
    val errorMessage: String? = null,
    val receiptCount: Int = 0,                  // renamed from transactionCount
    val expenseCount: Int = 0,
    val totalCollected: Double = 0.0,           // paid receipts only
    val totalPending: Double = 0.0,             // balance receipts
    val totalExpenses: Double = 0.0,
    val exportPath: String = ""
)

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val receiptDao = database.receiptDao()          // renamed from transactionDao
    private val expenseDao = database.expenseDao()
    private val deviceInfoDao = database.deviceInfoDao()
    private val exportManager = ExportManager(application)

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        loadSummaryData()
        _uiState.value = _uiState.value.copy(
            exportPath = exportManager.getExportDirectoryPath()
        )
    }

    private fun loadSummaryData() {
        viewModelScope.launch {
            try {
                val receipts = receiptDao.getAllReceipts().first()
                val expenses = expenseDao.getAllExpenses().first()

                _uiState.value = _uiState.value.copy(
                    receiptCount = receipts.size,
                    expenseCount = expenses.size,
                    totalCollected = receipts
                        .filter { it.paymentStatus == "paid" }
                        .sumOf { it.amount },
                    totalPending = receipts
                        .filter { it.paymentStatus == "balance" }
                        .sumOf { it.amount },
                    totalExpenses = expenses.sumOf { it.amount }  // fixed: was it.price
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportData(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(
            isExporting = true,
            exportSuccess = false,
            exportedFiles = emptyList(),
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val deviceInfo = deviceInfoDao.getDeviceInfo()
                val deviceId = deviceInfo?.deviceId ?: "DEVICE-001"
                val deviceName = deviceInfo?.deviceName ?: "Unknown Device"

                val receipts = receiptDao.getAllReceipts().first()
                val expenses = expenseDao.getAllExpenses().first()

                val result = when (format) {
                    is ExportFormat.JSON -> {
                        exportManager.exportToJson(deviceId, deviceName, receipts, expenses)
                            .map { listOf(it) }
                    }
                    is ExportFormat.ReceiptCSV -> {
                        exportManager.exportReceiptsToCSV(deviceId, receipts)
                            .map { listOf(it) }
                    }
                    is ExportFormat.ExpenseCSV -> {
                        exportManager.exportExpensesToCSV(deviceId, expenses)
                            .map { listOf(it) }
                    }
                    is ExportFormat.Database -> {
                        exportManager.exportDatabase(deviceId)
                            .map { listOf(it) }
                    }
                    is ExportFormat.CompleteBackup -> {
                        exportManager.exportCompleteBackup(deviceId, deviceName, receipts, expenses)
                    }
                }

                result.fold(
                    onSuccess = { files ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportSuccess = true,
                            exportedFiles = files
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportSuccess = false,
                            errorMessage = exception.message ?: "Export failed"
                        )
                    }
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = false,
                    errorMessage = e.message ?: "Unknown error occurred"
                )
                e.printStackTrace()
            }
        }
    }

    fun clearExportStatus() {
        _uiState.value = _uiState.value.copy(
            exportSuccess = false,
            exportedFiles = emptyList(),
            errorMessage = null
        )
    }
}