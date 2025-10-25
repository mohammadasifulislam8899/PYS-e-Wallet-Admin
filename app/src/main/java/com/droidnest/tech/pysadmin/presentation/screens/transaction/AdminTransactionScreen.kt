// admin_app/presentation/transaction_management/AdminTransactionScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidnest.tech.pysadmin.domain.models.TransactionModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.getDefault
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransactionScreen(
    viewModel: TransactionViewModels = hiltViewModel(),
    onNavigateToDetails: (String) -> Unit = {}
) {
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

    // ✅ Sort transactions - PENDING first
    val sortedTransactions = remember(state.filteredTransactions) {
        state.filteredTransactions.sortedWith(
            compareBy { transaction ->
                when (transaction.status.lowercase()) {
                    "pending" -> 0    // Pending সবার আগে
                    "success" -> 1    // তারপর Success
                    "failed" -> 2     // সবশেষে Failed
                    else -> 3         // Unknown status
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Transaction Management")
                        Text(
                            text = "${state.filteredTransactions.size} transactions",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllTransactions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Compact Modern Filter Section
            AdminModernFilterSection(
                selectedStatus = state.selectedStatusFilter,
                onStatusChange = { viewModel.filterByStatus(it) }
            )

            // Transaction List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    sortedTransactions.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = sortedTransactions, // ✅ Sorted list ব্যবহার করা হচ্ছে
                                key = { index, transaction ->
                                    "${transaction.appTransactionId}_$index"
                                }
                            ) { index, transaction ->
                                AdminTransactionItem(
                                    transaction = transaction,
                                    onClick = {
                                        onNavigateToDetails(transaction.appTransactionId)
                                    },
                                    isUpdating = state.isUpdating
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================
// COMPACT COLLAPSIBLE FILTER SECTION
// ========================================

@Composable
fun AdminModernFilterSection(
    selectedStatus: String,
    onStatusChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getStatusFilterIcon(selectedStatus),
                    contentDescription = null,
                    tint = getStatusFilterColor(selectedStatus),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Status Filter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getStatusFilterLabel(selectedStatus),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusFilterChip(
                        label = "All",
                        icon = Icons.Default.List,
                        color = MaterialTheme.colorScheme.primary,
                        isSelected = selectedStatus == "all",
                        onClick = {
                            onStatusChange("all")
                            expanded = false
                        }
                    )
                    StatusFilterChip(
                        label = "Pending",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFFFC107),
                        isSelected = selectedStatus == "pending",
                        onClick = {
                            onStatusChange("pending")
                            expanded = false
                        }
                    )
                    StatusFilterChip(
                        label = "Success",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50),
                        isSelected = selectedStatus == "success",
                        onClick = {
                            onStatusChange("success")
                            expanded = false
                        }
                    )
                    StatusFilterChip(
                        label = "Failed",
                        icon = Icons.Default.Cancel,
                        color = Color(0xFFF44336),
                        isSelected = selectedStatus == "failed",
                        onClick = {
                            onStatusChange("failed")
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterChip(
    label: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.15f),
            selectedLabelColor = color,
            selectedLeadingIconColor = color,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            selectedBorderColor = color,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            selectedBorderWidth = 1.5.dp
        )
    )
}

// Helper functions for filter
private fun getStatusFilterLabel(status: String): String {
    return when (status) {
        "all" -> "All Transactions"
        "pending" -> "Pending"
        "success" -> "Success"
        "failed" -> "Failed"
        else -> "All"
    }
}

private fun getStatusFilterIcon(status: String): ImageVector {
    return when (status) {
        "all" -> Icons.Default.List
        "pending" -> Icons.Default.Schedule
        "success" -> Icons.Default.CheckCircle
        "failed" -> Icons.Default.Cancel
        else -> Icons.Default.List
    }
}

private fun getStatusFilterColor(status: String): Color {
    return when (status) {
        "all" -> Color(0xFF6200EE)
        "pending" -> Color(0xFFFFC107)
        "success" -> Color(0xFF4CAF50)
        "failed" -> Color(0xFFF44336)
        else -> Color(0xFF6200EE)
    }
}

// ========================================
// TRANSACTION ITEM
// ========================================

@Composable
fun AdminTransactionItem(
    transaction: TransactionModel,
    onClick: () -> Unit,
    isUpdating: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isUpdating) { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transaction Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(getTransactionColor(transaction.type).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getTransactionIcon(transaction.type),
                        contentDescription = null,
                        tint = getTransactionColor(transaction.type),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.appTransactionId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Sender info
                    if (!transaction.senderName.isNullOrEmpty()) {
                        Text(
                            text = "From: ${transaction.senderName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "Sender: ${transaction.userId.take(12)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Recipient info for send_money
                    if (transaction.type == "send_money" && !transaction.recipientName.isNullOrBlank()) {
                        Text(
                            text = "To: ${transaction.recipientName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = getTransactionTypeLabel(transaction.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = getTransactionColor(transaction.type)
                    )
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${transaction.currency} ${abs(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "৳${transaction.netAmountBDT}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = transaction.status)

                Text(
                    text = formatDateTime(transaction.createdAt.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ========================================
// HELPER COMPONENTS
// ========================================

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "success" -> Color(0xFF4CAF50) to Color.White
        "pending" -> Color(0xFFFFC107) to Color.Black
        "failed" -> Color(0xFFF44336) to Color.White
        else -> Color.Gray to Color.White
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

fun getTransactionTypeLabel(type: String): String {
    return when (type) {
        "add_money" -> "Add Money"
        "send_money" -> "Send Money"
        "withdraw" -> "Withdraw"
        "referral_bonus" -> "Referral Bonus"
        else -> {
            type.replace("_", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
        }
    }
}

fun getTransactionIcon(icon: String): ImageVector {
    return when (icon) {
        "add_money" -> Icons.Default.Add
        "send_money" -> Icons.AutoMirrored.Filled.Send
        "withdraw" -> Icons.AutoMirrored.Filled.ArrowBack
        "referral_bonus" -> Icons.Default.Star
        else -> Icons.Default.Info
    }
}

fun getTransactionColor(type: String): Color {
    return when (type) {
        "add_money" -> Color(0xFF4CAF50)
        "send_money" -> Color(0xFF2196F3)
        "withdraw" -> Color(0xFFF44336)
        "referral_bonus" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
}

fun formatDateTime(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", getDefault())
    return formatter.format(date)
}