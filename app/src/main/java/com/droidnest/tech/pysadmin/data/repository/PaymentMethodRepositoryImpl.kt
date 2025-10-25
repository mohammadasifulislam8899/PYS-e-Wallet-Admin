// admin_app/data/repository/PaymentMethodRepositoryImpl.kt
package com.droidnest.tech.pysadmin.data.repository

import android.util.Log
import com.droidnest.tech.pysadmin.domain.models.FeeRange
import com.droidnest.tech.pysadmin.domain.models.FieldType
import com.droidnest.tech.pysadmin.domain.models.RequiredField
import com.droidnest.tech.pysadmin.domain.models.WithdrawPaymentMethod
import com.droidnest.tech.pysadmin.domain.repository.PaymentMethodRepository
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PaymentMethodRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PaymentMethodRepository {

    private val collectionPath = "settings"
    private val documentId = "withdraw_methods"

    override suspend fun getAllPaymentMethods(): Flow<Resource<List<WithdrawPaymentMethod>>> = callbackFlow {
        trySend(Resource.Loading())

        Log.d("PaymentMethodRepo", "📋 Loading payment methods...")

        val listener = firestore.collection(collectionPath)
            .document(documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PaymentMethodRepo", "❌ Error: ${error.message}")
                    trySend(Resource.Error(error.message ?: "Failed to load payment methods"))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val methods = mutableListOf<WithdrawPaymentMethod>()

                    snapshot.data?.forEach { (methodId, methodData) ->
                        try {
                            val data = methodData as? Map<String, Any> ?: return@forEach

                            val feesData = data["fees"] as? List<Map<String, Any>> ?: emptyList()
                            val fees = feesData.map { feeMap ->
                                FeeRange(
                                    min = (feeMap["min"] as? Number)?.toDouble() ?: 0.0,
                                    max = (feeMap["max"] as? Number)?.toDouble() ?: 0.0,
                                    fee = (feeMap["fee"] as? Number)?.toDouble() ?: 0.0,
                                    type = feeMap["type"] as? String ?: "fixed"
                                )
                            }

                            val requiredFields = parseRequiredFields(data["requiredFields"])

                            val method = WithdrawPaymentMethod(
                                id = data["id"] as? String ?: methodId,
                                name = data["name"] as? String ?: "",
                                icon = data["icon"] as? String ?: "💰",
                                type = data["type"] as? String ?: "withdraw",
                                category = data["category"] as? String ?: "mobile_banking",
                                country = data["country"] as? String ?: "BD",
                                currency = data["currency"] as? String ?: "BDT",
                                minAmount = (data["minAmount"] as? Number)?.toDouble() ?: 0.0,
                                maxAmount = (data["maxAmount"] as? Number)?.toDouble() ?: 50000.0,
                                processingTime = data["processingTime"] as? String ?: "",
                                enabled = data["enabled"] as? Boolean ?: true,
                                fees = fees,
                                requiredFields = requiredFields,
                                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                            )

                            methods.add(method)

                        } catch (e: Exception) {
                            Log.e("PaymentMethodRepo", "❌ Parse error for $methodId: ${e.message}")
                        }
                    }

                    Log.d("PaymentMethodRepo", "✅ Loaded ${methods.size} payment methods")
                    trySend(Resource.Success(methods))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getPaymentMethodById(id: String): Resource<WithdrawPaymentMethod?> {
        return try {
            Log.d("PaymentMethodRepo", "🔍 Loading method: $id")

            val doc = firestore.collection(collectionPath)
                .document(documentId)
                .get()
                .await()

            if (!doc.exists()) {
                return Resource.Error("Payment methods not found")
            }

            val methodData = doc.data?.get(id) as? Map<String, Any>

            if (methodData == null) {
                return Resource.Error("Payment method not found")
            }

            val feesData = methodData["fees"] as? List<Map<String, Any>> ?: emptyList()
            val fees = feesData.map { feeMap ->
                FeeRange(
                    min = (feeMap["min"] as? Number)?.toDouble() ?: 0.0,
                    max = (feeMap["max"] as? Number)?.toDouble() ?: 0.0,
                    fee = (feeMap["fee"] as? Number)?.toDouble() ?: 0.0,
                    type = feeMap["type"] as? String ?: "fixed"
                )
            }

            val requiredFields = parseRequiredFields(methodData["requiredFields"])

            val method = WithdrawPaymentMethod(
                id = methodData["id"] as? String ?: id,
                name = methodData["name"] as? String ?: "",
                icon = methodData["icon"] as? String ?: "💰",
                type = methodData["type"] as? String ?: "withdraw",
                category = methodData["category"] as? String ?: "mobile_banking",
                country = methodData["country"] as? String ?: "BD",
                currency = methodData["currency"] as? String ?: "BDT",
                minAmount = (methodData["minAmount"] as? Number)?.toDouble() ?: 0.0,
                maxAmount = (methodData["maxAmount"] as? Number)?.toDouble() ?: 50000.0,
                processingTime = methodData["processingTime"] as? String ?: "",
                enabled = methodData["enabled"] as? Boolean ?: true,
                fees = fees,
                requiredFields = requiredFields,
                createdAt = methodData["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = methodData["updatedAt"] as? Timestamp ?: Timestamp.now()
            )

            Log.d("PaymentMethodRepo", "✅ Method loaded: ${method.name}")
            Resource.Success(method)

        } catch (e: Exception) {
            Log.e("PaymentMethodRepo", "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to load payment method")
        }
    }

    override suspend fun addPaymentMethod(method: WithdrawPaymentMethod): Resource<String> {
        return try {
            Log.d("PaymentMethodRepo", "➕ Adding method: ${method.name}")

            val methodWithTimestamp = method.copy(
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            firestore.collection(collectionPath)
                .document(documentId)
                .update(mapOf(method.id to methodWithTimestamp.toMap()))
                .await()

            Log.d("PaymentMethodRepo", "✅ Method added successfully")
            Resource.Success("Payment method added successfully")

        } catch (e: Exception) {
            try {
                firestore.collection(collectionPath)
                    .document(documentId)
                    .set(mapOf(method.id to method.toMap()))
                    .await()

                Log.d("PaymentMethodRepo", "✅ Method added successfully (new doc)")
                Resource.Success("Payment method added successfully")
            } catch (e2: Exception) {
                Log.e("PaymentMethodRepo", "❌ Error: ${e2.message}")
                Resource.Error(e2.message ?: "Failed to add payment method")
            }
        }
    }

    override suspend fun updatePaymentMethod(method: WithdrawPaymentMethod): Resource<String> {
        return try {
            Log.d("PaymentMethodRepo", "🔄 Updating method: ${method.name}")

            val methodWithTimestamp = method.copy(
                updatedAt = Timestamp.now()
            )

            firestore.collection(collectionPath)
                .document(documentId)
                .update(mapOf(method.id to methodWithTimestamp.toMap()))
                .await()

            Log.d("PaymentMethodRepo", "✅ Method updated successfully")
            Resource.Success("Payment method updated successfully")

        } catch (e: Exception) {
            Log.e("PaymentMethodRepo", "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to update payment method")
        }
    }

    override suspend fun deletePaymentMethod(id: String): Resource<String> {
        return try {
            Log.d("PaymentMethodRepo", "🗑️ Deleting method: $id")

            val updates = mapOf<String, Any?>(id to com.google.firebase.firestore.FieldValue.delete())

            firestore.collection(collectionPath)
                .document(documentId)
                .update(updates)
                .await()

            Log.d("PaymentMethodRepo", "✅ Method deleted successfully")
            Resource.Success("Payment method deleted successfully")

        } catch (e: Exception) {
            Log.e("PaymentMethodRepo", "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to delete payment method")
        }
    }

    override suspend fun togglePaymentMethodStatus(id: String, enabled: Boolean): Resource<String> {
        return try {
            Log.d("PaymentMethodRepo", "🔄 Toggling method: $id to $enabled")

            firestore.collection(collectionPath)
                .document(documentId)
                .update(mapOf(
                    "$id.enabled" to enabled,
                    "$id.updatedAt" to Timestamp.now()
                ))
                .await()

            Log.d("PaymentMethodRepo", "✅ Method status updated")
            Resource.Success("Payment method status updated")

        } catch (e: Exception) {
            Log.e("PaymentMethodRepo", "❌ Error: ${e.message}")
            Resource.Error(e.message ?: "Failed to update payment method status")
        }
    }

    private fun parseRequiredFields(data: Any?): List<RequiredField> {
        val fieldsData = data as? List<Map<String, Any>> ?: return emptyList()
        return fieldsData.map { fieldMap ->
            RequiredField(
                fieldName = fieldMap["fieldName"] as? String ?: "",
                label = fieldMap["label"] as? String ?: "",
                placeholder = fieldMap["placeholder"] as? String ?: "",
                type = FieldType.fromValue(fieldMap["type"] as? String ?: "text"),
                options = (fieldMap["options"] as? List<String>) ?: emptyList(),
                required = fieldMap["required"] as? Boolean ?: true
            )
        }
    }
}