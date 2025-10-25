// admin_app/domain/repository/AdminAuthRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AdminAuthRepository {
    
    /**
     * Login with email and password
     * Validates admin credentials and checks admin collection
     * @return Flow<Resource<Admin>> - Admin data if successful
     */
    suspend fun login(
        email: String,
        password: String
    ): Flow<Resource<Admin>>
    
    /**
     * Logout current admin
     * Clears Firebase Auth session
     * @return Resource<Unit> - Success or error
     */
    suspend fun logout(): Resource<Unit>
    
    /**
     * Get current logged-in admin with real-time updates
     * Listens to Firestore changes
     * @return Flow<Resource<Admin?>> - Admin data or null
     */
    suspend fun getCurrentAdmin(): Flow<Resource<Admin?>>
    
    /**
     * Check if admin is logged in
     * Quick synchronous check
     * @return Boolean - true if logged in
     */
    fun isAdminLoggedIn(): Boolean
    
    /**
     * Update admin's last active timestamp
     * Called periodically to track activity
     * @param adminId - Admin user ID
     * @return Resource<Unit> - Success or error
     */
    suspend fun updateAdminActivity(adminId: String): Resource<Unit>
    
    /**
     * Send password reset email
     * @param email - Admin email address
     * @return Resource<Unit> - Success or error
     */
    suspend fun resetPassword(email: String): Resource<Unit>

    suspend fun createAdmin(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Flow<Resource<Admin>>
}