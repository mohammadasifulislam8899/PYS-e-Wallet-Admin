// admin_app/domain/models/PaymentMethod.kt
package com.droidnest.tech.pysadmin.domain.models

import com.google.firebase.Timestamp

data class WithdrawPaymentMethod(
    val id: String = "",
    val name: String = "",
    val icon: String = "💰",
    val type: String = "",
    val category: String = "mobile_banking",
    val country: String = "BD",
    val currency: String = "BDT",
    val minAmount: Double = 0.0,
    val maxAmount: Double = 50000.0,
    val processingTime: String = "",
    val enabled: Boolean = true,
    val fees: List<FeeRange> = emptyList(),
    val requiredFields: List<RequiredField> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "icon" to icon,
            "type" to type,
            "category" to category,
            "country" to country,
            "currency" to currency,
            "minAmount" to minAmount,
            "maxAmount" to maxAmount,
            "processingTime" to processingTime,
            "enabled" to enabled,
            "fees" to fees.map { it.toMap() },
            "requiredFields" to requiredFields.map { it.toMap() },
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

data class FeeRange(
    val min: Double = 0.0,
    val max: Double = 0.0,
    val fee: Double = 0.0,
    val type: String = "fixed"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "min" to min,
            "max" to max,
            "fee" to fee,
            "type" to type
        )
    }
}



enum class FieldType(val value: String) {
    TEXT("text"),
    NUMBER("number"),
    PHONE("phone"),
    DROPDOWN("dropdown"),
    RADIO("radio"),
    TEXTAREA("textarea");

    companion object {
        fun fromValue(value: String) = values().find { it.value == value } ?: TEXT
    }
}

enum class PaymentCategory(val value: String, val displayName: String, val icon: String) {
    MOBILE_BANKING("mobile_banking", "Mobile Banking", "📱"),
    BANK_TRANSFER("bank_transfer", "Bank Transfer", "🏦");

    companion object {
        fun fromValue(value: String) = values().find { it.value == value } ?: MOBILE_BANKING

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