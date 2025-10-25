package com.droidnest.tech.pysadmin.presentation.screens.dashboard

import com.droidnest.tech.pysadmin.domain.models.DashboardStats

data class DashboardState(
    val isLoading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val error: String? = null,
    val isRefreshing: Boolean = false
)