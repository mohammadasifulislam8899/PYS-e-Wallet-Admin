// admin_app/presentation/settings/SettingsViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.domain.repository.AdminAuthRepository
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val admin: Admin? = null,
    val pendingTransactions: Int = 0,
    val pendingKyc: Int = 0,
    val totalUsers: Int = 0,
    val unreadNotifications: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AdminAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var adminDataJob: Job? = null

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        loadAdminData()
        loadStats()
    }

    private fun loadAdminData() {
        // Cancel previous job
        adminDataJob?.cancel()

        adminDataJob = viewModelScope.launch {
            authRepository.getCurrentAdmin()
                .catch { exception ->
                    Log.e(TAG, "Error in admin flow", exception)
                    emit(Resource.Error(exception.message ?: "Unknown error"))
                }
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            Log.d(TAG, "Admin loaded: ${result.data?.name}")
                            _state.update {
                                it.copy(
                                    admin = result.data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Error loading admin: ${result.message}")
                            _state.update {
                                it.copy(
                                    error = result.message,
                                    isLoading = false
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
                }
        }
    }

    private fun loadStats() {
        _state.update {
            it.copy(
                pendingTransactions = 23,
                pendingKyc = 45,
                totalUsers = 1234,
                unreadNotifications = 5
            )
        }
    }

    fun refreshStats() {
        loadStats()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 ViewModel cleared")
        adminDataJob?.cancel()
    }
}