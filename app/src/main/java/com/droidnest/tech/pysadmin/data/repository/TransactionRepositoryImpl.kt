package com.droidnest.tech.pysadmin.data.repository

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.ExpenseModel
import com.droidnest.tech.pysadmin.domain.models.RevenueModel
import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.droidnest.tech.pysadmin.domain.repository.TransactionRepository
import com.droidnest.tech.pysadmin.utils.Constants
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.absoluteValue

class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TransactionRepository {

    companion object {
        private const val TAG = "TransactionRepo"

        // ✅ Referral System Constants
        private const val FIRST_DEPOSIT_CASHBACK_PERCENT = 0.05 // 5%
        private const val REFERRAL_REWARD_MYR = 20.0
        private const val REFERRAL_MILESTONE_MYR = 1000.0
    }

    // ════════════════════════════════════════════════════════════
    // GET ALL TRANSACTIONS
    // ════════════════════════════════════════════════════════════

    override suspend fun getAllTransactions(): Flow<Resource<List<TransactionModel>>> =
        callbackFlow {
            trySend(Resource.Loading())

            val listenerRegistration = firestore
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Unknown error occurred"))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val transactions = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(TransactionModel::class.java)
                        }
                        trySend(Resource.Success(transactions))
                    } else {
                        trySend(Resource.Success(emptyList()))
                    }
                }

            awaitClose { listenerRegistration.remove() }
        }

    // ════════════════════════════════════════════════════════════
    // GET TRANSACTIONS BY STATUS
    // ════════════════════════════════════════════════════════════

    override suspend fun getTransactionsByStatus(status: String): Flow<Resource<List<TransactionModel>>> =
        callbackFlow {
            trySend(Resource.Loading())

            val listenerRegistration = firestore
                .collection("transactions")
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Unknown error occurred"))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val transactions = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(TransactionModel::class.java)
                        }
                        trySend(Resource.Success(transactions))
                    } else {
                        trySend(Resource.Success(emptyList()))
                    }
                }

            awaitClose { listenerRegistration.remove() }
        }

    // ════════════════════════════════════════════════════════════
    // GET TRANSACTION BY ID
    // ════════════════════════════════════════════════════════════

    override suspend fun getTransactionById(transactionId: String): Resource<TransactionModel> {
        return try {
            val snapshot = firestore.collection("transactions")
                .document(transactionId)
                .get()
                .await()

            if (snapshot.exists()) {
                val transaction = snapshot.toObject(TransactionModel::class.java)
                if (transaction != null) {
                    Resource.Success(transaction)
                } else {
                    Resource.Error("Transaction data is corrupted")
                }
            } else {
                Resource.Error("Transaction not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load transaction")
        }
    }

    // ════════════════════════════════════════════════════════════
    // HELPER: GET CURRENT EXCHANGE RATE
    // ════════════════════════════════════════════════════════════

    private suspend fun getCurrentExchangeRate(): Double {
        return try {
            val rateDoc = firestore.collection(Constants.COLLECTION_SETTINGS)
                .document(Constants.DOC_EXCHANGE_RATE)
                .get()
                .await()

            val ratesMap = rateDoc.get("rates") as? Map<*, *>
            val myrRate = ratesMap?.get("MYR") as? Double
                ?: Constants.DEFAULT_EXCHANGE_RATE  // ✅ Using constant

            Log.d(TAG, "✅ Exchange rate: $myrRate")
            myrRate

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Constants.DEFAULT_EXCHANGE_RATE  // ✅ Fallback to default
        }
    }
    // ════════════════════════════════════════════════════════════
    // HELPER: CALCULATE TOTAL BALANCE IN BDT
    // ════════════════════════════════════════════════════════════

    private fun calculateTotalBalanceBDT(
        balanceMap: Map<String, Double>,
        exchangeRate: Double
    ): Double {
        val bdtBalance = balanceMap["BDT"] ?: 0.0
        val myrBalance = balanceMap["MYR"] ?: 0.0

        return bdtBalance + (myrBalance * exchangeRate)
    }

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ CREATE REVENUE ENTRY (শুধু WITHDRAW থেকে) ✅✅✅
    // ════════════════════════════════════════════════════════════

    private suspend fun createRevenueEntry(transaction: TransactionModel) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💰 STARTING REVENUE ENTRY CREATION")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // ✅ Transaction Details
            Log.d(TAG, "📋 TRANSACTION DETAILS:")
            Log.d(TAG, "  Transaction ID: ${transaction.appTransactionId}")
            Log.d(TAG, "  Type: ${transaction.type}")
            Log.d(TAG, "  User ID: ${transaction.userId}")
            Log.d(TAG, "  User Name: ${transaction.senderName ?: "Unknown"}")
            Log.d(TAG, "  User Phone: ${transaction.senderPhone ?: "N/A"}")
            Log.d(TAG, "  Amount: ${transaction.amount} ${transaction.currency}")
            Log.d(TAG, "  Payment Method: ${transaction.paymentMethod ?: "N/A"}")

            val feeBDT = transaction.feeBDT
            val feeMYR = transaction.feeMYR
            val rateUsed = transaction.rateUsed.takeIf { it > 0 } ?: getCurrentExchangeRate()

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💵 FEE DETAILS:")
            Log.d(TAG, "  Fee BDT: ৳$feeBDT")
            Log.d(TAG, "  Fee MYR: RM$feeMYR")
            Log.d(TAG, "  Exchange Rate: $rateUsed")

            // Convert all fees to BDT
            val totalFeeBDT = feeBDT + (feeMYR * rateUsed)

            Log.d(TAG, "  Converted MYR to BDT: ৳${feeMYR * rateUsed}")
            Log.d(TAG, "  Total Fee (BDT): ৳$totalFeeBDT")

            // ✅ Skip if no fee
            if (totalFeeBDT <= 0.0) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "⚠️ SKIPPING REVENUE ENTRY")
                Log.d(TAG, "  Reason: No fee charged (totalFeeBDT = ৳$totalFeeBDT)")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            }

            // ✅✅✅ FIXED REVENUE ID - Prevents duplicates ✅✅✅
            val revenueId = "REV_${transaction.appTransactionId}"

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🆔 REVENUE ID GENERATION:")
            Log.d(TAG, "  Generated ID: $revenueId")

            // ✅ Check if already exists (extra safety)
            val existingRevenue = firestore.collection("revenue")
                .document(revenueId)
                .get()
                .await()

            if (existingRevenue.exists()) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "⚠️ REVENUE ENTRY ALREADY EXISTS")
                Log.d(TAG, "  ID: $revenueId")
                Log.d(TAG, "  Skipping duplicate entry")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            } else {
                Log.d(TAG, "  ✅ Revenue ID is unique - proceeding...")
            }

            // ✅ Get date parts
            val calendar = Calendar.getInstance()
            calendar.time = transaction.createdAt.toDate()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val day = dateFormat.format(calendar.time)
            val month = monthFormat.format(calendar.time)
            val year = calendar.get(Calendar.YEAR)

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📅 DATE INFORMATION:")
            Log.d(TAG, "  Day: $day")
            Log.d(TAG, "  Month: $month")
            Log.d(TAG, "  Year: $year")
            Log.d(TAG, "  Created At: ${transaction.createdAt.toDate()}")
            Log.d(TAG, "  Processed At: ${Date()}")

            // ✅ Create revenue model
            val revenue = RevenueModel(
                id = revenueId,
                transactionId = transaction.appTransactionId,
                userId = transaction.userId,
                userName = transaction.senderName ?: "Unknown",
                userPhone = transaction.senderPhone ?: "",
                type = transaction.type,
                paymentMethod = transaction.paymentMethod ?: "N/A",
                feeBDT = feeBDT,
                feeMYR = feeMYR,
                totalFeeBDT = totalFeeBDT,
                rateUsed = rateUsed,
                amount = transaction.amount,
                currency = transaction.currency,
                createdAt = transaction.createdAt,
                processedAt = Timestamp.now(),
                day = day,
                month = month,
                year = year
            )

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📦 REVENUE MODEL CREATED:")
            Log.d(TAG, "  Revenue ID: ${revenue.id}")
            Log.d(TAG, "  Transaction ID: ${revenue.transactionId}")
            Log.d(TAG, "  User: ${revenue.userName} (${revenue.userPhone})")
            Log.d(TAG, "  Type: ${revenue.type}")
            Log.d(TAG, "  Payment Method: ${revenue.paymentMethod}")
            Log.d(TAG, "  Fee BDT: ৳${revenue.feeBDT}")
            Log.d(TAG, "  Fee MYR: RM${revenue.feeMYR}")
            Log.d(TAG, "  Total Fee BDT: ৳${revenue.totalFeeBDT}")
            Log.d(TAG, "  Rate Used: ${revenue.rateUsed}")
            Log.d(TAG, "  Original Amount: ${revenue.amount} ${revenue.currency}")

            // ✅ Save to Firestore (set will overwrite if exists)
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💾 SAVING TO FIRESTORE...")
            Log.d(TAG, "  Collection: revenue")
            Log.d(TAG, "  Document: $revenueId")

            firestore.collection("revenue")
                .document(revenueId)
                .set(revenue)
                .await()

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✅✅✅ REVENUE ENTRY SAVED SUCCESSFULLY ✅✅✅")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 SUMMARY:")
            Log.d(TAG, "  ✓ Revenue ID: $revenueId")
            Log.d(TAG, "  ✓ Transaction: ${transaction.appTransactionId}")
            Log.d(TAG, "  ✓ Type: ${transaction.type}")
            Log.d(TAG, "  ✓ User: ${transaction.senderName}")
            Log.d(TAG, "  ✓ Fee BDT: ৳$feeBDT")
            Log.d(TAG, "  ✓ Fee MYR: RM$feeMYR")
            Log.d(TAG, "  ✓ Total Revenue: ৳$totalFeeBDT")
            Log.d(TAG, "  ✓ Date: $day")
            Log.d(TAG, "  ✓ Month: $month")
            Log.d(TAG, "  ✓ Year: $year")
            Log.d(TAG, "  ✓ Saved to: revenue/$revenueId")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌❌❌ ERROR CREATING REVENUE ENTRY ❌❌❌")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "Error Message: ${e.message}")
            Log.e(TAG, "Transaction ID: ${transaction.appTransactionId}")
            Log.e(TAG, "Stack Trace:", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ CREATE EXPENSE ENTRY (FIXED - NO DUPLICATES) ✅✅✅
    // ════════════════════════════════════════════════════════════

    private suspend fun createExpenseEntry(
        userId: String,
        userName: String,
        userPhone: String,
        type: String,  // "first_deposit_cashback" or "referral_bonus"
        amountMYR: Double,
        amountBDT: Double,
        rateUsed: Double,
        relatedUserId: String? = null,
        relatedUserName: String? = null,
        transactionId: String? = null
    ) {
        try {
            // Convert all to BDT
            val totalAmountBDT = amountBDT + (amountMYR * rateUsed)

            if (totalAmountBDT <= 0.0) {
                Log.d(TAG, "⚠️ No expense to record")
                return
            }

            // Get date parts
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val day = dateFormat.format(calendar.time)
            val month = monthFormat.format(calendar.time)
            val year = calendar.get(Calendar.YEAR)

            // ✅✅✅ FIXED EXPENSE ID - Prevents duplicates ✅✅✅
            val expenseId = if (!transactionId.isNullOrBlank()) {
                "EXP_${transactionId}"
            } else {
                "EXP_${type.uppercase()}_${userId}_${System.currentTimeMillis()}"
            }

            // ✅ Check if already exists (extra safety)
            val existingExpense = firestore.collection("expenses")
                .document(expenseId)
                .get()
                .await()

            if (existingExpense.exists()) {
                Log.d(TAG, "⚠️ Expense entry already exists: $expenseId")
                return
            }

            // Description
            val description = when (type) {
                "first_deposit_cashback" -> "প্রথম ডিপোজিট ক্যাশব্যাক (৫%)"
                "referral_bonus" -> "রেফারেল বোনাস - ${relatedUserName ?: "User"} ১০০০ MYR পূর্ণ করেছে"
                else -> "Expense"
            }

            // Create expense model
            val expense = ExpenseModel(
                id = expenseId,
                transactionId = transactionId ?: "",
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                type = type,
                amountBDT = amountBDT,
                amountMYR = amountMYR,
                totalAmountBDT = totalAmountBDT,
                rateUsed = rateUsed,
                relatedUserId = relatedUserId,
                relatedUserName = relatedUserName,
                description = description,
                createdAt = Timestamp.now(),
                day = day,
                month = month,
                year = year
            )

            // Save to Firestore (set will overwrite if exists)
            firestore.collection("expenses")
                .document(expenseId)
                .set(expense)
                .await()

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💸 EXPENSE ENTRY CREATED")
            Log.d(TAG, "  ID: $expenseId")
            Log.d(TAG, "  Type: $type")
            Log.d(TAG, "  User: $userName")
            Log.d(TAG, "  Amount BDT: ৳$amountBDT")
            Log.d(TAG, "  Amount MYR: RM$amountMYR")
            Log.d(TAG, "  Total BDT: ৳$totalAmountBDT")
            Log.d(TAG, "  Description: $description")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating expense entry: ${e.message}", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ REFERRAL PROCESSING FUNCTIONS ✅✅✅
    // ════════════════════════════════════════════════════════════

    private suspend fun processReferralBonuses(
        userId: String,
        depositAmountMYR: Double,
        transactionId: String
    ) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🎁 Processing Referral Bonuses")

            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                Log.e(TAG, "❌ User not found")
                return
            }

            val userName = userDoc.getString("name") ?: "User"
            val userEmail = userDoc.getString("email") ?: ""
            val userPhone = userDoc.getString("phone") ?: ""
            val referredBy = userDoc.getString("referredBy") ?: ""
            val firstDepositCashbackGiven = userDoc.getBoolean("firstDepositCashbackGiven") ?: false

            // ✅ Get both deposit counters
            val totalDepositedMYR = userDoc.getDouble("totalDepositedMYR") ?: 0.0
            val totalDepositedAfterReferralMYR = userDoc.getDouble("totalDepositedAfterReferralMYR") ?: 0.0
            val referralAddedAt = userDoc.getLong("referralAddedAt") ?: 0L

            Log.d(TAG, "User: $userName")
            Log.d(TAG, "Referred By: ${if (referredBy.isBlank()) "None" else referredBy}")
            Log.d(TAG, "Total Deposited MYR (All Time): $totalDepositedMYR")
            Log.d(TAG, "Total Deposited After Referral: $totalDepositedAfterReferralMYR")

            if (referralAddedAt > 0) {
                val dateFormat = java.text.SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    java.util.Locale.getDefault()
                )
                Log.d(TAG, "Referral Added At: ${dateFormat.format(java.util.Date(referralAddedAt))}")
            }

            // ✅ Calculate new totals
            val newTotalDeposited = totalDepositedMYR + depositAmountMYR

            // ✅ Only count this deposit if referral code exists
            val newTotalAfterReferral = if (referredBy.isNotBlank() && depositAmountMYR > 0) {
                totalDepositedAfterReferralMYR + depositAmountMYR
            } else {
                totalDepositedAfterReferralMYR
            }

            Log.d(TAG, "New Total Deposited: $newTotalDeposited")
            Log.d(TAG, "New Total After Referral: $newTotalAfterReferral")

            // ═══════════════════════════════════════════════════════
            // 1️⃣ UPDATE USER'S TOTAL DEPOSITED AMOUNTS
            // ═══════════════════════════════════════════════════════

            if (depositAmountMYR > 0) {
                val updates = mutableMapOf<String, Any>(
                    "totalDepositedMYR" to newTotalDeposited,
                    "firstDepositCompleted" to true
                )

                // ✅ Only update after-referral counter if referral exists
                if (referredBy.isNotBlank()) {
                    updates["totalDepositedAfterReferralMYR"] = newTotalAfterReferral
                }

                firestore.collection("users")
                    .document(userId)
                    .update(updates)
                    .await()

                Log.d(TAG, "✅ Updated deposited amounts")
            }

            // ═══════════════════════════════════════════════════════
            // 2️⃣ FIRST DEPOSIT CASHBACK (5%)
            // ═══════════════════════════════════════════════════════

            if (!firstDepositCashbackGiven && depositAmountMYR > 0) {
                val cashbackAmount = depositAmountMYR * FIRST_DEPOSIT_CASHBACK_PERCENT

                Log.d(TAG, "💰 Processing First Deposit Cashback: $cashbackAmount MYR")

                // ... existing cashback code ...
            }

            // ═══════════════════════════════════════════════════════
            // 3️⃣ REFERRAL REWARD (20 MYR at 1000 MYR milestone)
            // ✅ Use totalDepositedAfterReferralMYR
            // ═══════════════════════════════════════════════════════

            if (referredBy.isNotBlank() &&
                newTotalAfterReferral >= REFERRAL_MILESTONE_MYR &&
                depositAmountMYR > 0) {

                // ✅ Check if milestone was just crossed
                if (totalDepositedAfterReferralMYR < REFERRAL_MILESTONE_MYR) {
                    Log.d(TAG, "🎯 Referral Milestone Reached!")
                    Log.d(TAG, "Deposits After Referral: $newTotalAfterReferral MYR")

                    // ... existing referral bonus code ...
                }
            }

            Log.d(TAG, "✅ Referral processing completed")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing referral bonuses: ${e.message}", e)
        }
    }

    // ✅ Notification for first deposit cashback
    private suspend fun sendFirstDepositCashbackNotification(userId: String, cashbackAmount: Double) {
        try {
            val notification = hashMapOf(
                "userId" to userId,
                "message" to "🎉 Welcome Bonus! আপনি প্রথম ডিপোজিটে RM ${String.format("%.2f", cashbackAmount)} ক্যাশব্যাক পেয়েছেন!",
                "date" to System.currentTimeMillis(),
                "isRead" to false,
                "type" to "success",
                "transactionType" to "first_deposit_cashback"
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            Log.d(TAG, "📬 First deposit cashback notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send cashback notification: ${e.message}")
        }
    }

    private suspend fun sendReferralRewardNotification(
        referrerId: String,
        rewardAmount: Double,
        referredUserName: String,
        totalDeposited: Double
    ) {
        try {
            val notification = hashMapOf(
                "userId" to referrerId,
                "message" to "🎁 আপনি RM ${String.format("%.2f", rewardAmount)} রেফারেল বোনাস পেয়েছেন! $referredUserName ${String.format("%.2f", totalDeposited)} MYR ডিপোজিট করেছে।",
                "date" to System.currentTimeMillis(),
                "isRead" to false,
                "type" to "success",
                "transactionType" to "referral_bonus"
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            Log.d(TAG, "📬 Referral reward notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send referral notification: ${e.message}")
        }
    }

    // ════════════════════════════════════════════════════════════
    // UPDATE TRANSACTION STATUS
    // ════════════════════════════════════════════════════════════

    override suspend fun updateTransactionStatus(
        transactionId: String,
        userId: String,
        newStatus: String,
        processed: Boolean
    ): Resource<String> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "▶ Updating Transaction Status")
            Log.d(TAG, "  Transaction ID: $transactionId")
            Log.d(TAG, "  User ID: $userId")
            Log.d(TAG, "  New Status: $newStatus")

            val transactionSnapshot = firestore
                .collection("transactions")
                .whereEqualTo("appTransactionId", transactionId)
                .limit(1)
                .get()
                .await()

            if (transactionSnapshot.isEmpty) {
                Log.e(TAG, "❌ Transaction not found")
                return Resource.Error("Transaction not found")
            }

            val transactionDoc = transactionSnapshot.documents[0]
            val transaction = transactionDoc.toObject(TransactionModel::class.java)
                ?: return Resource.Error("Invalid transaction data")

            val oldStatus = transaction.status

            Log.d(TAG, "📋 Transaction Details:")
            Log.d(TAG, "  Type: ${transaction.type}")
            Log.d(TAG, "  Old Status: $oldStatus → New Status: $newStatus")

            // Special handling for send_money
            if (transaction.type == "send_money") {
                return handleSendMoneyStatusUpdate(
                    transaction = transaction,
                    transactionDoc = transactionDoc,
                    oldStatus = oldStatus,
                    newStatus = newStatus,
                    processed = processed
                )
            }

            val exchangeRate = getCurrentExchangeRate()
            Log.d(TAG, "📊 Exchange Rate: $exchangeRate")

            // ✅✅✅ Regular transaction handling ✅✅✅
            firestore.runTransaction { firestoreTransaction ->
                val userRef = firestore.collection("users").document(userId)
                val userSnapshot = firestoreTransaction.get(userRef)

                if (!userSnapshot.exists()) {
                    throw Exception("User not found")
                }

                val balanceData = userSnapshot.get("balance")
                val balanceMap = when (balanceData) {
                    is Map<*, *> -> {
                        balanceData.mapKeys { it.key.toString() }
                            .mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                    }
                    else -> mapOf("BDT" to 0.0, "MYR" to 0.0)
                }

                val currentTotalBalanceBDT = userSnapshot.getDouble("totalBalanceBDT") ?: 0.0

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📊 BEFORE Status Update:")
                Log.d(TAG, "  Balance: $balanceMap")
                Log.d(TAG, "  Total BDT: $currentTotalBalanceBDT")

                val balanceChanges = calculateBalanceChanges(
                    transaction, oldStatus, newStatus
                )

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "💰 Balance Changes:")
                balanceChanges.forEach { (currency, change) ->
                    if (change != 0.0) {
                        Log.d(TAG, "  $currency: ${if (change > 0) "+" else ""}$change")
                    }
                }

                val updatedBalanceMap = balanceMap.toMutableMap()
                balanceChanges.forEach { (currency, change) ->
                    if (change != 0.0) {
                        val currentBalance = updatedBalanceMap[currency] ?: 0.0
                        val newBalance = currentBalance + change

                        if (newBalance < 0) {
                            throw Exception("Insufficient $currency balance")
                        }

                        updatedBalanceMap[currency] = newBalance
                    }
                }

                val newTotalBalanceBDT = calculateTotalBalanceBDT(
                    updatedBalanceMap,
                    exchangeRate
                )

                if (newTotalBalanceBDT < 0) {
                    throw Exception("Insufficient total balance")
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📊 AFTER Status Update:")
                Log.d(TAG, "  New Balance: $updatedBalanceMap")
                Log.d(TAG, "  New Total BDT: $newTotalBalanceBDT")
                Log.d(TAG, "  Total BDT Change: ${newTotalBalanceBDT - currentTotalBalanceBDT}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                val updates = mapOf(
                    "status" to newStatus,
                    "processed" to processed,
                    "processedAt" to FieldValue.serverTimestamp()
                )

                firestoreTransaction.update(transactionDoc.reference, updates)

                val userTxnRef = firestore
                    .collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document(transactionId)
                firestoreTransaction.update(userTxnRef, updates)

                if (balanceChanges.values.any { it != 0.0 }) {
                    val balanceUpdates = hashMapOf<String, Any>(
                        "balance" to updatedBalanceMap,
                        "totalBalanceBDT" to newTotalBalanceBDT
                    )

                    firestoreTransaction.update(userRef, balanceUpdates)
                    Log.d(TAG, "✅ Updated user balance and totalBalanceBDT")
                }

            }.await()

            Log.d(TAG, "✅ Transaction updated successfully")

            // ═══════════════════════════════════════════════════════
            // ✅✅✅ ADD TO REVENUE COLLECTION (শুধু WITHDRAW) ✅✅✅
            // ═══════════════════════════════════════════════════════

            if (oldStatus == "pending" && newStatus == "success") {
                // ✅ শুধু withdraw থেকে revenue আসবে
                if (transaction.type == "withdraw") {
                    Log.d(TAG, "💰 Creating revenue entry for approved ${transaction.type}")

                    CoroutineScope(Dispatchers.IO).launch {
                        createRevenueEntry(transaction)
                    }
                } else {
                    Log.d(TAG, "⚠️ Skipping revenue entry for ${transaction.type} (no fee)")
                }
            }

            // ═══════════════════════════════════════════════════════
            // ✅✅✅ PROCESS REFERRAL BONUSES (ADD MONEY ONLY) ✅✅✅
            // ═══════════════════════════════════════════════════════

            if (transaction.type == "add_money" && oldStatus == "pending" && newStatus == "success") {
                Log.d(TAG, "🎁 Triggering referral bonus processing...")

                val depositAmountMYR = when (transaction.currency.uppercase()) {
                    "MYR" -> transaction.amount
                    "BDT" -> 0.0
                    else -> 0.0
                }

                Log.d(TAG, "Deposit Amount for Referral: $depositAmountMYR MYR (Currency: ${transaction.currency})")

                CoroutineScope(Dispatchers.IO).launch {
                    processReferralBonuses(
                        userId = userId,
                        depositAmountMYR = depositAmountMYR,
                        transactionId = transactionId
                    )
                }
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            sendTransactionStatusNotification(userId, transaction, transactionId, newStatus)

            Resource.Success("Transaction updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            e.printStackTrace()
            Resource.Error(e.message ?: "Failed to update transaction")
        }
    }

    // ════════════════════════════════════════════════════════════
    // HANDLE SEND MONEY STATUS UPDATE (NO REVENUE ENTRY)
    // ════════════════════════════════════════════════════════════

    private suspend fun handleSendMoneyStatusUpdate(
        transaction: TransactionModel,
        transactionDoc: com.google.firebase.firestore.DocumentSnapshot,
        oldStatus: String,
        newStatus: String,
        processed: Boolean
    ): Resource<String> {
        return try {
            val senderId = transaction.userId
            val recipientId = transaction.recipientId
                ?: return Resource.Error("Recipient ID not found")

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💸 Send Money Transaction:")
            Log.d(TAG, "  Sender: $senderId")
            Log.d(TAG, "  Recipient: $recipientId")
            Log.d(TAG, "  Currency: ${transaction.currency}")
            Log.d(TAG, "  Amount: ${transaction.amount.absoluteValue}")

            val exchangeRate = getCurrentExchangeRate()
            Log.d(TAG, "📊 Exchange Rate (MYR → BDT): $exchangeRate")

            firestore.runTransaction { firestoreTransaction ->
                val senderRef = firestore.collection("users").document(senderId)
                val recipientRef = firestore.collection("users").document(recipientId)

                val senderSnapshot = firestoreTransaction.get(senderRef)
                val recipientSnapshot = firestoreTransaction.get(recipientRef)

                if (!senderSnapshot.exists() || !recipientSnapshot.exists()) {
                    throw Exception("Sender or recipient not found")
                }

                val senderName = senderSnapshot.getString("name") ?: "Unknown"
                val senderPhone = senderSnapshot.getString("phone") ?: ""

                val currency = transaction.currency.uppercase()
                val amount = transaction.amount.absoluteValue

                if (oldStatus == "pending" && newStatus == "success") {
                    Log.d(TAG, "✅ Approving - Adding $currency $amount to recipient")

                    val recipientBalanceData = recipientSnapshot.get("balance")
                    val recipientBalanceMap = when (recipientBalanceData) {
                        is Map<*, *> -> {
                            recipientBalanceData.mapKeys { it.key.toString() }
                                .mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                        }
                        else -> mapOf("BDT" to 0.0, "MYR" to 0.0)
                    }

                    val updatedRecipientBalance = recipientBalanceMap.toMutableMap()
                    updatedRecipientBalance[currency] =
                        (updatedRecipientBalance[currency] ?: 0.0) + amount

                    val newTotalBDT = calculateTotalBalanceBDT(
                        updatedRecipientBalance,
                        exchangeRate
                    )

                    firestoreTransaction.update(
                        recipientRef,
                        hashMapOf<String, Any>(
                            "balance" to updatedRecipientBalance,
                            "totalBalanceBDT" to newTotalBDT
                        )
                    )

                    val receiveTransaction = TransactionModel(
                        appTransactionId = transaction.appTransactionId,
                        userId = recipientId,
                        senderName = senderName,
                        senderPhone = senderPhone,
                        recipientId = null,
                        recipientName = null,
                        recipientPhone = null,
                        recipientEmail = null,
                        type = "receive_money",
                        currency = transaction.currency,
                        amount = amount,
                        feeBDT = transaction.feeBDT,
                        feeMYR = transaction.feeMYR,
                        convertedAmountBDT = transaction.convertedAmountBDT,
                        convertedAmountMYR = transaction.convertedAmountMYR,
                        netAmountBDT = transaction.netAmountBDT,
                        netAmountMYR = transaction.netAmountMYR,
                        rateUsed = exchangeRate,
                        message = transaction.message,
                        status = "success",
                        processed = true,
                        createdAt = transaction.createdAt,
                    )

                    val recipientTxnRef = firestore
                        .collection("users")
                        .document(recipientId)
                        .collection("transactions")
                        .document(transaction.appTransactionId)
                    firestoreTransaction.set(recipientTxnRef, receiveTransaction)

                    val receiveDocId = "${transaction.appTransactionId}_RECV"
                    val mainRecipientTxnRef = firestore
                        .collection("transactions")
                        .document(receiveDocId)
                    firestoreTransaction.set(mainRecipientTxnRef, receiveTransaction)

                    Log.d(TAG, "✅ Recipient transaction saved with sender details")
                }
                else if (oldStatus == "pending" && newStatus == "failed") {
                    Log.d(TAG, "❌ Rejecting - Refunding $currency $amount to sender")

                    val senderBalanceData = senderSnapshot.get("balance")
                    val senderBalanceMap = when (senderBalanceData) {
                        is Map<*, *> -> {
                            senderBalanceData.mapKeys { it.key.toString() }
                                .mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                        }
                        else -> mapOf("BDT" to 0.0, "MYR" to 0.0)
                    }

                    val updatedSenderBalance = senderBalanceMap.toMutableMap()
                    updatedSenderBalance[currency] =
                        (updatedSenderBalance[currency] ?: 0.0) + amount

                    val newTotalBDT = calculateTotalBalanceBDT(
                        updatedSenderBalance,
                        exchangeRate
                    )

                    firestoreTransaction.update(
                        senderRef,
                        hashMapOf<String, Any>(
                            "balance" to updatedSenderBalance,
                            "totalBalanceBDT" to newTotalBDT
                        )
                    )

                    Log.d(TAG, "✅ Refunded to sender")
                }

                val senderTxnRef = firestore
                    .collection("users")
                    .document(senderId)
                    .collection("transactions")
                    .document(transaction.appTransactionId)
                firestoreTransaction.update(
                    senderTxnRef,
                    mapOf("status" to newStatus, "processed" to processed)
                )

                firestoreTransaction.update(
                    transactionDoc.reference,
                    mapOf("status" to newStatus, "processed" to processed)
                )

            }.await()

            // ✅ NO REVENUE ENTRY for send_money (removed)
            Log.d(TAG, "⚠️ Skipping revenue entry for send_money (no fee)")

            Log.d(TAG, "✅ Send money transaction completed successfully")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            sendTransactionStatusNotification(
                senderId,
                transaction,
                transaction.appTransactionId,
                newStatus
            )

            if (newStatus == "success") {
                sendTransactionStatusNotification(
                    recipientId,
                    transaction,
                    transaction.appTransactionId,
                    "received"
                )
            }

            Resource.Success("Send money transaction updated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling send_money: ${e.message}")
            e.printStackTrace()
            Resource.Error(e.message ?: "Failed to update send money transaction")
        }
    }

    // ════════════════════════════════════════════════════════════
    // CALCULATE BALANCE CHANGES
    // ════════════════════════════════════════════════════════════

    private fun calculateBalanceChanges(
        transaction: TransactionModel,
        oldStatus: String,
        newStatus: String
    ): Map<String, Double> {

        if (oldStatus == newStatus) {
            return mapOf("BDT" to 0.0, "MYR" to 0.0)
        }

        return when (transaction.type) {
            "withdraw" -> {
                when {
                    oldStatus == "pending" && newStatus == "failed" -> {
                        Log.d(TAG, "💰 WITHDRAW REFUND (Smart Deduction):")
                        Log.d(TAG, "  BDT: +${transaction.bdtDeducted}")
                        Log.d(TAG, "  MYR: +${transaction.myrDeducted}")
                        mapOf(
                            "BDT" to transaction.bdtDeducted,
                            "MYR" to transaction.myrDeducted
                        )
                    }
                    oldStatus == "pending" && newStatus == "success" -> {
                        Log.d(TAG, "✅ WITHDRAW APPROVED - No balance change")
                        mapOf("BDT" to 0.0, "MYR" to 0.0)
                    }
                    oldStatus == "failed" && newStatus == "success" -> {
                        mapOf(
                            "BDT" to -transaction.bdtDeducted,
                            "MYR" to -transaction.myrDeducted
                        )
                    }
                    else -> mapOf("BDT" to 0.0, "MYR" to 0.0)
                }
            }

            "send_money" -> {
                mapOf("BDT" to 0.0, "MYR" to 0.0)
            }

            "add_money", "referral_bonus", "receive_money", "first_deposit_cashback" -> {
                val currency = transaction.currency.uppercase()
                val amount = when (currency) {
                    "BDT" -> {
                        if (transaction.netAmountBDT > 0.0) transaction.netAmountBDT
                        else transaction.amount.absoluteValue
                    }
                    "MYR" -> {
                        if (transaction.netAmountMYR > 0.0) transaction.netAmountMYR
                        else transaction.amount.absoluteValue
                    }
                    else -> transaction.amount.absoluteValue
                }

                when {
                    oldStatus == "pending" && newStatus == "success" -> {
                        mapOf(currency to amount)
                    }
                    oldStatus == "success" && newStatus == "failed" -> {
                        mapOf(currency to -amount)
                    }
                    oldStatus == "failed" && newStatus == "success" -> {
                        mapOf(currency to amount)
                    }
                    else -> mapOf("BDT" to 0.0, "MYR" to 0.0)
                }
            }

            else -> mapOf("BDT" to 0.0, "MYR" to 0.0)
        }
    }

    // ════════════════════════════════════════════════════════════
    // SEND NOTIFICATION
    // ════════════════════════════════════════════════════════════

    private suspend fun sendTransactionStatusNotification(
        userId: String,
        transaction: TransactionModel,
        transactionId: String,
        newStatus: String
    ) {
        try {
            val message = createNotificationMessage(transaction, newStatus)
            val notificationType = when (newStatus) {
                "success", "received" -> "success"
                "failed" -> "error"
                "pending" -> "warning"
                else -> "info"
            }

            val notification = hashMapOf(
                "userId" to userId,
                "message" to message,
                "date" to System.currentTimeMillis(),
                "isRead" to false,
                "type" to notificationType,
                "transactionId" to transactionId,
                "transactionType" to transaction.type
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            Log.d(TAG, "📬 Notification sent to $userId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send notification: ${e.message}")
        }
    }

    // ════════════════════════════════════════════════════════════
    // CREATE NOTIFICATION MESSAGE
    // ════════════════════════════════════════════════════════════

    private fun createNotificationMessage(
        transaction: TransactionModel,
        status: String
    ): String {
        val currency = transaction.currency.uppercase()
        val currencySymbol = when (currency) {
            "BDT" -> "৳"
            "MYR" -> "RM"
            "USD" -> "$"
            "EUR" -> "€"
            else -> ""
        }

        val amount = when (currency) {
            "BDT" -> {
                if (transaction.netAmountBDT > 0.0) transaction.netAmountBDT
                else transaction.amount.absoluteValue
            }
            "MYR" -> {
                if (transaction.netAmountMYR > 0.0) transaction.netAmountMYR
                else transaction.amount.absoluteValue
            }
            else -> transaction.amount.absoluteValue
        }

        val formattedAmount = "$currencySymbol${String.format("%.2f", amount)}"

        return when {
            status == "received" -> "💰 You received $formattedAmount!"
            transaction.type == "add_money" -> when (status) {
                "success" -> "✅ Your add money request of $formattedAmount has been approved!"
                "failed" -> "❌ Your add money request of $formattedAmount has been rejected."
                "pending" -> "⏳ Your add money request of $formattedAmount is being processed."
                else -> "Your add money request status: $status."
            }
            transaction.type == "withdraw" -> when (status) {
                "success" -> "✅ Your withdrawal of $formattedAmount has been processed!"
                "failed" -> "❌ Your withdrawal request of $formattedAmount has been rejected and refunded."
                "pending" -> "⏳ Your withdrawal request of $formattedAmount is being processed."
                else -> "Your withdrawal status: $status."
            }
            transaction.type == "send_money" -> when (status) {
                "success" -> "✅ You have successfully sent $formattedAmount!"
                "failed" -> "❌ Failed to send $formattedAmount. Amount refunded."
                "pending" -> "⏳ Your money transfer of $formattedAmount is being processed."
                else -> "Your send money status: $status."
            }
            transaction.type == "first_deposit_cashback" -> when (status) {
                "success" -> "🎉 Welcome Bonus! You received $formattedAmount cashback on your first deposit!"
                "failed" -> "❌ First deposit cashback of $formattedAmount could not be processed."
                "pending" -> "⏳ Your first deposit cashback of $formattedAmount is being verified."
                else -> "Your cashback status: $status."
            }
            transaction.type == "referral_bonus" -> when (status) {
                "success" -> "🎁 You received a referral bonus of $formattedAmount!"
                "failed" -> "❌ Referral bonus of $formattedAmount could not be processed."
                "pending" -> "⏳ Your referral bonus of $formattedAmount is being verified."
                else -> "Your referral bonus status: $status."
            }
            else -> when (status) {
                "success" -> "✅ Your transaction of $formattedAmount completed!"
                "failed" -> "❌ Your transaction of $formattedAmount failed."
                "pending" -> "⏳ Your transaction of $formattedAmount is being processed."
                else -> "Your transaction status: $status."
            }
        }
    }
}