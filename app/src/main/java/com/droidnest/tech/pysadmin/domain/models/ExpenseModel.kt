package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class ExpenseModel(
    val id: String = "",
    val transactionId: String = "",          // Related transaction (if any)
    val userId: String = "",                 // Who received the expense
    val userName: String = "",
    val userPhone: String = "",
    val type: String = "",                   // "cashback" or "referral_bonus"
    val amountBDT: Double = 0.0,
    val amountMYR: Double = 0.0,
    val totalAmountBDT: Double = 0.0,        // BDT + (MYR * rate)
    val rateUsed: Double = 0.0,
    val relatedUserId: String? = null,       // For referral: who deposited
    val relatedUserName: String? = null,
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val day: String = "",                    // "2024-01-15"
    val month: String = "",                  // "2024-01"
    val year: Int = 0                        // 2024
)