// user_app/domain/model/KycStatus.kt
package com.droidnest.tech.pysadmin.domain.models

enum class KycStatus {
    UNVERIFIED,  // User hasn't submitted KYC yet
    PENDING,     // Submitted, waiting for admin approval
    VERIFIED,    // Approved by admin
    REJECTED     // Rejected by admin
}