package com.droidnest.tech.pysadmin.presentation.screens.usermanagement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User
import com.droidnest.tech.pysadmin.domain.usecase.*
import com.droidnest.tech.pysadmin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false,
    val selectedFilter: UserFilter = UserFilter.ALL,
    val searchQuery: String = ""
)

enum class UserFilter {
    ALL,
    ACTIVE,
    LOCKED,  // ✅ Changed from BLOCKED
    KYC_UNVERIFIED,
    KYC_PENDING,
    KYC_VERIFIED,
    KYC_REJECTED
}

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val searchUsersUseCase: SearchUsersUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "UserManagementVM"
    }

    private val _state = MutableStateFlow(UserManagementState())
    val state: StateFlow<UserManagementState> = _state.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        loadAllUsers()
    }

    // ════════════════════════════════════════════════════════════
    // LOAD ALL USERS
    // ════════════════════════════════════════════════════════════

    fun loadAllUsers() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "👥 Loading All Users")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        viewModelScope.launch {
            getAllUsersUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "⏳ Loading...")
                        _state.update { it.copy(isLoading = true, error = null) }
                    }

                    is Resource.Success -> {
                        val users = result.data ?: emptyList()
                        Log.d(TAG, "✅ Loaded ${users.size} users")

                        _state.update { currentState ->
                            val filtered = applyFilter(
                                users = users,
                                filter = currentState.selectedFilter,
                                searchQuery = currentState.searchQuery
                            )

                            Log.d(TAG, "📋 Filtered (${currentState.selectedFilter}): ${filtered.size} users")

                            currentState.copy(
                                isLoading = false,
                                users = users,
                                filteredUsers = filtered,
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
    // SET FILTER
    // ════════════════════════════════════════════════════════════

    fun setFilter(filter: UserFilter) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔍 Filtering Users")
        Log.d(TAG, "  Previous filter: ${_state.value.selectedFilter}")
        Log.d(TAG, "  New filter: $filter")
        Log.d(TAG, "  Total users: ${_state.value.users.size}")

        _state.update { currentState ->
            val filtered = applyFilter(
                users = currentState.users,
                filter = filter,
                searchQuery = currentState.searchQuery
            )

            Log.d(TAG, "  Filtered result: ${filtered.size} users")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            currentState.copy(
                selectedFilter = filter,
                filteredUsers = filtered
            )
        }
    }

    // ════════════════════════════════════════════════════════════
    // SEARCH USERS
    // ════════════════════════════════════════════════════════════

    fun searchUsers(query: String) {
        Log.d(TAG, "🔎 Search query: '$query'")

        _state.update { currentState ->
            if (query.isBlank()) {
                val filtered = applyFilter(
                    users = currentState.users,
                    filter = currentState.selectedFilter,
                    searchQuery = ""
                )

                currentState.copy(
                    searchQuery = "",
                    filteredUsers = filtered
                )
            } else {
                currentState.copy(searchQuery = query)
            }
        }

        if (query.isBlank()) {
            return
        }

        viewModelScope.launch {
            searchUsersUseCase(query).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val searchResults = result.data ?: emptyList()
                        Log.d(TAG, "  Search found: ${searchResults.size} users")

                        _state.update { currentState ->
                            val filtered = applyFilter(
                                users = searchResults,
                                filter = currentState.selectedFilter,
                                searchQuery = query
                            )

                            Log.d(TAG, "  After filter: ${filtered.size} users")

                            currentState.copy(filteredUsers = filtered)
                        }
                    }

                    is Resource.Error -> {
                        Log.e(TAG, "  Search error: ${result.message}")
                        _state.update { it.copy(error = result.message) }
                    }

                    is Resource.Loading -> {
                        // Optional: show search loading indicator
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅ HELPER: APPLY FILTER (UPDATED FOR ACCOUNT LOCK)
    // ════════════════════════════════════════════════════════════

    private fun applyFilter(
        users: List<User>,
        filter: UserFilter,
        searchQuery: String = ""
    ): List<User> {
        // First apply search filter if query exists
        val searchFiltered = if (searchQuery.isNotBlank()) {
            users.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.email.contains(searchQuery, ignoreCase = true) ||
                        user.phone.contains(searchQuery, ignoreCase = true) ||
                        user.refCode.contains(searchQuery, ignoreCase = true)
            }
        } else {
            users
        }

        // Then apply status filter
        val filtered = when (filter) {
            UserFilter.ALL -> {
                Log.d(TAG, "  Filter logic: Showing ALL users")
                searchFiltered
            }
            UserFilter.ACTIVE -> {
                // ✅ Updated: Check accountLock instead of isBlocked
                val result = searchFiltered.filter { user ->
                    user.accountLock?.isLocked != true
                }
                Log.d(TAG, "  Filter logic: ACTIVE users → ${result.size}")
                result
            }
            UserFilter.LOCKED -> {
                // ✅ Updated: Check accountLock instead of isBlocked
                val result = searchFiltered.filter { user ->
                    user.accountLock?.isLocked == true
                }
                Log.d(TAG, "  Filter logic: LOCKED users → ${result.size}")
                result
            }
            UserFilter.KYC_UNVERIFIED -> {
                val result = searchFiltered.filter { it.kycStatus == KycStatus.UNVERIFIED }
                Log.d(TAG, "  Filter logic: UNVERIFIED KYC → ${result.size}")
                result
            }
            UserFilter.KYC_PENDING -> {
                val result = searchFiltered.filter { it.kycStatus == KycStatus.PENDING }
                Log.d(TAG, "  Filter logic: PENDING KYC → ${result.size}")
                result
            }
            UserFilter.KYC_VERIFIED -> {
                val result = searchFiltered.filter { it.kycStatus == KycStatus.VERIFIED }
                Log.d(TAG, "  Filter logic: VERIFIED KYC → ${result.size}")
                result
            }
            UserFilter.KYC_REJECTED -> {
                val result = searchFiltered.filter { it.kycStatus == KycStatus.REJECTED }
                Log.d(TAG, "  Filter logic: REJECTED KYC → ${result.size}")
                result
            }
        }

        return filtered
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