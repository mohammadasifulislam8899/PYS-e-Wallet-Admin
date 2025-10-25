// domain/usecase/kyc/ApproveKycRequestUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase.kyc

import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import javax.inject.Inject

class ApproveKycRequestUseCase @Inject constructor(
    private val repository: KycManagementRepository
) {
    suspend operator fun invoke(requestId: String, adminId: String, adminNotes: String?) = 
        repository.approveKycRequest(requestId, adminId, adminNotes)
}