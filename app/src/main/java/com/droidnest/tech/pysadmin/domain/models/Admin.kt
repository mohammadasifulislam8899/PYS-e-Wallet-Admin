// admin_app/domain/model/Admin.kt
package com.droidnest.tech.pysadmin.domain.models

data class Admin(
    val adminId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: AdminRole = AdminRole.ADMIN,
    val permissions: List<String> = emptyList(),
    val profilePictureUrl: String = "",
    val createdAt: String = "",
    val lastActive: String = "",
    val isActive: Boolean = true,
    val loginAttempts: Int = 0,
    val lastLoginAt: String = ""
)

enum class AdminRole {
    SUPER_ADMIN,  // Full access to everything
    ADMIN,        // Standard admin access
    MODERATOR     // Limited access (view only, basic approvals)
}