// domain/models/User.kt
package com.droidnest.tech.pysadmin.domain.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val country: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val profilePictureUrl: String = "",

    // Referral
    val refCode: String = "",
    val referredBy: String = "",
    val referralAddedAt: Long = 0L,  // ✅ NEW - timestamp in milliseconds

    // Balance
    val balance: Map<String, Double> = mapOf("BDT" to 0.0, "MYR" to 0.0),
    val totalBalanceBDT: Double = 0.0,

    // KYC
    val kycStatus: KycStatus = KycStatus.UNVERIFIED,
    val kycRequestId: String? = null,
    val kycVerifiedAt: String? = null,
    val kycRejectionReason: String? = null,

    // Profile
    val profileCompleted: Boolean = false,
    val createdAt: String = "",

    // Referral Lists
    val friends: List<String> = emptyList(),
    val activeFriends: List<String> = emptyList(),
    val referralCount: Int = 0,
    val referralEarnings: Double = 0.0,

    // Deposit Tracking
    val totalDepositedMYR: Double = 0.0,
    val totalDepositedAfterReferralMYR: Double = 0.0,  // ✅ NEW
    val firstDepositCashbackGiven: Boolean = false,
    val firstDepositCompleted: Boolean = false,

    // Security & Status
    val accountLock: AccountLockStatus? = null,
    val lastActive: String = "",
    val pin: String = "",
    val isAdmin: Boolean = false,
) {
    // ✅ Helper properties
    val kycVerified: Boolean
        get() = kycStatus == KycStatus.VERIFIED

    val totalReferrals: Int
        get() = friends.size

    val activeReferrals: Int
        get() = activeFriends.size

    // ✅ Check if referral code was added
    val hasReferralCode: Boolean
        get() = referredBy.isNotBlank()

    // ✅ Get referral added date (human readable)
    fun getReferralAddedDate(): String {
        if (referralAddedAt == 0L) return "N/A"

        return try {
            val dateFormat = java.text.SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                java.util.Locale.getDefault()
            )
            dateFormat.format(java.util.Date(referralAddedAt))
        } catch (e: Exception) {
            "N/A"
        }
    }
}

// ✅ AccountLockStatus data class
data class AccountLockStatus(
    val isLocked: Boolean = false,
    val unlockTime: Long = 0L,
    val reason: String = "",
    val lockedAt: Long = 0L
) {
    fun getRemainingTime(): String {
        if (!isLocked || unlockTime <= System.currentTimeMillis()) return ""

        val remaining = unlockTime - System.currentTimeMillis()
        val minutes = (remaining / 1000 / 60).toInt()
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days days"
            hours > 0 -> "$hours hours"
            minutes > 0 -> "$minutes minutes"
            else -> "Less than a minute"
        }
    }
}
