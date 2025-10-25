package com.droidnest.tech.pysadmin.presentation.screens.dashboard.ratemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.repository.ExchangeRateRepository
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateManagementViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(RateManagementState())
    val state = _state.asStateFlow()

    init {
        loadExchangeRate()
    }

    private fun loadExchangeRate() {
        viewModelScope.launch {
            exchangeRateRepository.getExchangeRate().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                exchangeRate = resource.data ?: it.exchangeRate,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateExchangeRate(newRate: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }

            val adminName = auth.currentUser?.displayName ?: "Admin"
            val result = exchangeRateRepository.updateExchangeRate(newRate, adminName)

            when (result) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            showBottomSheet = false,
                            successMessage = "Exchange rate updated successfully!",
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun showBottomSheet() {
        _state.update { it.copy(showBottomSheet = true) }
    }

    fun hideBottomSheet() {
        _state.update { it.copy(showBottomSheet = false) }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}