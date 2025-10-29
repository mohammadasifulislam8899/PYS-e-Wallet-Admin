// admin_app/domain/repository/AdminAuthRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AdminAuthRepository {

    /**
     * Login with email and password
     * Validates admin credentials and checks admin collection
     */
    suspend fun login(
        email: String,
        password: String
    ): Flow<Resource<Admin>>

    /**
     * Logout current admin
     */
    suspend fun logout(): Resource<Unit>

    /**
     * Get current logged-in admin with real-time updates
     */
    suspend fun getCurrentAdmin(): Flow<Resource<Admin?>>

    /**
     * Check if admin is logged in and verified
     */
    suspend fun isAdminLoggedIn(): Boolean

    /**
     * Update admin's last active timestamp
     */
    suspend fun updateAdminActivity(adminId: String): Resource<Unit>

    /**
     * Send password reset email
     */
    suspend fun resetPassword(email: String): Resource<Unit>

    /**
     * Create new admin account (Signup)
     */
    suspend fun createAdmin(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Flow<Resource<Admin>>
}