package com.droidnest.tech.pysadmin.presentation.screens.userdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User
import com.droidnest.tech.pysadmin.domain.usecase.*
import com.droidnest.tech.pysadmin.domain.usecase.kyc.UpdateKycStatusUseCase
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserDetailsState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false
)

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val lockUserAccountUseCase: LockUserAccountUseCase,
    private val unlockUserAccountUseCase: UnlockUserAccountUseCase,
    private val updateUserBalanceUseCase: UpdateUserBalanceUseCase,
    private val updateKycStatusUseCase: UpdateKycStatusUseCase,
    private val resetUserPinUseCase: ResetUserPinUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UserDetailsState())
    val state: StateFlow<UserDetailsState> = _state.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (val result = getUserByIdUseCase(userId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        user = result.data
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

    // ✅ NEW: Lock User Account
    fun lockUserAccount(duration: Long, reason: String) {
        val userId = _state.value.user?.userId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = lockUserAccountUseCase(userId, duration, reason)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    loadUser(userId)
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

    // ✅ NEW: Unlock User Account
    fun unlockUserAccount() {
        val userId = _state.value.user?.userId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = unlockUserAccountUseCase(userId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    loadUser(userId)
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

    fun resetUserPin(newPin: String) {
        val userId = _state.value.user?.userId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = resetUserPinUseCase(userId, newPin)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    loadUser(userId)
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

    fun updateKycStatus(kycStatus: KycStatus, rejectionReason: String? = null) {
        val userId = _state.value.user?.userId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = updateKycStatusUseCase(userId, kycStatus, rejectionReason)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    loadUser(userId)
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

    fun updateBalance(currency: String, newBalance: Double) {
        val userId = _state.value.user?.userId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)

            when (val result = updateUserBalanceUseCase(userId, currency, newBalance)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        successMessage = result.data
                    )
                    loadUser(userId)
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