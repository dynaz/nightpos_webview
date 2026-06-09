package com.nightpos.app.ui.pos.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightpos.app.data.model.PosHomeState
import com.nightpos.app.data.repository.OdooRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PosHomeViewModel(private val repository: OdooRepository) : ViewModel() {

    private val _state = MutableStateFlow(PosHomeState(isLoading = true))
    val state: StateFlow<PosHomeState> = _state.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val bills = async { repository.fetchCurrentBills() }
                val summary = async { repository.fetchSalesSummary() }
                val categories = async { repository.fetchTopCategories() }
                val sellers = async { repository.fetchBestSellers() }
                _state.update {
                    it.copy(
                        isLoading = false,
                        currentBills = bills.await(),
                        salesSummary = summary.await(),
                        topCategories = categories.await(),
                        bestSellers = sellers.await(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
