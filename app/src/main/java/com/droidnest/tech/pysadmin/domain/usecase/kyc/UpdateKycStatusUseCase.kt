// domain/usecase/UpdateKycStatusUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase.kyc

import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.repository.UserManagementRepository
import javax.inject.Inject

class UpdateKycStatusUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(
        userId: String,
        kycStatus: KycStatus,
        rejectionReason: String? = null
    ) = repository.updateKycStatus(userId, kycStatus, rejectionReason)
}