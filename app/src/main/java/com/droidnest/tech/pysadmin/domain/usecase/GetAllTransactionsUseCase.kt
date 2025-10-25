// admin_app/domain/usecase/GetAllTransactionsUseCase.kt
package com.droidnest.tech.pysadmin.domain.usecase

import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.droidnest.tech.pysadmin.domain.repository.TransactionRepository
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(): Flow<Resource<List<TransactionModel>>> {
        return repository.getAllTransactions()
    }
}