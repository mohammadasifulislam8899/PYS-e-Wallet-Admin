// domain/model/WithdrawalRequest.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class WithdrawalRequest(
    val withdrawalId: String = "",
    val userId: String = "",
    val userName: String = "",
    val currency: String = "", // BDT or MYR
    val amount: Double = 0.0,
    val exchangeRate: Double = 1.0,
    val convertedAmountBDT: Double = 0.0,
    val fee: Double = 0.0,
    val feePercentage: Double = 1.5,
    val netAmount: Double = 0.0,
    val bankAccountId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val status: String = "pending", // pending, processing, completed, failed
    val notes: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null
)