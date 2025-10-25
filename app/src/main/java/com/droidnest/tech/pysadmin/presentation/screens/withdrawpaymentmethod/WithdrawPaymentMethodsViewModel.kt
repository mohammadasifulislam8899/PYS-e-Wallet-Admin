// admin_app/presentation/screens/payment_methods/WithdrawPaymentMethodsViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.withdrawpaymentmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.WithdrawPaymentMethod
import com.droidnest.tech.pysadmin.domain.repository.PaymentMethodRepository
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WithdrawPaymentMethodsViewModel @Inject constructor(
    private val repository: PaymentMethodRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentMethodsState())
    val state: StateFlow<PaymentMethodsState> = _state.asStateFlow()

    init {
        loadPaymentMethods()
    }

    fun loadPaymentMethods() {
        viewModelScope.launch {
            repository.getAllPaymentMethods().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                withdrawPaymentMethods = result.data ?: emptyList()
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleMethodStatus(id: String, enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true) }

            when (val result = repository.togglePaymentMethodStatus(id, enabled)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            successMessage = result.data
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
                else -> {}
            }
        }
    }

    fun deleteMethod(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true) }

            when (val result = repository.deletePaymentMethod(id)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            successMessage = result.data
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
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _state.update {
            it.copy(
                successMessage = null,
                error = null
            )
        }
    }
}

data class PaymentMethodsState(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val withdrawPaymentMethods: List<WithdrawPaymentMethod> = emptyList(),
    val successMessage: String? = null,
    val error: String? = null
)