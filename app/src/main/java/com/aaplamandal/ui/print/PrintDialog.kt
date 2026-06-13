package com.aaplamandal.ui.print

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintDialog(
    viewModel: PrintViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDevicePicker by remember { mutableStateOf(false) }

    // ── Bluetooth permissions ────────────────────────────────────────────────
    // Android 12+ requires BLUETOOTH_CONNECT + BLUETOOTH_SCAN at runtime.
    // Older versions only need BLUETOOTH (granted at install via manifest).
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(Manifest.permission.BLUETOOTH)
    }

    // Check whether all required permissions are currently granted
    fun hasBluetoothPermissions(): Boolean = bluetoothPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    var bluetoothPermissionsGranted by remember {
        mutableStateOf(hasBluetoothPermissions())
    }

    // Launcher that requests all Bluetooth permissions at once and updates state
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        bluetoothPermissionsGranted = results.values.all { it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Print,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Print Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Bluetooth Thermal Print ──────────────────────────────────
                Text(
                    "Bluetooth Thermal Printer",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                if (!bluetoothPermissionsGranted) {
                    // ── Permission rationale card ────────────────────────────
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.BluetoothDisabled,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Bluetooth permission required",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                "Allow Bluetooth access to discover and connect to your thermal printer.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { permissionLauncher.launch(bluetoothPermissions) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                } else {
                    // ── Printer selector ─────────────────────────────────────
                    OutlinedButton(
                        onClick = {
                            // Load paired devices when opening picker
                            if (!showDevicePicker) viewModel.loadPairedDevices()
                            showDevicePicker = !showDevicePicker
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.printerAddress.isBlank()) "Select Printer"
                            else uiState.pairedDevices
                                .find { it.address == uiState.printerAddress }
                                ?.name ?: uiState.printerAddress,
                            maxLines = 1
                        )
                    }

                    // Paired devices list
                    if (showDevicePicker) {
                        if (uiState.pairedDevices.isEmpty()) {
                            Text(
                                "No paired Bluetooth devices found.\nPair your printer in Android Settings first.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Card {
                                Column {
                                    uiState.pairedDevices.forEach { device ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updatePrinterAddress(device.address)
                                                    showDevicePicker = false
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.BluetoothConnected,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    device.name ?: "Unknown Device",
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    device.address,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (device.address == uiState.printerAddress) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.printViaBluetooth() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.printerAddress.isNotBlank() && !uiState.isPrinting
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print via Bluetooth")
                    }
                }

                HorizontalDivider()

                // ── PDF Share ────────────────────────────────────────────────
                Text(
                    "PDF / Share",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "Generate a PDF and share via WhatsApp, Email, or print from any app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { viewModel.shareAsPdf(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isPrinting
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share as PDF")
                }

                // Loading indicator
                if (uiState.isPrinting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Processing...", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}