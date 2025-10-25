// domain/models/AddMoneyPaymentMethod.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class AddMoneyPaymentMethod(
    val id: String = "",
    val name: String = "",
    val icon: String = "💰",
    val type: PaymentType = PaymentType.MANUAL,
    val category: AddMoneyPaymentCategory = AddMoneyPaymentCategory.MOBILE_BANKING,
    val currency: String = "BDT",
    val country: String = "BD",

    // Admin Account Info (যেখানে user টাকা পাঠাবে)
    val accountNumber: String = "",
    val accountName: String = "",
    val accountType: String = "", // Personal/Agent/Merchant

    val isEnabled: Boolean = true,
    val minAmount: Double = 0.0,
    val maxAmount: Double = 50000.0,
    val dailyLimit: Double = 100000.0,

    val instructions: String = "",
    val priority: Int = 0,

    // Dynamic Fields - User যা fill করবে
    val requiredFields: List<RequiredField> = emptyList(),

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val createdBy: String = "",
    val updatedBy: String = ""
) {
    // ✅ Format account number (hide middle digits for security)
    val formattedAccountNumber: String
        get() {
            return if (accountNumber.length > 6) {
                val first = accountNumber.take(3)
                val last = accountNumber.takeLast(3)
                val middle = "*".repeat((accountNumber.length - 6).coerceAtMost(6))
                "$first$middle$last"
            } else {
                accountNumber
            }
        }

    // ✅ Convert to map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "icon" to icon,
            "type" to type.name,
            "category" to category.value,
            "currency" to currency,
            "country" to country,
            "accountNumber" to accountNumber,
            "accountName" to accountName,
            "accountType" to accountType,
            "isEnabled" to isEnabled,
            "minAmount" to minAmount,
            "maxAmount" to maxAmount,
            "dailyLimit" to dailyLimit,
            "instructions" to instructions,
            "priority" to priority,
            "requiredFields" to requiredFields.map { it.toMap() },
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "createdBy" to createdBy,
            "updatedBy" to updatedBy
        )
    }
}

enum class PaymentType {
    MANUAL,      // Admin manually verify করবে
    AUTOMATIC    // Auto verify (future)
}

enum class AddMoneyPaymentCategory(val value: String, val displayName: String, val icon: String) {
    MOBILE_BANKING("mobile_banking", "Mobile Banking", "📱"),
    BANK_TRANSFER("bank_transfer", "Bank Transfer", "🏦");

    companion object {
        fun fromValue(value: String) = values().find { it.value == value } ?: MOBILE_BANKING

        // ✅ Default fields for Add Money (user input করবে)
        fun getDefaultRequiredFieldsForAddMoney(category: String, country: String): List<RequiredField> {
            return when (category) {
                "mobile_banking" -> {
                    when (country) {
                        "BD" -> listOf(
                            RequiredField(
                                fieldName = "senderNumber",
                                label = "Your Mobile Number",
                                placeholder = "01XXXXXXXXX",
                                type = FieldType.PHONE,
                                required = true
                            ),
                            RequiredField(
                                fieldName = "senderAccountType",
                                label = "Your Account Type",
                                placeholder = "Select type",
                                type = FieldType.DROPDOWN,
                                options = listOf("Personal", "Agent", "Merchant"),
                                required = true
                            ),
                            RequiredField(
                                fieldName = "transactionId",
                                label = "Transaction ID (TrxID)",
                                placeholder = "e.g., BGD1234ABCD",
                                type = FieldType.TEXT,
                                required = true
                            )
                        )
                        "MY" -> listOf(
                            RequiredField(
                                fieldName = "senderNumber",
                                label = "Your Mobile Number",
                                placeholder = "+60XXXXXXXXX",
                                type = FieldType.PHONE,
                                required = true
                            ),
                            RequiredField(
                                fieldName = "transactionId",
                                label = "Transaction ID",
                                placeholder = "Enter TrxID",
                                type = FieldType.TEXT,
                                required = true
                            )
                        )
                        else -> emptyList()
                    }
                }
                "bank_transfer" -> listOf(
                    RequiredField(
                        fieldName = "senderBankName",
                        label = "Your Bank Name",
                        placeholder = "e.g., DBBL, Brac Bank",
                        type = FieldType.TEXT,
                        required = true
                    ),
                    RequiredField(
                        fieldName = "senderAccountNumber",
                        label = "Your Account Number",
                        placeholder = "Enter your account number",
                        type = FieldType.NUMBER,
                        required = true
                    ),
                    RequiredField(
                        fieldName = "senderAccountHolder",
                        label = "Your Account Holder Name",
                        placeholder = "As per bank account",
                        type = FieldType.TEXT,
                        required = true
                    ),
                    RequiredField(
                        fieldName = "transactionId",
                        label = "Transaction/Reference ID",
                        placeholder = "Bank transaction ID",
                        type = FieldType.TEXT,
                        required = true
                    )
                )
                else -> emptyList()
            }
        }

        // ✅ Default fields for Withdraw (এটা আগের withdraw এর জন্য)
        fun getDefaultRequiredFields(category: String, country: String): List<RequiredField> {
            return when (category) {
                "mobile_banking" -> {
                    when (country) {
                        "BD" -> listOf(
                            RequiredField(
                                fieldName = "phoneNumber",
                                label = "Phone Number",
                                placeholder = "01XXXXXXXXX",
                                type = FieldType.PHONE
                            ),
                            RequiredField(
                                fieldName = "accountType",
                                label = "Account Type",
                                placeholder = "Select type",
                                type = FieldType.DROPDOWN,
                                options = listOf("Personal", "Agent")
                            )
                        )
                        "MY" -> listOf(
                            RequiredField(
                                fieldName = "phoneNumber",
                                label = "Phone Number",
                                placeholder = "+60XXXXXXXXX",
                                type = FieldType.PHONE
                            ),
                            RequiredField(
                                fieldName = "notes",
                                label = "Notes",
                                placeholder = "Additional information",
                                type = FieldType.TEXTAREA,
                                required = false
                            )
                        )
                        else -> emptyList()
                    }
                }
                "bank_transfer" -> listOf(
                    RequiredField(
                        fieldName = "bankName",
                        label = "Bank Name",
                        placeholder = "e.g., Dutch Bangla Bank",
                        type = FieldType.TEXT
                    ),
                    RequiredField(
                        fieldName = "accountNumber",
                        label = "Account Number",
                        placeholder = "Enter account number",
                        type = FieldType.NUMBER
                    ),
                    RequiredField(
                        fieldName = "accountHolderName",
                        label = "Account Holder Name",
                        placeholder = "Enter account holder name",
                        type = FieldType.TEXT
                    )
                )
                else -> emptyList()
            }
        }
    }
}

// ✅ Required Field Model
data class RequiredField(
    val fieldName: String = "",
    val label: String = "",
    val placeholder: String = "",
    val type: FieldType = FieldType.TEXT,
    val options: List<String> = emptyList(),
    val required: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "fieldName" to fieldName,
        "label" to label,
        "placeholder" to placeholder,
        "type" to type.value,
        "options" to options,
        "required" to required
    )
}
