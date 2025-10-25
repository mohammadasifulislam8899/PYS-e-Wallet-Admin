package com.droidnest.tech.pysadmin.domain.models

data class DashboardStats(
    // User Stats
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val blockedUsers: Int = 0,

    // KYC Stats
    val pendingKyc: Int = 0,
    val verifiedKyc: Int = 0,
    val rejectedKyc: Int = 0,

    // Transaction Stats
    val pendingTransactions: Int = 0,
    val successTransactions: Int = 0,
    val failedTransactions: Int = 0,

    // ✅✅✅ Revenue & Expense Stats ✅✅✅

    // Today
    val todayGrossRevenue: Double = 0.0,      // Total fees collected
    val todayExpenses: Double = 0.0,          // Cashback + Referral
    val todayNetRevenue: Double = 0.0,        // Gross - Expenses

    // Weekly
    val weeklyGrossRevenue: Double = 0.0,
    val weeklyExpenses: Double = 0.0,
    val weeklyNetRevenue: Double = 0.0,

    // Monthly
    val monthlyGrossRevenue: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyNetRevenue: Double = 0.0,

    // Other Stats
    val totalBalanceInSystem: Double = 0.0,
    val unreadNotifications: Int = 0,

    // ✅ For backward compatibility (deprecated)
    @Deprecated("Use todayGrossRevenue instead")
    val todayRevenue: Double = todayGrossRevenue,
    @Deprecated("Use weeklyGrossRevenue instead")
    val weeklyRevenue: Double = weeklyGrossRevenue,
    @Deprecated("Use monthlyGrossRevenue instead")
    val monthlyRevenue: Double = monthlyGrossRevenue
)