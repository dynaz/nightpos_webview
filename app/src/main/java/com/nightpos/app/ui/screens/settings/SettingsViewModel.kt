package com.nightpos.app.ui.screens.settings

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.PreferencesManager
import com.nightpos.app.data.SessionManager
import com.nightpos.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = Constants.DEFAULT_BASE_URL,
    val kioskModeEnabled: Boolean = false,
    val keepScreenOnEnabled: Boolean = true,
    val autoReopenPosEnabled: Boolean = false,
    val isClearingData: Boolean = false,
    val clearDataMessage: String? = null,
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _events = MutableStateFlow<SettingsEvent?>(null)
    val events: StateFlow<SettingsEvent?> = _events.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.serverUrl,
        preferencesManager.kioskModeEnabled,
        preferencesManager.keepScreenOnEnabled,
        preferencesManager.autoReopenPosEnabled,
    ) { serverUrl, kiosk, keepScreenOn, autoReopen ->
        SettingsUiState(
            serverUrl = serverUrl,
            kioskModeEnabled = kiosk,
            keepScreenOnEnabled = keepScreenOn,
            autoReopenPosEnabled = autoReopen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setServerUrl(url: String) = viewModelScope.launch {
        preferencesManager.setServerUrl(url)
    }

    fun setKioskModeEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setKioskModeEnabled(enabled)
        // Auto-reopen only makes sense alongside kiosk mode.
        if (!enabled) preferencesManager.setAutoReopenPosEnabled(false)
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setKeepScreenOnEnabled(enabled)
    }

    fun setAutoReopenPosEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAutoReopenPosEnabled(enabled)
    }

    fun clearWebViewData(webView: WebView?) = viewModelScope.launch {
        _events.value = SettingsEvent.ClearingStarted
        sessionManager.clearSession(webView)
        _events.value = SettingsEvent.ClearingFinished
    }

    fun consumeEvent() {
        _events.value = null
    }
}

sealed interface SettingsEvent {
    data object ClearingStarted : SettingsEvent
    data object ClearingFinished : SettingsEvent
}
