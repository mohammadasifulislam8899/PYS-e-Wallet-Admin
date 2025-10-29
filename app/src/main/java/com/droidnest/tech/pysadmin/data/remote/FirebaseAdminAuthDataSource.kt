// admin_app/data/remote/FirebaseAdminAuthDataSource.kt
package com.droidnest.tech.pysadmin.data.remote

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.domain.models.AdminRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
     * Check if admin is logged in and verified in Firestore
     */
    suspend fun isAdminLoggedIn(): Boolean {
        return try {
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Log.d(TAG, "❌ No user logged in")
                return false
            }

            Log.d(TAG, "🔍 User found: $userId, checking admin document...")

            // Check Firestore admin document
            val adminDoc = adminsCollection.document(userId).get().await()
            val exists = adminDoc.exists()

            if (exists) {
                val admin = adminDoc.toObject(Admin::class.java)
                val isActive = admin?.isActive ?: false

                if (!isActive) {
                    Log.w(TAG, "⚠️ Admin account is deactivated")
                    auth.signOut()
                    return false
                }

                Log.d(TAG, "✅ Admin verified: ${admin?.name}")
                true
            } else {
                Log.e(TAG, "❌ Admin document not found! Logging out...")
                auth.signOut()
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking admin status", e)
            auth.signOut()
            false
        }
    }

    /**
     * Get current admin with real-time updates
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
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔐 Login attempt: $email")

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("No user ID")

            Log.d(TAG, "✅ Auth login successful: $userId")

            val adminDoc = adminsCollection.document(userId).get().await()

            if (!adminDoc.exists()) {
                Log.e(TAG, "❌ Admin document not found!")
                auth.signOut()
                throw Exception("Access denied. You are not an admin.")
            }

            val admin = adminDoc.toObject(Admin::class.java)
                ?: throw Exception("Failed to parse admin data")

            if (!admin.isActive) {
                Log.w(TAG, "⚠️ Admin account is deactivated")
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
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(admin)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Login failed: ${e.message}", e)
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            val errorMessage = when {
                e.message?.contains("password", ignoreCase = true) == true -> "Invalid credentials"
                e.message?.contains("network", ignoreCase = true) == true -> "Network error"
                e.message?.contains("user", ignoreCase = true) == true -> "No account found with this email"
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
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🚪 Logging out")

            val userId = auth.currentUser?.uid

            if (userId != null) {
                try {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date())

                    adminsCollection.document(userId).update(
                        "lastActive", timestamp
                    ).await()
                    Log.d(TAG, "✅ Last active timestamp updated")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Could not update last active", e)
                }
            }

            auth.signOut()
            Log.d(TAG, "✅ Logout successful")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Logout failed", e)
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
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
            Log.d(TAG, "📧 Sending password reset email to: $email")
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "✅ Reset email sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send reset email", e)
            Result.failure(e)
        }
    }

    /**
     * Create admin (Signup)
     */
    suspend fun createAdmin(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: AdminRole = AdminRole.SUPER_ADMIN
    ): Result<Admin> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📝 Creating admin account")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Name: $name")
            Log.d(TAG, "Phone: $phone")

            // 1. Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

            Log.d(TAG, "✅ Auth account created: $userId")

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            // 2. Create Admin object
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
                lastLoginAt = timestamp
            )

            // 3. Save to Firestore
            adminsCollection.document(userId).set(admin).await()

            Log.d(TAG, "✅ Admin document created in Firestore")
            Log.d(TAG, "📄 Admin data: $admin")
            Log.d(TAG, "✅ Admin creation successful! User is now logged in.")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // DON'T LOGOUT - Keep user signed in!
            Result.success(admin)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create admin: ${e.message}", e)
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Cleanup on failure
            try {
                auth.currentUser?.delete()?.await()
                Log.d(TAG, "🧹 Cleaned up failed auth account")
            } catch (deleteError: Exception) {
                Log.e(TAG, "⚠️ Cleanup failed", deleteError)
            }

            val errorMessage = when {
                e.message?.contains("email", ignoreCase = true) == true -> "Invalid email format"
                e.message?.contains("password", ignoreCase = true) == true -> "Password too weak (min 6 characters)"
                e.message?.contains("already", ignoreCase = true) == true -> "Email already in use"
                e.message?.contains("network", ignoreCase = true) == true -> "Network error"
                else -> e.message ?: "Failed to create admin"
            }

            Result.failure(Exception(errorMessage))
        }
    }
}