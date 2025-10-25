// utils/Constants.kt
package com.droidnest.tech.pysadmin.utils

object Constants {
    
    // ═══════════════════════════════════════════════════════
    // 🔥 FIREBASE COLLECTIONS
    // ═══════════════════════════════════════════════════════
    const val COLLECTION_USERS = "users"
    const val COLLECTION_TRANSACTIONS = "transactions"
    const val COLLECTION_SETTINGS = "settings"
    const val COLLECTION_ADMINS = "admins"
    const val COLLECTION_WITHDRAW_METHODS = "withdraw_methods"
    
    // ═══════════════════════════════════════════════════════
    // 📄 FIREBASE DOCUMENTS
    // ═══════════════════════════════════════════════════════
    const val DOC_EXCHANGE_RATE = "exchange_rates"
    
    // ═══════════════════════════════════════════════════════
    // 💱 EXCHANGE RATES
    // ═══════════════════════════════════════════════════════
    const val DEFAULT_EXCHANGE_RATE = 29.2  // ✅ 1 MYR = 29.2 BDT
    
    // ═══════════════════════════════════════════════════════
    // 🔐 PIN & SECURITY
    // ═══════════════════════════════════════════════════════
    const val PIN_LENGTH = 6
    const val PIN_RETRY_LIMIT = 3
    const val ACCOUNT_LOCKOUT_DURATION_MINUTES = 30L
    
    // ═══════════════════════════════════════════════════════
    // 💰 TRANSACTION LIMITS
    // ═══════════════════════════════════════════════════════
    const val MIN_TRANSACTION_AMOUNT = 10.0
    const val MAX_TRANSACTION_AMOUNT_BDT = 100000.0
    const val MAX_TRANSACTION_AMOUNT_MYR = 5000.0
    
    // ═══════════════════════════════════════════════════════
    // 📱 PHONE NUMBERS
    // ═══════════════════════════════════════════════════════
    const val SUPPORT_WHATSAPP_NUMBER = "+601112638451"  // User support
    const val DEVELOPER_WHATSAPP_NUMBER = "+8801768773889"  // Developer support
    
    // ═══════════════════════════════════════════════════════
    // 🌍 COUNTRIES
    // ═══════════════════════════════════════════════════════
    const val COUNTRY_BANGLADESH = "Bangladesh"
    const val COUNTRY_MALAYSIA = "Malaysia"
    const val PHONE_PREFIX_BD = "+880"
    const val PHONE_PREFIX_MY = "+60"
    
    // ═══════════════════════════════════════════════════════
    // 💵 CURRENCIES
    // ═══════════════════════════════════════════════════════
    const val CURRENCY_BDT = "BDT"
    const val CURRENCY_MYR = "MYR"
    const val SYMBOL_BDT = "৳"
    const val SYMBOL_MYR = "RM"
    
    // ═══════════════════════════════════════════════════════
    // 📊 TRANSACTION STATUS
    // ═══════════════════════════════════════════════════════
    const val STATUS_PENDING = "PENDING"
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_CANCELLED = "CANCELLED"
    
    // ═══════════════════════════════════════════════════════
    // 📝 TRANSACTION TYPES
    // ═══════════════════════════════════════════════════════
    const val TYPE_ADD_MONEY = "ADD_MONEY"
    const val TYPE_WITHDRAW = "WITHDRAW"
    const val TYPE_SEND_MONEY = "SEND_MONEY"
    const val TYPE_RECEIVE_MONEY = "RECEIVE_MONEY"
    
    // ═══════════════════════════════════════════════════════
    // 👤 USER ROLES
    // ═══════════════════════════════════════════════════════
    const val ROLE_USER = "USER"
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_SUPER_ADMIN = "SUPER_ADMIN"
    
    // ═══════════════════════════════════════════════════════
    // 📸 IMAGE & FILE
    // ═══════════════════════════════════════════════════════
    const val MAX_IMAGE_SIZE_MB = 5
    const val ALLOWED_IMAGE_TYPES = "image/jpeg,image/png"
    
    // ═══════════════════════════════════════════════════════
    // ⏰ TIME & DATE
    // ═══════════════════════════════════════════════════════
    const val DATE_FORMAT = "dd MMM yyyy"
    const val TIME_FORMAT = "hh:mm a"
    const val DATE_TIME_FORMAT = "dd MMM yyyy, hh:mm a"
    
    // ═══════════════════════════════════════════════════════
    // 🔔 NOTIFICATIONS
    // ═══════════════════════════════════════════════════════
    const val NOTIFICATION_CHANNEL_ID = "pys_wallet_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "PYS Wallet Notifications"
    
    // ═══════════════════════════════════════════════════════
    // 🎨 APP INFO
    // ═══════════════════════════════════════════════════════
    const val APP_NAME = "PYS e-Wallet"
    const val APP_VERSION = "1.0.0"
    const val APP_BUILD = "001"
    const val DEVELOPER_NAME = "DroidNest Technologies"
    const val DEVELOPER_EMAIL = "droidnest.tech@gmail.com"
    
    // ═══════════════════════════════════════════════════════
    // 🔗 URLS
    // ═══════════════════════════════════════════════════════
    const val PRIVACY_POLICY_URL = "https://pyswallet.com/privacy"
    const val TERMS_URL = "https://pyswallet.com/terms"
    const val HELP_CENTER_URL = "https://pyswallet.com/help"
    
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
     * Get phone prefix
     */
    fun getPhonePrefix(country: String): String {
        return when (country) {
            COUNTRY_BANGLADESH -> PHONE_PREFIX_BD
            COUNTRY_MALAYSIA -> PHONE_PREFIX_MY
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
     * Get max transaction amount for currency
     */
    fun getMaxTransactionAmount(currency: String): Double {
        return when (currency) {
            CURRENCY_BDT -> MAX_TRANSACTION_AMOUNT_BDT
            CURRENCY_MYR -> MAX_TRANSACTION_AMOUNT_MYR
            else -> 0.0
        }
    }
}