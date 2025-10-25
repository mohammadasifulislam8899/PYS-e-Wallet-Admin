// domain/repository/AddMoneyPaymentMethodRepository.kt
package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.AddMoneyPaymentMethod
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AddMoneyPaymentMethodRepository {
    fun getAllPaymentMethods(): Flow<Resource<List<AddMoneyPaymentMethod>>>
    fun getPaymentMethodsByCurrency(currency: String): Flow<Resource<List<AddMoneyPaymentMethod>>>
    fun getEnabledPaymentMethods(): Flow<Resource<List<AddMoneyPaymentMethod>>>
    suspend fun addPaymentMethod(paymentMethod: AddMoneyPaymentMethod): Resource<String>
    suspend fun updatePaymentMethod(paymentMethod: AddMoneyPaymentMethod): Resource<String>
    suspend fun deletePaymentMethod(id: String): Resource<String>
    suspend fun togglePaymentMethod(id: String, isEnabled: Boolean): Resource<String>
}