// domain/model/DepositRequest.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class DepositRequest(
    val depositId: String = "",
    val userId: String = "",
    val userName: String = "",
    val currency: String = "", // BDT or MYR
    val amount: Double = 0.0,
    val exchangeRate: Double = 1.0,
    val convertedAmountBDT: Double = 0.0,
    val fee: Double = 0.0,
    val feePercentage: Double = 1.0,
    val netAmount: Double = 0.0,
    val paymentMethod: String = "", // bkash, nagad, bank_transfer
    val senderNumber: String = "",
    val transactionId: String = "",
    val proofUrl: String? = null,
    val status: String = "pending", // pending, success, failed
    val notes: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null
)