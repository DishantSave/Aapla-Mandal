package com.aaplamandal

import android.content.Context
import com.aaplamandal.data.local.database.AppDatabase
import com.aaplamandal.data.local.entities.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object InitializationHelper {

    fun initializeDeviceIfNeeded(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val deviceInfoDao = database.deviceInfoDao()

            val existingDevice = deviceInfoDao.getDeviceInfo()

            if (existingDevice == null) {
                val defaultDeviceInfo = DeviceInfo(
                    id = 1,
                    deviceId = "DEVICE-001",
                    deviceName = "Main Device",

                    // Mandal Details
                    mandalName = "Aapla Mandal",
                    mandalAddress = "Gokak, Karnataka",
                    mandalWard = "",
                    mandalCity = "Gokak",
                    mandalRegistrationNumber = "",

                    // Festival Details
                    festivalYear = Calendar.getInstance().get(Calendar.YEAR),
                    festivalName = "Ganesh Festival",

                    // Contact
                    contactNumber = "9876543210",
                    alternateContact = "",
                    email = "",

                    // Payment
                    upiId = "aaplamandal@paytm",
                    bankAccountName = "",
                    bankAccountNumber = "",
                    bankIfsc = "",
                    bankName = "",

                    // Printing
                    printerAddress = null,
                    receiptFooterNote = "Jay Ganesh! Thank you for your contribution.",

                    // Sync
                    lastSync = null
                )

                deviceInfoDao.insert(defaultDeviceInfo)
            }
        }
    }
}