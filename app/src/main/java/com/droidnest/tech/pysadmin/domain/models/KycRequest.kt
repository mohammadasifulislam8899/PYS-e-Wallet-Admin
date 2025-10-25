// user_app/domain/model/KycRequest.kt
package com.droidnest.tech.pysadmin.domain.models

data class KycRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val passportImageUrl: String = "",
    val passportNumber: String = "",
    val nationality: String = "",
    val status: KycStatus = KycStatus.PENDING,
    val submittedAt: String = "",
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,  // Admin userId who reviewed
    val rejectionReason: String? = null
)