package com.aaplamandal.ui.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.data.local.entities.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class AddExpenseUiState(
    // Where
    val expenseLocation: String = "",
    val expenseDescription: String = "",
    val category: String = "Decoration",

    // Who
    val expensedBy: String = "",
    val contactNumber: String = "",
    val authority: String = "Committee Member",

    // Payment
    val amount: String = "",
    val amountInWords: String = "",

    // Date (defaults to today)
    val expenseDate: Long = System.currentTimeMillis(),

    // Optional
    val notes: String = "",

    // Validation Errors
    val expenseLocationError: String? = null,
    val expenseDescriptionError: String? = null,
    val expensedByError: String? = null,
    val amountError: String? = null,
    val amountInWordsError: String? = null,

    // Form State
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

class AddExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()
    private val deviceInfoDao = database.deviceInfoDao()

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    // --- Update Functions ---

    fun updateExpenseLocation(value: String) {
        _uiState.value = _uiState.value.copy(expenseLocation = value, expenseLocationError = null)
    }

    fun updateExpenseDescription(value: String) {
        _uiState.value = _uiState.value.copy(expenseDescription = value, expenseDescriptionError = null)
    }

    fun updateCategory(value: String) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun updateExpensedBy(value: String) {
        _uiState.value = _uiState.value.copy(expensedBy = value, expensedByError = null)
    }

    fun updateContactNumber(value: String) {
        _uiState.value = _uiState.value.copy(contactNumber = value)
    }

    fun updateAuthority(value: String) {
        _uiState.value = _uiState.value.copy(authority = value)
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

    fun updateExpenseDate(value: Long) {
        _uiState.value = _uiState.value.copy(expenseDate = value)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    // --- Validation & Save ---

    fun saveExpense() {
        val state = _uiState.value
        var hasError = false

        if (state.expenseLocation.isBlank()) {
            _uiState.value = _uiState.value.copy(expenseLocationError = "Location is required")
            hasError = true
        }

        if (state.expenseDescription.isBlank()) {
            _uiState.value = _uiState.value.copy(expenseDescriptionError = "Description is required")
            hasError = true
        }

        if (state.expensedBy.isBlank()) {
            _uiState.value = _uiState.value.copy(expensedByError = "Name is required")
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

        if (hasError) return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val deviceInfo = deviceInfoDao.getDeviceInfo()
                val deviceId = deviceInfo?.deviceId ?: "DEVICE-001"
                val deviceName = deviceInfo?.deviceName ?: "DEVICE"
                val festivalYear = deviceInfo?.festivalYear ?: Calendar.getInstance().get(Calendar.YEAR)

                // Sequence number is scoped to deviceName so each device
                // counts independently: Booth1-47392 starts from EXP-0001
                val lastSeq = expenseDao.getLastSequenceNumber(deviceName) ?: 0
                val nextSeq = lastSeq + 1

                // Unique expense ID now uses deviceName:
                // e.g. Booth1-47392-EXP-2026-0001
                val uniqueExpenseId = "$deviceName-EXP-$festivalYear-${nextSeq.toString().padStart(4, '0')}"

                val expense = Expense(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    sequenceNumber = nextSeq,
                    uniqueExpenseId = uniqueExpenseId,
                    expenseLocation = state.expenseLocation.trim(),
                    expenseDescription = state.expenseDescription.trim(),
                    category = state.category,
                    expensedBy = state.expensedBy.trim(),
                    contactNumber = state.contactNumber.trim(),
                    authority = state.authority,
                    amount = amountValue!!,
                    amountInWords = state.amountInWords.trim(),
                    expenseDate = state.expenseDate,
                    createdAt = System.currentTimeMillis(),
                    notes = state.notes.ifBlank { null },
                    receiptImagePath = null,
                    synced = false
                )

                expenseDao.insert(expense)

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
        _uiState.value = AddExpenseUiState()
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