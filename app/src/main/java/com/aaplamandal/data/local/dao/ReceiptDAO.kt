package com.aaplamandal.data.local.dao

import androidx.room.*
import com.aaplamandal.data.local.entities.Receipt
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDAO {

    // --- Insert / Update / Delete ---

    @Insert
    suspend fun insert(receipt: Receipt): Long

    @Insert
    suspend fun insertAll(receipts: List<Receipt>)

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    // --- Fetch by ID ---

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): Receipt?

    @Query("SELECT * FROM receipts WHERE unique_receipt_id = :uniqueReceiptId LIMIT 1")
    suspend fun getByUniqueReceiptId(uniqueReceiptId: String): Receipt?

    // --- Fetch All ---

    @Query("SELECT * FROM receipts ORDER BY created_at DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    // --- Fetch by Device ---

    @Query("SELECT * FROM receipts WHERE device_id = :deviceId ORDER BY created_at DESC")
    fun getReceiptsByDevice(deviceId: String): Flow<List<Receipt>>

    // --- Fetch by Date Range ---

    @Query("SELECT * FROM receipts WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    suspend fun getReceiptsByDateRange(startDate: Long, endDate: Long): List<Receipt>

    // Get today's receipts
    @Query("SELECT * FROM receipts WHERE created_at >= :todayStart ORDER BY created_at DESC")
    fun getTodayReceipts(todayStart: Long): Flow<List<Receipt>>

    // --- Fetch by Payment Status ---

    // Get all paid receipts
    @Query("SELECT * FROM receipts WHERE payment_status = 'paid' ORDER BY created_at DESC")
    fun getPaidReceipts(): Flow<List<Receipt>>

    // Get all balance (pending) receipts
    @Query("SELECT * FROM receipts WHERE payment_status = 'balance' ORDER BY created_at DESC")
    fun getBalanceReceipts(): Flow<List<Receipt>>

    // --- Fetch by Collector ---

    @Query("SELECT * FROM receipts WHERE collector_name = :collectorName ORDER BY created_at DESC")
    fun getReceiptsByCollector(collectorName: String): Flow<List<Receipt>>

    // --- Search ---

    // Search by donor name (first, middle, surname), contact, building, or address
    @Query("""
        SELECT * FROM receipts 
        WHERE first_name LIKE '%' || :query || '%' 
           OR middle_name LIKE '%' || :query || '%' 
           OR surname LIKE '%' || :query || '%' 
           OR contact_number LIKE '%' || :query || '%'
           OR building_name LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
           OR room_number LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchReceipts(query: String): Flow<List<Receipt>>

    // --- Aggregates / Summary ---

    // Total collected (paid only)
    @Query("SELECT SUM(amount) FROM receipts WHERE payment_status = 'paid'")
    fun getTotalCollected(): Flow<Double?>

    // Total including balance (promised amount)
    @Query("SELECT SUM(amount) FROM receipts")
    fun getTotalPromised(): Flow<Double?>

    // Total balance pending
    @Query("SELECT SUM(amount) FROM receipts WHERE payment_status = 'balance'")
    fun getTotalPending(): Flow<Double?>

    // Total collected today
    @Query("SELECT SUM(amount) FROM receipts WHERE payment_status = 'paid' AND created_at >= :todayStart")
    fun getTodayCollected(todayStart: Long): Flow<Double?>

    // Total collected by date range
    @Query("SELECT SUM(amount) FROM receipts WHERE payment_status = 'paid' AND created_at BETWEEN :startDate AND :endDate")
    suspend fun getTotalCollectedByDateRange(startDate: Long, endDate: Long): Double?

    // Total collected by collector
    @Query("SELECT SUM(amount) FROM receipts WHERE collector_name = :collectorName AND payment_status = 'paid'")
    suspend fun getTotalCollectedByCollector(collectorName: String): Double?

    @Query("SELECT COUNT(*) FROM receipts")
    fun getReceiptCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM receipts WHERE payment_status = 'paid'")
    fun getPaidReceiptCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM receipts WHERE payment_status = 'balance'")
    fun getBalanceReceiptCount(): Flow<Int>

    // --- Distinct Dropdowns (for filter/search UI) ---

    @Query("SELECT DISTINCT collector_name FROM receipts ORDER BY collector_name")
    fun getAllCollectors(): Flow<List<String>>

    @Query("SELECT DISTINCT building_name FROM receipts WHERE building_name != '' ORDER BY building_name")
    fun getAllBuildings(): Flow<List<String>>

    @Query("SELECT DISTINCT wing FROM receipts WHERE wing != '' ORDER BY wing")
    fun getAllWings(): Flow<List<String>>

    // --- Sequence Number (for unique ID generation) ---

    @Query("SELECT MAX(sequence_number) FROM receipts WHERE device_id = :deviceId")
    suspend fun getLastSequenceNumber(deviceId: String): Int?

    // --- Sync ---

    @Query("SELECT * FROM receipts WHERE synced = 0 ORDER BY created_at DESC")
    suspend fun getUnsyncedReceipts(): List<Receipt>

    @Query("UPDATE receipts SET synced = 1, sync_batch_id = :batchId WHERE id = :receiptId")
    suspend fun markAsSynced(receiptId: Long, batchId: String)

    @Query("UPDATE receipts SET synced = 1, sync_batch_id = :batchId WHERE synced = 0")
    suspend fun markAllAsSynced(batchId: String)

    // --- Payment Status Update ---

    // Mark a balance receipt as paid (when donor completes payment later)
    @Query("UPDATE receipts SET payment_status = 'paid' WHERE id = :receiptId")
    suspend fun markAsPaid(receiptId: Long)

    // --- Reset (for testing) ---

    @Query("DELETE FROM receipts")
    suspend fun deleteAll()
}