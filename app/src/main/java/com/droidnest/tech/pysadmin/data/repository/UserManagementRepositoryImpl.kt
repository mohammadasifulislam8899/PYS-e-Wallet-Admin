package com.droidnest.tech.pysadmin.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.droidnest.tech.pysadmin.data.model.UserDto
import com.droidnest.tech.pysadmin.data.model.toDomain
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User
import com.droidnest.tech.pysadmin.domain.repository.UserManagementRepository
import com.droidnest.tech.pysadmin.utils.Constants
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserManagementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserManagementRepository {

    // ✅ Lock User Account
    override suspend fun lockUserAccount(
        userId: String,
        duration: Long,
        reason: String
    ): Resource<String> {
        return try {
            val unlockTime = System.currentTimeMillis() + (duration * 60 * 1000)

            val lockData = mapOf(
                "isLocked" to true,
                "unlockTime" to unlockTime,
                "reason" to reason,
                "lockedAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .update("accountLock", lockData)
                .await()

            Resource.Success("Account locked for $duration minutes")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to lock account")
        }
    }

    // ✅ UPDATED: Unlock User Account + Clear PIN Attempts
    override suspend fun unlockUserAccount(userId: String): Resource<String> {
        return try {
            // Step 1: Clear account lock
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "accountLock" to mapOf(
                            "isLocked" to false,
                            "unlockTime" to 0,
                            "reason" to "",
                            "lockedAt" to 0
                        )
                    )
                )
                .await()

            // Step 2: Clear PIN attempts (delete document)
            try {
                firestore.collection("pin_attempts")
                    .document(userId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // If pin_attempts document doesn't exist, ignore the error
                // but still return success for unlock
            }

            Resource.Success("Account unlocked and PIN attempts reset successfully")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unlock account")
        }
    }

    // ✅ Reset User PIN
    override suspend fun resetUserPin(userId: String, newPin: String): Resource<String> {
        return try {
            val encryptedPin = encryptPin(newPin)

            // Update PIN
            firestore.collection("users")
                .document(userId)
                .update("pin", encryptedPin)
                .await()

            // ✅ Also clear PIN attempts when resetting PIN
            try {
                firestore.collection("pin_attempts")
                    .document(userId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Ignore if doesn't exist
            }

            Resource.Success("PIN reset successfully")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to reset PIN")
        }
    }

    private fun encryptPin(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ✅ Update KYC Status
    override suspend fun updateKycStatus(
        userId: String,
        kycStatus: KycStatus,
        rejectionReason: String?
    ): Resource<String> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "kycStatus" to kycStatus.name
            )

            when (kycStatus) {
                KycStatus.VERIFIED -> {
                    updates["kycVerifiedAt"] = java.text.SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                    updates["kycRejectionReason"] =
                        com.google.firebase.firestore.FieldValue.delete()
                    updates["kycVerified"] = true
                }

                KycStatus.REJECTED -> {
                    rejectionReason?.let {
                        updates["kycRejectionReason"] = it
                    }
                    updates["kycVerifiedAt"] = com.google.firebase.firestore.FieldValue.delete()
                    updates["kycVerified"] = false
                }

                KycStatus.PENDING -> {
                    if (updates["kycRequestId"] == null) {
                        updates["kycRequestId"] = "KYC_${System.currentTimeMillis()}"
                    }
                    updates["kycVerified"] = false
                }

                KycStatus.UNVERIFIED -> {
                    updates["kycRequestId"] = com.google.firebase.firestore.FieldValue.delete()
                    updates["kycVerifiedAt"] = com.google.firebase.firestore.FieldValue.delete()
                    updates["kycRejectionReason"] =
                        com.google.firebase.firestore.FieldValue.delete()
                    updates["kycVerified"] = false
                }
            }

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Resource.Success("KYC status updated to ${kycStatus.name}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update KYC status")
        }
    }

    // ✅ Get All Users (Realtime)
    override suspend fun getAllUsers(): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())

        val listener = firestore.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load users"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserDto::class.java)?.copy(userId = doc.id)?.toDomain()
                    }
                    trySend(Resource.Success(users))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }

        awaitClose { listener.remove() }
    }

    // ✅ Get User By ID
    override suspend fun getUserById(userId: String): Resource<User> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user =
                snapshot.toObject(UserDto::class.java)?.copy(userId = snapshot.id)?.toDomain()
            if (user != null) {
                Resource.Success(user)
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load user")
        }
    }

    // ✅ Update User Balance
    override suspend fun updateUserBalance(
        userId: String,
        currency: String,
        newBalance: Double
    ): Resource<String> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userDto = userDoc.toObject(UserDto::class.java)
                ?: return Resource.Error("User not found")
            val user = userDto.toDomain()

            val updatedBalance = user.balance.toMutableMap()
            updatedBalance[currency] = newBalance

            val exchangeRate = getExchangeRate()
            val newTotalBDT = (updatedBalance["BDT"] ?: 0.0) +
                    ((updatedBalance["MYR"] ?: 0.0) * exchangeRate)

            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "balance" to updatedBalance,
                        "totalBalanceBDT" to newTotalBDT
                    )
                )
                .await()

            Resource.Success("Balance updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update balance")
        }
    }

    // ✅ Search Users
    override suspend fun searchUsers(query: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading())

        try {
            val snapshot = firestore.collection("users")
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserDto::class.java)?.copy(userId = doc.id)?.toDomain()
            }.filter { user ->
                user.name.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true) ||
                        user.phone.contains(query, ignoreCase = true) ||
                        user.userId.contains(query, ignoreCase = true) ||
                        user.refCode.contains(query, ignoreCase = true)
            }

            trySend(Resource.Success(users))
        } catch (e: Exception) {
            trySend(Resource.Error(e.message ?: "Search failed"))
        }

        awaitClose { }
    }

    // ✅ Get Exchange Rate
    private suspend fun getExchangeRate(): Double {
        return try {
            Log.d("EXCHANGERATEEEEE", "📊 Fetching exchange rate from Firestore...")

            val doc = firestore.collection(Constants.COLLECTION_SETTINGS)
                .document(Constants.DOC_EXCHANGE_RATE)
                .get()
                .await()

            if (!doc.exists()) {
                Log.w("EXCHANGERATEEEEE", "⚠️ Exchange rate document not found, using default")
                return Constants.DEFAULT_EXCHANGE_RATE
            }

            // Get rates map
            val ratesMap = doc.get("rates") as? Map<*, *>

            if (ratesMap == null) {
                Log.w(TAG, "⚠️ Rates map not found, using default")
                return Constants.DEFAULT_EXCHANGE_RATE
            }

            // Get MYR rate (handle different number types)
            val myrRate = when (val rate = ratesMap["MYR"]) {
                is Double -> rate
                is Long -> rate.toDouble()
                is Int -> rate.toDouble()
                else -> {
                    Log.w(TAG, "⚠️ Invalid MYR rate type, using default")
                    Constants.DEFAULT_EXCHANGE_RATE
                }
            }

            Log.d("EXCHANGERATEEEEE", "✅ Exchange rate fetched: 1 MYR = $myrRate BDT")
            myrRate

        } catch (e: Exception) {
            Log.e("EXCHANGERATEEEEE", "❌ Error fetching exchange rate: ${e.message}", e)
            Constants.DEFAULT_EXCHANGE_RATE
        }
    }
}
