// admin_app/presentation/screens/payment_methods/AddEditPaymentMethodViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.withdrawpaymentmethod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.FeeRange
import com.droidnest.tech.pysadmin.domain.models.PaymentCategory
import com.droidnest.tech.pysadmin.domain.models.RequiredField
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
class AddEditPaymentMethodViewModel @Inject constructor(
    private val repository: PaymentMethodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val methodId: String? = savedStateHandle["methodId"]

    private val _state = MutableStateFlow(AddEditPaymentMethodState())
    val state: StateFlow<AddEditPaymentMethodState> = _state.asStateFlow()

    init {
        if (methodId != null) {
            loadPaymentMethod(methodId)
        }
    }

    fun loadPaymentMethod(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = repository.getPaymentMethodById(id)) {
                is Resource.Success -> {
                    result.data?.let { method ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isEditMode = true,
                                id = method.id,
                                name = method.name,
                                icon = method.icon,
                                type = method.type,
                                category = method.category,
                                country = method.country,
                                currency = method.currency,
                                minAmount = method.minAmount.toString(),
                                maxAmount = method.maxAmount.toString(),
                                processingTime = method.processingTime,
                                enabled = method.enabled,
                                feeRanges = method.fees.toMutableList(),
                                requiredFields = method.requiredFields.toMutableList()
                            )
                        }
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
                else -> {}
            }
        }
    }

    fun onIdChanged(id: String) {
        _state.update { it.copy(id = id, idError = null) }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun onIconChanged(icon: String) {
        _state.update { it.copy(icon = icon) }
    }

    fun onTypeChanged(type: String) {
        _state.update { it.copy(type = type) }
    }

    fun onCategoryChanged(category: String) {
        val defaultFields = PaymentCategory.getDefaultRequiredFields(category, _state.value.country)
        _state.update {
            it.copy(
                category = category,
                requiredFields = defaultFields.toMutableList()
            )
        }
    }

    fun onCountryChanged(country: String) {
        val currency = when (country) {
            "BD" -> "BDT"
            "MY" -> "MYR"
            else -> "BDT"
        }
        val defaultFields = PaymentCategory.getDefaultRequiredFields(_state.value.category, country)
        _state.update {
            it.copy(
                country = country,
                currency = currency,
                requiredFields = defaultFields.toMutableList()
            )
        }
    }

    fun onMinAmountChanged(amount: String) {
        _state.update { it.copy(minAmount = amount, minAmountError = null) }
    }

    fun onMaxAmountChanged(amount: String) {
        _state.update { it.copy(maxAmount = amount, maxAmountError = null) }
    }

    fun onProcessingTimeChanged(time: String) {
        _state.update { it.copy(processingTime = time) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
    }

    fun addFeeRange() {
        val currentRanges = _state.value.feeRanges.toMutableList()
        currentRanges.add(FeeRange())
        _state.update { it.copy(feeRanges = currentRanges) }
    }

    fun removeFeeRange(index: Int) {
        val currentRanges = _state.value.feeRanges.toMutableList()
        if (index in currentRanges.indices) {
            currentRanges.removeAt(index)
            _state.update { it.copy(feeRanges = currentRanges) }
        }
    }

    fun updateFeeRange(index: Int, feeRange: FeeRange) {
        val currentRanges = _state.value.feeRanges.toMutableList()
        if (index in currentRanges.indices) {
            currentRanges[index] = feeRange
            _state.update { it.copy(feeRanges = currentRanges) }
        }
    }

    fun addRequiredField() {
        val currentFields = _state.value.requiredFields.toMutableList()
        currentFields.add(RequiredField())
        _state.update { it.copy(requiredFields = currentFields) }
    }

    fun removeRequiredField(index: Int) {
        val currentFields = _state.value.requiredFields.toMutableList()
        if (index in currentFields.indices) {
            currentFields.removeAt(index)
            _state.update { it.copy(requiredFields = currentFields) }
        }
    }

    fun updateRequiredField(index: Int, field: RequiredField) {
        val currentFields = _state.value.requiredFields.toMutableList()
        if (index in currentFields.indices) {
            currentFields[index] = field
            _state.update { it.copy(requiredFields = currentFields) }
        }
    }

    fun savePaymentMethod() {
        if (!validateForm()) return

        val currentState = _state.value

        val method = WithdrawPaymentMethod(
            id = currentState.id,
            name = currentState.name,
            icon = currentState.icon,
            type = currentState.type,
            category = currentState.category,
            country = currentState.country,
            currency = currentState.currency,
            minAmount = currentState.minAmount.toDoubleOrNull() ?: 0.0,
            maxAmount = currentState.maxAmount.toDoubleOrNull() ?: 0.0,
            processingTime = currentState.processingTime,
            enabled = currentState.enabled,
            fees = currentState.feeRanges,
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

        if (currentState.id.isBlank()) {
            _state.update { it.copy(idError = "ID is required") }
            isValid = false
        }

        if (currentState.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
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
            _state.update { it.copy(maxAmountError = "Max amount must be greater than min amount") }
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

data class AddEditPaymentMethodState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,

    val id: String = "",
    val name: String = "",
    val icon: String = "💰",
    val type: String = "withdraw",
    val category: String = "mobile_banking",
    val country: String = "BD",
    val currency: String = "BDT",
    val minAmount: String = "",
    val maxAmount: String = "",
    val processingTime: String = "",
    val enabled: Boolean = true,
    val feeRanges: List<FeeRange> = emptyList(),
    val requiredFields: List<RequiredField> = emptyList(),

    val idError: String? = null,
    val nameError: String? = null,
    val minAmountError: String? = null,
    val maxAmountError: String? = null,

    val successMessage: String? = null,
    val error: String? = null
)