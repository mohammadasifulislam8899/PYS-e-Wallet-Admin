// utils/AdminConstants.kt
package com.droidnest.tech.pysadmin.utils

object AdminConstants {
    
    // ═══════════════════════════════════════════════════════
    // 🔥 FIREBASE COLLECTIONS & DOCUMENTS
    // ═══════════════════════════════════════════════════════
    const val COLLECTION_SETTINGS = "settings"
    const val DOC_EXCHANGE_RATE = "exchange_rate"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ADMINS = "admins"
    const val COLLECTION_TRANSACTIONS = "transactions"
    
    // ═══════════════════════════════════════════════════════
    // 💱 EXCHANGE RATES
    // ═══════════════════════════════════════════════════════
    const val DEFAULT_EXCHANGE_RATE = 29.2  // 1 MYR = 29.2 BDT
    const val MIN_EXCHANGE_RATE = 20.0      // Minimum allowed rate
    const val MAX_EXCHANGE_RATE = 40.0      // Maximum allowed rate
    
    // ═══════════════════════════════════════════════════════
    // 💵 CURRENCIES
    // ═══════════════════════════════════════════════════════
    const val CURRENCY_BDT = "BDT"
    const val CURRENCY_MYR = "MYR"
    const val SYMBOL_BDT = "৳"
    const val SYMBOL_MYR = "RM"
    
    // ═══════════════════════════════════════════════════════
    // 👤 ADMIN ROLES
    // ═══════════════════════════════════════════════════════
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_SUPER_ADMIN = "SUPER_ADMIN"
    
    // ═══════════════════════════════════════════════════════
    // 📊 TRANSACTION STATUS
    // ═══════════════════════════════════════════════════════
    const val STATUS_PENDING = "PENDING"
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_COMPLETED = "COMPLETED"
    
    // ═══════════════════════════════════════════════════════
    // 📱 CONTACT
    // ═══════════════════════════════════════════════════════
    const val DEVELOPER_WHATSAPP = "+8801768773889"
    const val DEVELOPER_EMAIL = "droidnest.tech@gmail.com"
    
    // ═══════════════════════════════════════════════════════
    // 🎨 APP INFO
    // ═══════════════════════════════════════════════════════
    const val APP_NAME = "PYS Admin Panel"
    const val APP_VERSION = "1.0.0"
    const val APP_BUILD = "001"
    
    // ═══════════════════════════════════════════════════════
    // ⏰ TIME & DATE FORMATS
    // ═══════════════════════════════════════════════════════
    const val DATE_FORMAT = "dd MMM yyyy"
    const val TIME_FORMAT = "hh:mm a"
    const val DATE_TIME_FORMAT = "dd MMM yyyy, hh:mm a"
    
    // ═══════════════════════════════════════════════════════
    // 🎯 HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════
    
    /**
     * Get currency symbol
     */
    fun getCurrencySymbol(currency: String): String {
        return when (currency) {
            CURRENCY_BDT -> SYMBOL_BDT
            CURRENCY_MYR -> SYMBOL_MYR
            else -> ""
        }
    }
    
    /**
     * Format amount with currency
     */
    fun formatAmount(amount: Double, currency: String): String {
        val symbol = getCurrencySymbol(currency)
        return "$symbol${String.format("%,.2f", amount)}"
    }
    
    /**
     * Validate exchange rate
     */
    fun isValidExchangeRate(rate: Double): Boolean {
        return rate in MIN_EXCHANGE_RATE..MAX_EXCHANGE_RATE
    }
}