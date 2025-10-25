// admin_app/domain/usecase/UpdateTransactionStatusUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase

import com.droidnest.tech.pysadmin.domain.repository.TransactionRepository
import com.droidnest.tech.pysadmin.utils.Resource
import javax.inject.Inject

class UpdateTransactionStatusUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        userId: String,
        status: String,
        processed: Boolean
    ): Resource<String> {
        // Validate status
        if (status !in listOf("pending", "success", "failed")) {
            return Resource.Error("Invalid status")
        }

        return repository.updateTransactionStatus(
            transactionId = transactionId,
            userId = userId,
            newStatus = status,
            processed = processed
        )
    }
}