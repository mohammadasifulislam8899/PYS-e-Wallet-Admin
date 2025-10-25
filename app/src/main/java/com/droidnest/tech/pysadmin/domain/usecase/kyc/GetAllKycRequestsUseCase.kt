// domain/usecase/kyc/GetAllKycRequestsUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase.kyc

import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import javax.inject.Inject

class GetAllKycRequestsUseCase @Inject constructor(
    private val repository: KycManagementRepository
) {
    suspend operator fun invoke() = repository.getAllKycRequests()
}