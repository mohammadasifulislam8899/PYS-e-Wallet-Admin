// domain/repository/UserManagementRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UserManagementRepository {
    suspend fun getAllUsers(): Flow<Resource<List<User>>>
    suspend fun getUserById(userId: String): Resource<User>
    suspend fun lockUserAccount(userId: String, duration: Long, reason: String): Resource<String>
    suspend fun unlockUserAccount(userId: String): Resource<String>
    suspend fun updateUserBalance(userId: String, currency: String, newBalance: Double): Resource<String>
    suspend fun searchUsers(query: String): Flow<Resource<List<User>>>
    suspend fun resetUserPin(userId: String, newPin: String): Resource<String>

    suspend fun updateKycStatus(
        userId: String,
        kycStatus: KycStatus,
        rejectionReason: String? = null
    ): Resource<String>
}