// admin_app/domain/repository/TransactionRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun getAllTransactions(): Flow<Resource<List<TransactionModel>>>
    suspend fun getTransactionsByStatus(status: String): Flow<Resource<List<TransactionModel>>>
    suspend fun updateTransactionStatus(
        transactionId: String,
        userId: String,
        newStatus: String,
        processed: Boolean
    ): Resource<String>
    suspend fun getTransactionById(transactionId: String): Resource<TransactionModel>
}