package com.droidnest.tech.pysadmin.domain.usecase

import com.droidnest.tech.pysadmin.domain.repository.UserManagementRepository
import com.droidnest.tech.pysadmin.utils.Resource
import javax.inject.Inject

class UnlockUserAccountUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(userId: String): Resource<String> {
        return repository.unlockUserAccount(userId)
    }
}