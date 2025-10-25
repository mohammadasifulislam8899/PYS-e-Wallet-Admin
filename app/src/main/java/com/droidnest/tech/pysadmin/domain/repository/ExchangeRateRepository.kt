package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.ExchangeRate
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun getExchangeRate(): Flow<Resource<ExchangeRate>>
    suspend fun updateExchangeRate(myrRate: Double, adminName: String): Resource<Unit>
}