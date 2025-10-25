// domain/usecase/kyc/GetKycRequestByIdUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase.kyc

import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import javax.inject.Inject

class GetKycRequestByIdUseCase @Inject constructor(
    private val repository: KycManagementRepository
) {
    suspend operator fun invoke(requestId: String) = 
        repository.getKycRequestById(requestId)
}