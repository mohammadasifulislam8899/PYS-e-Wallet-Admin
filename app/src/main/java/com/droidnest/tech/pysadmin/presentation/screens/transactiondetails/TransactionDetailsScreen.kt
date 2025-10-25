// admin_app/presentation/screens/transactiondetails/TransactionDetailsScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.transactiondetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// ========================================
// MAIN SCREEN
// ========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    // Load transaction
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transaction Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // ✅ Admin Action Buttons in Bottom Bar
            state.transaction?.let { transaction ->
                AdminActionButtons(
                    transaction = transaction,
                    isUpdating = state.isUpdating,
                    onApprove = { viewModel.approveTransaction() },
                    onReject = { viewModel.rejectTransaction() }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.transaction == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Transaction not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    TransactionDetailsContent(transaction = state.transaction!!)
                }
            }
        }
    }
}

// ========================================
// ✅ HELPER FUNCTIONS
// ========================================

private fun getFieldsData(transaction: TransactionModel): Map<String, String> {
    return when (transaction.type) {
        "add_money" -> getMergedFieldsForAddMoney(transaction)
        "withdraw" -> getFieldsForWithdraw(transaction)
        "send_money" -> getFieldsForSendMoney(transaction)
        else -> emptyMap()
    }
}

private fun getMergedFieldsForAddMoney(transaction: TransactionModel): Map<String, String> {
    val fields = mutableMapOf<String, String>()

    // 1. Add payment method info first (so it shows at top)
    if (!transaction.paymentMethod.isNullOrBlank()) {
        fields["paymentMethodName"] = transaction.paymentMethod
    }

    // 2. Add admin account number (where user sent money)
    if (!transaction.accountNumber.isNullOrBlank()) {
        fields["adminAccountNumber"] = transaction.accountNumber
    }

    // 3. Add user provided fields
    fields.putAll(transaction.userProvidedFields)

    // 4. Backward compatibility: old transactionId field
    if (!transaction.transactionId.isNullOrBlank() && !fields.containsKey("transactionId")) {
        fields["transactionId"] = transaction.transactionId
    }

    return fields
}

private fun getFieldsForWithdraw(transaction: TransactionModel): Map<String, String> {
    val fields = mutableMapOf<String, String>()

    // ✅ 1. Add Withdrawal Method Name FIRST (so it shows at top)
    if (!transaction.paymentMethod.isNullOrBlank()) {
        fields["withdrawMethod"] = transaction.paymentMethod
    }

    // ✅ 2. Then add userProvidedFields (user's account details)
    if (transaction.userProvidedFields.isNotEmpty()) {
        fields.putAll(transaction.userProvidedFields)
    }

    // ✅ 3. Backward compatibility: old accountNumber field
    if (!transaction.accountNumber.isNullOrBlank() &&
        !fields.containsKey("accountNumber") &&
        !fields.containsKey("phoneNumber")) {
        fields["accountNumber"] = transaction.accountNumber
    }

    // ✅ 4. Backward compatibility: old transactionId field
    if (!transaction.transactionId.isNullOrBlank() && !fields.containsKey("transactionId")) {
        fields["transactionId"] = transaction.transactionId
    }

    return fields
}

private fun getFieldsForSendMoney(transaction: TransactionModel): Map<String, String> {
    val fields = mutableMapOf<String, String>()

    // Add user provided fields
    fields.putAll(transaction.userProvidedFields)

    // If no fields, show default info
    if (fields.isEmpty()) {
        fields["transferType"] = "Internal Transfer"
        if (!transaction.message.isNullOrBlank()) {
            fields["note"] = transaction.message
        }
    }

    return fields
}

// ========================================
// TRANSACTION DETAILS CONTENT
// ========================================

@Composable
fun TransactionDetailsContent(transaction: TransactionModel) {
    // ✅ Get fields data for current transaction type
    val fieldsData = remember(transaction) {
        getFieldsData(transaction)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Status Header Card
        TransactionStatusHeader(transaction)

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Card
        TransactionAmountCard(transaction)

        Spacer(modifier = Modifier.height(16.dp))

        // Smart Deduction Info (if withdraw with mixed/conversion)
        if (transaction.type == "withdraw" &&
            (transaction.bdtDeducted > 0.0 || transaction.myrDeducted > 0.0)) {
            SmartDeductionInfoCard(transaction)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ✅✅✅ Dynamic Fields Card (for withdraw, add_money, send_money) ✅✅✅
        if ((transaction.type == "withdraw" ||
                    transaction.type == "add_money" ||
                    transaction.type == "send_money") &&
            fieldsData.isNotEmpty()) {
            DynamicFieldsCard(transaction, fieldsData)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sender Details
        if (!transaction.senderName.isNullOrEmpty() || !transaction.senderPhone.isNullOrEmpty()) {
            SenderInfoCard(transaction)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recipient Info (if Send Money)
        if (transaction.type == "send_money" && transaction.recipientId != null) {
            RecipientInfoCard(transaction)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Fee & Rate Details
        FeeRateDetailsCard(transaction)

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Payment Method Card (only if DynamicFieldsCard was NOT shown)
        if ((transaction.type != "withdraw" &&
                    transaction.type != "add_money" &&
                    transaction.type != "send_money") ||
            fieldsData.isEmpty()) {
            if (transaction.paymentMethod != null) {
                PaymentMethodCard(transaction)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Transaction Info
        TransactionInfoCard(transaction)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ========================================
// ✅✅✅ DYNAMIC FIELDS CARD ✅✅✅
// ========================================

@Composable
fun DynamicFieldsCard(
    transaction: TransactionModel,
    fieldsData: Map<String, String> = emptyMap()  // ✅ PARAMETER ADDED
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyToClipboard(label: String, text: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
    }

    // 🎨 Card color based on transaction type
    val cardColor = when (transaction.type) {
        "withdraw" -> Color(0xFFE8F5E9)      // Light green
        "add_money" -> Color(0xFFFFF3E0)     // Light orange
        "send_money" -> Color(0xFFE3F2FD)    // Light blue
        else -> Color(0xFFF5F5F5)
    }

    val iconColor = when (transaction.type) {
        "withdraw" -> Color(0xFF4CAF50)
        "add_money" -> Color(0xFFFF6F00)
        "send_money" -> Color(0xFF2196F3)
        else -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 📌 Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        "withdraw" -> Icons.Default.AccountBalance
                        "add_money" -> Icons.Default.Payment
                        "send_money" -> Icons.AutoMirrored.Filled.Send
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = when (transaction.type) {
                            "withdraw" -> "💸 User's Account Details"
                            "add_money" -> "💰 Payment Details"
                            "send_money" -> "📤 Transfer Details"
                            else -> "Payment Information"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Text(
                        text = when (transaction.type) {
                            "withdraw" -> "Where to send money"
                            "add_money" -> "Where user sent money"
                            "send_money" -> "Transfer information"
                            else -> "Transaction details"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = iconColor.copy(alpha = 0.7f)
                    )
                }
            }

            // Payment Method Badge
            if (!transaction.paymentMethod.isNullOrBlank()) {
                Surface(
                    color = iconColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconColor
                        )
                        Text(
                            text = transaction.paymentMethod.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }
            }

            HorizontalDivider(
                color = iconColor.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            // ✅✅✅ Display all fields from fieldsData parameter ✅✅✅
            if (fieldsData.isNotEmpty()) {
                fieldsData.forEach { (fieldName, fieldValue) ->
                    DynamicFieldRow(
                        label = formatFieldLabel(fieldName),
                        value = fieldValue,
                        iconColor = iconColor,
                        onCopy = {
                            copyToClipboard(formatFieldLabel(fieldName), fieldValue)
                        }
                    )
                }
            }

            // ⚠️ Info banner with type-specific message
            Surface(
                color = iconColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconColor
                    )
                    Text(
                        text = when (transaction.type) {
                            "withdraw" -> "⚠️ Send money TO this account after approval"
                            "add_money" -> "💵 User sent money to admin account shown above"
                            "send_money" -> "📲 Internal transfer between users"
                            else -> "Payment information provided by user"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = iconColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicFieldRow(
    label: String,
    value: String,
    iconColor: Color,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            // Copy button
            FilledTonalButton(
                onClick = onCopy,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = iconColor.copy(alpha = 0.2f),
                    contentColor = iconColor
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ========================================
// ✅ HELPER: Format Field Labels
// ========================================

private fun formatFieldLabel(fieldName: String): String {
    return when (fieldName) {
        // ✅ Add Money specific fields
        "paymentMethodName" -> "Payment Method"
        "adminAccountNumber" -> "Admin Account (where money sent)"

        // ✅ Withdraw specific fields
        "withdrawMethod" -> "Withdrawal Method"
        "phoneNumber" -> "Phone Number"
        "accountNumber" -> "Account Number"
        "accountType" -> "Account Type"
        "type" -> "Account Type"
        "bankName" -> "Bank Name"
        "accountHolderName" -> "Account Holder Name"

        // ✅ Send Money specific fields
        "transferType" -> "Transfer Type"
        "note" -> "Note/Message"

        // ✅ Common fields
        "transactionId" -> "Transaction ID"
        "walletAddress" -> "Wallet Address"
        "walletId" -> "Wallet ID"
        "network" -> "Network"
        "duitNow" -> "DuitNow ID"
        "notes" -> "Notes"
        "referenceNumber" -> "Reference Number"

        // ✅ Fallback for any unknown field
        else -> fieldName.replace(Regex("([A-Z])"), " $1").trim()
            .split(" ")
            .joinToString(" ") { it.capitalize(Locale.ROOT) }
    }
}

// ========================================
// ✅✅✅ ADMIN ACTION BUTTONS ✅✅✅
// ========================================

@Composable
fun AdminActionButtons(
    transaction: TransactionModel,
    isUpdating: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        when (transaction.status) {
            // ✅ SUCCESS: শুধু message দেখাবে - NO BUTTONS
            "success" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                        .padding(20.dp)
                        .padding(bottom = 50.dp)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transaction Approved",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = when (transaction.type) {
                                    "add_money" -> "Money added to user's wallet"
                                    "withdraw" -> "Withdrawal processed successfully"
                                    "send_money" -> "Money sent to recipient"
                                    else -> "Transaction completed"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // ❌ FAILED: শুধু message দেখাবে - NO BUTTONS
            "failed" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF44336).copy(alpha = 0.1f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Transaction Rejected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (transaction.type) {
                                "add_money" -> "Add money request rejected"
                                "withdraw" -> "Withdrawal rejected - Amount refunded"
                                "send_money" -> "Send money rejected - Refunded to sender"
                                else -> "Amount has been refunded"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336).copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ⏳ PENDING: Approve + Reject buttons দেখাবে
            "pending" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 50.dp)
                    ,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ℹ️ Warning Message
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (transaction.type) {
                                    "send_money" -> "Approve: Money → Recipient | Reject: Refund → Sender"
                                    "withdraw" -> "Approve: Confirm withdrawal | Reject: Refund to wallet"
                                    "add_money" -> "Approve: Add to wallet | Reject: Cancel request"
                                    else -> "Status change will update user balance"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // 🔄 Smart Deduction Warning (for withdraw)
                    if (transaction.type == "withdraw" &&
                        transaction.deductionStrategy == "mixed") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Smart Deduction Applied:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "BDT: ${String.format("%.2f", transaction.bdtDeducted)} | " +
                                            "MYR: ${String.format("%.2f", transaction.myrDeducted)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ If rejected, both amounts will be refunded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 🎯 Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ❌ Reject Button
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !isUpdating,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFFF44336),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reject",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // ✅ Approve Button
                        Button(
                            onClick = onApprove,
                            enabled = !isUpdating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Approve",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🔄 Other statuses
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(bottom = 50.dp)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Status: ${transaction.status.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ========================================
// STATUS HEADER CARD
// ========================================

@Composable
fun TransactionStatusHeader(transaction: TransactionModel) {
    val statusColor = when (transaction.status) {
        "completed", "success" -> Color(0xFF4CAF50)
        "pending" -> Color(0xFFFF9800)
        "failed" -> Color(0xFFF44336)
        "processing" -> Color(0xFF2196F3)
        else -> Color.Gray
    }

    val statusIcon = when (transaction.status) {
        "completed", "success" -> Icons.Default.CheckCircle
        "pending" -> Icons.Default.Schedule
        "failed" -> Icons.Default.Cancel
        "processing" -> Icons.Default.Sync
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.15f),
                            statusColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = statusColor
                )
                Text(
                    text = transaction.status.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = transaction.type.replace("_", " ").uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========================================
// AMOUNT CARD
// ========================================

@Composable
fun TransactionAmountCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "${transaction.currency} ${String.format("%.2f", kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (transaction.convertedAmountBDT > 0 || transaction.convertedAmountMYR > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (transaction.convertedAmountBDT > 0) {
                        ConversionItem(
                            label = "In BDT",
                            amount = "৳ ${String.format("%.2f", transaction.convertedAmountBDT)}"
                        )
                    }
                    if (transaction.convertedAmountMYR > 0) {
                        ConversionItem(
                            label = "In MYR",
                            amount = "RM ${String.format("%.2f", transaction.convertedAmountMYR)}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversionItem(label: String, amount: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ========================================
// SMART DEDUCTION INFO CARD
// ========================================

@Composable
fun SmartDeductionInfoCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Smart Deduction Applied",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DeductionItem(
                    currency = "BDT",
                    amount = transaction.bdtDeducted,
                    icon = "৳"
                )
                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    color = Color(0xFF1976D2).copy(alpha = 0.3f)
                )
                DeductionItem(
                    currency = "MYR",
                    amount = transaction.myrDeducted,
                    icon = "RM"
                )
            }

            Surface(
                color = Color(0xFF1976D2).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Text(
                        text = "Strategy: ${transaction.deductionStrategy?.uppercase() ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

@Composable
fun DeductionItem(currency: String, amount: Double, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = currency,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF1976D2).copy(alpha = 0.7f)
        )
        Text(
            text = "$icon ${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Text(
            text = "deducted",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF1976D2).copy(alpha = 0.6f)
        )
    }
}

// ========================================
// SENDER INFO CARD
// ========================================

@Composable
fun SenderInfoCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Sender Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!transaction.senderName.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.AccountCircle,
                        label = "Name",
                        value = transaction.senderName,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                if (!transaction.senderPhone.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = transaction.senderPhone,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                if (!transaction.senderEmail.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = transaction.senderEmail,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                InfoRow(
                    icon = Icons.Default.Tag,
                    label = "User ID",
                    value = transaction.userId,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            }
        }
    }
}

// ========================================
// RECIPIENT INFO CARD
// ========================================

@Composable
fun RecipientInfoCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Recipient Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!transaction.recipientName.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.AccountCircle,
                        label = "Name",
                        value = transaction.recipientName,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                if (!transaction.recipientPhone.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = transaction.recipientPhone,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                if (!transaction.recipientEmail.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = transaction.recipientEmail,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                if (!transaction.recipientId.isNullOrEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Tag,
                        label = "Recipient ID",
                        value = transaction.recipientId,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        }
    }
}

// ========================================
// FEE & RATE DETAILS CARD
// ========================================

@Composable
fun FeeRateDetailsCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fee & Rate Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fee Details
                if (transaction.feeBDT > 0 || transaction.feeMYR > 0) {
                    DetailRow(
                        label = "Fee (BDT)",
                        value = "৳ ${String.format("%.2f", transaction.feeBDT)}"
                    )
                    DetailRow(
                        label = "Fee (MYR)",
                        value = "RM ${String.format("%.2f", transaction.feeMYR)}"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                // Net Amount
                if (transaction.netAmountBDT > 0 || transaction.netAmountMYR > 0) {
                    DetailRow(
                        label = "Net Amount (BDT)",
                        value = "৳ ${String.format("%.2f", transaction.netAmountBDT)}",
                        isBold = true
                    )
                    DetailRow(
                        label = "Net Amount (MYR)",
                        value = "RM ${String.format("%.2f", transaction.netAmountMYR)}",
                        isBold = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                // Exchange Rate
                DetailRow(
                    label = "Exchange Rate",
                    value = "1 MYR = ${String.format("%.2f", transaction.rateUsed)} BDT",
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ========================================
// PAYMENT METHOD CARD (OLD WAY)
// ========================================

@Composable
fun PaymentMethodCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Payment Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(
                    label = "Payment Method",
                    value = transaction.paymentMethod?.uppercase() ?: "N/A"
                )

                if (!transaction.accountNumber.isNullOrEmpty()) {
                    DetailRow(
                        label = "Account Number",
                        value = transaction.accountNumber
                    )
                }

                if (!transaction.transactionId.isNullOrEmpty()) {
                    DetailRow(
                        label = "Transaction ID",
                        value = transaction.transactionId
                    )
                }
            }

            if (!transaction.proofUrl.isNullOrEmpty()) {
                FilledTonalButton(
                    onClick = { /* TODO: Show image */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Payment Proof")
                }
            }
        }
    }
}

// ========================================
// TRANSACTION INFO CARD
// ========================================

@Composable
fun TransactionInfoCard(transaction: TransactionModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Transaction Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(
                    label = "App Transaction ID",
                    value = transaction.appTransactionId
                )

                DetailRow(
                    label = "Created At",
                    value = formatTimestamp(transaction.createdAt)
                )

                if (transaction.processed && transaction.processedAt != null) {
                    DetailRow(
                        label = "Processed At",
                        value = formatTimestamp(transaction.processedAt)
                    )
                }

                if (!transaction.processedBy.isNullOrEmpty()) {
                    DetailRow(
                        label = "Processed By",
                        value = transaction.processedBy
                    )
                }
            }

            if (!transaction.message.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Message",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ========================================
// REUSABLE COMPONENTS
// ========================================

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.contentColorFor(containerColor).copy(alpha = 0.7f)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.contentColorFor(containerColor).copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.contentColorFor(containerColor)
            )
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}