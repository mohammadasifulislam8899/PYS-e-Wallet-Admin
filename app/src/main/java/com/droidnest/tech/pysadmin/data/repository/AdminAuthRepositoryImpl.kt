// admin_app/data/repository/AdminAuthRepositoryImpl.kt
package com.droidnest.tech.pysadmin.data.repository

import android.util.Log
import com.droidnest.tech.pysadmin.data.remote.FirebaseAdminAuthDataSource
import com.droidnest.tech.pysadmin.domain.repository.AdminAuthRepository
import com.droidnest.tech.pysadmin.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.domain.models.AdminRole

@Singleton
class AdminAuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseAdminAuthDataSource
) : AdminAuthRepository {

    companion object {
        private const val TAG = "AdminAuthRepositoryImpl"
    }

    override suspend fun createAdmin(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Flow<Resource<Admin>> = flow {
        Log.d(TAG, "📝 Repository: Create admin requested")

        emit(Resource.Loading())

        val result = dataSource.createAdmin(
            name = name,
            email = email,
            phone = phone,
            password = password,
            role = AdminRole.SUPER_ADMIN
        )

        if (result.isSuccess) {
            val admin = result.getOrNull()!!
            Log.d(TAG, "✅ Repository: Admin created successfully")
            emit(Resource.Success(admin))
        } else {
            val error = result.exceptionOrNull()?.message ?: "Failed to create admin"
            Log.e(TAG, "❌ Repository: Admin creation failed - $error")
            emit(Resource.Error(error))
        }
    }

    override suspend fun login(email: String, password: String): Flow<Resource<Admin>> = flow {
        Log.d(TAG, "🔐 Repository: Login requested")

        emit(Resource.Loading())

        val result = dataSource.login(email, password)

        if (result.isSuccess) {
            val admin = result.getOrNull()!!
            Log.d(TAG, "✅ Repository: Login successful")
            emit(Resource.Success(admin))
        } else {
            val error = result.exceptionOrNull()?.message ?: "Login failed"
            Log.e(TAG, "❌ Repository: Login failed - $error")
            emit(Resource.Error(error))
        }
    }

    override suspend fun logout(): Resource<Unit> {
        Log.d(TAG, "🚪 Repository: Logout requested")

        return dataSource.logout().fold(
            onSuccess = {
                Log.d(TAG, "✅ Repository: Logout successful")
                Resource.Success(Unit)
            },
            onFailure = {
                Log.e(TAG, "❌ Repository: Logout failed - ${it.message}")
                Resource.Error(it.message ?: "Logout failed")
            }
        )
    }

    // ✅ FIXED: Proper error handling in the flow chain
    override suspend fun getCurrentAdmin(): Flow<Resource<Admin?>> {
        Log.d(TAG, "👤 Repository: Getting current admin")

        return dataSource.getCurrentAdmin()
            .map<Admin?, Resource<Admin?>> { admin ->
                if (admin != null) {
                    Log.d(TAG, "✅ Repository: Admin found - ${admin.name}")
                    Resource.Success(admin)
                } else {
                    Log.d(TAG, "⚠️ Repository: No admin found")
                    Resource.Success(null)
                }
            }
            .catch { error ->
                Log.e(TAG, "❌ Repository: Error getting admin - ${error.message}", error)
                emit(Resource.Error(error.message ?: "Failed to get admin"))
            }
    }

    override fun isAdminLoggedIn(): Boolean {
        val isLoggedIn = dataSource.isAdminLoggedIn()
        Log.d(TAG, "🔍 Repository: Is admin logged in - $isLoggedIn")
        return isLoggedIn
    }

    override suspend fun updateAdminActivity(adminId: String): Resource<Unit> {
        Log.d(TAG, "📊 Repository: Updating activity for admin: $adminId")

        return dataSource.updateAdminActivity(adminId).fold(
            onSuccess = {
                Log.d(TAG, "✅ Repository: Activity updated")
                Resource.Success(Unit)
            },
            onFailure = {
                Log.e(TAG, "❌ Repository: Failed to update activity - ${it.message}")
                Resource.Error(it.message ?: "Failed to update activity")
            }
        )
    }

    override suspend fun resetPassword(email: String): Resource<Unit> {
        Log.d(TAG, "📧 Repository: Reset password for: $email")

        return dataSource.resetPassword(email).fold(
            onSuccess = {
                Log.d(TAG, "✅ Repository: Reset email sent")
                Resource.Success(Unit)
            },
            onFailure = {
                Log.e(TAG, "❌ Repository: Failed to send reset email - ${it.message}")
                Resource.Error(it.message ?: "Failed to send reset email")
            }
        )
    }
}