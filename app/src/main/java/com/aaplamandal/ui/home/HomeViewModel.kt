package com.aaplamandal.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.data.local.entities.Receipt
import com.aaplamandal.data.local.entities.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    // ── Today ────────────────────────────────────────────────
    val todayCollected: Double = 0.0,       // paid receipts today
    val todayPending: Double = 0.0,         // balance receipts today
    val todayExpenses: Double = 0.0,
    val todayNetBalance: Double = 0.0,      // today collected - today expenses

    // ── Overall ──────────────────────────────────────────────
    val overallCollected: Double = 0.0,     // all paid receipts ever
    val overallPending: Double = 0.0,       // all balance receipts ever
    val overallExpenses: Double = 0.0,      // all expenses ever
    val overallNetBalance: Double = 0.0,    // overall collected - overall expenses

    // ── Activity Feed ────────────────────────────────────────
    val recentReceipts: List<Receipt> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val combinedItems: List<CombinedItem> = emptyList(),
    val receiptCount: Int = 0,
    val expenseCount: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val receiptDao = database.receiptDao()
    private val expenseDao = database.expenseDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            receiptDao.getTodayReceipts(todayStart).collect { todayReceipts ->
                val todayCollected = todayReceipts
                    .filter { it.paymentStatus == "paid" }
                    .sumOf { it.amount }
                val todayPending = todayReceipts
                    .filter { it.paymentStatus == "balance" }
                    .sumOf { it.amount }

                expenseDao.getTodayExpenses(todayStart).collect { todayExpenses ->
                    val todayExpenseTotal = todayExpenses.sumOf { it.amount }

                    receiptDao.getAllReceipts().collect { allReceipts ->
                        expenseDao.getAllExpenses().collect { allExpenses ->

                            // ── Overall calculations ──────────────────
                            val overallCollected = allReceipts
                                .filter { it.paymentStatus == "paid" }
                                .sumOf { it.amount }
                            val overallPending = allReceipts
                                .filter { it.paymentStatus == "balance" }
                                .sumOf { it.amount }
                            val overallExpenses = allExpenses.sumOf { it.amount }

                            // ── Activity feed ─────────────────────────
                            val combined = mutableListOf<CombinedItem>()
                            allReceipts.take(10).forEach {
                                combined.add(CombinedItem.ReceiptItem(it))
                            }
                            allExpenses.take(10).forEach {
                                combined.add(CombinedItem.ExpenseItem(it))
                            }
                            val sortedCombined = combined.sortedByDescending { it.timestamp }

                            receiptDao.getReceiptCount().collect { receiptCount ->
                                expenseDao.getExpenseCount().collect { expenseCount ->
                                    _uiState.value = HomeUiState(
                                        // Today
                                        todayCollected = todayCollected,
                                        todayPending = todayPending,
                                        todayExpenses = todayExpenseTotal,
                                        todayNetBalance = todayCollected - todayExpenseTotal,
                                        // Overall
                                        overallCollected = overallCollected,
                                        overallPending = overallPending,
                                        overallExpenses = overallExpenses,
                                        overallNetBalance = overallCollected - overallExpenses,
                                        // Feed
                                        recentReceipts = allReceipts.take(5),
                                        recentExpenses = allExpenses.take(5),
                                        combinedItems = sortedCombined.take(10),
                                        receiptCount = receiptCount,
                                        expenseCount = expenseCount,
                                        isLoading = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun refreshData() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadDashboardData()
    }
}