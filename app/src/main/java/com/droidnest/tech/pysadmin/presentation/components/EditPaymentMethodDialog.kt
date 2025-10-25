//// presentation/screens/paymentmethods/components/EditPaymentMethodDialog.kt
//package com.droidnest.tech.pysadmin.presentation.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import com.droidnest.tech.pysadmin.domain.models.FeeType
//import com.droidnest.tech.pysadmin.domain.models.AddMoneyPaymentMethod
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun EditPaymentMethodDialog(
//    method: AddMoneyPaymentMethod,
//    onDismiss: () -> Unit,
//    onConfirm: (AddMoneyPaymentMethod) -> Unit,
//    isLoading: Boolean
//) {
//    var accountName by remember { mutableStateOf(method.accountName) }
//    var accountNumber by remember { mutableStateOf(method.accountNumber) }
//    var accountType by remember { mutableStateOf(method.accountType) }
//    var minAmount by remember { mutableStateOf(method.minAmount.toString()) }
//    var maxAmount by remember { mutableStateOf(method.maxAmount.toString()) }
//    var dailyLimit by remember { mutableStateOf(method.dailyLimit.toString()) }
//    var fee by remember { mutableStateOf(method.fee.toString()) }
//    var feeType by remember { mutableStateOf(method.feeType) }
//    var instructions by remember { mutableStateOf(method.instructions) }
//    var priority by remember { mutableStateOf(method.priority.toString()) }
//    var isEnabled by remember { mutableStateOf(method.isEnabled) }
//
//    var showAccountTypeMenu by remember { mutableStateOf(false) }
//    var showFeeTypeMenu by remember { mutableStateOf(false) }
//
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(usePlatformDefaultWidth = false)
//    ) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth(0.95f)
//                .fillMaxHeight(0.9f)
//        ) {
//            Column(modifier = Modifier.fillMaxSize()) {
//                // Header
//                TopAppBar(
//                    title = { Text("Edit ${method.name}") },
//                    navigationIcon = {
//                        IconButton(onClick = onDismiss) {
//                            Icon(Icons.Default.Close, "Close")
//                        }
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant
//                    )
//                )
//
//                // Form
//                Column(
//                    modifier = Modifier
//                        .weight(1f)
//                        .verticalScroll(rememberScrollState())
//                        .padding(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    // Status Switch
//                    Card(
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
//                        )
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Column {
//                                Text(
//                                    "Payment Method Status",
//                                    style = MaterialTheme.typography.titleSmall
//                                )
//                                Text(
//                                    if (isEnabled) "Currently Active" else "Currently Disabled",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                            Switch(
//                                checked = isEnabled,
//                                onCheckedChange = { isEnabled = it }
//                            )
//                        }
//                    }
//
//                    // Account Details
//                    Text(
//                        "Account Details",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//
//                    OutlinedTextField(
//                        value = accountName,
//                        onValueChange = { accountName = it },
//                        label = { Text("Account Holder Name") },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true
//                    )
//
//                    OutlinedTextField(
//                        value = accountNumber,
//                        onValueChange = { accountNumber = it },
//                        label = { Text("Account Number / Phone") },
//                        modifier = Modifier.fillMaxWidth(),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        singleLine = true
//                    )
//
//                    ExposedDropdownMenuBox(
//                        expanded = showAccountTypeMenu,
//                        onExpandedChange = { showAccountTypeMenu = !showAccountTypeMenu }
//                    ) {
//                        OutlinedTextField(
//                            value = accountType,
//                            onValueChange = {},
//                            readOnly = true,
//                            label = { Text("Account Type") },
//                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showAccountTypeMenu) },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .menuAnchor()
//                        )
//                        ExposedDropdownMenu(
//                            expanded = showAccountTypeMenu,
//                            onDismissRequest = { showAccountTypeMenu = false }
//                        ) {
//                            listOf("Personal", "Agent", "Merchant").forEach { type ->
//                                DropdownMenuItem(
//                                    text = { Text(type) },
//                                    onClick = {
//                                        accountType = type
//                                        showAccountTypeMenu = false
//                                    }
//                                )
//                            }
//                        }
//                    }
//
//                    // Limits
//                    Text(
//                        "Transaction Limits",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        OutlinedTextField(
//                            value = minAmount,
//                            onValueChange = { minAmount = it },
//                            label = { Text("Min") },
//                            modifier = Modifier.weight(1f),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                        )
//                        OutlinedTextField(
//                            value = maxAmount,
//                            onValueChange = { maxAmount = it },
//                            label = { Text("Max") },
//                            modifier = Modifier.weight(1f),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                        )
//                    }
//
//                    OutlinedTextField(
//                        value = dailyLimit,
//                        onValueChange = { dailyLimit = it },
//                        label = { Text("Daily Limit") },
//                        modifier = Modifier.fillMaxWidth(),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                    )
//
//                    // Fee
//                    Text(
//                        "Fee Configuration",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        OutlinedTextField(
//                            value = fee,
//                            onValueChange = { fee = it },
//                            label = { Text("Fee") },
//                            modifier = Modifier.weight(1f),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
//                        )
//
//                        ExposedDropdownMenuBox(
//                            expanded = showFeeTypeMenu,
//                            onExpandedChange = { showFeeTypeMenu = !showFeeTypeMenu },
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            OutlinedTextField(
//                                value = feeType.name,
//                                onValueChange = {},
//                                readOnly = true,
//                                label = { Text("Type") },
//                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showFeeTypeMenu) },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .menuAnchor()
//                            )
//                            ExposedDropdownMenu(
//                                expanded = showFeeTypeMenu,
//                                onDismissRequest = { showFeeTypeMenu = false }
//                            ) {
//                                FeeType.values().forEach { type ->
//                                    DropdownMenuItem(
//                                        text = { Text(type.name) },
//                                        onClick = {
//                                            feeType = type
//                                            showFeeTypeMenu = false
//                                        }
//                                    )
//                                }
//                            }
//                        }
//                    }
//
//                    OutlinedTextField(
//                        value = instructions,
//                        onValueChange = { instructions = it },
//                        label = { Text("Instructions") },
//                        modifier = Modifier.fillMaxWidth(),
//                        minLines = 3
//                    )
//
//                    OutlinedTextField(
//                        value = priority,
//                        onValueChange = { priority = it },
//                        label = { Text("Priority") },
//                        modifier = Modifier.fillMaxWidth(),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                    )
//                }
//
//                // Actions
//                HorizontalDivider()
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    OutlinedButton(
//                        onClick = onDismiss,
//                        modifier = Modifier.weight(1f),
//                        enabled = !isLoading
//                    ) {
//                        Text("Cancel")
//                    }
//
//                    Button(
//                        onClick = {
//                            onConfirm(
//                                method.copy(
//                                    accountName = accountName,
//                                    accountNumber = accountNumber,
//                                    accountType = accountType,
//                                    minAmount = minAmount.toDoubleOrNull() ?: method.minAmount,
//                                    maxAmount = maxAmount.toDoubleOrNull() ?: method.maxAmount,
//                                    dailyLimit = dailyLimit.toDoubleOrNull() ?: method.dailyLimit,
//                                    fee = fee.toDoubleOrNull() ?: method.fee,
//                                    feeType = feeType,
//                                    instructions = instructions,
//                                    priority = priority.toIntOrNull() ?: method.priority,
//                                    isEnabled = isEnabled
//                                )
//                            )
//                        },
//                        modifier = Modifier.weight(1f),
//                        enabled = !isLoading
//                    ) {
//                        if (isLoading) {
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(20.dp),
//                                strokeWidth = 2.dp
//                            )
//                        } else {
//                            Text("Save Changes")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}