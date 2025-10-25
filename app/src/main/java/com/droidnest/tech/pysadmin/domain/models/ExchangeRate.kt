// domain/models/ExchangeRate.kt
package com.droidnest.tech.pysadmin.domain.models

import com.droidnest.tech.pysadmin.utils.AdminConstants
import com.google.firebase.Timestamp

data class ExchangeRate(
    val fromCurrency: String = "MYR",
    val toCurrency: String = "BDT",
    val rate: Double = AdminConstants.DEFAULT_EXCHANGE_RATE,  // ✅ Use constant
    val symbol: String = AdminConstants.SYMBOL_BDT,          // ✅ Use constant
    val lastUpdated: Timestamp = Timestamp.now(),
    val updatedBy: String = "System"                         // ✅ Default value
) {
    companion object {
        /**
         * Get default exchange rate when Firestore fails
         */
        fun getDefault() = ExchangeRate(
            fromCurrency = "MYR",
            toCurrency = "BDT",
            rate = AdminConstants.DEFAULT_EXCHANGE_RATE,
            symbol = AdminConstants.SYMBOL_BDT,
            lastUpdated = Timestamp.now(),
            updatedBy = "System (Default)"
        )
    }

    /**
     * Convert MYR to BDT
     */
    fun convertMyrToBdt(myrAmount: Double): Double {
        return myrAmount * rate
    }

    /**
     * Convert BDT to MYR
     */
    fun convertBdtToMyr(bdtAmount: Double): Double {
        return if (rate > 0) bdtAmount / rate else 0.0
    }

    /**
     * Get formatted rate string
     * Example: "1 MYR = ৳29.50"
     */
    fun getFormattedRate(): String {
        return "1 $fromCurrency = $symbol${String.format("%.2f", rate)}"
    }

    /**
     * Get last updated time as readable string
     */
    fun getFormattedLastUpdated(): String {
        return try {
            val date = lastUpdated.toDate()
            val format = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Check if rate is within valid range
     */
    fun isValidRate(): Boolean {
        return rate in AdminConstants.MIN_EXCHANGE_RATE..AdminConstants.MAX_EXCHANGE_RATE
    }

    /**
     * Get rate change percentage from default
     */
    fun getRateChangePercent(): Double {
        val defaultRate = AdminConstants.DEFAULT_EXCHANGE_RATE
        return if (defaultRate > 0) {
            ((rate - defaultRate) / defaultRate) * 100
        } else {
            0.0
        }
    }
}