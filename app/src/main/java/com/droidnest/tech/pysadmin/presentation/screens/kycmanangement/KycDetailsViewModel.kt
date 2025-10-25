// presentation/kycdetails/KycDetailsViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.kycmanangement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.KycRequest
import com.droidnest.tech.pysadmin.domain.usecase.kyc.*
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KycDetailsState(
    val isLoading: Boolean = false,
    val kycRequest: KycRequest? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false
)

@HiltViewModel
class KycDetailsViewModel @Inject constructor(
    private val getKycRequestByIdUseCase: GetKycRequestByIdUseCase,
    private val approveKycRequestUseCase: ApproveKycRequestUseCase,
    private val rejectKycRequestUseCase: RejectKycRequestUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(KycDetailsState())
    val state: StateFlow<KycDetailsState> = _state.asStateFlow()

    fun loadKycRequest(requestId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (val result = getKycRequestByIdUseCase(requestId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        kycRequest = result.data
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

    fun approveKyc(adminNotes: String? = null) {
        val requestId = _state.value.kycRequest?.id ?: return
        val adminId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = approveKycRequestUseCase(requestId, adminId, adminNotes)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    // Reload to get updated data
                    loadKycRequest(requestId)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun rejectKyc(rejectionReason: String) {
        val requestId = _state.value.kycRequest?.id ?: return
        val adminId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = rejectKycRequestUseCase(requestId, adminId, rejectionReason)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    // Reload to get updated data
                    loadKycRequest(requestId)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = result.message
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