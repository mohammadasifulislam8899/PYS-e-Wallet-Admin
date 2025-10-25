// domain/usecase/kyc/GetPendingKycCountUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase.kyc

import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import javax.inject.Inject

class GetPendingKycCountUseCase @Inject constructor(
    private val repository: KycManagementRepository
) {
    suspend operator fun invoke() = repository.getPendingKycCount()
}