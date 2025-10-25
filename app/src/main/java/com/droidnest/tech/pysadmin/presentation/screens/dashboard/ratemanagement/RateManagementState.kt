package com.droidnest.tech.pysadmin.presentation.screens.dashboard.ratemanagement

import com.droidnest.tech.pysadmin.domain.models.ExchangeRate

data class RateManagementState(
    val isLoading: Boolean = false,
    val exchangeRate: ExchangeRate = ExchangeRate(),
    val error: String? = null,
    val successMessage: String? = null,
    val isUpdating: Boolean = false,
    val showBottomSheet: Boolean = false
)