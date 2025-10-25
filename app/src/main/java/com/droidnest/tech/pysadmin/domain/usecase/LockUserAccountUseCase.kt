package com.droidnest.tech.pysadmin.domain.usecase

import com.droidnest.tech.pysadmin.domain.repository.UserManagementRepository
import com.droidnest.tech.pysadmin.utils.Resource
import javax.inject.Inject

class LockUserAccountUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(
        userId: String,
        duration: Long,
        reason: String
    ): Resource<String> {
        return repository.lockUserAccount(userId, duration, reason)
    }
}