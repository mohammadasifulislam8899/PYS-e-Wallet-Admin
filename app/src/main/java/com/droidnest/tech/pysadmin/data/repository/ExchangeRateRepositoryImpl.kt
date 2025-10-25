package com.droidnest.tech.pysadmin.data.repository

import com.droidnest.tech.pysadmin.domain.models.ExchangeRate
import com.droidnest.tech.pysadmin.domain.repository.ExchangeRateRepository
import com.droidnest.tech.pysadmin.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExchangeRateRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExchangeRateRepository {

    private val settingsCollection = firestore.collection("settings")
    private val exchangeRateDoc = settingsCollection.document("exchange_rates")

    override fun getExchangeRate(): Flow<Resource<ExchangeRate>> = callbackFlow {
        trySend(Resource.Loading())

        val listener = exchangeRateDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Unknown error"))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    // ✅ Parse the rates map
                    val ratesMap = snapshot.get("rates") as? Map<String, Any> ?: mapOf()
                    val myrRate = (ratesMap["MYR"] as? Number)?.toDouble() ?: 30.0
                    val bdtRate = (ratesMap["BDT"] as? Number)?.toDouble() ?: 1.0

                    // ✅ Create ExchangeRate object
                    val exchangeRate = ExchangeRate(
                        fromCurrency = "MYR",
                        toCurrency = "BDT",
                        rate = myrRate / bdtRate,  // Calculate actual rate (30/1 = 30)
                        symbol = "৳",
                        lastUpdated = snapshot.getTimestamp("lastUpdated") ?: Timestamp.now(),
                        updatedBy = snapshot.getString("updatedBy") ?: ""
                    )

                    trySend(Resource.Success(exchangeRate))
                } catch (e: Exception) {
                    trySend(Resource.Error(e.message ?: "Failed to parse data"))
                }
            } else {
                // ✅ Return default rate if document doesn't exist
                trySend(Resource.Success(ExchangeRate()))
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun updateExchangeRate(
        myrRate: Double,
        adminName: String
    ): Resource<Unit> {
        return try {
            // ✅ Save in Firestore format (map structure)
            val data = hashMapOf(
                "rates" to hashMapOf(
                    "BDT" to 1.0,
                    "MYR" to myrRate
                ),
                "lastUpdated" to Timestamp.now(),
                "updatedBy" to adminName
            )

            exchangeRateDoc.set(data).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update exchange rate")
        }
    }
}