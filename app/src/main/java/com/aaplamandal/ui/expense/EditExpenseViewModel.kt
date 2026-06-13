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

private const val DELETE_PASSWORD = "mandal@123"

data class EditExpenseUiState(
    val expenseLocation: String = "",
    val expenseDescription: String = "",
    val category: String = "Decoration",
    val expensedBy: String = "",
    val contactNumber: String = "",
    val authority: String = "Committee Member",
    val amount: String = "",
    val amountInWords: String = "",
    val expenseDate: Long = System.currentTimeMillis(),
    val notes: String = "",

    // Errors
    val expenseLocationError: String? = null,
    val expenseDescriptionError: String? = null,
    val expensedByError: String? = null,
    val amountError: String? = null,
    val amountInWordsError: String? = null,

    // Delete Dialog
    val showDeleteDialog: Boolean = false,
    val deletePassword: String = "",
    val deletePasswordError: String? = null,

    // State
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

class EditExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()

    private val _uiState = MutableStateFlow(EditExpenseUiState())
    val uiState: StateFlow<EditExpenseUiState> = _uiState.asStateFlow()

    private var currentExpense: Expense? = null

    fun loadExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                val expense = expenseDao.getById(expenseId)
                if (expense != null) {
                    currentExpense = expense
                    _uiState.value = _uiState.value.copy(
                        expenseLocation = expense.expenseLocation,
                        expenseDescription = expense.expenseDescription,
                        category = expense.category,
                        expensedBy = expense.expensedBy,
                        contactNumber = expense.contactNumber,
                        authority = expense.authority,
                        amount = expense.amount.toString(),
                        amountInWords = expense.amountInWords,
                        expenseDate = expense.expenseDate,
                        notes = expense.notes ?: "",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

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
        if (_uiState.value.deletePassword != DELETE_PASSWORD) {
            _uiState.value = _uiState.value.copy(deletePasswordError = "Incorrect password")
            return
        }
        _uiState.value = _uiState.value.copy(isDeleting = true, showDeleteDialog = false)
        viewModelScope.launch {
            try {
                currentExpense?.let { expenseDao.delete(it) }
                _uiState.value = _uiState.value.copy(isDeleting = false, deletedSuccessfully = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDeleting = false, errorMessage = e.message)
            }
        }
    }

    // --- Save ---

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
                val updated = currentExpense!!.copy(
                    expenseLocation = state.expenseLocation.trim(),
                    expenseDescription = state.expenseDescription.trim(),
                    category = state.category,
                    expensedBy = state.expensedBy.trim(),
                    contactNumber = state.contactNumber.trim(),
                    authority = state.authority,
                    amount = amountValue!!,
                    amountInWords = state.amountInWords.trim(),
                    expenseDate = state.expenseDate,
                    notes = state.notes.ifBlank { null }
                )
                expenseDao.update(updated)
                _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = e.message)
            }
        }
    }

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