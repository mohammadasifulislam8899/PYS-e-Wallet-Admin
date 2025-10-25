//// presentation/screens/paymentmethods/components/AddPaymentMethodDialog.kt
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
//import com.droidnest.tech.pysadmin.domain.models.PaymentMethods
//import com.droidnest.tech.pysadmin.domain.models.PaymentType
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddPaymentMethodDialog(
//    onDismiss: () -> Unit,
//    onConfirm: (
//        name: String,
//        type: PaymentType,
//        currency: String,
//        accountNumber: String,
//        accountName: String,
//        accountType: String,
//        minAmount: Double,
//        maxAmount: Double,
//        dailyLimit: Double,
//        fee: Double,
//        feeType: FeeType,
//        instructions: String,
//        priority: Int
//    ) -> Unit,
//    isLoading: Boolean
//) {
//    var selectedTemplate by remember { mutableStateOf<String?>(null) }
//    var currency by remember { mutableStateOf("BDT") }
//    var name by remember { mutableStateOf("") }
//    var type by remember { mutableStateOf(PaymentType.MOBILE_BANKING) }
//    var accountNumber by remember { mutableStateOf("") }
//    var accountName by remember { mutableStateOf("") }
//    var accountType by remember { mutableStateOf("Personal") }
//    var minAmount by remember { mutableStateOf("100") }
//    var maxAmount by remember { mutableStateOf("100000") }
//    var dailyLimit by remember { mutableStateOf("500000") }
//    var fee by remember { mutableStateOf("0") }
//    var feeType by remember { mutableStateOf(FeeType.PERCENTAGE) }
//    var instructions by remember { mutableStateOf("") }
//    var priority by remember { mutableStateOf("0") }
//
//    var showTemplateMenu by remember { mutableStateOf(false) }
//    var showCurrencyMenu by remember { mutableStateOf(false) }
//    var showTypeMenu by remember { mutableStateOf(false) }
//    var showAccountTypeMenu by remember { mutableStateOf(false) }
//    var showFeeTypeMenu by remember { mutableStateOf(false) }
//
//    val templates = if (currency == "BDT") PaymentMethods.BDT_METHODS else PaymentMethods.MYR_METHODS
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
//            Column(
//                modifier = Modifier.fillMaxSize()
//            ) {
//                // Header
//                TopAppBar(
//                    title = { Text("Add Payment Method") },
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
//                    // Currency Selection
//                    Text(
//                        "Currency",
//                        style = MaterialTheme.typography.labelLarge
//                    )
//                    ExposedDropdownMenuBox(
//                        expanded = showCurrencyMenu,
//                        onExpandedChange = { showCurrencyMenu = !showCurrencyMenu }
//                    ) {
//                        OutlinedTextField(
//                            value = currency,
//                            onValueChange = {},
//                            readOnly = true,
//                            label = { Text("Select Currency") },
//                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCurrencyMenu) },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .menuAnchor()
//                        )
//                        ExposedDropdownMenu(
//                            expanded = showCurrencyMenu,
//                            onDismissRequest = { showCurrencyMenu = false }
//                        ) {
//                            DropdownMenuItem(
//                                text = { Text("BDT - Bangladesh Taka (৳)") },
//                                onClick = {
//                                    currency = "BDT"
//                                    selectedTemplate = null
//                                    name = ""
//                                    showCurrencyMenu = false
//                                }
//                            )
//                            DropdownMenuItem(
//                                text = { Text("MYR - Malaysian Ringgit (RM)") },
//                                onClick = {
//                                    currency = "MYR"
//                                    selectedTemplate = null
//                                    name = ""
//                                    showCurrencyMenu = false
//                                }
//                            )
//                        }
//                    }
//
//                    // Template Selection
//                    Text(
//                        "Payment Method Template",
//                        style = MaterialTheme.typography.labelLarge
//                    )
//                    ExposedDropdownMenuBox(
//                        expanded = showTemplateMenu,
//                        onExpandedChange = { showTemplateMenu = !showTemplateMenu }
//                    ) {
//                        OutlinedTextField(
//                            value = selectedTemplate ?: "Select a template",
//                            onValueChange = {},
//                            readOnly = true,
//                            label = { Text("Choose Template") },
//                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showTemplateMenu) },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .menuAnchor()
//                        )
//                        ExposedDropdownMenu(
//                            expanded = showTemplateMenu,
//                            onDismissRequest = { showTemplateMenu = false }
//                        ) {
//                            templates.forEach { template ->
//                                DropdownMenuItem(
//                                    text = { Text(template.name) },
//                                    onClick = {
//                                        selectedTemplate = template.name
//                                        name = template.name
//                                        type = template.type
//                                        showTemplateMenu = false
//                                    }
//                                )
//                            }
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
//                    // Account Type
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
//                            listOf("Personal", "Agent", "Merchant").forEach { typeOption ->
//                                DropdownMenuItem(
//                                    text = { Text(typeOption) },
//                                    onClick = {
//                                        accountType = typeOption
//                                        showAccountTypeMenu = false
//                                    }
//                                )
//                            }
//                        }
//                    }
//
//                    // Transaction Limits
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
//                            label = { Text("Min Amount") },
//                            modifier = Modifier.weight(1f),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                        )
//                        OutlinedTextField(
//                            value = maxAmount,
//                            onValueChange = { maxAmount = it },
//                            label = { Text("Max Amount") },
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
//                    // Fee Configuration
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
//                                label = { Text("Fee Type") },
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
//                    // Instructions
//                    OutlinedTextField(
//                        value = instructions,
//                        onValueChange = { instructions = it },
//                        label = { Text("Instructions (Optional)") },
//                        modifier = Modifier.fillMaxWidth(),
//                        minLines = 3,
//                        maxLines = 5
//                    )
//
//                    // Priority
//                    OutlinedTextField(
//                        value = priority,
//                        onValueChange = { priority = it },
//                        label = { Text("Display Priority (0 = highest)") },
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
//                                name,
//                                type,
//                                currency,
//                                accountNumber,
//                                accountName,
//                                accountType,
//                                minAmount.toDoubleOrNull() ?: 0.0,
//                                maxAmount.toDoubleOrNull() ?: 0.0,
//                                dailyLimit.toDoubleOrNull() ?: 0.0,
//                                fee.toDoubleOrNull() ?: 0.0,
//                                feeType,
//                                instructions,
//                                priority.toIntOrNull() ?: 0
//                            )
//                        },
//                        modifier = Modifier.weight(1f),
//                        enabled = !isLoading && name.isNotBlank() &&
//                                 accountNumber.isNotBlank() && accountName.isNotBlank()
//                    ) {
//                        if (isLoading) {
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(20.dp),
//                                strokeWidth = 2.dp,
//                                color = MaterialTheme.colorScheme.onPrimary
//                            )
//                        } else {
//                            Text("Add Method")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}