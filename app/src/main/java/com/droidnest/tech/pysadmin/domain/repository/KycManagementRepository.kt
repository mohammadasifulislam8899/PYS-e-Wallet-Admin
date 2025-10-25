// domain/repository/KycManagementRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.KycRequest
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface KycManagementRepository {
    suspend fun getAllKycRequests(): Flow<Resource<List<KycRequest>>>
    suspend fun getKycRequestById(requestId: String): Resource<KycRequest>
    suspend fun getKycRequestsByStatus(status: KycStatus): Flow<Resource<List<KycRequest>>>
    suspend fun approveKycRequest(requestId: String, adminId: String, adminNotes: String?): Resource<String>
    suspend fun rejectKycRequest(requestId: String, adminId: String, rejectionReason: String): Resource<String>
    suspend fun getPendingKycCount(): Flow<Resource<Int>>
}