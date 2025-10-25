// presentation/screens/paymentmethods/AddEditAddMoneyPaymentMethodViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.paymentmethods.add_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.*
import com.droidnest.tech.pysadmin.domain.repository.AddMoneyPaymentMethodRepository
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditAddMoneyPaymentMethodViewModel @Inject constructor(
    private val repository: AddMoneyPaymentMethodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val methodId: String? = savedStateHandle["methodId"]

    private val _state = MutableStateFlow(AddEditAddMoneyPaymentMethodState())
    val state: StateFlow<AddEditAddMoneyPaymentMethodState> = _state.asStateFlow()

    init {
        if (methodId != null) {
            loadPaymentMethod(methodId)
        }
    }

    fun loadPaymentMethod(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Get single method from repository
            repository.getAllPaymentMethods().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val method = resource.data?.find { it.id == id }
                        if (method != null) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isEditMode = true,
                                    id = method.id,
                                    name = method.name,
                                    icon = method.icon,
                                    category = method.category.value,
                                    country = method.country,
                                    currency = method.currency,
                                    accountNumber = method.accountNumber,
                                    accountName = method.accountName,
                                    accountType = method.accountType,
                                    minAmount = method.minAmount.toString(),
                                    maxAmount = method.maxAmount.toString(),
                                    dailyLimit = method.dailyLimit.toString(),
                                    instructions = method.instructions,
                                    priority = method.priority,
                                    enabled = method.isEnabled,
                                    requiredFields = method.requiredFields
                                )
                            }
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
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun onIconChanged(icon: String) {
        _state.update { it.copy(icon = icon) }
    }

    fun onCategoryChanged(category: String) {
        val defaultFields = AddMoneyPaymentCategory.getDefaultRequiredFieldsForAddMoney(
            category,
            _state.value.country
        )
        _state.update {
            it.copy(
                category = category,
                requiredFields = defaultFields
            )
        }
    }

    fun onCountryChanged(country: String) {
        val currency = when (country) {
            "BD" -> "BDT"
            "MY" -> "MYR"
            else -> "BDT"
        }
        val defaultFields = AddMoneyPaymentCategory.getDefaultRequiredFieldsForAddMoney(
            _state.value.category,
            country
        )
        _state.update {
            it.copy(
                country = country,
                currency = currency,
                requiredFields = defaultFields
            )
        }
    }

    fun onAccountNumberChanged(number: String) {
        _state.update { it.copy(accountNumber = number, accountNumberError = null) }
    }

    fun onAccountNameChanged(name: String) {
        _state.update { it.copy(accountName = name, accountNameError = null) }
    }

    fun onAccountTypeChanged(type: String) {
        _state.update { it.copy(accountType = type) }
    }

    fun onMinAmountChanged(amount: String) {
        _state.update { it.copy(minAmount = amount, minAmountError = null) }
    }

    fun onMaxAmountChanged(amount: String) {
        _state.update { it.copy(maxAmount = amount, maxAmountError = null) }
    }

    fun onDailyLimitChanged(limit: String) {
        _state.update { it.copy(dailyLimit = limit) }
    }

    fun onInstructionsChanged(instructions: String) {
        _state.update { it.copy(instructions = instructions) }
    }

    fun onPriorityChanged(priority: Int) {
        _state.update { it.copy(priority = priority) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
    }

    fun savePaymentMethod() {
        if (!validateForm()) return

        val currentState = _state.value

        val method = AddMoneyPaymentMethod(
            id = if (currentState.isEditMode) currentState.id else "",
            name = currentState.name,
            icon = currentState.icon,
            type = PaymentType.MANUAL,
            category = AddMoneyPaymentCategory.fromValue(currentState.category),
            currency = currentState.currency,
            country = currentState.country,
            accountNumber = currentState.accountNumber,
            accountName = currentState.accountName,
            accountType = currentState.accountType,
            isEnabled = currentState.enabled,
            minAmount = currentState.minAmount.toDoubleOrNull() ?: 0.0,
            maxAmount = currentState.maxAmount.toDoubleOrNull() ?: 50000.0,
            dailyLimit = currentState.dailyLimit.toDoubleOrNull() ?: 100000.0,
            instructions = currentState.instructions,
            priority = currentState.priority,
            requiredFields = currentState.requiredFields
        )

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = if (currentState.isEditMode) {
                repository.updatePaymentMethod(method)
            } else {
                repository.addPaymentMethod(method)
            }

            when (result) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            successMessage = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _state.value
        var isValid = true

        if (currentState.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
            isValid = false
        }

        if (currentState.accountNumber.isBlank()) {
            _state.update { it.copy(accountNumberError = "Account number is required") }
            isValid = false
        }

        if (currentState.accountName.isBlank()) {
            _state.update { it.copy(accountNameError = "Account name is required") }
            isValid = false
        }

        val minAmount = currentState.minAmount.toDoubleOrNull()
        if (minAmount == null) {
            _state.update { it.copy(minAmountError = "Invalid amount") }
            isValid = false
        }

        val maxAmount = currentState.maxAmount.toDoubleOrNull()
        if (maxAmount == null) {
            _state.update { it.copy(maxAmountError = "Invalid amount") }
            isValid = false
        }

        if (minAmount != null && maxAmount != null && minAmount >= maxAmount) {
            _state.update { it.copy(maxAmountError = "Max must be greater than min") }
            isValid = false
        }

        return isValid
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

// ✅ Updated State (Without Fee Fields)
data class AddEditAddMoneyPaymentMethodState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,

    val id: String = "",
    val name: String = "",
    val icon: String = "💰",
    val category: String = "mobile_banking",
    val country: String = "BD",
    val currency: String = "BDT",

    val accountNumber: String = "",
    val accountName: String = "",
    val accountType: String = "",

    val minAmount: String = "",
    val maxAmount: String = "",
    val dailyLimit: String = "",

    val instructions: String = "",
    val priority: Int = 0,
    val enabled: Boolean = true,

    val requiredFields: List<RequiredField> = emptyList(),

    val nameError: String? = null,
    val accountNumberError: String? = null,
    val accountNameError: String? = null,
    val minAmountError: String? = null,
    val maxAmountError: String? = null,

    val successMessage: String? = null,
    val error: String? = null
)