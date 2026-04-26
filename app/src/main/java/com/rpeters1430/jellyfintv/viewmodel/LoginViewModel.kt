package com.rpeters1430.jellyfintv.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpeters1430.jellyfintv.api.ApiClient
import com.rpeters1430.jellyfintv.models.AuthenticationResult
import com.rpeters1430.jellyfintv.repository.JellyfinRepository
import com.rpeters1430.jellyfintv.utils.PreferenceManager
import kotlinx.coroutines.launch

class LoginViewModel(
    private val prefs: PreferenceManager,
    private val repository: JellyfinRepository
) : ViewModel() {

    sealed class LoginState {
        object Idle : LoginState()
        object CheckingServer : LoginState()
        object ServerOk : LoginState()
        object LoggingIn : LoginState()
        data class Success(val result: AuthenticationResult) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _state = MutableLiveData<LoginState>(LoginState.Idle)
    val state: LiveData<LoginState> = _state

    fun connectToServer(rawUrl: String) {
        val url = normaliseUrl(rawUrl)
        _state.value = LoginState.CheckingServer

        // Initialise API client without a token for the connectivity check
        ApiClient.init(
            baseUrl = url,
            tokenProvider = { null },
            deviceId = prefs.deviceId
        )

        viewModelScope.launch {
            repository.checkServer(url).fold(
                onSuccess = {
                    prefs.serverUrl = url
                    _state.value = LoginState.ServerOk
                },
                onFailure = {
                    _state.value = LoginState.Error(
                        "Cannot reach server: ${it.message}"
                    )
                }
            )
        }
    }

    fun login(username: String, password: String) {
        _state.value = LoginState.LoggingIn
        viewModelScope.launch {
            repository.login(username, password).fold(
                onSuccess = { result ->
                    // Persist session data
                    prefs.accessToken = result.accessToken
                    prefs.userId = result.user?.id
                    prefs.username = result.user?.name

                    // Re-init client with token
                    ApiClient.init(
                        baseUrl = prefs.serverUrl!!,
                        tokenProvider = { prefs.accessToken },
                        deviceId = prefs.deviceId
                    )

                    _state.value = LoginState.Success(result)
                },
                onFailure = {
                    _state.value = LoginState.Error(
                        "Login failed: ${it.message}"
                    )
                }
            )
        }
    }

    /** Attempt to resume a previous session from stored preferences. */
    fun tryResumeSession() {
        if (!prefs.isLoggedIn) return
        val url = prefs.serverUrl ?: return
        ApiClient.init(
            baseUrl = url,
            tokenProvider = { prefs.accessToken },
            deviceId = prefs.deviceId
        )
        _state.value = LoginState.Success(
            AuthenticationResult(
                user = null,
                accessToken = prefs.accessToken,
                serverId = null
            )
        )
    }

    private fun normaliseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
