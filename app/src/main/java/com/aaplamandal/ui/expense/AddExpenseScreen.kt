package com.aaplamandal.ui.expense

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onNavigateBack()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Where ───────────────────────────────────────────────
            ExpenseSectionHeader("Where")

            OutlinedTextField(
                value = uiState.expenseLocation,
                onValueChange = { viewModel.updateExpenseLocation(it) },
                label = { Text("Location *") },
                placeholder = { Text("e.g. Flower Market, Dadar") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.expenseLocationError != null,
                supportingText = {
                    uiState.expenseLocationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.expenseDescription,
                onValueChange = { viewModel.updateExpenseDescription(it) },
                label = { Text("Description *") },
                placeholder = { Text("e.g. Decoration Flowers") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.expenseDescriptionError != null,
                supportingText = {
                    uiState.expenseDescriptionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            val categoryOptions = listOf(
                "Decoration", "Prasad", "Sound System", "Lighting",
                "Transport", "Pooja Sadhane", "Printing", "Catering", "Miscellaneous"
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateCategory(option)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Who ─────────────────────────────────────────────────
            ExpenseSectionHeader("Who")

            OutlinedTextField(
                value = uiState.expensedBy,
                onValueChange = { viewModel.updateExpensedBy(it) },
                label = { Text("Expensed By *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.expensedByError != null,
                supportingText = {
                    uiState.expensedByError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.contactNumber,
                onValueChange = { viewModel.updateContactNumber(it) },
                label = { Text("Contact Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            // Authority dropdown
            var authorityExpanded by remember { mutableStateOf(false) }
            val authorityOptions = listOf(
                "President", "Vice President", "Treasurer",
                "Secretary", "Committee Member", "Volunteer"
            )

            ExposedDropdownMenuBox(
                expanded = authorityExpanded,
                onExpandedChange = { authorityExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.authority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Authority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authorityExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = authorityExpanded,
                    onDismissRequest = { authorityExpanded = false }
                ) {
                    authorityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateAuthority(option)
                                authorityExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Payment Details ─────────────────────────────────────
            ExpenseSectionHeader("Payment Details")

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.amountError != null,
                supportingText = {
                    uiState.amountError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                prefix = { Text("₹") }
            )

            // Amount in Words (auto-filled but editable)
            OutlinedTextField(
                value = uiState.amountInWords,
                onValueChange = { viewModel.updateAmountInWords(it) },
                label = { Text("Amount in Words *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.amountInWordsError != null,
                supportingText = {
                    uiState.amountInWordsError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("Auto-filled — edit if needed")
                },
                minLines = 2,
                maxLines = 3
            )

            // ── Date ────────────────────────────────────────────────
            ExpenseSectionHeader("Expense Date")

            // Date picker button
            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = uiState.expenseDate
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val selected = Calendar.getInstance().apply {
                                set(year, month, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            viewModel.updateExpenseDate(selected)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(dateFormatter.format(Date(uiState.expenseDate)))
            }

            // ── Notes ───────────────────────────────────────────────
            ExpenseSectionHeader("Additional Notes")

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save / Cancel ───────────────────────────────────────
            Button(
                onClick = { viewModel.saveExpense() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save Expense")
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Section Header Helper ───────────────────────────────────────────────────
@Composable
fun ExpenseSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider()
}