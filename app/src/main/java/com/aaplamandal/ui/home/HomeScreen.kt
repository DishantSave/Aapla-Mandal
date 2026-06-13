package com.aaplamandal.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaplamandal.R
import com.aaplamandal.data.local.entities.Receipt
import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.ui.print.PrintDialog
import com.aaplamandal.ui.print.PrintViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddReceipt: () -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    onNavigateToEditReceipt: (Long) -> Unit = {},
    onNavigateToEditExpense: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    printViewModel: PrintViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val uiState by viewModel.uiState.collectAsState()
    val printUiState by printViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(printUiState.successMessage) {
        printUiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            printViewModel.clearMessages()
        }
    }

    LaunchedEffect(printUiState.errorMessage) {
        printUiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            printViewModel.clearMessages()
        }
    }

    if (printUiState.showPrintDialog) {
        PrintDialog(
            viewModel = printViewModel,
            onDismiss = { printViewModel.dismissPrintDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 1. OVERALL SUMMARY ──────────────────────────────
                item {
                    Text(
                        text = "Overall Summary",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Collected + Expenses
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Collected",
                            amount = uiState.overallCollected,
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF4CAF50)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Expenses",
                            amount = uiState.overallExpenses,
                            icon = Icons.Default.TrendingDown,
                            color = Color(0xFFF44336)
                        )
                    }
                }

                // Balance Pending
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Balance Pending",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "₹${String.format("%.2f", uiState.overallPending)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                // Net Balance
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.overallNetBalance >= 0)
                                Color(0xFF2196F3) else Color(0xFFF44336)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Net Balance (Collected - Expenses)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "₹${String.format("%.2f", uiState.overallNetBalance)}",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Divider ─────────────────────────────────────────
                item {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                // ── 2. TODAY'S SUMMARY ──────────────────────────────
                item {
                    Text(
                        text = "Today's Summary",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Collected + Expenses today
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Collected",
                            amount = uiState.todayCollected,
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF4CAF50)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Expenses",
                            amount = uiState.todayExpenses,
                            icon = Icons.Default.TrendingDown,
                            color = Color(0xFFF44336)
                        )
                    }
                }

                // Balance Pending today
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Balance Pending",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "₹${String.format("%.2f", uiState.todayPending)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                // Net Balance today
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.todayNetBalance >= 0)
                                Color(0xFF2196F3) else Color(0xFFF44336)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Net Balance (Collected - Expenses)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "₹${String.format("%.2f", uiState.todayNetBalance)}",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Divider ─────────────────────────────────────────
                item {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                // ── 3. QUICK ACTIONS ────────────────────────────────
                item {
                    Text(
                        text = "Quick Actions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ReceiptLong,
                            text = "New Receipt",
                            onClick = onNavigateToAddReceipt
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.RemoveCircle,
                            text = "New Expense",
                            onClick = onNavigateToAddExpense
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Upload,
                            text = "Export Data",
                            onClick = onNavigateToExport
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Download,
                            text = "Import Data",
                            onClick = onNavigateToImport
                        )
                    }
                }

                // ── 4. RECENT ACTIVITY ──────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${uiState.receiptCount} receipts · ${uiState.expenseCount} expenses",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Empty state
                if (uiState.combinedItems.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No activity yet",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Add receipts or expenses to get started",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.combinedItems) { item ->
                        when (item) {
                            is CombinedItem.ReceiptItem -> ReceiptItemCard(
                                receipt = item.receipt,
                                onClick = { onNavigateToEditReceipt(item.receipt.id) },
                                onPrintClick = {
                                    printViewModel.showPrintOptions(receiptId = item.receipt.id)
                                }
                            )
                            is CombinedItem.ExpenseItem -> ExpenseItemCard(
                                expense = item.expense,
                                onClick = { onNavigateToEditExpense(item.expense.id) },
                                onPrintClick = {
                                    printViewModel.showPrintOptions(expenseId = item.expense.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Stat Card ────────────────────────────────────────────────────────────────
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹${String.format("%.2f", amount)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ── Action Button ────────────────────────────────────────────────────────────
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, fontSize = 12.sp)
        }
    }
}

// ── Receipt Item Card ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptItemCard(
    receipt: Receipt,
    onClick: () -> Unit = {},
    onPrintClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listOf(
                            receipt.suffix, receipt.firstName,
                            receipt.middleName, receipt.surname
                        ).filter { it.isNotBlank() }.joinToString(" "),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (receipt.buildingName.isNotBlank() || receipt.wing.isNotBlank()
                        || receipt.roomNumber.isNotBlank()
                    ) {
                        Text(
                            text = listOf(receipt.buildingName, receipt.wing, receipt.roomNumber)
                                .filter { it.isNotBlank() }.joinToString(", "),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Collector: ${receipt.collectorName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(receipt.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+₹${String.format("%.2f", receipt.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (receipt.paymentStatus == "paid")
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else Color(0xFFFF9800).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = receipt.paymentStatus.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (receipt.paymentStatus == "paid")
                                Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onPrintClick) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = "Print Receipt",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print", fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Expense Item Card ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit = {},
    onPrintClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.expenseDescription,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = expense.expenseLocation,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "By: ${expense.expensedBy} (${expense.authority})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(expense.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-₹${String.format("%.2f", expense.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = expense.category.uppercase(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onPrintClick) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = "Print Expense",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print", fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Timestamp Formatter ──────────────────────────────────────────────────────
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}