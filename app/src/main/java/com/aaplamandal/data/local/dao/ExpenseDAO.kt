package com.aaplamandal.data.local.dao

import androidx.room.*
import com.aaplamandal.data.local.entities.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDAO {

    // --- Insert / Update / Delete ---

    @Insert
    suspend fun insert(expense: Expense): Long

    @Insert
    suspend fun insertAll(expenses: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    // --- Fetch by ID ---

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE unique_expense_id = :uniqueExpenseId LIMIT 1")
    suspend fun getByUniqueExpenseId(uniqueExpenseId: String): Expense?

    // --- Fetch All ---

    @Query("SELECT * FROM expenses ORDER BY created_at DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // --- Fetch by Device ---

    @Query("SELECT * FROM expenses WHERE device_id = :deviceId ORDER BY created_at DESC")
    fun getExpensesByDevice(deviceId: String): Flow<List<Expense>>

    // --- Fetch by Date Range ---
    // Use expense_date for actual expense date filtering
    @Query("SELECT * FROM expenses WHERE expense_date BETWEEN :startDate AND :endDate ORDER BY expense_date DESC")
    suspend fun getExpensesByDateRange(startDate: Long, endDate: Long): List<Expense>

    // Get today's expenses by entry time
    @Query("SELECT * FROM expenses WHERE created_at >= :todayStart ORDER BY created_at DESC")
    fun getTodayExpenses(todayStart: Long): Flow<List<Expense>>

    // --- Fetch by Category ---

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY expense_date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    // --- Fetch by Person ---

    @Query("SELECT * FROM expenses WHERE expensed_by = :person ORDER BY expense_date DESC")
    fun getExpensesByPerson(person: String): Flow<List<Expense>>

    // --- Fetch by Authority ---

    @Query("SELECT * FROM expenses WHERE authority = :authority ORDER BY expense_date DESC")
    fun getExpensesByAuthority(authority: String): Flow<List<Expense>>

    // --- Fetch by Location ---

    @Query("SELECT * FROM expenses WHERE expense_location = :location ORDER BY expense_date DESC")
    fun getExpensesByLocation(location: String): Flow<List<Expense>>

    // --- Search ---

    // Search by description or location or expensed_by
    @Query("""
        SELECT * FROM expenses 
        WHERE expense_description LIKE '%' || :query || '%' 
           OR expense_location LIKE '%' || :query || '%' 
           OR expensed_by LIKE '%' || :query || '%' 
        ORDER BY created_at DESC
    """)
    fun searchExpenses(query: String): Flow<List<Expense>>

    // --- Aggregates / Summary ---

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE expense_date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesByDateRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category")
    suspend fun getTotalExpensesByCategory(category: String): Double?

    @Query("SELECT COUNT(*) FROM expenses")
    fun getExpenseCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM expenses WHERE expense_date BETWEEN :startDate AND :endDate")
    suspend fun getExpenseCountByDateRange(startDate: Long, endDate: Long): Int

    // --- Distinct Dropdowns (for filter/search UI) ---

    @Query("SELECT DISTINCT category FROM expenses ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT expensed_by FROM expenses ORDER BY expensed_by")
    fun getAllExpensors(): Flow<List<String>>

    @Query("SELECT DISTINCT authority FROM expenses ORDER BY authority")
    fun getAllAuthorities(): Flow<List<String>>

    @Query("SELECT DISTINCT expense_location FROM expenses ORDER BY expense_location")
    fun getAllLocations(): Flow<List<String>>

    // --- Sync ---

    @Query("SELECT * FROM expenses WHERE synced = 0 ORDER BY created_at DESC")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Query("UPDATE expenses SET synced = 1, sync_batch_id = :batchId WHERE id = :expenseId")
    suspend fun markAsSynced(expenseId: Long, batchId: String)

    @Query("UPDATE expenses SET synced = 1, sync_batch_id = :batchId WHERE synced = 0")
    suspend fun markAllAsSynced(batchId: String)

    // --- Sequence Number (for unique ID generation) ---

    @Query("SELECT MAX(sequence_number) FROM expenses WHERE device_id = :deviceId")
    suspend fun getLastSequenceNumber(deviceId: String): Int?

    // --- Reset (for testing) ---

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}