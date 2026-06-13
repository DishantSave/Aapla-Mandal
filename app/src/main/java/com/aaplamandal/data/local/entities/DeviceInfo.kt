package com.aaplamandal.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_info")
data class DeviceInfo(

    @PrimaryKey
    val id: Int = 1,                        // Always 1 — only one config per device database

    // --- Device Identity ---
    @ColumnInfo(name = "device_id")
    val deviceId: String,                   // e.g. DEVICE-001, DEVICE-002

    @ColumnInfo(name = "device_name")
    val deviceName: String,                 // e.g. "Main Table", "Team A", "North Wing"

    // --- Mandal / Organization Details ---
    @ColumnInfo(name = "mandal_name")
    val mandalName: String,                 // e.g. "Aapla Mandal"

    @ColumnInfo(name = "mandal_address")
    val mandalAddress: String,              // Full address of the Mandal

    @ColumnInfo(name = "mandal_ward")
    val mandalWard: String = "",            // Ward / Division / Area name

    @ColumnInfo(name = "mandal_city")
    val mandalCity: String,                 // e.g. "Mumbai", "Pune"

    @ColumnInfo(name = "mandal_registration_number")
    val mandalRegistrationNumber: String = "", // Official trust/society registration number if any

    // --- Festival Details ---
    @ColumnInfo(name = "festival_year")
    val festivalYear: Int,                  // e.g. 2025 — used in unique ID generation

    @ColumnInfo(name = "festival_name")
    val festivalName: String = "Ganesh Festival", // In case it's reused for other festivals

    // --- Contact Details ---
    @ColumnInfo(name = "contact_number")
    val contactNumber: String,              // Primary contact of the Mandal

    @ColumnInfo(name = "alternate_contact")
    val alternateContact: String = "",      // Secondary contact

    @ColumnInfo(name = "email")
    val email: String = "",                 // Optional email for records

    // --- Payment Details ---
    @ColumnInfo(name = "upi_id")
    val upiId: String = "",                 // For generating UPI payment QR codes

    @ColumnInfo(name = "bank_account_name")
    val bankAccountName: String = "",       // Account holder name

    @ColumnInfo(name = "bank_account_number")
    val bankAccountNumber: String = "",     // For records/printing on receipts

    @ColumnInfo(name = "bank_ifsc")
    val bankIfsc: String = "",              // IFSC code

    @ColumnInfo(name = "bank_name")
    val bankName: String = "",              // e.g. "SBI", "Bank of Maharashtra"

    // --- Printing ---
    @ColumnInfo(name = "printer_address")
    val printerAddress: String? = null,     // Bluetooth MAC address of thermal printer

    @ColumnInfo(name = "receipt_footer_note")
    val receiptFooterNote: String = "",     // e.g. "Jay Ganesh! Thank you for your contribution."

    // --- Sync & Audit ---
    @ColumnInfo(name = "last_sync")
    val lastSync: Long? = null,             // Timestamp of last data export/sync

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(), // When device was first configured

    @ColumnInfo(name = "logo_image_path")
    val logoImagePath: String? = null,
)