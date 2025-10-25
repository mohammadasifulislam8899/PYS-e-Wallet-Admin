package com.droidnest.tech.pysadmin.domain.repository

import com.droidnest.tech.pysadmin.domain.models.DashboardStats
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardStats(): Flow<Resource<DashboardStats>>
    suspend fun refreshDashboardStats()
}