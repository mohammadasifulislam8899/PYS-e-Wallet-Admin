// domain/usecase/GetTransactionByIdUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase

import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.droidnest.tech.pysadmin.domain.repository.TransactionRepository
import com.droidnest.tech.pysadmin.utils.Resource
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String): Resource<TransactionModel> {
        return repository.getTransactionById(transactionId)
    }
}