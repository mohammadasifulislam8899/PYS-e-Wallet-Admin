// admin_app/presentation/splash/SplashViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.repository.AdminAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AdminAuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()
    
    companion object {
        private const val TAG = "SplashViewModel"
        private const val SPLASH_DURATION = 2500L // 2.5 seconds
    }
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔍 Checking admin authentication status...")
                
                // Show splash for minimum duration
                delay(SPLASH_DURATION)
                
                val isLoggedIn = authRepository.isAdminLoggedIn()
                
                Log.d(TAG, "Admin logged in: $isLoggedIn")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = isLoggedIn
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking auth status", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    error = e.message
                )
            }
        }
    }
}