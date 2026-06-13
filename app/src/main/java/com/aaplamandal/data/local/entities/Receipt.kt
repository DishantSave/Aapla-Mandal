package com.aaplamandal.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // --- Device Info ---
    @ColumnInfo(name = "device_id")
    val deviceId: String,                   // e.g. DEVICE-001

    @ColumnInfo(name = "device_name")
    val deviceName: String,                 // e.g. "Main Counter", "Team A"

    // --- Receipt Numbering ---
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,                // Per-device counter: 1, 2, 3...

    @ColumnInfo(name = "unique_receipt_id")
    val uniqueReceiptId: String,            // e.g. DEVICE-001-2025-0001 (unique across all devices)

    // --- Donor Personal Details ---
    @ColumnInfo(name = "suffix")
    val suffix: String,                     // Mr, Mrs, Ms, Dr, Shri, Smt, etc.

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "middle_name")
    val middleName: String = "",

    @ColumnInfo(name = "surname")
    val surname: String,

    // --- Address Details ---
    @ColumnInfo(name = "building_name")
    val buildingName: String = "",

    @ColumnInfo(name = "wing")
    val wing: String = "",                  // e.g. A, B, C

    @ColumnInfo(name = "room_number")
    val roomNumber: String = "",

    @ColumnInfo(name = "address")
    val address: String = "",               // Street / Area / Locality

    @ColumnInfo(name = "contact_number")
    val contactNumber: String = "",

    // --- Payment Details ---
    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "amount_in_words")
    val amountInWords: String,              // e.g. "Five Hundred Rupees Only"

    @ColumnInfo(name = "payment_status")
    val paymentStatus: String,              // "paid" or "balance"

    // --- Collection Details ---
    @ColumnInfo(name = "collector_name")
    val collectorName: String,              // Fund collector's name

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // --- Sync Tracking (for multi-device merge) ---
    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "sync_batch_id")
    val syncBatchId: String? = null
)