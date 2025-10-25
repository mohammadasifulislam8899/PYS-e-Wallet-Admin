// admin_app/presentation/navigation/AdminRoutes.kt
package com.droidnest.tech.pysadmin.presentation.navigation

import kotlinx.serialization.Serializable

// ========== SPLASH ==========
@Serializable
object SplashRoute

// ========== ONE-TIME SETUP (Development Only - Remove in Production) ==========
@Serializable
object OneTimeSetupRoute  // ⚠️ Use only for first-time admin creation, then remove
@Serializable
object AuthRoute  // ⚠️ Use only for first-time admin creation, then remove

// ========== AUTH ==========
@Serializable
object AdminLoginRoute

// ========== MAIN ROUTES ==========
@Serializable
object AdminDashboardRoute

@Serializable
object AdminTransactionsRoute

@Serializable
object AdminKycRoute

@Serializable
object AdminUsersRoute

@Serializable
object AdminRateManagementRoute

@Serializable
object AdminSettingsRoute

// ========== DETAIL ROUTES ==========
@Serializable
data class UserDetailsRoute(val userId: String)

@Serializable
data class TransactionDetailsRoute(val transactionId: String)

@Serializable
data class KycDetailsRoute(val kycRequestId: String)

// ========== REPORTS ==========
@Serializable
object ReportsRoute

@Serializable
data class AddEditAddMoneyPaymentMethodScreenRoute(
    val id : String? = null
)
@Serializable
object AddAddMoneyPaymentMethodScreenRoute

@Serializable
data class ReportDetailsRoute(
    val reportType: String,
    val date: String
)

// ========== NOTIFICATIONS ==========
@Serializable
object NotificationsRoute

@Serializable
object AddPaymentMethodRoute

@Serializable
data class EditPaymentMethodRoute(val methodId: String)

@Serializable
object SendNotificationRoute

// ========== PAYMENT METHODS ==========
@Serializable
object AddMoneyPaymentMethodsRoute

// ==========WITHDRAW PAYMENT METHODS ==========
@Serializable
object WithdrawPaymentMethodsRoute

// ========== ADMIN PROFILE ==========
@Serializable
object AdminProfileRoute

@Serializable
object ChangePasswordRoute

// ========== LOGS & ACTIVITY ==========
@Serializable
object ActivityLogsRoute

@Serializable
object AdminActivityRoute