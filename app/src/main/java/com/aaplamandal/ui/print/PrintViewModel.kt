package com.aaplamandal.ui.print

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.data.local.entities.DeviceInfo
import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.data.local.entities.Receipt
import com.aaplamandal.utils.PrintManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PrintUiState(
    val isPrinting: Boolean = false,
    val showPrintDialog: Boolean = false,
    val currentReceiptId: Long? = null,
    val currentExpenseId: Long? = null,
    val printerAddress: String = "",
    val pairedDevices: List<android.bluetooth.BluetoothDevice> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class PrintViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val receiptDao = database.receiptDao()
    private val expenseDao = database.expenseDao()
    private val deviceInfoDao = database.deviceInfoDao()
    private val printManager = PrintManager(application)

    private val _uiState = MutableStateFlow(PrintUiState())
    val uiState: StateFlow<PrintUiState> = _uiState.asStateFlow()

    private var deviceInfo: DeviceInfo? = null

    init {
        viewModelScope.launch {
            deviceInfo = deviceInfoDao.getDeviceInfo()
            // Pre-load printer address from device config
            val printerAddress = deviceInfo?.printerAddress ?: ""
            val paired = try {
                printManager.getPairedBluetoothDevices()
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(
                printerAddress = printerAddress,
                pairedDevices = paired
            )
        }
    }

    fun showPrintOptions(receiptId: Long? = null, expenseId: Long? = null) {
        viewModelScope.launch {
            // Reload fresh DeviceInfo every time so latest settings reflect
            deviceInfo = deviceInfoDao.getDeviceInfo()

            _uiState.value = _uiState.value.copy(
                showPrintDialog = true,
                currentReceiptId = receiptId,
                currentExpenseId = expenseId,
                successMessage = null,
                errorMessage = null
            )
        }
    }

    fun dismissPrintDialog() {
        _uiState.value = _uiState.value.copy(showPrintDialog = false)
    }

    fun updatePrinterAddress(address: String) {
        _uiState.value = _uiState.value.copy(printerAddress = address)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    // ── Load Paired Devices ─────────────────────────────────────────
    // Called by PrintDialog when the user opens the device picker,
    // so the list is always fresh (user may have paired a new printer
    // since the ViewModel was created).
    fun loadPairedDevices() {
        viewModelScope.launch {
            val paired = try {
                printManager.getPairedBluetoothDevices()
            } catch (e: SecurityException) {
                // Permission was revoked after the dialog opened — clear
                // the list so the UI falls back to the permission card
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(pairedDevices = paired)
        }
    }

    // ── Bluetooth Print ─────────────────────────────────────────────

    fun printViaBluetooth() {
        val state = _uiState.value
        if (state.printerAddress.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "No printer selected")
            return
        }

        _uiState.value = _uiState.value.copy(isPrinting = true, showPrintDialog = false)

        viewModelScope.launch {
            try {
                val result = when {
                    state.currentReceiptId != null -> {
                        val receipt = receiptDao.getById(state.currentReceiptId)
                            ?: return@launch setError("Receipt not found")
                        printManager.printReceiptViaBluetooth(receipt, deviceInfo, state.printerAddress)
                    }
                    state.currentExpenseId != null -> {
                        val expense = expenseDao.getById(state.currentExpenseId)
                            ?: return@launch setError("Expense not found")
                        printManager.printExpenseViaBluetooth(expense, deviceInfo, state.printerAddress)
                    }
                    else -> return@launch setError("Nothing to print")
                }

                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    successMessage = if (result.success) "Printed successfully!" else null,
                    errorMessage = if (!result.success) result.errorMessage else null
                )
            } catch (e: Exception) {
                setError(e.message ?: "Print failed")
            }
        }
    }

    // ── PDF Share ───────────────────────────────────────────────────

    fun shareAsPdf(context: Context) {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isPrinting = true, showPrintDialog = false)

        viewModelScope.launch {
            try {
                val result = when {
                    state.currentReceiptId != null -> {
                        val receipt = receiptDao.getById(state.currentReceiptId)
                            ?: return@launch setError("Receipt not found")
                        printManager.generateReceiptPdf(receipt, deviceInfo)
                    }
                    state.currentExpenseId != null -> {
                        val expense = expenseDao.getById(state.currentExpenseId)
                            ?: return@launch setError("Expense not found")
                        printManager.generateExpensePdf(expense, deviceInfo)
                    }
                    else -> return@launch setError("Nothing to print")
                }

                result.fold(
                    onSuccess = { file ->
                        _uiState.value = _uiState.value.copy(isPrinting = false)
                        sharePdfFile(context, file)
                    },
                    onFailure = { e ->
                        setError(e.message ?: "PDF generation failed")
                    }
                )
            } catch (e: Exception) {
                setError(e.message ?: "PDF generation failed")
            }
        }
    }

    private fun sharePdfFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share / Print Receipt"))
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Could not open share dialog")
        }
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(isPrinting = false, errorMessage = message)
    }
}