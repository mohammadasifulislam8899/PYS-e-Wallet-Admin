// data/repository/AddMoneyPaymentMethodRepositoryImpl.kt
package com.droidnest.tech.pysadmin.data.repository

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.*
import com.droidnest.tech.pysadmin.domain.repository.AddMoneyPaymentMethodRepository
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AddMoneyPaymentMethodRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AddMoneyPaymentMethodRepository {

    companion object {
        private const val TAG = "AddMoneyPaymentRepo"
        private const val COLLECTION = "payment_methods"
    }

    override fun getAllPaymentMethods(): Flow<Resource<List<AddMoneyPaymentMethod>>> = callbackFlow {
        Log.d(TAG, "🔄 Loading all payment methods")
        trySend(Resource.Loading())

        val listener = firestore.collection(COLLECTION)
            .orderBy("priority", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error: ${error.message}")
                    trySend(Resource.Error(error.message ?: "Failed to load"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val methods = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            parsePaymentMethod(doc.id, data)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Parse error for ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    Log.d(TAG, "✅ Loaded ${methods.size} methods")
                    trySend(Resource.Success(methods))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }

        awaitClose {
            Log.d(TAG, "🔇 Removing listener")
            listener.remove()
        }
    }

    override fun getPaymentMethodsByCurrency(currency: String): Flow<Resource<List<AddMoneyPaymentMethod>>> = callbackFlow {
        trySend(Resource.Loading())

        val listener = firestore.collection(COLLECTION)
            .whereEqualTo("currency", currency)
            .orderBy("priority", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed"))
                    return@addSnapshotListener
                }

                val methods = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parsePaymentMethod(doc.id, doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Resource.Success(methods))
            }

        awaitClose { listener.remove() }
    }

    override fun getEnabledPaymentMethods(): Flow<Resource<List<AddMoneyPaymentMethod>>> = callbackFlow {
        trySend(Resource.Loading())

        val listener = firestore.collection(COLLECTION)
            .whereEqualTo("isEnabled", true)
            .orderBy("priority", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed"))
                    return@addSnapshotListener
                }

                val methods = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parsePaymentMethod(doc.id, doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Resource.Success(methods))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addPaymentMethod(paymentMethod: AddMoneyPaymentMethod): Resource<String> {
        return try {
            Log.d(TAG, "➕ Adding: ${paymentMethod.name}")

            val adminId = auth.currentUser?.uid ?: return Resource.Error("Not authenticated")

            val methodWithMetadata = paymentMethod.copy(
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                createdBy = adminId,
                updatedBy = adminId
            )

            firestore.collection(COLLECTION)
                .add(methodWithMetadata.toMap())
                .await()

            Log.d(TAG, "✅ Added successfully")
            Resource.Success("Payment method added successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to add")
        }
    }

    override suspend fun updatePaymentMethod(paymentMethod: AddMoneyPaymentMethod): Resource<String> {
        return try {
            Log.d(TAG, "🔄 Updating: ${paymentMethod.id}")

            val adminId = auth.currentUser?.uid ?: return Resource.Error("Not authenticated")

            val updates = paymentMethod.copy(
                updatedAt = Timestamp.now(),
                updatedBy = adminId
            ).toMap()

            firestore.collection(COLLECTION)
                .document(paymentMethod.id)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Updated")
            Resource.Success("Payment method updated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to update")
        }
    }

    override suspend fun deletePaymentMethod(id: String): Resource<String> {
        return try {
            Log.d(TAG, "🗑️ Deleting: $id")

            firestore.collection(COLLECTION)
                .document(id)
                .delete()
                .await()

            Log.d(TAG, "✅ Deleted")
            Resource.Success("Payment method deleted successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to delete")
        }
    }

    override suspend fun togglePaymentMethod(id: String, isEnabled: Boolean): Resource<String> {
        return try {
            Log.d(TAG, "🔄 Toggle: $id -> $isEnabled")

            val adminId = auth.currentUser?.uid ?: return Resource.Error("Not authenticated")

            firestore.collection(COLLECTION)
                .document(id)
                .update(
                    mapOf(
                        "isEnabled" to isEnabled,
                        "updatedAt" to Timestamp.now(),
                        "updatedBy" to adminId
                    )
                )
                .await()

            val status = if (isEnabled) "enabled" else "disabled"
            Log.d(TAG, "✅ Payment method $status")
            Resource.Success("Payment method $status successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to toggle")
        }
    }

    // ✅ Updated: PaymentCategory → AddMoneyPaymentCategory
    private fun parsePaymentMethod(id: String, data: Map<String, Any>): AddMoneyPaymentMethod {
        val requiredFieldsData = data["requiredFields"] as? List<Map<String, Any>> ?: emptyList()
        val requiredFields = requiredFieldsData.map { fieldMap ->
            RequiredField(
                fieldName = fieldMap["fieldName"] as? String ?: "",
                label = fieldMap["label"] as? String ?: "",
                placeholder = fieldMap["placeholder"] as? String ?: "",
                type = FieldType.fromValue(fieldMap["type"] as? String ?: "text"),
                options = (fieldMap["options"] as? List<String>) ?: emptyList(),
                required = fieldMap["required"] as? Boolean ?: true
            )
        }

        return AddMoneyPaymentMethod(
            id = id,
            name = data["name"] as? String ?: "",
            icon = data["icon"] as? String ?: "💰",
            type = PaymentType.valueOf(data["type"] as? String ?: "MANUAL"),
            category = AddMoneyPaymentCategory.fromValue(data["category"] as? String ?: "mobile_banking"), // ✅ Changed
            currency = data["currency"] as? String ?: "BDT",
            country = data["country"] as? String ?: "BD",
            accountNumber = data["accountNumber"] as? String ?: "",
            accountName = data["accountName"] as? String ?: "",
            accountType = data["accountType"] as? String ?: "",
            isEnabled = data["isEnabled"] as? Boolean ?: true,
            minAmount = (data["minAmount"] as? Number)?.toDouble() ?: 0.0,
            maxAmount = (data["maxAmount"] as? Number)?.toDouble() ?: 50000.0,
            dailyLimit = (data["dailyLimit"] as? Number)?.toDouble() ?: 100000.0,
            instructions = data["instructions"] as? String ?: "",
            priority = (data["priority"] as? Number)?.toInt() ?: 0,
            requiredFields = requiredFields,
            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now(),
            createdBy = data["createdBy"] as? String ?: "",
            updatedBy = data["updatedBy"] as? String ?: ""
        )
    }
}