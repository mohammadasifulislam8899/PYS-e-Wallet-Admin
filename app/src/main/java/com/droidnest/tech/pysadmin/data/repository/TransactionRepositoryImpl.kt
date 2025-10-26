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
                ?: Constants.DEFAULT_EXCHANGE_RATE

            Log.d(TAG, "✅ Exchange rate: $myrRate")
            myrRate

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Constants.DEFAULT_EXCHANGE_RATE
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

            val feeBDT = transaction.feeBDT
            val feeMYR = transaction.feeMYR
            val rateUsed = transaction.rateUsed.takeIf { it > 0 } ?: getCurrentExchangeRate()

            val totalFeeBDT = feeBDT + (feeMYR * rateUsed)

            if (totalFeeBDT <= 0.0) {
                Log.d(TAG, "⚠️ SKIPPING REVENUE ENTRY (No fee)")
                return
            }

            val revenueId = "REV_${transaction.appTransactionId}"

            val existingRevenue = firestore.collection("revenue")
                .document(revenueId)
                .get()
                .await()

            if (existingRevenue.exists()) {
                Log.d(TAG, "⚠️ REVENUE ENTRY ALREADY EXISTS: $revenueId")
                return
            }

            val calendar = Calendar.getInstance()
            calendar.time = transaction.createdAt.toDate()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val day = dateFormat.format(calendar.time)
            val month = monthFormat.format(calendar.time)
            val year = calendar.get(Calendar.YEAR)

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

            firestore.collection("revenue")
                .document(revenueId)
                .set(revenue)
                .await()

            Log.d(TAG, "✅✅✅ REVENUE ENTRY SAVED SUCCESSFULLY ✅✅✅")
            Log.d(TAG, "  Revenue ID: $revenueId")
            Log.d(TAG, "  Total Revenue: ৳$totalFeeBDT")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR CREATING REVENUE ENTRY: ${e.message}", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // ✅✅✅ CREATE EXPENSE ENTRY ✅✅✅
    // ════════════════════════════════════════════════════════════

    private suspend fun createExpenseEntry(
        userId: String,
        userName: String,
        userPhone: String,
        type: String,
        amountMYR: Double,
        amountBDT: Double,
        rateUsed: Double,
        relatedUserId: String? = null,
        relatedUserName: String? = null,
        transactionId: String? = null
    ) {
        try {
            val totalAmountBDT = amountBDT + (amountMYR * rateUsed)

            if (totalAmountBDT <= 0.0) {
                Log.d(TAG, "⚠️ No expense to record")
                return
            }

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            val day = dateFormat.format(calendar.time)
            val month = monthFormat.format(calendar.time)
            val year = calendar.get(Calendar.YEAR)

            val expenseId = if (!transactionId.isNullOrBlank()) {
                "EXP_${transactionId}"
            } else {
                "EXP_${type.uppercase()}_${userId}_${System.currentTimeMillis()}"
            }

            val existingExpense = firestore.collection("expenses")
                .document(expenseId)
                .get()
                .await()

            if (existingExpense.exists()) {
                Log.d(TAG, "⚠️ Expense entry already exists: $expenseId")
                return
            }

            val description = when (type) {
                "first_deposit_cashback" -> "প্রথম ডিপোজিট ক্যাশব্যাক (৫%)"
                "referral_bonus" -> "রেফারেল বোনাস - ${relatedUserName ?: "User"} ১০০০ MYR পূর্ণ করেছে"
                else -> "Expense"
            }

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

            val totalDepositedMYR = userDoc.getDouble("totalDepositedMYR") ?: 0.0
            val totalDepositedAfterReferralMYR = userDoc.getDouble("totalDepositedAfterReferralMYR") ?: 0.0
            val referralMilestoneAchieved = userDoc.getBoolean("referralMilestoneAchieved") ?: false
            val referralAddedAt = userDoc.getLong("referralAddedAt") ?: 0L

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "👤 User: $userName")
            Log.d(TAG, "📱 Phone: $userPhone")
            Log.d(TAG, "🔗 Referred By: ${if (referredBy.isBlank()) "None" else referredBy}")
            Log.d(TAG, "💰 Total Deposited (All Time): $totalDepositedMYR MYR")
            Log.d(TAG, "💰 Total After Referral: $totalDepositedAfterReferralMYR MYR")
            Log.d(TAG, "📥 Current Deposit: $depositAmountMYR MYR")
            Log.d(TAG, "🎯 Milestone Achieved: $referralMilestoneAchieved")

            if (referralAddedAt > 0) {
                val dateFormat = SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                )
                Log.d(TAG, "📅 Referral Added At: ${dateFormat.format(Date(referralAddedAt))}")
            }

            // ═══════════════════════════════════════════════════════
            // 1️⃣ UPDATE USER'S TOTAL DEPOSITED AMOUNTS
            // ═══════════════════════════════════════════════════════

            if (depositAmountMYR > 0) {
                val newTotalDeposited = totalDepositedMYR + depositAmountMYR

                val newTotalAfterReferral = if (referredBy.isNotBlank()) {
                    totalDepositedAfterReferralMYR + depositAmountMYR
                } else {
                    totalDepositedAfterReferralMYR
                }

                val updates = mutableMapOf<String, Any>(
                    "totalDepositedMYR" to newTotalDeposited,
                    "firstDepositCompleted" to true
                )

                if (referredBy.isNotBlank()) {
                    updates["totalDepositedAfterReferralMYR"] = newTotalAfterReferral
                }

                firestore.collection("users")
                    .document(userId)
                    .update(updates)
                    .await()

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ Updated Deposit Counters:")
                Log.d(TAG, "  Total Deposited: $totalDepositedMYR → $newTotalDeposited MYR")
                if (referredBy.isNotBlank()) {
                    Log.d(TAG, "  After Referral: $totalDepositedAfterReferralMYR → $newTotalAfterReferral MYR")
                }

                // ═══════════════════════════════════════════════════════
                // 2️⃣ FIRST DEPOSIT CASHBACK (5%)
                // ✅✅✅ WITH TRANSACTION CREATION ✅✅✅
                // ═══════════════════════════════════════════════════════

                if (!firstDepositCashbackGiven && depositAmountMYR > 0) {
                    val cashbackAmount = depositAmountMYR * FIRST_DEPOSIT_CASHBACK_PERCENT
                    val exchangeRate = getCurrentExchangeRate()

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "💰 FIRST DEPOSIT CASHBACK")
                    Log.d(TAG, "  Deposit Amount: $depositAmountMYR MYR")
                    Log.d(TAG, "  Cashback (5%): $cashbackAmount MYR")

                    // ✅✅✅ CREATE CASHBACK TRANSACTION ✅✅✅
                    val cashbackTransactionId = "TXN_CASHBACK_${System.currentTimeMillis()}_${userId}"

                    val cashbackTransaction = TransactionModel(
                        appTransactionId = cashbackTransactionId,
                        userId = userId,
                        senderName = userName,
                        senderPhone = userPhone,
                        senderEmail = userEmail,
                        type = "first_deposit_cashback",
                        currency = "MYR",
                        amount = cashbackAmount,
                        netAmountMYR = cashbackAmount,
                        convertedAmountMYR = cashbackAmount,
                        rateUsed = exchangeRate,
                        message = "🎉 প্রথম ডিপোজিট ক্যাশব্যাক (৫%)! ${String.format("%.2f", depositAmountMYR)} MYR ডিপোজিট করেছেন",
                        status = "success",
                        processed = true,
                        createdAt = Timestamp.now(),
                        processedAt = Timestamp.now(),
                        feeBDT = 0.0,
                        feeMYR = 0.0,
                        convertedAmountBDT = 0.0,
                        netAmountBDT = 0.0,
                        bdtDeducted = 0.0,
                        myrDeducted = 0.0
                    )

                    // 📁 Save to main transactions collection
                    firestore.collection("transactions")
                        .document(cashbackTransactionId)
                        .set(cashbackTransaction)
                        .await()

                    Log.d(TAG, "✅ Cashback transaction created: $cashbackTransactionId")

                    // 📁 Save to user's transactions subcollection
                    firestore.collection("users")
                        .document(userId)
                        .collection("transactions")
                        .document(cashbackTransactionId)
                        .set(cashbackTransaction)
                        .await()

                    Log.d(TAG, "✅ Saved to user's transaction history")

                    // ✅ Add cashback to user balance
                    firestore.collection("users")
                        .document(userId)
                        .update(
                            mapOf(
                                "balance.MYR" to FieldValue.increment(cashbackAmount),
                                "firstDepositCashbackGiven" to true
                            )
                        )
                        .await()

                    Log.d(TAG, "✅ Added $cashbackAmount MYR to user balance")

                    // ✅ Update totalBalanceBDT
                    val userDocUpdated = firestore.collection("users")
                        .document(userId)
                        .get()
                        .await()

                    val balanceMap = (userDocUpdated.get("balance") as? Map<*, *>)
                        ?.mapKeys { it.key.toString() }
                        ?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                        ?: mapOf("BDT" to 0.0, "MYR" to 0.0)

                    val newTotalBDT = calculateTotalBalanceBDT(balanceMap, exchangeRate)

                    firestore.collection("users")
                        .document(userId)
                        .update("totalBalanceBDT", newTotalBDT)
                        .await()

                    // ✅ Create expense entry
                    createExpenseEntry(
                        userId = userId,
                        userName = userName,
                        userPhone = userPhone,
                        type = "first_deposit_cashback",
                        amountMYR = cashbackAmount,
                        amountBDT = 0.0,
                        rateUsed = exchangeRate,
                        transactionId = cashbackTransactionId
                    )

                    // ✅ Send notification
                    sendFirstDepositCashbackNotification(userId, cashbackAmount)

                    Log.d(TAG, "✅✅✅ First Deposit Cashback Processed Successfully ✅✅✅")
                    Log.d(TAG, "  Transaction ID: $cashbackTransactionId")
                    Log.d(TAG, "  User: $userName")
                    Log.d(TAG, "  Cashback: $cashbackAmount MYR")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

                // ═══════════════════════════════════════════════════════
                // 3️⃣ REFERRAL REWARD (20 MYR at 1000 MYR milestone)
                // ✅✅✅ WITH TRANSACTION CREATION ✅✅✅
                // ═══════════════════════════════════════════════════════

                if (referredBy.isNotBlank() &&
                    !referralMilestoneAchieved &&
                    depositAmountMYR > 0) {

                    val newTotalAfterReferral = totalDepositedAfterReferralMYR + depositAmountMYR

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "🎯 CHECKING REFERRAL MILESTONE")
                    Log.d(TAG, "  Deposits After Referral: $totalDepositedAfterReferralMYR → $newTotalAfterReferral MYR")
                    Log.d(TAG, "  Milestone Target: $REFERRAL_MILESTONE_MYR MYR")
                    Log.d(TAG, "  Remaining: ${REFERRAL_MILESTONE_MYR - newTotalAfterReferral} MYR")

                    if (newTotalAfterReferral >= REFERRAL_MILESTONE_MYR &&
                        totalDepositedAfterReferralMYR < REFERRAL_MILESTONE_MYR) {

                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "🎉🎉🎉 MILESTONE ACHIEVED! 🎉🎉🎉")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "🔍 SEARCHING FOR REFERRER")
                        Log.d(TAG, "  Referral Code: '$referredBy'")

                        // ✅ Try multiple queries (case variations)
                        var referrerDoc = firestore.collection("users")
                            .whereEqualTo("refCode", referredBy)
                            .get()
                            .await()

                        Log.d(TAG, "  Query 1 (exact): ${referrerDoc.size()} documents found")

                        if (referrerDoc.isEmpty) {
                            referrerDoc = firestore.collection("users")
                                .whereEqualTo("refCode", referredBy.uppercase())
                                .get()
                                .await()
                            Log.d(TAG, "  Query 2 (uppercase): ${referrerDoc.size()} documents found")
                        }

                        if (referrerDoc.isEmpty) {
                            referrerDoc = firestore.collection("users")
                                .whereEqualTo("refCode", referredBy.lowercase())
                                .get()
                                .await()
                            Log.d(TAG, "  Query 3 (lowercase): ${referrerDoc.size()} documents found")
                        }

                        if (referrerDoc.isEmpty) {
                            val trimmedCode = referredBy.trim()
                            referrerDoc = firestore.collection("users")
                                .whereEqualTo("refCode", trimmedCode)
                                .get()
                                .await()
                            Log.d(TAG, "  Query 4 (trimmed): ${referrerDoc.size()} documents found")
                        }

                        if (!referrerDoc.isEmpty) {
                            val referrerId = referrerDoc.documents[0].id
                            val referrerData = referrerDoc.documents[0].data

                            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                            Log.d(TAG, "✅ REFERRER FOUND!")
                            Log.d(TAG, "  Referrer ID: $referrerId")
                            Log.d(TAG, "  Referrer Name: ${referrerData?.get("name")}")
                            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                            val referrerDoc2 = firestore.collection("users")
                                .document(referrerId)
                                .get()
                                .await()

                            if (referrerDoc2.exists()) {
                                val referrerName = referrerDoc2.getString("name") ?: "User"
                                val referrerPhone = referrerDoc2.getString("phone") ?: ""
                                val referrerEmail = referrerDoc2.getString("email") ?: ""
                                val exchangeRate = getCurrentExchangeRate()

                                Log.d(TAG, "💰 Processing referral bonus...")
                                Log.d(TAG, "  Referrer: $referrerName")
                                Log.d(TAG, "  Reward: $REFERRAL_REWARD_MYR MYR")

                                // ✅✅✅ CREATE REFERRAL BONUS TRANSACTION ✅✅✅
                                val bonusTransactionId = "TXN_REFBONUS_${System.currentTimeMillis()}_${referrerId}"

                                val bonusTransaction = TransactionModel(
                                    appTransactionId = bonusTransactionId,
                                    userId = referrerId,
                                    senderName = referrerName,
                                    senderPhone = referrerPhone,
                                    senderEmail = referrerEmail,
                                    type = "referral_bonus",
                                    currency = "MYR",
                                    amount = REFERRAL_REWARD_MYR,
                                    netAmountMYR = REFERRAL_REWARD_MYR,
                                    convertedAmountMYR = REFERRAL_REWARD_MYR,
                                    rateUsed = exchangeRate,
                                    message = "🎁 রেফারেল বোনাস! $userName ${String.format("%.2f", newTotalAfterReferral)} MYR ডিপোজিট করেছে",
                                    status = "success",
                                    processed = true,
                                    createdAt = Timestamp.now(),
                                    processedAt = Timestamp.now(),
                                    feeBDT = 0.0,
                                    feeMYR = 0.0,
                                    convertedAmountBDT = 0.0,
                                    netAmountBDT = 0.0,
                                    bdtDeducted = 0.0,
                                    myrDeducted = 0.0
                                )

                                // 📁 Save to main transactions collection
                                firestore.collection("transactions")
                                    .document(bonusTransactionId)
                                    .set(bonusTransaction)
                                    .await()

                                Log.d(TAG, "✅ Referral bonus transaction created: $bonusTransactionId")

                                // 📁 Save to user's transactions subcollection
                                firestore.collection("users")
                                    .document(referrerId)
                                    .collection("transactions")
                                    .document(bonusTransactionId)
                                    .set(bonusTransaction)
                                    .await()

                                Log.d(TAG, "✅ Saved to referrer's transaction history")

                                // ✅ Add 20 MYR to referrer balance
                                firestore.collection("users")
                                    .document(referrerId)
                                    .update("balance.MYR", FieldValue.increment(REFERRAL_REWARD_MYR))
                                    .await()

                                Log.d(TAG, "✅ Added $REFERRAL_REWARD_MYR MYR to referrer balance")

                                // ✅ Update referrer's totalBalanceBDT
                                val referrerDocUpdated = firestore.collection("users")
                                    .document(referrerId)
                                    .get()
                                    .await()

                                val referrerBalanceMap = (referrerDocUpdated.get("balance") as? Map<*, *>)
                                    ?.mapKeys { it.key.toString() }
                                    ?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                                    ?: mapOf("BDT" to 0.0, "MYR" to 0.0)

                                val referrerNewTotalBDT = calculateTotalBalanceBDT(referrerBalanceMap, exchangeRate)

                                firestore.collection("users")
                                    .document(referrerId)
                                    .update(
                                        mapOf(
                                            "totalBalanceBDT" to referrerNewTotalBDT,
                                            "referralEarnings" to FieldValue.increment(REFERRAL_REWARD_MYR),
                                            "referralCount" to FieldValue.increment(1)
                                        )
                                    )
                                    .await()

                                // ✅ Mark milestone as achieved
                                firestore.collection("users")
                                    .document(userId)
                                    .update("referralMilestoneAchieved", true)
                                    .await()

                                Log.d(TAG, "✅ Marked milestone as achieved for user")

                                // ✅ Create expense entry
                                createExpenseEntry(
                                    userId = referrerId,
                                    userName = referrerName,
                                    userPhone = referrerPhone,
                                    type = "referral_bonus",
                                    amountMYR = REFERRAL_REWARD_MYR,
                                    amountBDT = 0.0,
                                    rateUsed = exchangeRate,
                                    relatedUserId = userId,
                                    relatedUserName = userName,
                                    transactionId = bonusTransactionId
                                )

                                // ✅ Send notification to referrer
                                sendReferralRewardNotification(
                                    referrerId = referrerId,
                                    rewardAmount = REFERRAL_REWARD_MYR,
                                    referredUserName = userName,
                                    totalDeposited = newTotalAfterReferral
                                )

                                Log.d(TAG, "✅✅✅ Referral Bonus Processed Successfully ✅✅✅")
                                Log.d(TAG, "  Transaction ID: $bonusTransactionId")
                                Log.d(TAG, "  Referrer: $referrerName")
                                Log.d(TAG, "  Reward: $REFERRAL_REWARD_MYR MYR")
                                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                            } else {
                                Log.e(TAG, "❌ Referrer document not found: $referrerId")
                            }
                        } else {
                            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                            Log.e(TAG, "❌ REFERRER NOT FOUND AFTER ALL ATTEMPTS")
                            Log.e(TAG, "  Searched for refCode: '$referredBy'")
                            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        }
                    } else {
                        Log.d(TAG, "⏳ Milestone not yet reached")
                        Log.d(TAG, "  Need ${REFERRAL_MILESTONE_MYR - newTotalAfterReferral} MYR more")
                    }
                } else if (referralMilestoneAchieved) {
                    Log.d(TAG, "⚠️ Milestone already achieved previously")
                } else if (referredBy.isBlank()) {
                    Log.d(TAG, "⚠️ No referral code - skipping referral bonus check")
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