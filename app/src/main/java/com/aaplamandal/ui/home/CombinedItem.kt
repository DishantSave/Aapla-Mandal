package com.aaplamandal.ui.home

import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.data.local.entities.Receipt

sealed class CombinedItem(val timestamp: Long) {
    data class ReceiptItem(val receipt: Receipt) : CombinedItem(receipt.createdAt)
    data class ExpenseItem(val expense: Expense) : CombinedItem(expense.createdAt)
}