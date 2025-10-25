// admin_app/domain/models/TransactionModel.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class TransactionModel(
    // ═══════════════════════════════════════════════════════
    // BASIC INFO
    // ═══════════════════════════════════════════════════════
    val appTransactionId: String = "",
    val userId: String = "",

    // ═══════════════════════════════════════════════════════
    // SENDER DETAILS (for admin to see who sent)
    // ═══════════════════════════════════════════════════════
    val senderName: String? = null,
    val senderPhone: String? = null,
    val senderEmail: String? = null,

    // ═══════════════════════════════════════════════════════
    // RECIPIENT DETAILS (for send_money)
    // ═══════════════════════════════════════════════════════
    val recipientId: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val recipientEmail: String? = null,

    // ═══════════════════════════════════════════════════════
    // TRANSACTION DETAILS
    // ═══════════════════════════════════════════════════════
    val type: String = "",
    val currency: String = "",
    val amount: Double = 0.0,

    // ═══════════════════════════════════════════════════════
    // FEE & CONVERSION - BDT
    // ═══════════════════════════════════════════════════════
    val feeBDT: Double = 0.0,
    val convertedAmountBDT: Double = 0.0,
    val netAmountBDT: Double = 0.0,

    // ═══════════════════════════════════════════════════════
    // FEE & CONVERSION - MYR
    // ═══════════════════════════════════════════════════════
    val feeMYR: Double = 0.0,
    val convertedAmountMYR: Double = 0.0,
    val netAmountMYR: Double = 0.0,

    // ═══════════════════════════════════════════════════════
    // EXCHANGE RATE
    // ═══════════════════════════════════════════════════════
    val rateUsed: Double = 1.0,

    // ═══════════════════════════════════════════════════════
    // PAYMENT DETAILS (for add_money & withdraw)
    // ═══════════════════════════════════════════════════════
    val paymentMethod: String? = null,
    val accountNumber: String? = null,
    val transactionId: String? = null,
    val paymentProofId: String? = null,
    val proofUrl: String? = null,

    // ═══════════════════════════════════════════════════════
    // 👇 NEW: DYNAMIC FIELD DATA
    // ═══════════════════════════════════════════════════════
    val dynamicFieldData: Map<String, String> = emptyMap(),

    // ✅✅✅ ADD THIS LINE HERE ✅✅✅
    val userProvidedFields: Map<String, String> = emptyMap(),

    // ═══════════════════════════════════════════════════════
    // ADDITIONAL INFO
    // ═══════════════════════════════════════════════════════
    val message: String? = null,

    // ═══════════════════════════════════════════════════════
    // STATUS & PROCESSING
    // ═══════════════════════════════════════════════════════
    val status: String = "pending",
    val processed: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null,
    val processedBy: String? = null,

    // ═══════════════════════════════════════════════════════
    // SMART DEDUCTION TRACKING (for Withdraw only)
    // ═══════════════════════════════════════════════════════
    val bdtDeducted: Double = 0.0,
    val myrDeducted: Double = 0.0,
    val deductionStrategy: String? = null
)