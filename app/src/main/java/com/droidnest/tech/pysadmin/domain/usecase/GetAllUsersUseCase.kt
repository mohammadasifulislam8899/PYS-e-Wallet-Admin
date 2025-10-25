// domain/usecase/GetAllUsersUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase

import com.droidnest.tech.pysadmin.domain.repository.UserManagementRepository
import javax.inject.Inject

class GetAllUsersUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke() = repository.getAllUsers()
}

// domain/usecase/GetUserByIdUseCase.kt
class GetUserByIdUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(userId: String) = repository.getUserById(userId)
}
class ResetUserPinUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(userId: String, newPin: String) =
        repository.resetUserPin(userId, newPin)
}


// domain/usecase/UpdateUserBalanceUseCase.kt
class UpdateUserBalanceUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(userId: String, currency: String, newBalance: Double) =
        repository.updateUserBalance(userId, currency, newBalance)
}

// domain/usecase/SearchUsersUseCase.kt
class SearchUsersUseCase @Inject constructor(
    private val repository: UserManagementRepository
) {
    suspend operator fun invoke(query: String) = repository.searchUsers(query)
}