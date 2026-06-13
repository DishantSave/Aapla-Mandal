package com.aaplamandal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.aaplamandal.ui.expense.AddExpenseScreen
import com.aaplamandal.ui.expense.EditExpenseScreen
import com.aaplamandal.ui.export.ExportScreen
import com.aaplamandal.ui.home.HomeScreen
import com.aaplamandal.ui.import_data.ImportScreen
import com.aaplamandal.ui.login.LoginScreen
import com.aaplamandal.ui.receipt.AddReceiptScreen
import com.aaplamandal.ui.receipt.EditReceiptScreen
import androidx.activity.compose.BackHandler
import com.aaplamandal.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InitializationHelper.initializeDeviceIfNeeded(this)

        setContent {
            MaterialTheme {
                Surface {
                    // App always starts on the login screen
                    var currentScreen by remember { mutableStateOf("login") }
                    var selectedReceiptId by remember { mutableStateOf(0L) }
                    var selectedExpenseId by remember { mutableStateOf(0L) }

                    BackHandler(enabled = currentScreen !in listOf("login", "home")) {
                        currentScreen = "home"
                    }

                    when (currentScreen) {
                        "login" -> LoginScreen(
                            onLoginSuccess = { currentScreen = "home" }
                        )
                        "home" -> HomeScreen(
                            onNavigateToAddReceipt = { currentScreen = "add_receipt" },
                            onNavigateToAddExpense = { currentScreen = "add_expense" },
                            onNavigateToExport = { currentScreen = "export" },
                            onNavigateToImport = { currentScreen = "import" },
                            onNavigateToSettings = { currentScreen = "settings" },
                            onNavigateToEditReceipt = { receiptId ->
                                selectedReceiptId = receiptId
                                currentScreen = "edit_receipt"
                            },
                            onNavigateToEditExpense = { expenseId ->
                                selectedExpenseId = expenseId
                                currentScreen = "edit_expense"
                            }
                        )
                        "add_receipt" -> AddReceiptScreen(
                            onNavigateBack = { currentScreen = "home" }
                        )
                        "edit_receipt" -> EditReceiptScreen(
                            receiptId = selectedReceiptId,
                            onNavigateBack = { currentScreen = "home" }
                        )
                        "add_expense" -> AddExpenseScreen(
                            onNavigateBack = { currentScreen = "home" }
                        )
                        "edit_expense" -> EditExpenseScreen(
                            expenseId = selectedExpenseId,
                            onNavigateBack = { currentScreen = "home" }
                        )
                        "export" -> ExportScreen(
                            onNavigateBack = { currentScreen = "home" }
                        )
                        "import" -> ImportScreen(
                            onNavigateBack = { currentScreen = "home" }
                        )
                        "settings" -> SettingsScreen(
                            onNavigateBack = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}