package com.aaplamandal.ui.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddReceiptViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onNavigateBack()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Receipt") },
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

            // ── Donor Personal Details ──────────────────────────────
            SectionHeader("Donor Details")

            // Suffix dropdown
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = suffixExpanded,
                    onDismissRequest = { suffixExpanded = false }
                ) {
                    suffixOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateSuffix(option)
                                suffixExpanded = false
                            }
                        )
                    }
                }
            }

            // First Name
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = { viewModel.updateFirstName(it) },
                label = { Text("First Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.firstNameError != null,
                supportingText = {
                    uiState.firstNameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            // Middle Name
            OutlinedTextField(
                value = uiState.middleName,
                onValueChange = { viewModel.updateMiddleName(it) },
                label = { Text("Middle Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Surname
            OutlinedTextField(
                value = uiState.surname,
                onValueChange = { viewModel.updateSurname(it) },
                label = { Text("Surname *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.surnameError != null,
                supportingText = {
                    uiState.surnameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )

            // ── Address Details ─────────────────────────────────────
            SectionHeader("Address Details")

            // Building Name
            OutlinedTextField(
                value = uiState.buildingName,
                onValueChange = { viewModel.updateBuildingName(it) },
                label = { Text("Building Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Wing + Room Number side by side
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

            // Area / Street Address
            OutlinedTextField(
                value = uiState.address,
                onValueChange = { viewModel.updateAddress(it) },
                label = { Text("Area / Street") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            // Contact Number
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

            // Amount
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

            // Payment Status
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
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save Receipt")
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
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider()
}