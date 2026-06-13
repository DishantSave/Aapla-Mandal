package com.aaplamandal.ui.import_data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaplamandal.utils.ImportManager
import com.aaplamandal.utils.ImportResult
import com.aaplamandal.utils.ImportSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ImportUiState(
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false,
    val importResult: ImportResult? = null,
    val batchImportSummary: ImportSummary? = null,
    val selectedFiles: List<FileInfo> = emptyList(),
    val errorMessage: String? = null
)

data class FileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val type: FileType
)

enum class FileType {
    JSON,
    DATABASE,
    CSV,
    UNKNOWN
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val importManager = ImportManager(application)
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun addFile(uri: Uri, name: String, size: Long) {
        val type = when {
            name.endsWith(".json", ignoreCase = true) -> FileType.JSON
            name.endsWith(".db", ignoreCase = true) -> FileType.DATABASE
            name.endsWith(".csv", ignoreCase = true) -> FileType.CSV
            else -> FileType.UNKNOWN
        }

        val fileInfo = FileInfo(uri, name, size, type)
        val currentFiles = _uiState.value.selectedFiles.toMutableList()

        if (!currentFiles.any { it.uri == uri }) {
            currentFiles.add(fileInfo)
            _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
        }
    }

    fun removeFile(uri: Uri) {
        val currentFiles = _uiState.value.selectedFiles.filter { it.uri != uri }
        _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
    }

    fun clearFiles() {
        _uiState.value = _uiState.value.copy(selectedFiles = emptyList())
    }

    fun importSelectedFiles() {
        if (_uiState.value.selectedFiles.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No files selected"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isImporting = true,
            importSuccess = false,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val files = _uiState.value.selectedFiles.mapNotNull { fileInfo ->
                    try {
                        // Copy URI to temporary file
                        val tempFile = File(getApplication<Application>().cacheDir, fileInfo.name)
                        getApplication<Application>().contentResolver.openInputStream(fileInfo.uri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                if (files.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Failed to read files"
                    )
                    return@launch
                }

                // Import files
                if (files.size == 1) {
                    // Single file import
                    // Single file import
                    val file = files.first()
                    val originalName = _uiState.value.selectedFiles.first().name

                    android.util.Log.d("ImportViewModel", "File name: ${file.name}")
                    android.util.Log.d("ImportViewModel", "File extension: ${file.extension}")
                    android.util.Log.d("ImportViewModel", "Original name: $originalName")

// Check extension from both the temp file AND original filename
                    val isJson = file.extension.equals("json", ignoreCase = true) ||
                            originalName.endsWith(".json", ignoreCase = true)
                    val isDb = file.extension.equals("db", ignoreCase = true) ||
                            originalName.endsWith(".db", ignoreCase = true)

                    val result = when {
                        isJson -> importManager.importFromJson(file)
                        isDb -> importManager.importFromDatabase(file)
                        else -> ImportResult(
                            success = false,
                            errorMessage = "Unsupported file type: ${file.name} (original: $originalName)"
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importSuccess = result.success,
                        importResult = result,
                        errorMessage = if (!result.success) result.errorMessage else null
                    )
                } else {
                    // Batch import
                    val summary = importManager.importMultipleFiles(
                        files = files,
                        originalNames = _uiState.value.selectedFiles.map { it.name }
                    )

                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importSuccess = summary.successfulImports > 0,
                        batchImportSummary = summary,
                        errorMessage = if (summary.errors.isNotEmpty()) summary.errors.joinToString("\n") else null
                    )
                }

                // Clean up temp files
                files.forEach { it.delete() }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = e.message ?: "Import failed"
                )
                e.printStackTrace()
            }
        }
    }

    fun clearImportStatus() {
        _uiState.value = _uiState.value.copy(
            importSuccess = false,
            importResult = null,
            batchImportSummary = null,
            errorMessage = null,
            selectedFiles = emptyList()
        )
    }
}