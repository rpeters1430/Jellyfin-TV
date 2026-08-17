package com.example.jellyfintv.ui.screens

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfintv.data.update.AppUpdateManager
import com.example.jellyfintv.data.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Activity-scoped (not per-screen) - the update prompt should surface regardless of which
 * screen (Connect/Home/Library/...) is currently showing, so this is owned by MainActivity
 * rather than any individual screen's ViewModel.
 */
class UpdateViewModel(
    private val appContext: Context,
    private val updateManager: AppUpdateManager = AppUpdateManager()
) : ViewModel() {

    data class UiState(
        val updateInfo: UpdateInfo? = null,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f,
        val errorMessage: String? = null,
        val needsInstallPermission: Boolean = false,
        val dismissed: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            updateManager.checkForUpdate(updateManager.currentVersionName(appContext))
                .onSuccess { info -> _uiState.update { it.copy(updateInfo = info) } }
            // A failed background check shouldn't interrupt the user - fail silently and
            // just try again next launch.
        }
    }

    fun downloadAndInstall() {
        val info = _uiState.value.updateInfo ?: return
        if (!updateManager.canRequestPackageInstalls(appContext)) {
            _uiState.update { it.copy(needsInstallPermission = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f, errorMessage = null) }
            try {
                val file = updateManager.downloadApk(appContext, info.downloadUrl) { bytesRead, totalBytes ->
                    if (totalBytes > 0) {
                        _uiState.update { it.copy(downloadProgress = bytesRead.toFloat() / totalBytes.toFloat()) }
                    }
                }
                _uiState.update { it.copy(isDownloading = false) }
                updateManager.installApk(appContext, file)
            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloading = false, errorMessage = e.message ?: "Download failed") }
            }
        }
    }

    fun installPermissionSettingsIntent(): Intent = updateManager.installPermissionSettingsIntent(appContext)

    fun onInstallPermissionHandled() {
        _uiState.update { it.copy(needsInstallPermission = false) }
    }

    fun dismiss() {
        _uiState.update { it.copy(dismissed = true) }
    }
}
