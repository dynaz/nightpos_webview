package com.nightpos.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.PreferencesManager
import com.nightpos.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = Constants.DEFAULT_BASE_URL,
    val isConnecting: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = null) }
    }

    fun connect(onSuccess: () -> Unit) {
        val url = _uiState.value.serverUrl.trim().trimEnd('/')
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Server URL is required") }
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.update { it.copy(error = "URL must start with https://") }
            return
        }
        _uiState.update { it.copy(isConnecting = true, error = null) }
        viewModelScope.launch {
            preferencesManager.setServerUrl(url)
            _uiState.update { it.copy(isConnecting = false) }
            onSuccess()
        }
    }
}
