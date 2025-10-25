// data/repository/KycManagementRepositoryImpl.kt
package com.droidnest.tech.pysadmin.data.repository

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.KycRequest
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.repository.KycManagementRepository
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class KycManagementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : KycManagementRepository {

    companion object {
        private const val TAG = "KycManagementRepository"
        private const val COLLECTION_KYC = "kyc_requests"
        private const val COLLECTION_USERS = "users"
    }

    override suspend fun getAllKycRequests(): Flow<Resource<List<KycRequest>>> = callbackFlow {
        Log.d(TAG, "🔄 getAllKycRequests() শুরু হয়েছে")

        trySend(Resource.Loading())
        Log.d(TAG, "📤 Loading state পাঠানো হয়েছে")

        val listener = firestore.collection(COLLECTION_KYC)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error in getAllKycRequests: ${error.message}", error)
                    Log.e(TAG, "🔍 Error code: ${error.localizedMessage}")
                    trySend(Resource.Error(error.message ?: "Failed to load KYC requests"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d(TAG, "✅ Snapshot পাওয়া গেছে")
                    Log.d(TAG, "📊 Total documents: ${snapshot.size()}")

                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            val request = doc.toObject(KycRequest::class.java)?.copy(id = doc.id)
                            Log.d(TAG, "📋 Document ID: ${doc.id}")
                            Log.d(TAG, "📝 Request object: $request")
                            request
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Failed to parse document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }

                    Log.d(TAG, "✨ Total parsed requests: ${requests.size}")
                    trySend(Resource.Success(requests))
                } else {
                    Log.w(TAG, "⚠️ Snapshot is null")
                    trySend(Resource.Success(emptyList()))
                }
            }

        Log.d(TAG, "🎧 Snapshot listener added")

        awaitClose {
            Log.d(TAG, "🔌 Snapshot listener removed (Flow closed)")
            listener.remove()
        }
    }

    override suspend fun getKycRequestById(requestId: String): Resource<KycRequest> {
        Log.d(TAG, "🔍 getKycRequestById() শুরু: ID = $requestId")

        return try {
            Log.d(TAG, "📡 Firebase থেকে data fetch করছি...")

            val snapshot = firestore.collection(COLLECTION_KYC)
                .document(requestId)
                .get()
                .await()

            Log.d(TAG, "✅ Snapshot পাওয়া গেছে")
            Log.d(TAG, "📄 Document exists: ${snapshot.exists()}")

            val kycRequest = snapshot.toObject(KycRequest::class.java)?.copy(id = snapshot.id) ?: run {
                Log.e(TAG, "❌ KYC request parse ব্যর্থ")
                Log.w(TAG, "📋 Raw document data: ${snapshot.data}")
                return Resource.Error("KYC request not found")
            }

            Log.d(TAG, "✨ Request found: $kycRequest")
            Log.i(TAG, "✅ getKycRequestById সফল হয়েছে")
            Resource.Success(kycRequest)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in getKycRequestById: ${e.message}", e)
            Log.e(TAG, "🔍 Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "📍 Stack trace: ${e.stackTrace.joinToString("\n")}")
            Resource.Error(e.message ?: "Failed to load KYC request")
        }
    }

    override suspend fun getKycRequestsByStatus(status: KycStatus): Flow<Resource<List<KycRequest>>> = callbackFlow {
        Log.d(TAG, "🔄 getKycRequestsByStatus() শুরু: status = ${status.name}")

        trySend(Resource.Loading())
        Log.d(TAG, "📤 Loading state পাঠানো হয়েছে")

        val listener = firestore.collection(COLLECTION_KYC)
            .whereEqualTo("status", status.name)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error in getKycRequestsByStatus: ${error.message}", error)
                    Log.e(TAG, "🔍 Filtering by status: ${status.name}")
                    trySend(Resource.Error(error.message ?: "Failed to load KYC requests"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d(TAG, "✅ Snapshot পাওয়া গেছে")
                    Log.d(TAG, "📊 Total ${status.name} documents: ${snapshot.size()}")

                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            val request = doc.toObject(KycRequest::class.java)?.copy(id = doc.id)
                            Log.d(TAG, "📋 Document: ${doc.id} - Status: ${status.name}")
                            request
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Failed to parse ${status.name} document ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "✨ Total parsed ${status.name} requests: ${requests.size}")
                    trySend(Resource.Success(requests))
                } else {
                    Log.w(TAG, "⚠️ Snapshot is null for status: ${status.name}")
                    trySend(Resource.Success(emptyList()))
                }
            }

        Log.d(TAG, "🎧 Listener added for status: ${status.name}")

        awaitClose {
            Log.d(TAG, "🔌 Listener removed for status: ${status.name}")
            listener.remove()
        }
    }

    override suspend fun approveKycRequest(
        requestId: String,
        adminId: String,
        adminNotes: String?
    ): Resource<String> {
        Log.d(TAG, "✅ approveKycRequest() শুরু")
        Log.d(TAG, "📋 Request ID: $requestId")
        Log.d(TAG, "👤 Admin ID: $adminId")
        Log.d(TAG, "📝 Admin Notes: $adminNotes")

        return try {
            Log.d(TAG, "📦 Batch operation শুরু করছি...")
            val batch = firestore.batch()

            Log.d(TAG, "🔍 KYC request ডকুমেন্ট খুঁজছি...")

            val kycDoc = firestore.collection(COLLECTION_KYC)
                .document(requestId)
                .get()
                .await()

            Log.d(TAG, "✅ Document পাওয়া গেছে - exists: ${kycDoc.exists()}")

            val kycRequest = kycDoc.toObject(KycRequest::class.java) ?: run {
                Log.e(TAG, "❌ KYC request parse ব্যর্থ")
                return Resource.Error("KYC request not found")
            }

            Log.d(TAG, "✨ Request object: $kycRequest")
            Log.d(TAG, "👥 User ID: ${kycRequest.userId}")

            val currentTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            Log.d(TAG, "⏰ Current Time: $currentTime")

            // Update KYC request status
            Log.d(TAG, "🔧 Updating KYC request status to VERIFIED...")

            val kycRef = firestore.collection(COLLECTION_KYC).document(requestId)
            batch.update(
                kycRef,
                mapOf(
                    "status" to KycStatus.VERIFIED.name,
                    "reviewedAt" to currentTime,
                    "reviewedBy" to adminId,
                    "rejectionReason" to FieldValue.delete()
                )
            )
            Log.d(TAG, "✅ KYC request batch update added")

            // Update user's KYC status
            Log.d(TAG, "🔧 Updating user KYC status...")

            val userRef = firestore.collection(COLLECTION_USERS).document(kycRequest.userId)
            batch.update(
                userRef,
                mapOf(
                    "kycStatus" to KycStatus.VERIFIED.name,
                    "kycVerifiedAt" to currentTime,
                    "kycRequestId" to requestId,
                    "kycRejectionReason" to FieldValue.delete()
                )
            )
            Log.d(TAG, "✅ User batch update added")

            Log.d(TAG, "💾 Batch commit করছি...")
            batch.commit().await()

            Log.i(TAG, "🎉 KYC approval সফল!")
            Log.i(TAG, "✅ Request: $requestId approved by Admin: $adminId")

            Resource.Success("KYC request approved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in approveKycRequest: ${e.message}", e)
            Log.e(TAG, "🔍 Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "📍 Full stack: ${e.stackTraceToString()}")
            Resource.Error(e.message ?: "Failed to approve KYC request")
        }
    }

    override suspend fun rejectKycRequest(
        requestId: String,
        adminId: String,
        rejectionReason: String
    ): Resource<String> {
        Log.d(TAG, "❌ rejectKycRequest() শুরু")
        Log.d(TAG, "📋 Request ID: $requestId")
        Log.d(TAG, "👤 Admin ID: $adminId")
        Log.d(TAG, "📝 Rejection Reason: $rejectionReason")

        return try {
            Log.d(TAG, "📦 Batch operation শুরু করছি...")
            val batch = firestore.batch()

            Log.d(TAG, "🔍 KYC request ডকুমেন্ট খুঁজছি...")

            val kycDoc = firestore.collection(COLLECTION_KYC)
                .document(requestId)
                .get()
                .await()

            Log.d(TAG, "✅ Document পাওয়া গেছে - exists: ${kycDoc.exists()}")

            val kycRequest = kycDoc.toObject(KycRequest::class.java) ?: run {
                Log.e(TAG, "❌ KYC request parse ব্যর্থ")
                return Resource.Error("KYC request not found")
            }

            Log.d(TAG, "✨ Request object: $kycRequest")
            Log.d(TAG, "👥 User ID: ${kycRequest.userId}")

            val currentTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            Log.d(TAG, "⏰ Current Time: $currentTime")

            // Update KYC request status
            Log.d(TAG, "🔧 Updating KYC request status to REJECTED...")

            val kycRef = firestore.collection(COLLECTION_KYC).document(requestId)
            batch.update(
                kycRef,
                mapOf(
                    "status" to KycStatus.REJECTED.name,
                    "reviewedAt" to currentTime,
                    "reviewedBy" to adminId,
                    "rejectionReason" to rejectionReason
                )
            )
            Log.d(TAG, "✅ KYC request batch update added")

            // Update user's KYC status
            Log.d(TAG, "🔧 Updating user KYC status...")

            val userRef = firestore.collection(COLLECTION_USERS).document(kycRequest.userId)
            batch.update(
                userRef,
                mapOf(
                    "kycStatus" to KycStatus.REJECTED.name,
                    "kycRejectionReason" to rejectionReason,
                    "kycRequestId" to requestId,
                    "kycVerifiedAt" to FieldValue.delete()
                )
            )
            Log.d(TAG, "✅ User batch update added")

            Log.d(TAG, "💾 Batch commit করছি...")
            batch.commit().await()

            Log.i(TAG, "🎉 KYC rejection সফল!")
            Log.i(TAG, "❌ Request: $requestId rejected by Admin: $adminId")
            Log.i(TAG, "📝 Reason: $rejectionReason")

            Resource.Success("KYC request rejected")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in rejectKycRequest: ${e.message}", e)
            Log.e(TAG, "🔍 Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "📍 Full stack: ${e.stackTraceToString()}")
            Resource.Error(e.message ?: "Failed to reject KYC request")
        }
    }

    override suspend fun getPendingKycCount(): Flow<Resource<Int>> = callbackFlow {
        Log.d(TAG, "🔄 getPendingKycCount() শুরু")

        val listener = firestore.collection(COLLECTION_KYC)
            .whereEqualTo("status", KycStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error in getPendingKycCount: ${error.message}", error)
                    trySend(Resource.Error(error.message ?: "Failed"))
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                Log.d(TAG, "📊 Pending KYC count: $count")
                Log.d(TAG, "📋 Total documents in snapshot: ${snapshot?.size() ?: 0}")

                trySend(Resource.Success(count))
            }

        Log.d(TAG, "🎧 Listener added for pending count")

        awaitClose {
            Log.d(TAG, "🔌 Listener removed for pending count")
            listener.remove()
        }
    }
}