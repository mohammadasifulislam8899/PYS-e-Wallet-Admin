// presentation/components/PaymentMethodCard.kt
package com.droidnest.tech.pysadmin.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnest.tech.pysadmin.domain.models.AddMoneyPaymentMethod
import com.droidnest.tech.pysadmin.domain.models.FieldType
import com.droidnest.tech.pysadmin.domain.models.AddMoneyPaymentCategory

@Composable
fun PaymentMethodCard(
    method: AddMoneyPaymentMethod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (method.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (method.isEnabled) 1.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(getPaymentMethodColor(method.name).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (method.icon.isNotBlank()) method.icon
                            else getPaymentMethodIcon(method.name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    // Name & Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (method.isEnabled)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = method.category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = method.currency,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Status
                Surface(
                    shape = CircleShape,
                    color = if (method.isEnabled)
                        Color(0xFF4CAF50).copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (method.isEnabled) Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        )
                        Text(
                            text = if (method.isEnabled) "Active" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (method.isEnabled) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InfoRow(
                        label = "Account",
                        value = method.accountName
                    )
                    InfoRow(
                        label = "Number",
                        value = method.formattedAccountNumber
                    )
                    if (method.accountType.isNotBlank()) {
                        InfoRow(
                            label = "Type",
                            value = method.accountType
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LimitItem(
                    label = "Min",
                    value = formatAmount(method.minAmount, method.currency),
                    modifier = Modifier.weight(1f)
                )
                LimitItem(
                    label = "Max",
                    value = formatAmount(method.maxAmount, method.currency),
                    modifier = Modifier.weight(1f)
                )
                LimitItem(
                    label = "Daily",
                    value = formatAmount(method.dailyLimit, method.currency),
                    modifier = Modifier.weight(1f)
                )
            }

            // Expandable Details
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Required Fields
                    if (method.requiredFields.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "User Fields (${method.requiredFields.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            method.requiredFields.forEach { field ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (field.type) {
                                            FieldType.PHONE -> Icons.Outlined.Phone
                                            FieldType.NUMBER -> Icons.Outlined.Numbers
                                            FieldType.DROPDOWN -> Icons.Outlined.ArrowDropDown
                                            FieldType.TEXTAREA -> Icons.Outlined.Notes
                                            else -> Icons.Outlined.Edit
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = field.label + if (field.required) " *" else "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // Instructions
                    if (method.instructions.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Instructions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = method.instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }

                FilledTonalButton(
                    onClick = onToggle,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (method.isEnabled)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        if (method.isEnabled) Icons.Outlined.ToggleOff else Icons.Outlined.ToggleOn,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (method.isEnabled) "Disable" else "Enable")
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Outlined.DeleteOutline, "Delete")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LimitItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatAmount(amount: Double, currency: String): String {
    return when (currency) {
        "BDT" -> "৳${String.format("%.0f", amount)}"
        "MYR" -> "RM${String.format("%.2f", amount)}"
        else -> "$${String.format("%.2f", amount)}"
    }
}

private fun getPaymentMethodIcon(name: String): String {
    return when (name.lowercase()) {
        "bkash" -> "📱"
        "nagad" -> "📲"
        "rocket" -> "🚀"
        "upay" -> "💳"
        "tap" -> "💰"
        "touchngo", "touch 'n go" -> "💙"
        "grabpay" -> "💚"
        "boost" -> "⚡"
        "shopeepay" -> "🛍️"
        "maybank", "cimb", "rhb" -> "🏦"
        "dbbl", "brac bank", "city bank" -> "🏦"
        else -> "💰"
    }
}

private fun getPaymentMethodColor(name: String): Color {
    return when (name.lowercase()) {
        "bkash" -> Color(0xFFE2136E)
        "nagad" -> Color(0xFFFF6B00)
        "rocket" -> Color(0xFF8B4789)
        "upay" -> Color(0xFF6C63FF)
        "tap" -> Color(0xFF1E88E5)
        "touchngo", "touch 'n go" -> Color(0xFF0066CC)
        "grabpay" -> Color(0xFF00B14F)
        "boost" -> Color(0xFFFF9800)
        "shopeepay" -> Color(0xFFEE4D2D)
        "maybank" -> Color(0xFFFFD700)
        "cimb" -> Color(0xFFDC143C)
        "rhb" -> Color(0xFF003DA5)
        else -> Color(0xFF6200EE)
    }
}