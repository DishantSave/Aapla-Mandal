package com.aaplamandal.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.data.local.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val deviceName: String = "",
    val password: String = "",
    val deviceNameError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val deviceInfoDao = database.deviceInfoDao()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    companion object {
        // Hardcoded app password — same across all installations
        private const val APP_PASSWORD = "Raja123#"
    }

    fun onDeviceNameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            deviceName = value,
            deviceNameError = null
        )
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            passwordError = null
        )
    }

    fun login() {
        val state = _uiState.value

        // ── Validate Device Name ──────────────────────────────────
        val trimmedName = state.deviceName.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = state.copy(deviceNameError = "Device name cannot be empty")
            return
        }

        // ── Validate Password ─────────────────────────────────────
        if (state.password != APP_PASSWORD) {
            _uiState.value = state.copy(passwordError = "Incorrect password")
            return
        }

        // ── Generate unique device name with 5-digit random postfix ─
        // Range 10000..99999 gives exactly 5 digits, 90,000 possibilities
        val postfix = (10000..99999).random()
        val uniqueDeviceName = "$trimmedName-$postfix"

        // ── Persist to DB ─────────────────────────────────────────
        _uiState.value = state.copy(isLoading = true, deviceNameError = null, passwordError = null)

        viewModelScope.launch {
            deviceInfoDao.updateDeviceName(uniqueDeviceName)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loginSuccess = true
            )
        }
    }
}