package com.aaplamandal.utils

import android.content.Context
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.data.local.entities.Receipt
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

data class ImportResult(
    val success: Boolean,
    val receiptsImported: Int = 0,
    val expensesImported: Int = 0,
    val duplicatesSkipped: Int = 0,
    val errorMessage: String? = null
)

data class ImportSummary(
    val totalFiles: Int,
    val successfulImports: Int,
    val totalReceipts: Int,
    val totalExpenses: Int,
    val totalDuplicates: Int,
    val errors: List<String> = emptyList()
)

class ImportManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val receiptDao = database.receiptDao()
    private val expenseDao = database.expenseDao()
    private val gson = Gson()

    /**
     * Import data from JSON file
     */
    suspend fun importFromJson(file: File): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = FileReader(file).use { it.readText() }
            val exportData = gson.fromJson(json, ExportData::class.java)

            var receiptsImported = 0
            var expensesImported = 0
            var duplicatesSkipped = 0

            // Import receipts
            // Duplicate check: uniqueReceiptId
            // ID: always 0 so Room auto-generates a fresh local ID
            exportData.receipts.forEach { receipt ->
                try {
                    val exists = receiptDao.getByUniqueReceiptId(receipt.uniqueReceiptId)
                    if (exists == null) {
                        receiptDao.insert(receipt.copy(id = 0))
                        receiptsImported++
                    } else {
                        duplicatesSkipped++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImportManager", "Receipt insert error: ${e.message}")
                    duplicatesSkipped++
                }
            }

            // Import expenses
            // Duplicate check: uniqueExpenseId
            // ID: always 0 so Room auto-generates a fresh local ID
            exportData.expenses.forEach { expense ->
                try {
                    val exists = expenseDao.getByUniqueExpenseId(expense.uniqueExpenseId)
                    if (exists == null) {
                        expenseDao.insert(expense.copy(id = 0))
                        expensesImported++
                    } else {
                        duplicatesSkipped++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImportManager", "Expense insert error: ${e.message}")
                    duplicatesSkipped++
                }
            }

            ImportResult(
                success = true,
                receiptsImported = receiptsImported,
                expensesImported = expensesImported,
                duplicatesSkipped = duplicatesSkipped
            )

        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                errorMessage = e.message ?: "Failed to import JSON"
            )
        }
    }

    /**
     * Import data from SQLite database file
     */
    suspend fun importFromDatabase(file: File): ImportResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext ImportResult(
                    success = false,
                    errorMessage = "Database file not found at: ${file.absolutePath}"
                )
            }

            // Step 1: Copy .db to cache
            val cacheDb = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.db")
            val cacheWal = File(context.cacheDir, "${cacheDb.name}-wal")
            val cacheShm = File(context.cacheDir, "${cacheDb.name}-shm")

            file.copyTo(cacheDb, overwrite = true)

            // Step 2: Copy WAL/SHM if they exist alongside the .db file
            val walFile = File(file.parent, "${file.name}-wal")
            val shmFile = File(file.parent, "${file.name}-shm")

            if (walFile.exists()) {
                walFile.copyTo(cacheWal, overwrite = true)
                android.util.Log.d("ImportManager", "WAL file copied, size: ${walFile.length()}")
            }
            if (shmFile.exists()) {
                shmFile.copyTo(cacheShm, overwrite = true)
            }

            android.util.Log.d("ImportManager", "DB file size: ${file.length()}")

            // Step 3: Open and checkpoint
            val externalDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                cacheDb.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            )
            externalDb.execSQL("PRAGMA wal_checkpoint(FULL)")

            // Step 4: List tables
            val tablesCursor = externalDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null
            )
            val tables = mutableListOf<String>()
            tablesCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }
            android.util.Log.d("ImportManager", "Tables found: $tables")

            if (!tables.any { it.equals("receipts", ignoreCase = true) }) {
                externalDb.close()
                cacheDb.delete(); cacheWal.delete(); cacheShm.delete()
                return@withContext ImportResult(
                    success = false,
                    errorMessage = "No receipts table found. Tables: $tables"
                )
            }

            var receiptsImported = 0
            var expensesImported = 0
            var duplicatesSkipped = 0

            // Step 5: Import receipts
            // id = 0 → Room auto-generates a fresh local ID, ignoring source device's ID
            // Duplicate check → uniqueReceiptId only
            val receiptCursor = externalDb.rawQuery("SELECT * FROM receipts", null)
            receiptCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val uniqueReceiptId = cursor.getString(
                            cursor.getColumnIndexOrThrow("unique_receipt_id")
                        )
                        val exists = receiptDao.getByUniqueReceiptId(uniqueReceiptId)
                        if (exists == null) {
                            val receipt = Receipt(
                                id = 0,             // ← never read from source, always 0
                                deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                                deviceName = cursor.getString(cursor.getColumnIndexOrThrow("device_name")),
                                sequenceNumber = cursor.getInt(cursor.getColumnIndexOrThrow("sequence_number")),
                                uniqueReceiptId = uniqueReceiptId,
                                suffix = cursor.getString(cursor.getColumnIndexOrThrow("suffix")),
                                firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                                middleName = cursor.getString(cursor.getColumnIndexOrThrow("middle_name")),
                                surname = cursor.getString(cursor.getColumnIndexOrThrow("surname")),
                                buildingName = cursor.getString(cursor.getColumnIndexOrThrow("building_name")),
                                wing = cursor.getString(cursor.getColumnIndexOrThrow("wing")),
                                roomNumber = cursor.getString(cursor.getColumnIndexOrThrow("room_number")),
                                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                                contactNumber = cursor.getString(cursor.getColumnIndexOrThrow("contact_number")),
                                amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                                amountInWords = cursor.getString(cursor.getColumnIndexOrThrow("amount_in_words")),
                                paymentStatus = cursor.getString(cursor.getColumnIndexOrThrow("payment_status")),
                                collectorName = cursor.getString(cursor.getColumnIndexOrThrow("collector_name")),
                                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                                synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1,
                                syncBatchId = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("sync_batch_id"))
                            )
                            receiptDao.insert(receipt)
                            receiptsImported++
                        } else {
                            duplicatesSkipped++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ImportManager", "Receipt row error: ${e.message}")
                    }
                }
            }

            // Step 6: Import expenses
            // id = 0 → Room auto-generates a fresh local ID, ignoring source device's ID
            // Duplicate check → uniqueExpenseId only
            if (tables.any { it.equals("expenses", ignoreCase = true) }) {
                val expenseCursor = externalDb.rawQuery("SELECT * FROM expenses", null)
                expenseCursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val uniqueExpenseId = cursor.getString(
                                cursor.getColumnIndexOrThrow("unique_expense_id")
                            )
                            val exists = expenseDao.getByUniqueExpenseId(uniqueExpenseId)
                            if (exists == null) {
                                val expense = Expense(
                                    id = 0,         // ← never read from source, always 0
                                    deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                                    deviceName = cursor.getString(cursor.getColumnIndexOrThrow("device_name")),
                                    sequenceNumber = cursor.getInt(cursor.getColumnIndexOrThrow("sequence_number")),
                                    uniqueExpenseId = uniqueExpenseId,
                                    expenseLocation = cursor.getString(cursor.getColumnIndexOrThrow("expense_location")),
                                    expenseDescription = cursor.getString(cursor.getColumnIndexOrThrow("expense_description")),
                                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                                    expensedBy = cursor.getString(cursor.getColumnIndexOrThrow("expensed_by")),
                                    contactNumber = cursor.getString(cursor.getColumnIndexOrThrow("contact_number")),
                                    authority = cursor.getString(cursor.getColumnIndexOrThrow("authority")),
                                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                                    amountInWords = cursor.getString(cursor.getColumnIndexOrThrow("amount_in_words")),
                                    expenseDate = cursor.getLong(cursor.getColumnIndexOrThrow("expense_date")),
                                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                                    notes = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("notes")),
                                    receiptImagePath = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("receipt_image_path")),
                                    synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1,
                                    syncBatchId = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("sync_batch_id"))
                                )
                                expenseDao.insert(expense)
                                expensesImported++
                            } else {
                                duplicatesSkipped++
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ImportManager", "Expense row error: ${e.message}")
                        }
                    }
                }
            }

            externalDb.close()
            cacheDb.delete(); cacheWal.delete(); cacheShm.delete()

            ImportResult(
                success = true,
                receiptsImported = receiptsImported,
                expensesImported = expensesImported,
                duplicatesSkipped = duplicatesSkipped
            )

        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                errorMessage = e.message ?: "Failed to import database"
            )
        }
    }

    /**
     * Import multiple files at once (batch import for consolidation)
     */
    suspend fun importMultipleFiles(
        files: List<File>,
        originalNames: List<String> = emptyList()
    ): ImportSummary = withContext(Dispatchers.IO) {
        val results = mutableListOf<ImportResult>()
        val errors = mutableListOf<String>()

        files.forEachIndexed { index, file ->
            val originalName = originalNames.getOrNull(index) ?: file.name
            android.util.Log.d("ImportManager", "Processing: $originalName")

            val result = when {
                originalName.endsWith(".json", ignoreCase = true) -> importFromJson(file)
                originalName.endsWith(".db", ignoreCase = true) -> importFromDatabase(file)
                else -> {
                    errors.add("Unsupported file type: $originalName")
                    ImportResult(success = false, errorMessage = "Unsupported file type")
                }
            }

            results.add(result)
            if (!result.success && result.errorMessage != null) {
                errors.add("$originalName: ${result.errorMessage}")
            }
        }

        ImportSummary(
            totalFiles = files.size,
            successfulImports = results.count { it.success },
            totalReceipts = results.sumOf { it.receiptsImported },
            totalExpenses = results.sumOf { it.expensesImported },
            totalDuplicates = results.sumOf { it.duplicatesSkipped },
            errors = errors
        )
    }

    private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }
}