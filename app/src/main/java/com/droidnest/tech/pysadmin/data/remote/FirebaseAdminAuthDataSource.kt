// admin_app/data/remote/FirebaseAdminAuthDataSource.kt
package com.droidnest.tech.pysadmin.data.remote

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.domain.models.AdminRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAdminAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val adminsCollection = firestore.collection("admins")

    companion object {
        private const val TAG = "FirebaseAdminAuth"
    }

    /**
     * Get current admin with real-time updates
     * ✅ COMPLETELY REWRITTEN FOR STABILITY
     */
    fun getCurrentAdmin(): Flow<Admin?> = callbackFlow {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.d(TAG, "⚠️ No user logged in - sending null")
            trySend(null)
            awaitClose {
                Log.d(TAG, "🔇 Flow closed (no user)")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        } else {
            Log.d(TAG, "👂 Starting listener for admin: $userId")

            val listener = adminsCollection.document(userId)
                .addSnapshotListener { snapshot, error ->
                    when {
                        error != null -> {
                            Log.e(TAG, "❌ Listener error", error)
                            trySend(null)
                        }
                        snapshot?.exists() == true -> {
                            val admin = snapshot.toObject(Admin::class.java)
                            Log.d(TAG, "🔔 Admin updated: ${admin?.name}")
                            trySend(admin)
                        }
                        else -> {
                            Log.d(TAG, "⚠️ Admin document not found")
                            trySend(null)
                        }
                    }
                }

            awaitClose {
                Log.d(TAG, "🔇 Removing listener for: $userId")
                listener.remove()
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }

    /**
     * Login admin
     */
    suspend fun login(email: String, password: String): Result<Admin> {
        return try {
            Log.d(TAG, "🔐 Login attempt: $email")

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("No user ID")

            val adminDoc = adminsCollection.document(userId).get().await()

            if (!adminDoc.exists()) {
                auth.signOut()
                throw Exception("Access denied. You are not an admin.")
            }

            val admin = adminDoc.toObject(Admin::class.java)
                ?: throw Exception("Failed to parse admin data")

            if (!admin.isActive) {
                auth.signOut()
                throw Exception("Your admin account has been deactivated.")
            }

            // Update last login
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            adminsCollection.document(userId).update(
                mapOf(
                    "lastLoginAt" to timestamp,
                    "lastActive" to timestamp,
                    "loginAttempts" to 0
                )
            ).await()

            Log.d(TAG, "✅ Login successful: ${admin.name}")
            Result.success(admin)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Login failed", e)
            val errorMessage = when {
                e.message?.contains("password") == true -> "Invalid credentials"
                e.message?.contains("network") == true -> "Network error"
                else -> e.message ?: "Login failed"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Logout admin
     */
    suspend fun logout(): Result<Unit> {
        return try {
            Log.d(TAG, "🚪 Logging out")

            val userId = auth.currentUser?.uid

            if (userId != null) {
                try {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date())

                    adminsCollection.document(userId).update(
                        "lastActive", timestamp
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Could not update last active", e)
                }
            }

            auth.signOut()
            Log.d(TAG, "✅ Logout successful")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Logout failed", e)
            Result.failure(e)
        }
    }

    /**
     * Check if admin is logged in
     */
    fun isAdminLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Update admin activity
     */
    suspend fun updateAdminActivity(adminId: String): Result<Unit> {
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            adminsCollection.document(adminId).update("lastActive", timestamp).await()
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create admin
     */
    suspend fun createAdmin(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: AdminRole = AdminRole.ADMIN
    ): Result<Admin> {
        return try {
            Log.d(TAG, "📝 Creating admin: $email")

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            val admin = Admin(
                adminId = userId,
                name = name,
                email = email,
                phone = phone,
                role = role,
                permissions = listOf(
                    "APPROVE_TRANSACTIONS",
                    "REJECT_TRANSACTIONS",
                    "APPROVE_KYC",
                    "REJECT_KYC",
                    "MANAGE_USERS",
                    "BLOCK_USERS",
                    "UPDATE_RATES",
                    "VIEW_REPORTS",
                    "MANAGE_SETTINGS",
                    "SEND_NOTIFICATIONS"
                ),
                profilePictureUrl = "",
                createdAt = timestamp,
                lastActive = timestamp,
                isActive = true,
                loginAttempts = 0,
                lastLoginAt = ""
            )

            adminsCollection.document(userId).set(admin).await()
            auth.signOut()

            Log.d(TAG, "✅ Admin created successfully")
            Result.success(admin)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create admin", e)

            try {
                auth.currentUser?.delete()?.await()
            } catch (deleteError: Exception) {
                Log.e(TAG, "Cleanup failed", deleteError)
            }

            Result.failure(e)
        }
    }
}