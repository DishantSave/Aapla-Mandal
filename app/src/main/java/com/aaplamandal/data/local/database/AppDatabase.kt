package com.aaplamandal.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aaplamandal.data.local.dao.DeviceInfoDAO
import com.aaplamandal.data.local.dao.ExpenseDAO
import com.aaplamandal.data.local.dao.ReceiptDAO
import com.aaplamandal.data.local.entities.DeviceInfo
import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.data.local.entities.Receipt

@Database(
    entities = [
        Receipt::class,
        Expense::class,
        DeviceInfo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract functions to get DAOs
    abstract fun receiptDao(): ReceiptDAO
    abstract fun expenseDao(): ExpenseDAO
    abstract fun deviceInfoDao(): DeviceInfoDAO

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Database name
        private const val DATABASE_NAME = "aapla_mandal_app.db"

        // Get database instance (creates if doesn't exist)
        fun getDatabase(context: Context): AppDatabase {
            // Return existing instance if available
            return INSTANCE ?: synchronized(this) {
                // Create new instance if null
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // For testing: clear instance
        fun clearInstance() {
            INSTANCE = null
        }
    }
}