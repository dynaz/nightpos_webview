package com.nightpos.app.ui.pos.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.model.PosActivityState
import com.nightpos.app.data.repository.OdooRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PosActivityViewModel(private val repository: OdooRepository) : ViewModel() {

    private val _state = MutableStateFlow(PosActivityState(isLoading = true))
    val state: StateFlow<PosActivityState> = _state.asStateFlow()

    init { load() }

    fun onSearchChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        load()
    }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val orders = repository.fetchActivity(searchQuery = _state.value.searchQuery)
                _state.update { it.copy(isLoading = false, orders = orders) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
