package com.nightpos.geckoview.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.geckoview.data.OdooAuthClient
import com.nightpos.geckoview.data.OdooAuthResult
import com.nightpos.geckoview.data.PreferencesManager
import com.nightpos.geckoview.print.PosConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The two ways a user can sign in from the Login screen. */
enum class LoginMode { PIN, NORMAL }

/** Reasons a login attempt can fail, mapped to user-facing strings by the screen. */
enum class LoginError { EMPTY_FIELDS, INVALID_CREDENTIALS, NETWORK }

data class LoginUiState(
    val mode: LoginMode = LoginMode.NORMAL,
    val login: String = "",
    val password: String = "",
    val pin: String = "",
    val isLoading: Boolean = false,
    val errorMessage: LoginError? = null,
    val loginSuccess: Boolean = false,
    val posConfigs: List<PosConfig> = emptyList(),
)

/**
 * Backs the native [com.nightpos.geckoview.ui.screens.login.LoginScreen]. Authenticates against
 * Odoo via [OdooAuthClient] and persists the session via [PreferencesManager] so the app
 * skips straight to the dashboard on the next launch.
 */
class LoginViewModel(
    private val authClient: OdooAuthClient,
    private val preferencesManager: PreferencesManager,
    posConfigsFlow: StateFlow<List<PosConfig>>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val last = preferencesManager.lastLogin.first()
            if (last.isNotBlank()) {
                _uiState.update { it.copy(login = last, mode = LoginMode.PIN) }
            }
        }
        viewModelScope.launch {
            posConfigsFlow.collect { configs -> _uiState.update { it.copy(posConfigs = configs) } }
        }
    }

    fun setMode(mode: LoginMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun setLogin(value: String) {
        _uiState.update { it.copy(login = value, errorMessage = null) }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun appendPinDigit(digit: Char) {
        _uiState.update {
            if (it.pin.length < 12) it.copy(pin = it.pin + digit, errorMessage = null) else it
        }
    }

    fun backspacePin() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1), errorMessage = null) }
    }

    /** Switches to Normal mode with a blank login, so a different user can sign in. */
    fun switchUser() {
        _uiState.update {
            it.copy(mode = LoginMode.NORMAL, login = "", password = "", pin = "", errorMessage = null)
        }
    }

    fun submit(baseUrl: String) {
        val state = _uiState.value
        val login = state.login.trim()
        val password = if (state.mode == LoginMode.PIN) state.pin else state.password

        if (login.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = LoginError.EMPTY_FIELDS) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val isPin = state.mode == LoginMode.PIN
            when (val result = authClient.authenticate(baseUrl, login, password, isPin)) {
                is OdooAuthResult.Success -> {
                    preferencesManager.setLastLogin(result.login)
                    preferencesManager.setDisplayName(result.name)
                    preferencesManager.setLoggedIn(true)
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true, pin = "") }
                }
                is OdooAuthResult.InvalidCredentials -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = LoginError.INVALID_CREDENTIALS, pin = "")
                    }
                }
                is OdooAuthResult.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = LoginError.NETWORK) }
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
}
