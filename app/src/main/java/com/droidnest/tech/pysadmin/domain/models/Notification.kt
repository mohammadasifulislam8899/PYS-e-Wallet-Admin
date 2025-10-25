// domain/model/Notification.kt
package com.droidnest.tech.pysadmin.domain.models

data class Notification(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val date: Long = 0L, // Timestamp in milliseconds
    val isRead: Boolean = false,
    val type: String = "info" // "success", "warning", "error", "info"
)