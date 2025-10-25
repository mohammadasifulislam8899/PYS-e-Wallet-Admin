// admin_app/presentation/setup/OneTimeSetupViewModel.kt
package com.droidnest.tech.pysadmin.presentation.screens.auth.signup

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.repository.AdminAuthRepository
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupState(
    val name: String = "",  // ✅ Blank
    val email: String = "",  // ✅ Blank
    val phone: String = "",  // ✅ Blank
    val password: String = "",  // ✅ Blank
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val createdCredentials: Pair<String, String>? = null // email, password
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
                email.isNotBlank() &&
                phone.isNotBlank() &&
                password.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.length >= 8 &&
                nameError == null &&
                emailError == null &&
                phoneError == null &&
                passwordError == null
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AdminAuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()
    
    companion object {
        private const val TAG = "OneTimeSetupViewModel"
    }
    
    fun onNameChange(name: String) {
        _state.update {
            it.copy(
                name = name,
                nameError = null,
                error = null
            )
        }
    }
    
    fun onEmailChange(email: String) {
        _state.update {
            it.copy(
                email = email.trim(),
                emailError = null,
                error = null
            )
        }
    }
    
    fun onPhoneChange(phone: String) {
        _state.update {
            it.copy(
                phone = phone.trim(),
                phoneError = null,
                error = null
            )
        }
    }
    
    fun onPasswordChange(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null,
                error = null
            )
        }
    }
    
    fun togglePasswordVisibility() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }
    
    fun createAdmin() {
        // Validate inputs
        if (!validateInputs()) {
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📝 Creating admin account")
            Log.d(TAG, "Name: ${_state.value.name}")
            Log.d(TAG, "Email: ${_state.value.email}")
            Log.d(TAG, "Phone: ${_state.value.phone}")
            
            authRepository.createAdmin(
                name = _state.value.name,
                email = _state.value.email,
                phone = _state.value.phone,
                password = _state.value.password
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "⏳ Creating admin...")
                    }
                    
                    is Resource.Success -> {
                        val admin = result.data
                        Log.d(TAG, "✅ Admin created successfully!")
                        Log.d(TAG, "Admin ID: ${admin?.adminId}")
                        Log.d(TAG, "Name: ${admin?.name}")
                        Log.d(TAG, "Email: ${admin?.email}")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null,
                                createdCredentials = Pair(
                                    _state.value.email,
                                    _state.value.password
                                )
                            )
                        }
                    }
                    
                    is Resource.Error -> {
                        Log.e(TAG, "❌ Admin creation failed: ${result.message}")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to create admin"
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val name = _state.value.name.trim()
        val email = _state.value.email.trim()
        val phone = _state.value.phone.trim()
        val password = _state.value.password
        
        var isValid = true
        
        // Validate name
        if (name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
            isValid = false
        } else if (name.length < 3) {
            _state.update { it.copy(nameError = "Name must be at least 3 characters") }
            isValid = false
        }
        
        // Validate email
        if (email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }
        
        // Validate phone
        if (phone.isBlank()) {
            _state.update { it.copy(phoneError = "Phone number is required") }
            isValid = false
        } else if (phone.length < 10) {
            _state.update { it.copy(phoneError = "Invalid phone number") }
            isValid = false
        }
        
        // Validate password
        if (password.isBlank()) {
            _state.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (password.length < 8) {
            _state.update { it.copy(passwordError = "Password must be at least 8 characters") }
            isValid = false
        } else if (!password.any { it.isDigit() }) {
            _state.update { it.copy(passwordError = "Password must contain at least one number") }
            isValid = false
        } else if (!password.any { it.isUpperCase() }) {
            _state.update { it.copy(passwordError = "Password must contain at least one uppercase letter") }
            isValid = false
        }
        
        return isValid
    }
}