package com.droidnest.tech.pysadmin.data.repository

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.DashboardStats
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.repository.DashboardRepository
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DashboardRepository {

    companion object {
        private const val TAG = "DashboardRepo"
    }

    // ════════════════════════════════════════════════════════════
    // GET DASHBOARD STATS (REALTIME LISTENER)
    // ════════════════════════════════════════════════════════════

    override fun getDashboardStats(): Flow<Resource<DashboardStats>> = callbackFlow {
        Log.d(TAG, "🔄 Starting dashboard stats listener")

        trySend(Resource.Loading())

        val listener: ListenerRegistration = firestore
            .collection("dashboard_stats")
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error in stats listener", error)
                    trySend(Resource.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val stats = snapshot.toObject(DashboardStats::class.java)
                        ?: DashboardStats()
                    Log.d(TAG, "✅ Stats updated from cache")
                    trySend(Resource.Success(stats))
                } else {
                    Log.d(TAG, "⚠️ No cached stats found")
                    trySend(Resource.Success(DashboardStats()))
                }
            }

        awaitClose {
            Log.d(TAG, "🔇 Removing dashboard stats listener")
            listener.remove()
        }
    }

    // ════════════════════════════════════════════════════════════
    // REFRESH DASHBOARD STATS
    // ════════════════════════════════════════════════════════════

    override suspend fun refreshDashboardStats() {
        Log.d(TAG, "🔄 Refreshing dashboard stats")

        try {
            // Get revenues
            val todayGross = getTodayRevenue()
            val weeklyGross = getWeeklyRevenue()
            val monthlyGross = getMonthlyRevenue()

            // Get expenses
            val todayExp = getTodayExpenses()
            val weeklyExp = getWeeklyExpenses()
            val monthlyExp = getMonthlyExpenses()

            val stats = DashboardStats(
                totalUsers = getTotalUsers(),
                activeUsers = getActiveUsers(),
                blockedUsers = getBlockedUsers(),

                pendingKyc = getPendingKyc(),
                verifiedKyc = getVerifiedKyc(),
                rejectedKyc = getRejectedKyc(),

                pendingTransactions = getPendingTransactions(),
                successTransactions = getSuccessTransactions(),
                failedTransactions = getFailedTransactions(),

                // ✅ Gross Revenue
                todayGrossRevenue = todayGross,
                weeklyGrossRevenue = weeklyGross,
                monthlyGrossRevenue = monthlyGross,

                // ✅ Expenses
                todayExpenses = todayExp,
                weeklyExpenses = weeklyExp,
                monthlyExpenses = monthlyExp,

                // ✅ Net Revenue
                todayNetRevenue = todayGross - todayExp,
                weeklyNetRevenue = weeklyGross - weeklyExp,
                monthlyNetRevenue = monthlyGross - monthlyExp,

                totalBalanceInSystem = getTotalBalanceInSystem(),
                unreadNotifications = getUnreadNotifications()
            )

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💰 Revenue & Expense Summary:")
            Log.d(TAG, "  TODAY:")
            Log.d(TAG, "    Gross Revenue: ৳${stats.todayGrossRevenue}")
            Log.d(TAG, "    Expenses: ৳${stats.todayExpenses}")
            Log.d(TAG, "    Net Revenue: ৳${stats.todayNetRevenue}")
            Log.d(TAG, "  MONTHLY:")
            Log.d(TAG, "    Gross Revenue: ৳${stats.monthlyGrossRevenue}")
            Log.d(TAG, "    Expenses: ৳${stats.monthlyExpenses}")
            Log.d(TAG, "    Net Revenue: ৳${stats.monthlyNetRevenue}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            firestore.collection("dashboard_stats")
                .document("current")
                .set(stats)
                .await()

            Log.d(TAG, "✅ Dashboard stats refreshed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing stats", e)
            throw e
        }
    }

    // ════════════════════════════════════════════════════════════
    // USER STATS
    // ════════════════════════════════════════════════════════════

    private suspend fun getTotalUsers(): Int {
        return try {
            val count = firestore.collection("users")
                .get()
                .await()
                .size()
            Log.d(TAG, "👥 Total users: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting total users", e)
            0
        }
    }

    private suspend fun getActiveUsers(): Int {
        return try {
            val count = firestore.collection("users")
                .whereEqualTo("isBlocked", false)
                .get()
                .await()
                .size()
            Log.d(TAG, "✅ Active users: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active users", e)
            0
        }
    }

    private suspend fun getBlockedUsers(): Int {
        return try {
            val count = firestore.collection("users")
                .whereEqualTo("isBlocked", true)
                .get()
                .await()
                .size()
            Log.d(TAG, "🚫 Blocked users: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting blocked users", e)
            0
        }
    }

    // ════════════════════════════════════════════════════════════
    // KYC STATS
    // ════════════════════════════════════════════════════════════

    private suspend fun getPendingKyc(): Int {
        return try {
            firestore.collection("kyc_requests")
                .whereEqualTo("status", KycStatus.PENDING.name)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting pending KYC", e)
            0
        }
    }

    private suspend fun getVerifiedKyc(): Int {
        return try {
            firestore.collection("kyc_requests")
                .whereEqualTo("status", KycStatus.VERIFIED.name)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getRejectedKyc(): Int {
        return try {
            firestore.collection("kyc_requests")
                .whereEqualTo("status", KycStatus.REJECTED.name)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    // ════════════════════════════════════════════════════════════
    // TRANSACTION STATS
    // ════════════════════════════════════════════════════════════

    private suspend fun getPendingTransactions(): Int {
        return try {
            firestore.collection("transactions")
                .whereEqualTo("status", "pending")
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getSuccessTransactions(): Int {
        return try {
            firestore.collection("transactions")
                .whereEqualTo("status", "success")
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getFailedTransactions(): Int {
        return try {
            firestore.collection("transactions")
                .whereEqualTo("status", "failed")
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ REVENUE QUERIES FROM REVENUE COLLECTION ✅✅✅
    // ════════════════════════════════════════════════════════════

    private suspend fun getTodayRevenue(): Double {
        return try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(calendar.time)

            Log.d(TAG, "📅 Fetching today's revenue for date: $today")

            val revenues = firestore.collection("revenue")
                .whereEqualTo("day", today)
                .get()
                .await()

            val total = revenues.documents.sumOf { doc ->
                doc.getDouble("totalFeeBDT") ?: 0.0
            }

            Log.d(TAG, "💰 Today's gross revenue: ৳$total from ${revenues.size()} transactions")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching today's revenue", e)
            0.0
        }
    }

    private suspend fun getWeeklyRevenue(): Double {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = Timestamp(calendar.time)

            val revenues = firestore.collection("revenue")
                .whereGreaterThanOrEqualTo("createdAt", weekAgo)
                .get()
                .await()

            val total = revenues.documents.sumOf {
                it.getDouble("totalFeeBDT") ?: 0.0
            }

            Log.d(TAG, "💰 Weekly gross revenue: ৳$total from ${revenues.size()} transactions")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching weekly revenue", e)
            0.0
        }
    }

    private suspend fun getMonthlyRevenue(): Double {
        return try {
            val calendar = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = monthFormat.format(calendar.time)

            val revenues = firestore.collection("revenue")
                .whereEqualTo("month", currentMonth)
                .get()
                .await()

            val total = revenues.documents.sumOf {
                it.getDouble("totalFeeBDT") ?: 0.0
            }

            Log.d(TAG, "💰 Monthly gross revenue: ৳$total from ${revenues.size()} transactions")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching monthly revenue", e)
            0.0
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ EXPENSE QUERIES FROM EXPENSES COLLECTION ✅✅✅
    // ════════════════════════════════════════════════════════════

    private suspend fun getTodayExpenses(): Double {
        return try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(calendar.time)

            Log.d(TAG, "📅 Fetching today's expenses for date: $today")

            val expenses = firestore.collection("expenses")
                .whereEqualTo("day", today)
                .get()
                .await()

            val total = expenses.documents.sumOf {
                it.getDouble("totalAmountBDT") ?: 0.0
            }

            Log.d(TAG, "💸 Today's expenses: ৳$total from ${expenses.size()} items")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting today's expenses", e)
            0.0
        }
    }

    private suspend fun getWeeklyExpenses(): Double {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = Timestamp(calendar.time)

            val expenses = firestore.collection("expenses")
                .whereGreaterThanOrEqualTo("createdAt", weekAgo)
                .get()
                .await()

            val total = expenses.documents.sumOf {
                it.getDouble("totalAmountBDT") ?: 0.0
            }

            Log.d(TAG, "💸 Weekly expenses: ৳$total from ${expenses.size()} items")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting weekly expenses", e)
            0.0
        }
    }

    private suspend fun getMonthlyExpenses(): Double {
        return try {
            val calendar = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = monthFormat.format(calendar.time)

            val expenses = firestore.collection("expenses")
                .whereEqualTo("month", currentMonth)
                .get()
                .await()

            val total = expenses.documents.sumOf {
                it.getDouble("totalAmountBDT") ?: 0.0
            }

            Log.d(TAG, "💸 Monthly expenses: ৳$total from ${expenses.size()} items")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting monthly expenses", e)
            0.0
        }
    }

    // ════════════════════════════════════════════════════════════
    // OTHER STATS
    // ════════════════════════════════════════════════════════════

    private suspend fun getTotalBalanceInSystem(): Double {
        return try {
            val users = firestore.collection("users")
                .get()
                .await()

            val total = users.documents.sumOf {
                it.getDouble("totalBalanceBDT") ?: 0.0
            }

            Log.d(TAG, "💰 Total balance in system: ৳$total")
            total

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting total balance", e)
            0.0
        }
    }

    private suspend fun getUnreadNotifications(): Int {
        return try {
            firestore.collection("admin_notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }
}