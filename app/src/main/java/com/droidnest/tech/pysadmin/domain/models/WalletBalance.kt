// user_app/domain/model/WalletBalance.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class WalletBalance(
    val currency: String = "",
    val amount: Double = 0.0,
    val amountInBDT: Double = 0.0,
    val symbol: String = "৳"
)

data class WalletSummary(
    val balances: List<WalletBalance> = emptyList(),
    val totalBalanceBDT: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now()
)