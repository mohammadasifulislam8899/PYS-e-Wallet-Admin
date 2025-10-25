// domain/model/BankAccount.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class BankAccount(
    val accountId: String = "",
    val userId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val branchName: String = "",
    val routingNumber: String? = null,
    val accountType: String = "savings", // savings, current
    val isDefault: Boolean = false,
    val isVerified: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)