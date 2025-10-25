// user_app/domain/model/QuickStats.kt
package com.droidnest.tech.pysadmin.domain.models

data class QuickStats(
    val totalTransactions: Int = 0,
    val pendingTransactions: Int = 0,
    val totalReferrals: Int = 0,
    val referralEarnings: Double = 0.0
)