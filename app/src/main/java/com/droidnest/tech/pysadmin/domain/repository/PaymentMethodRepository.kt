// admin_app/domain/repository/PaymentMethodRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.WithdrawPaymentMethod
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    suspend fun getAllPaymentMethods(): Flow<Resource<List<WithdrawPaymentMethod>>>
    suspend fun getPaymentMethodById(id: String): Resource<WithdrawPaymentMethod?>
    suspend fun addPaymentMethod(method: WithdrawPaymentMethod): Resource<String>
    suspend fun updatePaymentMethod(method: WithdrawPaymentMethod): Resource<String>
    suspend fun deletePaymentMethod(id: String): Resource<String>
    suspend fun togglePaymentMethodStatus(id: String, enabled: Boolean): Resource<String>
}