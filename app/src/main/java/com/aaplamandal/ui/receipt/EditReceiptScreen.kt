package com.aaplamandal.ui.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptScreen(
    receiptId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditReceiptViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load receipt on first launch
    LaunchedEffect(receiptId) {
        viewModel.loadReceipt(receiptId)
    }

    // Navigate back on save or delete
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            viewModel.resetSavedFlag()
            onNavigateBack()
        }
    }
    LaunchedEffect(uiState.deletedSuccessfully) {
        if (uiState.deletedSuccessfully) onNavigateBack()
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Receipt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This action cannot be undone. Enter the delete password to confirm.")
                    OutlinedTextField(
                        value = uiState.deletePassword,
                        onValueChange = { viewModel.updateDeletePassword(it) },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = uiState.deletePasswordError != null,
                        supportingText = {
                            uiState.deletePasswordError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Delete button in top bar
                    IconButton(
                        onClick = { viewModel.showDeleteDialog() },
                        enabled = !uiState.isDeleting && !uiState.isSaving
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Receipt",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Donor Personal Details ──────────────────────────────
            SectionHeader("Donor Details")

            var suffixExpanded by remember { mutableStateOf(false) }
            val suffixOptions = listOf("Shri", "Smt", "Mr", "Mrs", "Ms", "Dr", "Kumari")

            ExposedDropdownMenuBox(
                expanded = suffixExpanded,
                onExpandedChange = { suffixExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.suffix,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Suffix") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = suffixExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = suffixExpanded,
                    onDismissRequest = { suffixExpanded = false }
                ) {
                    suffixOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { viewModel.updateSuffix(option); suffixExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = { viewModel.updateFirstName(it) },
                label = { Text("First Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.firstNameError != null,
                supportingText = {
                    uiState.firstNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.middleName,
                onValueChange = { viewModel.updateMiddleName(it) },
                label = { Text("Middle Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.surname,
                onValueChange = { viewModel.updateSurname(it) },
                label = { Text("Surname *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.surnameError != null,
                supportingText = {
                    uiState.surnameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true
            )

            // ── Address Details ─────────────────────────────────────
            SectionHeader("Address Details")

            OutlinedTextField(
                value = uiState.buildingName,
                onValueChange = { viewModel.updateBuildingName(it) },
                label = { Text("Building Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.wing,
                    onValueChange = { viewModel.updateWing(it) },
                    label = { Text("Wing") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.roomNumber,
                    onValueChange = { viewModel.updateRoomNumber(it) },
                    label = { Text("Room No.") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = uiState.address,
                onValueChange = { viewModel.updateAddress(it) },
                label = { Text("Area / Street") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            OutlinedTextField(
                value = uiState.contactNumber,
                onValueChange = { viewModel.updateContactNumber(it) },
                label = { Text("Contact Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            // ── Payment Details ─────────────────────────────────────
            SectionHeader("Payment Details")

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.amountError != null,
                supportingText = {
                    uiState.amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                prefix = { Text("₹") }
            )

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

            // Payment Status toggle — most important for balance → paid updates
            Text(
                text = "Payment Status *",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("paid" to "Paid", "balance" to "Balance").forEach { (value, label) ->
                    FilterChip(
                        selected = uiState.paymentStatus == value,
                        onClick = { viewModel.updatePaymentStatus(value) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Collection Details ──────────────────────────────────
            SectionHeader("Collection Details")

            OutlinedTextField(
                value = uiState.collectorName,
                onValueChange = { viewModel.updateCollectorName(it) },
                label = { Text("Collector Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.collectorNameError != null,
                supportingText = {
                    uiState.collectorNameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save / Cancel ───────────────────────────────────────
            Button(
                onClick = { viewModel.saveReceipt() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && !uiState.isDeleting
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save Changes")
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