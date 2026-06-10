package com.nightpos.app.ui.screens.settings

import org.mozilla.geckoview.GeckoSession
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
    val printerPaperWidthMm: Int = 58,
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
        preferencesManager.printerPaperWidthMm,
    ) { serverUrl, kiosk, keepScreenOn, autoReopen, paperWidth ->
        SettingsUiState(
            serverUrl = serverUrl,
            kioskModeEnabled = kiosk,
            keepScreenOnEnabled = keepScreenOn,
            autoReopenPosEnabled = autoReopen,
            printerPaperWidthMm = paperWidth,
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

    fun setPrinterPaperWidthMm(widthMm: Int) = viewModelScope.launch {
        preferencesManager.setPrinterPaperWidthMm(widthMm)
    }

    fun clearWebViewData(session: GeckoSession?) = viewModelScope.launch {
        _events.value = SettingsEvent.ClearingStarted
        sessionManager.clearSession(session)
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
