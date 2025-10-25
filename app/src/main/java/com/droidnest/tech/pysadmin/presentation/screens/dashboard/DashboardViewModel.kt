package com.droidnest.tech.pysadmin.presentation.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.repository.DashboardRepository
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isLoading = true,
                                error = null
                            )
                        }
                    }

                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                stats = result.data ?: it.stats,
                                error = null,
                                isRefreshing = false
                            )
                        }
                    }

                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message ?: "Unknown error occurred"
                            )
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            try {
                Log.d("DashboardViewModel", "Starting refresh...")

                // Refresh dashboard stats in Firestore
                dashboardRepository.refreshDashboardStats()

                // ✅ Optional: Small delay to show the refresh animation
                kotlinx.coroutines.delay(500)

                Log.d("DashboardViewModel", "Refresh completed")

                _state.update { it.copy(isRefreshing = false) }

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Refresh failed", e)
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh"
                    )
                }
            }
        }
    }
}