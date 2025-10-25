package com.droidnest.tech.pysadmin.presentation.screens.kycmanangement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.KycRequest
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.usecase.kyc.*
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KycManagementState(
    val isLoading: Boolean = false,
    val kycRequests: List<KycRequest> = emptyList(),
    val filteredRequests: List<KycRequest> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false,
    val selectedFilter: KycFilter = KycFilter.ALL,
    val pendingCount: Int = 0
)

enum class KycFilter {
    ALL, PENDING, VERIFIED, REJECTED
}

@HiltViewModel
class KycManagementViewModel @Inject constructor(
    private val getAllKycRequestsUseCase: GetAllKycRequestsUseCase,
    private val getPendingKycCountUseCase: GetPendingKycCountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(KycManagementState())
    val state: StateFlow<KycManagementState> = _state.asStateFlow()

    init {
        loadAllKycRequests()
        loadPendingCount()
    }

    fun loadAllKycRequests() {
        viewModelScope.launch {
            getAllKycRequestsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        val requests = result.data ?: emptyList()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            kycRequests = requests,
                            filteredRequests = applyFilter(requests, _state.value.selectedFilter), // ✅ Filter pass করছি
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadPendingCount() {
        viewModelScope.launch {
            getPendingKycCountUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(pendingCount = result.data ?: 0)
                    }
                    else -> {}
                }
            }
        }
    }

    fun setFilter(filter: KycFilter) {
        _state.value = _state.value.copy(
            selectedFilter = filter,
            filteredRequests = applyFilter(_state.value.kycRequests, filter)  // ✅ নতুন filter parameter পাস করছি
        )
    }

    // ✅ Filter parameter যোগ করেছি
    private fun applyFilter(requests: List<KycRequest>, filter: KycFilter): List<KycRequest> {
        return when (filter) {
            KycFilter.ALL -> requests
            KycFilter.PENDING -> requests.filter { it.status == KycStatus.PENDING }
            KycFilter.VERIFIED -> requests.filter { it.status == KycStatus.VERIFIED }
            KycFilter.REJECTED -> requests.filter { it.status == KycStatus.REJECTED }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            successMessage = null,
            error = null
        )
    }
}