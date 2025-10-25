// data/model/UserDto.kt
package com.droidnest.tech.pysadmin.data.model

import com.droidnest.tech.pysadmin.domain.models.AccountLockStatus
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.*

data class UserDto(
    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("name")
    val name: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("phone")
    val phone: String = "",

    @PropertyName("country")
    val country: String = "",

    @PropertyName("address")
    val address: String = "",

    @PropertyName("dateOfBirth")
    val dateOfBirth: String = "",

    @PropertyName("profilePictureUrl")
    val profilePictureUrl: String = "",

    @PropertyName("refCode")
    val refCode: String = "",

    @PropertyName("referredBy")
    val referredBy: String = "",

    // ✅ NEW: Referral tracking
    @PropertyName("referralAddedAt")
    val referralAddedAt: Long = 0L,

    @PropertyName("balance")
    val balance: Map<String, Double> = mapOf("BDT" to 0.0, "MYR" to 0.0),

    @PropertyName("totalBalanceBDT")
    val totalBalanceBDT: Double = 0.0,

    // ═══════════════════════════════════════════════════════
    // KYC FIELDS
    // ═══════════════════════════════════════════════════════
    @PropertyName("kycStatus")
    val kycStatus: String = KycStatus.UNVERIFIED.name,

    @PropertyName("kycRequestId")
    val kycRequestId: String? = null,

    @PropertyName("kycVerifiedAt")
    val kycVerifiedAt: Any? = null,

    @PropertyName("kycRejectionReason")
    val kycRejectionReason: String? = null,

    @PropertyName("profileCompleted")
    val profileCompleted: Boolean = false,

    @PropertyName("createdAt")
    val createdAt: Any? = null,

    // ═══════════════════════════════════════════════════════
    // REFERRAL FIELDS
    // ═══════════════════════════════════════════════════════
    @PropertyName("friends")
    val friends: List<String> = emptyList(),

    @PropertyName("activeFriends")
    val activeFriends: List<String> = emptyList(),

    @PropertyName("referralCount")
    val referralCount: Int = 0,

    @PropertyName("referralEarnings")
    val referralEarnings: Double = 0.0,

    // ═══════════════════════════════════════════════════════
    // DEPOSIT TRACKING
    // ═══════════════════════════════════════════════════════
    @PropertyName("totalDepositedMYR")
    val totalDepositedMYR: Double = 0.0,

    // ✅ NEW: Track deposits after referral code added
    @PropertyName("totalDepositedAfterReferralMYR")
    val totalDepositedAfterReferralMYR: Double = 0.0,

    @PropertyName("firstDepositCashbackGiven")
    val firstDepositCashbackGiven: Boolean = false,

    @PropertyName("firstDepositCompleted")
    val firstDepositCompleted: Boolean = false,

    // ═══════════════════════════════════════════════════════
    // SECURITY & STATUS
    // ═══════════════════════════════════════════════════════
    @PropertyName("isBlocked")
    val isBlocked: Boolean = false, // ✅ Keep for backward compatibility

    // ✅ Account Lock Fields
    @PropertyName("accountLock")
    val accountLock: Map<String, Any>? = null,

    @PropertyName("lastActive")
    val lastActive: Any? = null,

    @PropertyName("pin")
    val pin: String = "",

    @PropertyName("isPinSet")
    val isPinSet: Boolean = false,

    @PropertyName("isAdmin")
    val isAdmin: Boolean = false,
)

// ═══════════════════════════════════════════════════════
// ✅ UPDATED MAPPERS
// ═══════════════════════════════════════════════════════

/**
 * Convert DTO to Domain Model (From Firestore → App)
 */
fun UserDto.toDomain(): User {
    return User(
        userId = userId,
        name = name,
        email = email,
        phone = phone,
        country = country,
        address = address,
        dateOfBirth = dateOfBirth,
        profilePictureUrl = profilePictureUrl,
        refCode = refCode,
        referredBy = referredBy,
        referralAddedAt = referralAddedAt, // ✅ NEW
        balance = balance,
        totalBalanceBDT = totalBalanceBDT,
        kycStatus = try {
            KycStatus.valueOf(kycStatus)
        } catch (e: Exception) {
            KycStatus.UNVERIFIED
        },
        kycRequestId = kycRequestId,
        kycVerifiedAt = kycVerifiedAt.toFormattedString(),
        kycRejectionReason = kycRejectionReason,
        profileCompleted = profileCompleted,
        createdAt = createdAt.toFormattedString(),
        friends = friends,
        activeFriends = activeFriends,
        referralCount = referralCount,
        referralEarnings = referralEarnings,
        totalDepositedMYR = totalDepositedMYR,
        totalDepositedAfterReferralMYR = totalDepositedAfterReferralMYR, // ✅ NEW
        firstDepositCashbackGiven = firstDepositCashbackGiven,
        firstDepositCompleted = firstDepositCompleted,
        accountLock = accountLock.toAccountLockStatus(), // ✅ Convert Map to AccountLockStatus
        lastActive = lastActive.toFormattedString(),
        pin = pin,
        isAdmin = isAdmin
    )
}

/**
 * Convert Domain Model to DTO (From App → Firestore)
 */
fun User.toDto(): UserDto {
    return UserDto(
        userId = userId,
        name = name,
        email = email,
        phone = phone,
        country = country,
        address = address,
        dateOfBirth = dateOfBirth,
        profilePictureUrl = profilePictureUrl,
        refCode = refCode,
        referredBy = referredBy,
        referralAddedAt = referralAddedAt, // ✅ NEW
        balance = balance,
        totalBalanceBDT = totalBalanceBDT,
        kycStatus = kycStatus.name,
        kycRequestId = kycRequestId,
        kycVerifiedAt = kycVerifiedAt.toTimestampOrNull(),
        kycRejectionReason = kycRejectionReason,
        profileCompleted = profileCompleted,
        createdAt = createdAt.toTimestampOrNull(),
        friends = friends,
        activeFriends = activeFriends,
        referralCount = referralCount,
        referralEarnings = referralEarnings,
        totalDepositedMYR = totalDepositedMYR,
        totalDepositedAfterReferralMYR = totalDepositedAfterReferralMYR, // ✅ NEW
        firstDepositCashbackGiven = firstDepositCashbackGiven,
        firstDepositCompleted = firstDepositCompleted,
        isBlocked = accountLock?.isLocked ?: false, // ✅ For backward compatibility
        accountLock = accountLock.toAccountLockMap(), // ✅ Convert AccountLockStatus to Map
        lastActive = lastActive.toTimestampOrNull(),
        pin = pin,
        isPinSet = pin.isNotEmpty(),
        isAdmin = isAdmin
    )
}

// ═══════════════════════════════════════════════════════
// ✅ FLEXIBLE EXTENSION FUNCTIONS
// ═══════════════════════════════════════════════════════

/**
 * Convert Any? (String or Timestamp) to formatted String
 */
private fun Any?.toFormattedString(): String {
    return when (this) {
        is Timestamp -> {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.format(this.toDate())
            } catch (e: Exception) {
                ""
            }
        }
        is String -> this
        else -> ""
    }
}

/**
 * Convert String to Timestamp (for saving to Firestore)
 */
private fun String?.toTimestampOrNull(): Timestamp? {
    if (this.isNullOrEmpty()) return null

    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(this)
        if (date != null) Timestamp(date) else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert Map to AccountLockStatus (From Firestore → Domain)
 */
private fun Map<String, Any>?.toAccountLockStatus(): AccountLockStatus? {
    if (this == null) return null

    return try {
        AccountLockStatus(
            isLocked = this["isLocked"] as? Boolean ?: false,
            unlockTime = (this["unlockTime"] as? Number)?.toLong() ?: 0L,
            reason = this["reason"] as? String ?: "",
            lockedAt = (this["lockedAt"] as? Number)?.toLong() ?: 0L
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert AccountLockStatus to Map (From Domain → Firestore)
 */
private fun AccountLockStatus?.toAccountLockMap(): Map<String, Any>? {
    if (this == null) return null

    return mapOf(
        "isLocked" to isLocked,
        "unlockTime" to unlockTime,
        "reason" to reason,
        "lockedAt" to lockedAt
    )
}