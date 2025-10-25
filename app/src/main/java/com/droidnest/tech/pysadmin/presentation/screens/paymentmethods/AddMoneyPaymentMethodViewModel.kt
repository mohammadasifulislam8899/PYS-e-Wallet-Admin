// presentation/screens/paymentmethods/PaymentMethodsViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.paymentmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.AddMoneyPaymentMethod
import com.droidnest.tech.pysadmin.domain.models.PaymentType
import com.droidnest.tech.pysadmin.domain.repository.AddMoneyPaymentMethodRepository
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentMethodsState(
    val isLoading: Boolean = false,
    val allMethods: List<AddMoneyPaymentMethod> = emptyList(),
    val bdtMethods: List<AddMoneyPaymentMethod> = emptyList(),
    val myrMethods: List<AddMoneyPaymentMethod> = emptyList(),
    val selectedCurrency: String = "ALL",
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingMethod: AddMoneyPaymentMethod? = null
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    private val repository: AddMoneyPaymentMethodRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentMethodsState())
    val state: StateFlow<PaymentMethodsState> = _state.asStateFlow()

    init {
        loadPaymentMethods()
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            repository.getAllPaymentMethods()
                .catch { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load payment methods"
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            _state.update { it.copy(isLoading = true, error = null) }
                        }
                        is Resource.Success -> {
                            val methods = result.data ?: emptyList()
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    allMethods = methods,
                                    bdtMethods = methods.filter { m -> m.currency == "BDT" },
                                    myrMethods = methods.filter { m -> m.currency == "MYR" },
                                    error = null
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

    fun selectCurrency(currency: String) {
        _state.update { it.copy(selectedCurrency = currency) }
    }

    fun showAddDialog() {
        _state.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _state.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(method: AddMoneyPaymentMethod) {
        _state.update { it.copy(editingMethod = method) }
    }

    fun hideEditDialog() {
        _state.update { it.copy(editingMethod = null) }
    }

    fun addPaymentMethod(
        name: String,
        type: PaymentType,
        currency: String,
        accountNumber: String,
        accountName: String,
        accountType: String,
        minAmount: Double,
        maxAmount: Double,
        dailyLimit: Double,
        instructions: String,
        priority: Int
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }

            val method = AddMoneyPaymentMethod(
                name = name,
                type = type,
                currency = currency,
                accountNumber = accountNumber,
                accountName = accountName,
                accountType = accountType,
                isEnabled = true,
                minAmount = minAmount,
                maxAmount = maxAmount,
                dailyLimit = dailyLimit,
                instructions = instructions,
                priority = priority
            )

            when (val result = repository.addPaymentMethod(method)) {
                is Resource.Success -> {
                    loadPaymentMethods()
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            showAddDialog = false,
                            successMessage = result.data ?: "Payment method added successfully",
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

    fun updatePaymentMethod(method: AddMoneyPaymentMethod) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }

            when (val result = repository.updatePaymentMethod(method)) {
                is Resource.Success -> {
                    loadPaymentMethods()
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            editingMethod = null,
                            successMessage = result.data ?: "Payment method updated successfully",
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

    fun deletePaymentMethod(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }

            when (val result = repository.deletePaymentMethod(id)) {
                is Resource.Success -> {
                    loadPaymentMethods()
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            successMessage = result.data ?: "Payment method deleted successfully",
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

    fun togglePaymentMethod(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            when (val result = repository.togglePaymentMethod(id, isEnabled)) {
                is Resource.Success -> {
                    _state.update { currentState ->
                        val updatedAllMethods = currentState.allMethods.map { method ->
                            if (method.id == id) {
                                method.copy(isEnabled = isEnabled)
                            } else {
                                method
                            }
                        }

                        currentState.copy(
                            allMethods = updatedAllMethods,
                            bdtMethods = updatedAllMethods.filter { it.currency == "BDT" },
                            myrMethods = updatedAllMethods.filter { it.currency == "MYR" },
                            successMessage = result.data ?: if (isEnabled) "Payment method enabled" else "Payment method disabled"
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(error = result.message)
                    }
                }
                is Resource.Loading -> {}
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

    fun refresh() {
        loadPaymentMethods()
    }
}