// admin_app/presentation/screens/transactiondetails/TransactionDetailsViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.transactiondetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.droidnest.tech.pysadmin.domain.usecase.GetTransactionByIdUseCase
import com.droidnest.tech.pysadmin.domain.usecase.UpdateTransactionStatusUseCase
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailsState(
    val isLoading: Boolean = false,
    val transaction: TransactionModel? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false
)

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailsState())
    val state: StateFlow<TransactionDetailsState> = _state.asStateFlow()

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (val result = getTransactionByIdUseCase(transactionId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        transaction = result.data
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun approveTransaction() {
        val transaction = _state.value.transaction ?: return

        // ✅ Safety check: Only allow if status is pending
        if (transaction.status != "pending") {
            _state.value = _state.value.copy(
                error = "Only pending transactions can be approved"
            )
            return
        }

        updateStatus("success")
    }

    fun rejectTransaction() {
        val transaction = _state.value.transaction ?: return

        // ✅ Safety check: Only allow if status is pending
        if (transaction.status != "pending") {
            _state.value = _state.value.copy(
                error = "Only pending transactions can be rejected"
            )
            return
        }

        updateStatus("failed")
    }

    private fun updateStatus(newStatus: String) {
        val transaction = _state.value.transaction ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true, error = null, successMessage = null)

            val result = updateTransactionStatusUseCase(
                transactionId = transaction.appTransactionId,
                userId = transaction.userId,
                status = newStatus,
                processed = true
            )

            when (result) {
                is Resource.Success -> {
                    // ✅ Update local state with new status
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = when (newStatus) {
                            "success" -> "✅ Transaction approved successfully"
                            "failed" -> "❌ Transaction rejected - Amount refunded"
                            else -> "Transaction status updated to $newStatus"
                        },
                        transaction = _state.value.transaction?.copy(
                            status = newStatus,
                            processed = true
                        )
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = result.message ?: "Failed to update transaction"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            successMessage = null,
            error = null
        )
    }
}