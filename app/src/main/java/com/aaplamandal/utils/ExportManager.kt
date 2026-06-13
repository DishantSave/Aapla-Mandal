package com.aaplamandal.utils

import android.content.Context
import android.os.Environment
import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.data.local.entities.Receipt
import com.aaplamandal.data.local.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class ExportData(
    val deviceId: String,
    val deviceName: String,
    val exportDate: String,
    val exportTimestamp: Long,
    val receipts: List<Receipt>,                // renamed from transactions
    val expenses: List<Expense>,
    val summary: ExportSummary
)

data class ExportSummary(
    val totalReceipts: Int,                     // renamed from totalTransactions
    val totalExpenses: Int,
    val totalCollected: Double,                 // paid receipts only
    val totalPending: Double,                   // balance receipts only
    val totalExpenditure: Double,
    val netBalance: Double,                     // totalCollected - totalExpenditure
    val dateRange: DateRange
)

data class DateRange(
    val startDate: String?,
    val endDate: String?
)

class ExportManager(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Export data to JSON format
     */
    fun exportToJson(
        deviceId: String,
        deviceName: String,
        receipts: List<Receipt>,
        expenses: List<Expense>
    ): Result<File> {
        return try {
            val exportData = createExportData(deviceId, deviceName, receipts, expenses)
            val json = gson.toJson(exportData)

            val fileName = generateFileName("json", deviceId)
            val file = createExportFile(fileName)

            FileWriter(file).use { writer ->
                writer.write(json)
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export receipts to CSV format
     */
    fun exportReceiptsToCSV(
        deviceId: String,
        receipts: List<Receipt>
    ): Result<File> {
        return try {
            val fileName = generateFileName("receipts_csv", deviceId)
            val file = createExportFile(fileName, ".csv")

            FileWriter(file).use { writer ->
                // Header
                writer.append(
                    "Receipt ID,Sequence No,Device ID,Device Name," +
                            "Suffix,First Name,Middle Name,Surname," +
                            "Building Name,Wing,Room No,Address,Contact," +
                            "Amount,Amount In Words,Payment Status," +
                            "Collector Name,Created Date,Synced\n"
                )

                // Rows
                receipts.forEach { receipt ->
                    val date = formatDate(receipt.createdAt)
                    writer.append("${receipt.uniqueReceiptId},")
                    writer.append("${receipt.sequenceNumber},")
                    writer.append("${receipt.deviceId},")
                    writer.append("${receipt.deviceName},")
                    writer.append("${receipt.suffix},")
                    writer.append("\"${receipt.firstName}\",")
                    writer.append("\"${receipt.middleName}\",")
                    writer.append("\"${receipt.surname}\",")
                    writer.append("\"${receipt.buildingName}\",")
                    writer.append("\"${receipt.wing}\",")
                    writer.append("\"${receipt.roomNumber}\",")
                    writer.append("\"${receipt.address}\",")
                    writer.append("${receipt.contactNumber},")
                    writer.append("${receipt.amount},")
                    writer.append("\"${receipt.amountInWords}\",")
                    writer.append("${receipt.paymentStatus},")
                    writer.append("\"${receipt.collectorName}\",")
                    writer.append("$date,")
                    writer.append("${receipt.synced}\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export expenses to CSV format
     */
    fun exportExpensesToCSV(
        deviceId: String,
        expenses: List<Expense>
    ): Result<File> {
        return try {
            val fileName = generateFileName("expenses_csv", deviceId)
            val file = createExportFile(fileName, ".csv")

            FileWriter(file).use { writer ->
                // Header
                writer.append(
                    "Expense ID,Sequence No,Device ID,Device Name," +
                            "Location,Description,Category," +
                            "Expensed By,Contact,Authority," +
                            "Amount,Amount In Words," +
                            "Expense Date,Created Date,Notes,Synced\n"
                )

                // Rows
                expenses.forEach { expense ->
                    val expenseDate = formatDate(expense.expenseDate)
                    val createdDate = formatDate(expense.createdAt)
                    writer.append("${expense.uniqueExpenseId},")
                    writer.append("${expense.sequenceNumber},")
                    writer.append("${expense.deviceId},")
                    writer.append("${expense.deviceName},")
                    writer.append("\"${expense.expenseLocation}\",")
                    writer.append("\"${expense.expenseDescription}\",")
                    writer.append("${expense.category},")
                    writer.append("\"${expense.expensedBy}\",")
                    writer.append("${expense.contactNumber},")
                    writer.append("${expense.authority},")
                    writer.append("${expense.amount},")
                    writer.append("\"${expense.amountInWords}\",")
                    writer.append("$expenseDate,")
                    writer.append("$createdDate,")
                    writer.append("\"${expense.notes ?: ""}\",")
                    writer.append("${expense.synced}\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export complete database backup
     */
    fun exportDatabase(deviceId: String): Result<File> {
        return try {
            val dbName = "aapla_mandal_app.db"             // updated database name
            val currentDbPath = context.getDatabasePath(dbName)

            if (!currentDbPath.exists()) {
                val dbDir = File(context.applicationInfo.dataDir, "databases")
                if (dbDir.exists()) {
                    val dbFiles = dbDir.listFiles()?.joinToString { it.name } ?: "none"
                    return Result.failure(Exception("Database not found. Available: $dbFiles"))
                }
                return Result.failure(Exception("Database directory not found"))
            }

            val roomDb = AppDatabase.getDatabase(context)
            roomDb.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray<Any?>())
                .close()

            val fileName = generateFileName("database_backup", deviceId)
            val backupFile = createExportFile(fileName, ".db")

            currentDbPath.copyTo(backupFile, overwrite = true)

            val walFile = File(currentDbPath.parent, "$dbName-wal")
            val shmFile = File(currentDbPath.parent, "$dbName-shm")

            if (walFile.exists() && walFile.length() > 0) {
                val walBackup = File(backupFile.parent, "$fileName.db-wal")
                walFile.copyTo(walBackup, overwrite = true)
                android.util.Log.d("ExportManager", "WAL file size: ${walFile.length()}")
            }

            if (shmFile.exists()) {
                val shmBackup = File(backupFile.parent, "$fileName.db-shm")
                shmFile.copyTo(shmBackup, overwrite = true)
            }

            android.util.Log.d("ExportManager", "DB file size: ${currentDbPath.length()}")
            android.util.Log.d("ExportManager", "Backup file size: ${backupFile.length()}")

            Result.success(backupFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Export all data (JSON + CSVs + Database) as a complete backup
     */
    fun exportCompleteBackup(
        deviceId: String,
        deviceName: String,
        receipts: List<Receipt>,
        expenses: List<Expense>
    ): Result<List<File>> {
        val exportedFiles = mutableListOf<File>()

        return try {
            exportToJson(deviceId, deviceName, receipts, expenses).getOrNull()?.let {
                exportedFiles.add(it)
            }
            exportReceiptsToCSV(deviceId, receipts).getOrNull()?.let {
                exportedFiles.add(it)
            }
            exportExpensesToCSV(deviceId, expenses).getOrNull()?.let {
                exportedFiles.add(it)
            }
            exportDatabase(deviceId).getOrNull()?.let {
                exportedFiles.add(it)
            }

            if (exportedFiles.isNotEmpty()) {
                Result.success(exportedFiles)
            } else {
                Result.failure(Exception("No files were exported"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createExportData(
        deviceId: String,
        deviceName: String,
        receipts: List<Receipt>,
        expenses: List<Expense>
    ): ExportData {
        val totalCollected = receipts
            .filter { it.paymentStatus == "paid" }
            .sumOf { it.amount }
        val totalPending = receipts
            .filter { it.paymentStatus == "balance" }
            .sumOf { it.amount }
        val totalExpenditure = expenses.sumOf { it.amount }   // fixed: was it.price

        val allDates = (receipts.map { it.createdAt } + expenses.map { it.createdAt }).sorted()
        val dateRange = if (allDates.isNotEmpty()) {
            DateRange(
                startDate = formatDate(allDates.first()),
                endDate = formatDate(allDates.last())
            )
        } else {
            DateRange(null, null)
        }

        return ExportData(
            deviceId = deviceId,
            deviceName = deviceName,
            exportDate = getCurrentDate(),
            exportTimestamp = System.currentTimeMillis(),
            receipts = receipts,
            expenses = expenses,
            summary = ExportSummary(
                totalReceipts = receipts.size,
                totalExpenses = expenses.size,
                totalCollected = totalCollected,
                totalPending = totalPending,
                totalExpenditure = totalExpenditure,
                netBalance = totalCollected - totalExpenditure,
                dateRange = dateRange
            )
        )
    }

    private fun createExportFile(fileName: String, extension: String = ".json"): File {
        val exportDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "AaplaMandal/Exports"
        )
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return File(exportDir, fileName + extension)
    }

    private fun generateFileName(prefix: String, deviceId: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "${prefix}_${deviceId}_$timestamp"
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())
    }

    fun getExportDirectoryPath(): String {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "AaplaMandal/Exports"
        ).absolutePath
    }
}