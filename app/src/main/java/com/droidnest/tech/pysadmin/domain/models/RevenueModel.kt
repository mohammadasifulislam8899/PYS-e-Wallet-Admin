package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class RevenueModel(
    val id: String = "",
    val transactionId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val type: String = "",              // add_money, withdraw, send_money
    val paymentMethod: String = "",     // bKash, Nagad, etc.
    val feeBDT: Double = 0.0,
    val feeMYR: Double = 0.0,
    val totalFeeBDT: Double = 0.0,      // BDT + (MYR * rate)
    val rateUsed: Double = 0.0,
    val amount: Double = 0.0,           // Original transaction amount
    val currency: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp = Timestamp.now(),
    val day: String = "",               // "2024-01-15"
    val month: String = "",             // "2024-01"
    val year: Int = 0                   // 2024
)