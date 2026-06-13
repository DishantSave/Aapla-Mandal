package com.aaplamandal.data.local.dao

import androidx.room.*
import com.aaplamandal.data.local.entities.DeviceInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceInfoDAO {

    // Insert or replace device info (only one record with id = 1)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deviceInfo: DeviceInfo)

    // Update device info
    @Update
    suspend fun update(deviceInfo: DeviceInfo)

    // Get device info (always id = 1)
    @Query("SELECT * FROM device_info WHERE id = 1")
    suspend fun getDeviceInfo(): DeviceInfo?

    // Get device info as Flow (live updates)
    @Query("SELECT * FROM device_info WHERE id = 1")
    fun getDeviceInfoFlow(): Flow<DeviceInfo?>

    // Check if device is configured
    @Query("SELECT COUNT(*) FROM device_info WHERE id = 1")
    suspend fun isDeviceConfigured(): Int

    // --- Device ---
    @Query("UPDATE device_info SET device_name = :deviceName WHERE id = 1")
    suspend fun updateDeviceName(deviceName: String)

    // --- Mandal Info ---
    @Query("""
        UPDATE device_info 
        SET mandal_name = :mandalName, 
            mandal_address = :mandalAddress, 
            mandal_ward = :mandalWard, 
            mandal_city = :mandalCity, 
            mandal_registration_number = :mandalRegistrationNumber 
        WHERE id = 1
    """)
    suspend fun updateMandalInfo(
        mandalName: String,
        mandalAddress: String,
        mandalWard: String,
        mandalCity: String,
        mandalRegistrationNumber: String
    )

    // --- Festival Info ---
    @Query("""
        UPDATE device_info 
        SET festival_year = :festivalYear, 
            festival_name = :festivalName 
        WHERE id = 1
    """)
    suspend fun updateFestivalInfo(
        festivalYear: Int,
        festivalName: String
    )

    // --- Contact Info ---
    @Query("""
        UPDATE device_info 
        SET contact_number = :contactNumber, 
            alternate_contact = :alternateContact, 
            email = :email 
        WHERE id = 1
    """)
    suspend fun updateContactInfo(
        contactNumber: String,
        alternateContact: String,
        email: String
    )

    // --- Payment / UPI ---
    @Query("UPDATE device_info SET upi_id = :upiId WHERE id = 1")
    suspend fun updateUpiId(upiId: String)

    @Query("""
        UPDATE device_info 
        SET bank_account_name = :accountName, 
            bank_account_number = :accountNumber, 
            bank_ifsc = :ifsc, 
            bank_name = :bankName 
        WHERE id = 1
    """)
    suspend fun updateBankDetails(
        accountName: String,
        accountNumber: String,
        ifsc: String,
        bankName: String
    )

    // --- Printer ---
    @Query("UPDATE device_info SET printer_address = :printerAddress WHERE id = 1")
    suspend fun updatePrinterAddress(printerAddress: String?)

    // --- Receipt Footer ---
    @Query("UPDATE device_info SET receipt_footer_note = :footerNote WHERE id = 1")
    suspend fun updateReceiptFooterNote(footerNote: String)

    // --- Sync ---
    @Query("UPDATE device_info SET last_sync = :syncTime WHERE id = 1")
    suspend fun updateLastSync(syncTime: Long)

    // Delete device info (for reset)
    @Query("DELETE FROM device_info WHERE id = 1")
    suspend fun deleteDeviceInfo()

    @Query("UPDATE device_info SET logo_image_path = :path WHERE id = 1")
    suspend fun updateLogoImagePath(path: String?)
}