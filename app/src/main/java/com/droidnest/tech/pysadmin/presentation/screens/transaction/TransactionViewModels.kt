package com.droidnest.tech.pysadmin.presentation.screens.transaction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.droidnest.tech.pysadmin.domain.usecase.GetAllTransactionsUseCase
import com.droidnest.tech.pysadmin.domain.usecase.UpdateTransactionStatusUseCase
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminTransactionState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionModel> = emptyList(),
    val filteredTransactions: List<TransactionModel> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false,
    val selectedStatusFilter: String = "all" // all, pending, success, failed
)

@HiltViewModel
class TransactionViewModels @Inject constructor(
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "TransactionViewModel"
    }

    private val _state = MutableStateFlow(AdminTransactionState())
    val state: StateFlow<AdminTransactionState> = _state.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        loadAllTransactions()
    }

    // ════════════════════════════════════════════════════════════
    // LOAD ALL TRANSACTIONS
    // ════════════════════════════════════════════════════════════

    fun loadAllTransactions() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📊 Loading All Transactions")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        viewModelScope.launch {
            getAllTransactionsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "⏳ Loading...")
                        _state.update { it.copy(isLoading = true, error = null) }
                    }

                    is Resource.Success -> {
                        val transactions = result.data ?: emptyList()
                        Log.d(TAG, "✅ Loaded ${transactions.size} transactions")

                        _state.update { currentState ->
                            val filtered = applyStatusFilter(
                                transactions = transactions,
                                status = currentState.selectedStatusFilter
                            )

                            Log.d(TAG, "📋 Filtered (${currentState.selectedStatusFilter}): ${filtered.size}")

                            currentState.copy(
                                isLoading = false,
                                transactions = transactions,
                                filteredTransactions = filtered,
                                error = null
                            )
                        }
                    }

                    is Resource.Error -> {
                        Log.e(TAG, "❌ Error: ${result.message}")
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

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ FILTER BY STATUS (FIXED) ✅✅✅
    // ════════════════════════════════════════════════════════════

    fun filterByStatus(status: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔍 Filtering Transactions")
        Log.d(TAG, "  Previous filter: ${_state.value.selectedStatusFilter}")
        Log.d(TAG, "  New filter: $status")
        Log.d(TAG, "  Total transactions: ${_state.value.transactions.size}")

        _state.update { currentState ->
            // ✅ Pass status as parameter instead of reading from state
            val filtered = applyStatusFilter(
                transactions = currentState.transactions,
                status = status  // ✅ Use the new status directly
            )

            Log.d(TAG, "  Filtered result: ${filtered.size} transactions")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            currentState.copy(
                selectedStatusFilter = status,
                filteredTransactions = filtered
            )
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅ HELPER: APPLY STATUS FILTER (FIXED)
    // ════════════════════════════════════════════════════════════

    private fun applyStatusFilter(
        transactions: List<TransactionModel>,
        status: String  // ✅ Take status as parameter
    ): List<TransactionModel> {
        return when (status.lowercase()) {
            "all" -> {
                Log.d(TAG, "  Filter logic: Showing ALL transactions")
                transactions
            }
            else -> {
                val filtered = transactions.filter {
                    it.status.equals(status, ignoreCase = true)
                }
                Log.d(TAG, "  Filter logic: status='$status' → ${filtered.size} matches")
                filtered
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // UPDATE TRANSACTION STATUS
    // ════════════════════════════════════════════════════════════

    fun approveTransaction(transaction: TransactionModel) {
        Log.d(TAG, "✅ Approving transaction: ${transaction.appTransactionId}")
        updateStatus(transaction, "success")
    }

    fun rejectTransaction(transaction: TransactionModel) {
        Log.d(TAG, "❌ Rejecting transaction: ${transaction.appTransactionId}")
        updateStatus(transaction, "failed")
    }

    fun markAsPending(transaction: TransactionModel) {
        Log.d(TAG, "⏳ Marking as pending: ${transaction.appTransactionId}")
        updateStatus(transaction, "pending")
    }

    private fun updateStatus(transaction: TransactionModel, newStatus: String) {
        viewModelScope.launch {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Updating Transaction Status")
            Log.d(TAG, "  Transaction: ${transaction.appTransactionId}")
            Log.d(TAG, "  Current status: ${transaction.status}")
            Log.d(TAG, "  New status: $newStatus")

            _state.update {
                it.copy(
                    isUpdating = true,
                    error = null,
                    successMessage = null
                )
            }

            val result = updateTransactionStatusUseCase(
                transactionId = transaction.appTransactionId,
                userId = transaction.userId,
                status = newStatus,
                processed = true
            )

            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "✅ Status updated successfully")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            successMessage = "Transaction status updated to $newStatus. User balance adjusted."
                        )
                    }
                }

                is Resource.Error -> {
                    Log.e(TAG, "❌ Update failed: ${result.message}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            error = result.message
                        )
                    }
                }

                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // CLEAR MESSAGES
    // ════════════════════════════════════════════════════════════

    fun clearMessages() {
        Log.d(TAG, "🧹 Clearing messages")
        _state.update {
            it.copy(
                successMessage = null,
                error = null
            )
        }
    }

    // ════════════════════════════════════════════════════════════
    // ON CLEARED
    // ════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 ViewModel cleared")
    }
}