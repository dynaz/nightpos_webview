package com.nightpos.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.PreferencesManager
import com.nightpos.app.data.SessionManager
import com.nightpos.app.print.PosConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

data class DashboardUiState(
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutCompleted: Boolean = false,
    val posConfigs: List<PosConfig> = emptyList(),
    val displayName: String = "",
)

class DashboardViewModel(
    private val sessionManager: SessionManager,
    private val posConfigsFlow: StateFlow<List<PosConfig>>,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            posConfigsFlow.collect { configs ->
                _uiState.value = _uiState.value.copy(posConfigs = configs)
            }
        }
        viewModelScope.launch {
            preferencesManager.displayName.collect { name ->
                _uiState.value = _uiState.value.copy(displayName = name)
            }
        }
    }

    fun requestLogout() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun dismissLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    fun confirmLogout(session: GeckoSession?) {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false, isLoggingOut = true)
        viewModelScope.launch {
            sessionManager.clearSession(session)
            _uiState.value = _uiState.value.copy(isLoggingOut = false, logoutCompleted = true)
        }
    }

    fun consumeLogoutCompleted() {
        _uiState.value = _uiState.value.copy(logoutCompleted = false)
    }
}
