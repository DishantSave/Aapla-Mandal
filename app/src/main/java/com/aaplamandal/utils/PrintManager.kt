package com.aaplamandal.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.aaplamandal.data.local.entities.DeviceInfo
import com.aaplamandal.data.local.entities.Expense
import com.aaplamandal.data.local.entities.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val CHARS_PER_LINE = 32

data class PrintResult(
    val success: Boolean,
    val errorMessage: String? = null
)

class PrintManager(private val context: Context) {

    // ─────────────────────────────────────────────────────────────────
    // BLUETOOTH THERMAL PRINTING
    // ─────────────────────────────────────────────────────────────────

    suspend fun printReceiptViaBluetooth(
        receipt: Receipt,
        deviceInfo: DeviceInfo?,
        printerAddress: String
    ): PrintResult = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext PrintResult(false, "Bluetooth not available on this device")

            if (!bluetoothAdapter.isEnabled)
                return@withContext PrintResult(false, "Please enable Bluetooth and try again")

            val device: BluetoothDevice = try {
                bluetoothAdapter.getRemoteDevice(printerAddress)
            } catch (e: SecurityException) {
                return@withContext PrintResult(false, "Bluetooth permission denied. Please grant it in Settings.")
            }

            socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return@withContext PrintResult(false, "Bluetooth permission denied. Please grant it in Settings.")
            }

            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                // BLUETOOTH_SCAN not granted — not critical, continue without cancelling
            }

            try {
                socket.connect()
            } catch (e: SecurityException) {
                return@withContext PrintResult(false, "Bluetooth permission denied. Please grant it in Settings.")
            }

            val outputStream: OutputStream = socket.outputStream
            outputStream.write(buildReceiptEscPos(receipt, deviceInfo))
            outputStream.flush()
            kotlinx.coroutines.delay(1000)
            PrintResult(success = true)

        } catch (e: IOException) {
            PrintResult(false, "Could not connect to printer: ${e.message}")
        } catch (e: Exception) {
            PrintResult(false, e.message ?: "Print failed")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun printExpenseViaBluetooth(
        expense: Expense,
        deviceInfo: DeviceInfo?,
        printerAddress: String
    ): PrintResult = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext PrintResult(false, "Bluetooth not available on this device")

            if (!bluetoothAdapter.isEnabled)
                return@withContext PrintResult(false, "Please enable Bluetooth and try again")

            val device: BluetoothDevice = try {
                bluetoothAdapter.getRemoteDevice(printerAddress)
            } catch (e: SecurityException) {
                return@withContext PrintResult(false, "Bluetooth permission denied. Please grant it in Settings.")
            }

            socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return@withContext PrintResult(false, "Bluetooth permission denied. Please grant it in Settings.")
            }

            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                // not critical, continue
            }

            try {
                socket.connect()
            } catch (e: SecurityException) {
                return@withContext PrintResult(false, "Bluetooth permission denied. Please grant it in Settings.")
            }

            val outputStream: OutputStream = socket.outputStream
            outputStream.write(buildExpenseEscPos(expense, deviceInfo))
            outputStream.flush()
            kotlinx.coroutines.delay(1000)
            PrintResult(success = true)

        } catch (e: IOException) {
            PrintResult(false, "Could not connect to printer: ${e.message}")
        } catch (e: Exception) {
            PrintResult(false, e.message ?: "Print failed")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ESC/POS COMMAND BUILDERS
    // ─────────────────────────────────────────────────────────────────

    private fun buildReceiptEscPos(receipt: Receipt, deviceInfo: DeviceInfo?): ByteArray {
        val cmds = mutableListOf<ByteArray>()

        cmds.add(byteArrayOf(0x1B, 0x40))
        cmds.add(byteArrayOf(0x1B, 0x61, 0x01))

        cmds.add(byteArrayOf(0x1B, 0x21, 0x30))
        cmds.add(textLine(deviceInfo?.mandalName ?: "Aapla Mandal"))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x00))

        cmds.add(textLine(deviceInfo?.mandalAddress ?: ""))
        if (deviceInfo?.mandalCity?.isNotBlank() == true)
            cmds.add(textLine(deviceInfo.mandalCity))
        if (deviceInfo?.contactNumber?.isNotBlank() == true)
            cmds.add(textLine("Ph: ${deviceInfo.contactNumber}"))

        cmds.add(textLine(divider()))
        cmds.add(textLine("DONATION RECEIPT"))
        cmds.add(textLine(divider()))
        cmds.add(byteArrayOf(0x1B, 0x61, 0x00))

        cmds.add(textLine("Receipt No : ${receipt.uniqueReceiptId}"))
        cmds.add(textLine("Date       : ${formatDate(receipt.createdAt)}"))
        cmds.add(textLine(""))

        val fullName = listOf(receipt.suffix, receipt.firstName, receipt.middleName, receipt.surname)
            .filter { it.isNotBlank() }.joinToString(" ")
        cmds.add(textLine("Name       : $fullName"))

        if (receipt.buildingName.isNotBlank() || receipt.wing.isNotBlank() || receipt.roomNumber.isNotBlank()) {
            val addr = listOf(receipt.buildingName, receipt.wing, receipt.roomNumber)
                .filter { it.isNotBlank() }.joinToString(", ")
            cmds.add(textLine("Address    : $addr"))
        }
        if (receipt.address.isNotBlank())
            cmds.add(textLine("           : ${receipt.address}"))
        if (receipt.contactNumber.isNotBlank())
            cmds.add(textLine("Contact    : ${receipt.contactNumber}"))

        cmds.add(textLine(divider()))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x08))
        cmds.add(textLine("Amount     : Rs. ${String.format("%.2f", receipt.amount)}"))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x00))
        cmds.add(textLine("In Words   : ${receipt.amountInWords}"))
        cmds.add(textLine("Status     : ${receipt.paymentStatus.uppercase()}"))

        cmds.add(textLine(divider()))
        cmds.add(textLine("Collector  : ${receipt.collectorName}"))
        cmds.add(textLine(divider()))

        cmds.add(byteArrayOf(0x1B, 0x61, 0x01))
        val footer = deviceInfo?.receiptFooterNote ?: "Jay Ganesh! Thank you for your contribution."
        wrapText(footer, CHARS_PER_LINE).forEach { line -> cmds.add(textLine(line)) }

        cmds.add(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
        cmds.add(byteArrayOf(0x1D, 0x56, 0x41, 0x00))

        return cmds.reduce { acc, bytes -> acc + bytes }
    }

    private fun buildExpenseEscPos(expense: Expense, deviceInfo: DeviceInfo?): ByteArray {
        val cmds = mutableListOf<ByteArray>()

        cmds.add(byteArrayOf(0x1B, 0x40))
        cmds.add(byteArrayOf(0x1B, 0x61, 0x01))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x30))
        cmds.add(textLine(deviceInfo?.mandalName ?: "Aapla Mandal"))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x00))
        cmds.add(textLine(deviceInfo?.mandalAddress ?: ""))
        if (deviceInfo?.contactNumber?.isNotBlank() == true)
            cmds.add(textLine("Ph: ${deviceInfo.contactNumber}"))

        cmds.add(textLine(divider()))
        cmds.add(textLine("EXPENSE VOUCHER"))
        cmds.add(textLine(divider()))
        cmds.add(byteArrayOf(0x1B, 0x61, 0x00))

        cmds.add(textLine("Expense No : ${expense.uniqueExpenseId}"))
        cmds.add(textLine("Date       : ${formatDate(expense.expenseDate)}"))
        cmds.add(textLine("Entry Date : ${formatDate(expense.createdAt)}"))
        cmds.add(textLine(""))
        cmds.add(textLine("Category   : ${expense.category}"))
        cmds.add(textLine("Location   : ${expense.expenseLocation}"))
        cmds.add(textLine("Details    : ${expense.expenseDescription}"))

        cmds.add(textLine(divider()))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x08))
        cmds.add(textLine("Amount     : Rs. ${String.format("%.2f", expense.amount)}"))
        cmds.add(byteArrayOf(0x1B, 0x21, 0x00))
        cmds.add(textLine("In Words   : ${expense.amountInWords}"))

        cmds.add(textLine(divider()))
        cmds.add(textLine("Expensed By: ${expense.expensedBy}"))
        if (expense.contactNumber.isNotBlank())
            cmds.add(textLine("Contact    : ${expense.contactNumber}"))
        cmds.add(textLine("Authority  : ${expense.authority}"))
        if (expense.notes?.isNotBlank() == true)
            cmds.add(textLine("Notes      : ${expense.notes}"))
        cmds.add(textLine(divider()))

        cmds.add(byteArrayOf(0x1B, 0x61, 0x01))
        cmds.add(textLine("Authorized Signature"))
        cmds.add(textLine(""))
        cmds.add(textLine("_________________"))
        cmds.add(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
        cmds.add(byteArrayOf(0x1D, 0x56, 0x41, 0x00))

        return cmds.reduce { acc, bytes -> acc + bytes }
    }

    // ─────────────────────────────────────────────────────────────────
    // PDF GENERATION
    // ─────────────────────────────────────────────────────────────────

    fun generateReceiptPdf(receipt: Receipt, deviceInfo: DeviceInfo?): Result<File> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint  = Paint().apply { textSize = 18f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            val boldPaint   = Paint().apply { textSize = 14f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            val normalPaint = Paint().apply { textSize = 12f; textAlign = Paint.Align.LEFT }
            val centerPaint = Paint().apply { textSize = 12f; textAlign = Paint.Align.CENTER }
            val smallPaint  = Paint().apply { textSize = 10f; textAlign = Paint.Align.LEFT; color = android.graphics.Color.GRAY }
            val dividerPaint = Paint().apply { strokeWidth = 1f; color = android.graphics.Color.GRAY }

            val centerX     = 297f
            val leftMargin  = 60f
            val rightMargin = 535f
            var y = 40f

            fun drawDivider() { canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint); y += 16f }
            fun drawText(text: String, paint: Paint, x: Float = leftMargin) { canvas.drawText(text, x, y, paint); y += paint.textSize + 6f }
            fun drawLabelValue(label: String, value: String) {
                canvas.drawText(label, leftMargin, y, normalPaint)
                canvas.drawText(value, leftMargin + 130f, y, normalPaint)
                y += normalPaint.textSize + 6f
            }

            // ── Logo image ───────────────────────────────────────────
            val logoPath = deviceInfo?.logoImagePath
            if (!logoPath.isNullOrBlank()) {
                val logoFile = File(logoPath)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(logoPath)
                    if (bitmap != null) {
                        val maxW = 475f; val maxH = 100f
                        val scale = minOf(maxW / bitmap.width, maxH / bitmap.height)
                        val drawW = bitmap.width * scale
                        val drawH = bitmap.height * scale
                        val left  = centerX - drawW / 2f
                        val dst   = RectF(left, y, left + drawW, y + drawH)
                        canvas.drawBitmap(bitmap, null, dst, null)
                        y += drawH + 10f
                        bitmap.recycle()
                    }
                }
            }

            // ── Header text ──────────────────────────────────────────
            drawText(deviceInfo?.mandalName ?: "Aapla Mandal", titlePaint, centerX)
            if (deviceInfo?.mandalAddress?.isNotBlank() == true)
                drawText(deviceInfo.mandalAddress, centerPaint, centerX)
            if (deviceInfo?.mandalCity?.isNotBlank() == true)
                drawText(deviceInfo.mandalCity, centerPaint, centerX)
            if (deviceInfo?.contactNumber?.isNotBlank() == true)
                drawText("Ph: ${deviceInfo.contactNumber}", centerPaint, centerX)

            y += 8f; drawDivider()
            drawText("DONATION RECEIPT", boldPaint, centerX)
            drawDivider(); y += 4f

            drawLabelValue("Receipt No :", receipt.uniqueReceiptId)
            drawLabelValue("Date       :", formatDate(receipt.createdAt))
            y += 8f; drawDivider()

            val fullName = listOf(receipt.suffix, receipt.firstName, receipt.middleName, receipt.surname)
                .filter { it.isNotBlank() }.joinToString(" ")
            drawLabelValue("Name       :", fullName)
            if (receipt.buildingName.isNotBlank() || receipt.wing.isNotBlank() || receipt.roomNumber.isNotBlank()) {
                val addr = listOf(receipt.buildingName, receipt.wing, receipt.roomNumber)
                    .filter { it.isNotBlank() }.joinToString(", ")
                drawLabelValue("Address    :", addr)
            }
            if (receipt.address.isNotBlank())       drawLabelValue("           :", receipt.address)
            if (receipt.contactNumber.isNotBlank()) drawLabelValue("Contact    :", receipt.contactNumber)

            y += 8f; drawDivider()
            drawLabelValue("Amount     :", "Rs. ${String.format("%.2f", receipt.amount)}")
            drawLabelValue("In Words   :", receipt.amountInWords)
            drawLabelValue("Status     :", receipt.paymentStatus.uppercase())

            y += 8f; drawDivider()
            drawLabelValue("Collector  :", receipt.collectorName)
            y += 8f; drawDivider(); y += 8f

            val footer = deviceInfo?.receiptFooterNote ?: "Jay Ganesh! Thank you for your contribution."
            drawText(footer, centerPaint, centerX)

            y += 40f; drawDivider()
            val sigY = y + 60f
            canvas.drawLine(leftMargin, sigY, leftMargin + 150f, sigY, dividerPaint)
            canvas.drawLine(rightMargin - 150f, sigY, rightMargin, sigY, dividerPaint)
            canvas.drawText("Collector Signature", leftMargin, sigY + 16f, smallPaint)
            val authPaint = Paint().apply { textSize = 10f; textAlign = Paint.Align.RIGHT; color = android.graphics.Color.GRAY }
            canvas.drawText("Authorized Signatory", rightMargin, sigY + 16f, authPaint)

            pdfDocument.finishPage(page)
            val file = File(context.cacheDir, "Receipt_${receipt.uniqueReceiptId}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            Result.success(file)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun generateExpensePdf(expense: Expense, deviceInfo: DeviceInfo?): Result<File> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint  = Paint().apply { textSize = 18f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            val boldPaint   = Paint().apply { textSize = 14f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            val normalPaint = Paint().apply { textSize = 12f }
            val centerPaint = Paint().apply { textSize = 12f; textAlign = Paint.Align.CENTER }
            val smallPaint  = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }
            val dividerPaint = Paint().apply { strokeWidth = 1f; color = android.graphics.Color.GRAY }

            val centerX     = 297f
            val leftMargin  = 60f
            val rightMargin = 535f
            var y = 40f

            fun drawDivider() { canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint); y += 16f }
            fun drawText(text: String, paint: Paint, x: Float = leftMargin) { canvas.drawText(text, x, y, paint); y += paint.textSize + 6f }
            fun drawLabelValue(label: String, value: String) {
                canvas.drawText(label, leftMargin, y, normalPaint)
                canvas.drawText(value, leftMargin + 130f, y, normalPaint)
                y += normalPaint.textSize + 6f
            }

            // ── Logo image ───────────────────────────────────────────
            val logoPath = deviceInfo?.logoImagePath
            if (!logoPath.isNullOrBlank()) {
                val logoFile = File(logoPath)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(logoPath)
                    if (bitmap != null) {
                        val maxW = 475f; val maxH = 100f
                        val scale = minOf(maxW / bitmap.width, maxH / bitmap.height)
                        val drawW = bitmap.width * scale
                        val drawH = bitmap.height * scale
                        val left  = centerX - drawW / 2f
                        val dst   = RectF(left, y, left + drawW, y + drawH)
                        canvas.drawBitmap(bitmap, null, dst, null)
                        y += drawH + 10f
                        bitmap.recycle()
                    }
                }
            }

            // ── Header text ──────────────────────────────────────────
            drawText(deviceInfo?.mandalName ?: "Aapla Mandal", titlePaint, centerX)
            if (deviceInfo?.mandalAddress?.isNotBlank() == true)
                drawText(deviceInfo.mandalAddress, centerPaint, centerX)
            if (deviceInfo?.contactNumber?.isNotBlank() == true)
                drawText("Ph: ${deviceInfo.contactNumber}", centerPaint, centerX)

            y += 8f; drawDivider()
            drawText("EXPENSE VOUCHER", boldPaint, centerX)
            drawDivider(); y += 4f

            drawLabelValue("Expense No :", expense.uniqueExpenseId)
            drawLabelValue("Date       :", formatDate(expense.expenseDate))
            drawLabelValue("Entry Date :", formatDate(expense.createdAt))
            y += 8f; drawDivider()

            drawLabelValue("Category   :", expense.category)
            drawLabelValue("Location   :", expense.expenseLocation)
            drawLabelValue("Details    :", expense.expenseDescription)
            y += 8f; drawDivider()

            drawLabelValue("Amount     :", "Rs. ${String.format("%.2f", expense.amount)}")
            drawLabelValue("In Words   :", expense.amountInWords)
            y += 8f; drawDivider()

            drawLabelValue("Expensed By:", expense.expensedBy)
            if (expense.contactNumber.isNotBlank()) drawLabelValue("Contact    :", expense.contactNumber)
            drawLabelValue("Authority  :", expense.authority)
            if (expense.notes?.isNotBlank() == true) drawLabelValue("Notes      :", expense.notes)

            y += 8f; drawDivider(); y += 40f
            canvas.drawLine(leftMargin, y, leftMargin + 150f, y, dividerPaint)
            canvas.drawLine(rightMargin - 150f, y, rightMargin, y, dividerPaint)
            canvas.drawText("Expensor Signature", leftMargin, y + 16f, smallPaint)
            val authPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }
            canvas.drawText("Authorized Signatory", rightMargin - 130f, y + 16f, authPaint)

            pdfDocument.finishPage(page)
            val file = File(context.cacheDir, "Expense_${expense.uniqueExpenseId}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            Result.success(file)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────

    private fun textLine(text: String): ByteArray = (text + "\n").toByteArray(Charsets.UTF_8)
    private fun divider(): String = "-".repeat(CHARS_PER_LINE)

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            if ((current + " " + word).trim().length <= maxChars) {
                current = (current + " " + word).trim()
            } else {
                if (current.isNotBlank()) lines.add(current)
                current = word
            }
        }
        if (current.isNotBlank()) lines.add(current)
        return lines
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))

    fun getPairedBluetoothDevices(): List<BluetoothDevice> {
        return try {
            BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}