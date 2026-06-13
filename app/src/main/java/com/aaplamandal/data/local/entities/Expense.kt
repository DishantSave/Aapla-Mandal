package com.aaplamandal.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // --- Device Info ---
    @ColumnInfo(name = "device_id")
    val deviceId: String,                   // e.g. DEVICE-001

    @ColumnInfo(name = "device_name")
    val deviceName: String,                 // e.g. "Main Counter", "Team A"

    // --- Expense Numbering ---
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,                // Per-device counter: 1, 2, 3...

    @ColumnInfo(name = "unique_expense_id")
    val uniqueExpenseId: String,            // e.g. DEVICE-001-EXP-2025-0001 (unique across all devices)

    // --- Where ---
    @ColumnInfo(name = "expense_location")
    val expenseLocation: String,            // Where the expense was made e.g. "Flower Market, Dadar"

    @ColumnInfo(name = "expense_description")
    val expenseDescription: String,         // What was purchased/paid for e.g. "Decoration Flowers"

    @ColumnInfo(name = "category")
    val category: String,                   // e.g. Decoration, Prasad, Sound, Transport, Pooja Sadhane, Other

    // --- Who Expensed ---
    @ColumnInfo(name = "expensed_by")
    val expensedBy: String,                 // Name of person who made the expense

    @ColumnInfo(name = "contact_number")
    val contactNumber: String,              // Contact of the expensor

    @ColumnInfo(name = "authority")
    val authority: String,                  // Role/authority of expensor e.g. "Treasurer", "President", "Committee Member"

    // --- Payment Details ---
    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "amount_in_words")
    val amountInWords: String,              // e.g. "Two Thousand Rupees Only"

    // --- Time Tracking ---
    @ColumnInfo(name = "expense_date")
    val expenseDate: Long,                  // Date of actual expense (user selected)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(), // When the record was entered in app

    // --- Optional ---
    @ColumnInfo(name = "notes")
    val notes: String? = null,              // Any additional remarks

    @ColumnInfo(name = "receipt_image_path")
    val receiptImagePath: String? = null,   // Future: photo of physical receipt/bill

    // --- Sync Tracking ---
    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "sync_batch_id")
    val syncBatchId: String? = null
)