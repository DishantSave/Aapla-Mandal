package com.aaplamandal.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val mandalName: String = "",
    val mandalAddress: String = "",
    val mandalCity: String = "",
    val contactNumber: String = "",
    val alternateContact: String = "",
    val receiptFooterNote: String = "",
    val logoImagePath: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val deviceInfoDao = database.deviceInfoDao()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            val deviceInfo = deviceInfoDao.getDeviceInfo()
            _uiState.value = SettingsUiState(
                mandalName        = deviceInfo?.mandalName ?: "",
                mandalAddress     = deviceInfo?.mandalAddress ?: "",
                mandalCity        = deviceInfo?.mandalCity ?: "",
                contactNumber     = deviceInfo?.contactNumber ?: "",
                alternateContact  = deviceInfo?.alternateContact ?: "",
                receiptFooterNote = deviceInfo?.receiptFooterNote ?: "",
                logoImagePath     = deviceInfo?.logoImagePath,
                isLoading         = false
            )
        }
    }

    fun updateMandalName(value: String)        { _uiState.value = _uiState.value.copy(mandalName = value) }
    fun updateMandalAddress(value: String)     { _uiState.value = _uiState.value.copy(mandalAddress = value) }
    fun updateMandalCity(value: String)        { _uiState.value = _uiState.value.copy(mandalCity = value) }
    fun updateContactNumber(value: String)     { _uiState.value = _uiState.value.copy(contactNumber = value) }
    fun updateAlternateContact(value: String)  { _uiState.value = _uiState.value.copy(alternateContact = value) }
    fun updateReceiptFooterNote(value: String) { _uiState.value = _uiState.value.copy(receiptFooterNote = value) }

    // Copies the picked gallery URI into app internal storage so it persists
    // (content:// URIs can become invalid after reboot)
    fun saveLogoImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val logoDir = File(context.filesDir, "logo").apply { mkdirs() }
                val logoFile = File(logoDir, "mandal_logo.png")
                logoFile.outputStream().use { output -> inputStream.copyTo(output) }
                inputStream.close()
                _uiState.value = _uiState.value.copy(logoImagePath = logoFile.absolutePath)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun removeLogo() {
        _uiState.value.logoImagePath?.let { try { File(it).delete() } catch (ex: Exception) {} }
        _uiState.value = _uiState.value.copy(logoImagePath = null)
    }

    fun saveSettings() {
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            val s = _uiState.value
            deviceInfoDao.updateMandalInfo(s.mandalName.trim(), s.mandalAddress.trim(), "", s.mandalCity.trim(), "")
            deviceInfoDao.updateContactInfo(s.contactNumber.trim(), s.alternateContact.trim(), "")
            deviceInfoDao.updateReceiptFooterNote(s.receiptFooterNote.trim())
            deviceInfoDao.updateLogoImagePath(s.logoImagePath)
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }

    fun resetSavedFlag() { _uiState.value = _uiState.value.copy(savedSuccessfully = false) }
}