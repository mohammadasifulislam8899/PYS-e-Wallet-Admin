// domain/usecase/kyc/RejectKycRequestUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase.kyc

import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import javax.inject.Inject

class RejectKycRequestUseCase @Inject constructor(
    private val repository: KycManagementRepository
) {
    suspend operator fun invoke(requestId: String, adminId: String, rejectionReason: String) = 
        repository.rejectKycRequest(requestId, adminId, rejectionReason)
}