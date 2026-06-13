package com.aaplamandal.ui.receipt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.data.local.entities.Receipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AddReceiptUiState(
    // Donor Personal Details
    val suffix: String = "Shri",
    val firstName: String = "",
    val middleName: String = "",
    val surname: String = "",

    // Address Details
    val buildingName: String = "",
    val wing: String = "",
    val roomNumber: String = "",
    val address: String = "",
    val contactNumber: String = "",

    // Payment Details
    val amount: String = "",
    val amountInWords: String = "",
    val paymentStatus: String = "paid",         // "paid" or "balance"

    // Collection Details
    val collectorName: String = "",

    // Validation Errors
    val firstNameError: String? = null,
    val surnameError: String? = null,
    val amountError: String? = null,
    val amountInWordsError: String? = null,
    val collectorNameError: String? = null,

    // Form State
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

class AddReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val receiptDao = database.receiptDao()
    private val deviceInfoDao = database.deviceInfoDao()

    private val _uiState = MutableStateFlow(AddReceiptUiState())
    val uiState: StateFlow<AddReceiptUiState> = _uiState.asStateFlow()

    // --- Update Functions ---

    fun updateSuffix(value: String) {
        _uiState.value = _uiState.value.copy(suffix = value)
    }

    fun updateFirstName(value: String) {
        _uiState.value = _uiState.value.copy(firstName = value, firstNameError = null)
    }

    fun updateMiddleName(value: String) {
        _uiState.value = _uiState.value.copy(middleName = value)
    }

    fun updateSurname(value: String) {
        _uiState.value = _uiState.value.copy(surname = value, surnameError = null)
    }

    fun updateBuildingName(value: String) {
        _uiState.value = _uiState.value.copy(buildingName = value)
    }

    fun updateWing(value: String) {
        _uiState.value = _uiState.value.copy(wing = value)
    }

    fun updateRoomNumber(value: String) {
        _uiState.value = _uiState.value.copy(roomNumber = value)
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun updateContactNumber(value: String) {
        _uiState.value = _uiState.value.copy(contactNumber = value)
    }

    fun updateAmount(value: String) {
        _uiState.value = _uiState.value.copy(
            amount = value,
            amountError = null,
            // Auto-generate amount in words when amount changes
            amountInWords = value.toDoubleOrNull()?.let { convertToWords(it) } ?: ""
        )
    }

    fun updateAmountInWords(value: String) {
        _uiState.value = _uiState.value.copy(amountInWords = value, amountInWordsError = null)
    }

    fun updatePaymentStatus(value: String) {
        _uiState.value = _uiState.value.copy(paymentStatus = value)
    }

    fun updateCollectorName(value: String) {
        _uiState.value = _uiState.value.copy(collectorName = value, collectorNameError = null)
    }

    // --- Validation & Save ---

    fun saveReceipt() {
        val state = _uiState.value
        var hasError = false

        if (state.firstName.isBlank()) {
            _uiState.value = _uiState.value.copy(firstNameError = "First name is required")
            hasError = true
        }

        if (state.surname.isBlank()) {
            _uiState.value = _uiState.value.copy(surnameError = "Surname is required")
            hasError = true
        }

        val amountValue = state.amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _uiState.value = _uiState.value.copy(amountError = "Enter a valid amount")
            hasError = true
        }

        if (state.amountInWords.isBlank()) {
            _uiState.value = _uiState.value.copy(amountInWordsError = "Amount in words is required")
            hasError = true
        }

        if (state.collectorName.isBlank()) {
            _uiState.value = _uiState.value.copy(collectorNameError = "Collector name is required")
            hasError = true
        }

        if (hasError) return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val deviceInfo = deviceInfoDao.getDeviceInfo()
                val deviceId = deviceInfo?.deviceId ?: "DEVICE-001"
                val deviceName = deviceInfo?.deviceName ?: "DEVICE"
                val festivalYear = deviceInfo?.festivalYear ?: Calendar.getInstance().get(Calendar.YEAR)

                // Sequence number is scoped to deviceName so each device
                // counts independently: Booth1-47392 starts from 0001
                val lastSeq = receiptDao.getLastSequenceNumber(deviceName) ?: 0
                val nextSeq = lastSeq + 1

                // Unique receipt ID now uses deviceName:
                // e.g. Booth1-47392-2026-0001
                val uniqueReceiptId = "$deviceName-$festivalYear-${nextSeq.toString().padStart(4, '0')}"

                val receipt = Receipt(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    sequenceNumber = nextSeq,
                    uniqueReceiptId = uniqueReceiptId,
                    suffix = state.suffix,
                    firstName = state.firstName.trim(),
                    middleName = state.middleName.trim(),
                    surname = state.surname.trim(),
                    buildingName = state.buildingName.trim(),
                    wing = state.wing.trim(),
                    roomNumber = state.roomNumber.trim(),
                    address = state.address.trim(),
                    contactNumber = state.contactNumber.trim(),
                    amount = amountValue!!,
                    amountInWords = state.amountInWords.trim(),
                    paymentStatus = state.paymentStatus,
                    collectorName = state.collectorName.trim(),
                    createdAt = System.currentTimeMillis(),
                    synced = false
                )

                receiptDao.insert(receipt)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                e.printStackTrace()
            }
        }
    }

    fun resetState() {
        _uiState.value = AddReceiptUiState()
    }

    // --- Amount to Words Helper ---
    private fun convertToWords(amount: Double): String {
        val rupees = amount.toLong()
        val paise = ((amount - rupees) * 100).toLong()

        val words = numberToWords(rupees)
        return if (paise > 0) {
            "$words and ${numberToWords(paise)} Paise Only"
        } else {
            "$words Rupees Only"
        }
    }

    private fun numberToWords(n: Long): String {
        if (n == 0L) return "Zero"

        val ones = listOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = listOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun helper(num: Long): String {
            return when {
                num == 0L -> ""
                num < 20L -> ones[num.toInt()] + " "
                num < 100L -> tens[(num / 10).toInt()] + " " + helper(num % 10)
                num < 1000L -> ones[(num / 100).toInt()] + " Hundred " + helper(num % 100)
                num < 100000L -> helper(num / 1000) + "Thousand " + helper(num % 1000)
                num < 10000000L -> helper(num / 100000) + "Lakh " + helper(num % 100000)
                else -> helper(num / 10000000) + "Crore " + helper(num % 10000000)
            }
        }

        return helper(n).trim()
    }
}