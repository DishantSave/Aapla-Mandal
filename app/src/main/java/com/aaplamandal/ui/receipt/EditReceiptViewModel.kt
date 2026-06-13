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
import java.util.*

// Hardcoded delete password
private const val DELETE_PASSWORD = "mandal@123"

data class EditReceiptUiState(
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
    val paymentStatus: String = "paid",

    // Collection Details
    val collectorName: String = "",

    // Validation Errors
    val firstNameError: String? = null,
    val surnameError: String? = null,
    val amountError: String? = null,
    val amountInWordsError: String? = null,
    val collectorNameError: String? = null,

    // Delete Dialog
    val showDeleteDialog: Boolean = false,
    val deletePassword: String = "",
    val deletePasswordError: String? = null,

    // Form State
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

class EditReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val receiptDao = database.receiptDao()

    private val _uiState = MutableStateFlow(EditReceiptUiState())
    val uiState: StateFlow<EditReceiptUiState> = _uiState.asStateFlow()

    private var currentReceipt: Receipt? = null

    fun loadReceipt(receiptId: Long) {
        viewModelScope.launch {
            try {
                val receipt = receiptDao.getById(receiptId)
                if (receipt != null) {
                    currentReceipt = receipt
                    _uiState.value = _uiState.value.copy(
                        suffix = receipt.suffix,
                        firstName = receipt.firstName,
                        middleName = receipt.middleName,
                        surname = receipt.surname,
                        buildingName = receipt.buildingName,
                        wing = receipt.wing,
                        roomNumber = receipt.roomNumber,
                        address = receipt.address,
                        contactNumber = receipt.contactNumber,
                        amount = receipt.amount.toString(),
                        amountInWords = receipt.amountInWords,
                        paymentStatus = receipt.paymentStatus,
                        collectorName = receipt.collectorName,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Receipt not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

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

    // --- Delete Dialog ---

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            deletePassword = "",
            deletePasswordError = null
        )
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            deletePassword = "",
            deletePasswordError = null
        )
    }

    fun updateDeletePassword(value: String) {
        _uiState.value = _uiState.value.copy(deletePassword = value, deletePasswordError = null)
    }

    fun confirmDelete() {
        val state = _uiState.value
        if (state.deletePassword != DELETE_PASSWORD) {
            _uiState.value = _uiState.value.copy(
                deletePasswordError = "Incorrect password"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isDeleting = true, showDeleteDialog = false)

        viewModelScope.launch {
            try {
                currentReceipt?.let { receiptDao.delete(it) }
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deletedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // --- Save ---

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
                val updated = currentReceipt!!.copy(
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
                    collectorName = state.collectorName.trim()
                )
                receiptDao.update(updated)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = e.message)
            }
        }
    }

    // --- Amount to Words ---
    private fun convertToWords(amount: Double): String {
        val rupees = amount.toLong()
        val paise = ((amount - rupees) * 100).toLong()
        val words = numberToWords(rupees)
        return if (paise > 0) "$words and ${numberToWords(paise)} Paise Only"
        else "$words Rupees Only"
    }

    private fun numberToWords(n: Long): String {
        if (n == 0L) return "Zero"
        val ones = listOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
            "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen")
        val tens = listOf("", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety")
        fun helper(num: Long): String = when {
            num == 0L -> ""
            num < 20L -> ones[num.toInt()] + " "
            num < 100L -> tens[(num / 10).toInt()] + " " + helper(num % 10)
            num < 1000L -> ones[(num / 100).toInt()] + " Hundred " + helper(num % 100)
            num < 100000L -> helper(num / 1000) + "Thousand " + helper(num % 1000)
            num < 10000000L -> helper(num / 100000) + "Lakh " + helper(num % 100000)
            else -> helper(num / 10000000) + "Crore " + helper(num % 10000000)
        }
        return helper(n).trim()
    }

    fun resetSavedFlag() {
        _uiState.value = _uiState.value.copy(savedSuccessfully = false)
    }
}