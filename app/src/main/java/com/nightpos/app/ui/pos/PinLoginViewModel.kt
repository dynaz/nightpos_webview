package com.nightpos.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.repository.AuthRepository
import com.nightpos.app.data.repository.PinResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinLoginUiState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnecting: Boolean = false,
    val connectError: String? = null,
)

class PinLoginViewModel(
    private val authRepository: AuthRepository,
    private val baseUrl: String,
    private val db: String,
    private val apiLogin: String,
    private val apiPassword: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PinLoginUiState())
    val state: StateFlow<PinLoginUiState> = _state.asStateFlow()

    init {
        if (!authRepository.isAuthenticated()) {
            connectToOdoo()
        }
    }

    private fun connectToOdoo() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, connectError = null) }
            val ok = authRepository.authenticate(baseUrl, db, apiLogin, apiPassword)
            _state.update {
                if (ok) it.copy(isConnecting = false)
                else it.copy(isConnecting = false, connectError = "Cannot connect to server. Check settings.")
            }
        }
    }

    fun onKeyPress(digit: String) {
        if (_state.value.pin.length >= 6) return
        _state.update { it.copy(pin = it.pin + digit, error = null) }
    }

    fun onBackspace() {
        _state.update { it.copy(pin = it.pin.dropLast(1), error = null) }
    }

    fun onClear() {
        _state.update { it.copy(pin = "", error = null) }
    }

    fun verifyPin(onSuccess: (isManager: Boolean, employeeName: String) -> Unit) {
        val pin = _state.value.pin
        if (pin.length < 4) {
            _state.update { it.copy(error = "Enter at least 4 digits") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyPin(pin)) {
                is PinResult.Success -> {
                    _state.update { it.copy(isLoading = false, pin = "") }
                    onSuccess(result.employee.isManager, result.employee.name)
                }
                is PinResult.InvalidPin -> _state.update { it.copy(isLoading = false, pin = "", error = "Invalid PIN") }
                is PinResult.Error -> _state.update { it.copy(isLoading = false, pin = "", error = result.message) }
            }
        }
    }
}
