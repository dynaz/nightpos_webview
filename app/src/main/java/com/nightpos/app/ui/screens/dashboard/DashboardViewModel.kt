package com.nightpos.app.ui.screens.dashboard

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the dashboard's logout confirmation flow. */
data class DashboardUiState(
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutCompleted: Boolean = false,
)

class DashboardViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun requestLogout() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun dismissLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    /**
     * Clears all WebView session data (cookies, cache, storage). [webView] is the
     * shared WebView instance if one has already been created (i.e. the user opened
     * POS/Reports/Customers before logging out); it may be null otherwise.
     */
    fun confirmLogout(webView: WebView?) {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false, isLoggingOut = true)
        viewModelScope.launch {
            sessionManager.clearSession(webView)
            _uiState.value = _uiState.value.copy(isLoggingOut = false, logoutCompleted = true)
        }
    }

    fun consumeLogoutCompleted() {
        _uiState.value = _uiState.value.copy(logoutCompleted = false)
    }
}
