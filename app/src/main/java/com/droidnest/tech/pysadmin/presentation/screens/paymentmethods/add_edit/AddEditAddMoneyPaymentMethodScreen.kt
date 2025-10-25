// presentation/screens/paymentmethods/AddEditAddMoneyPaymentMethodScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidnest.tech.pysadmin.presentation.screens.paymentmethods.add_edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnest.tech.pysadmin.domain.models.*

@Composable
fun AddEditAddMoneyPaymentMethodScreen(
    methodId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: AddEditAddMoneyPaymentMethodViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(methodId) {
        methodId?.let { viewModel.loadPaymentMethod(it) }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (methodId == null) "Add Payment Method" else "Edit Payment Method",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${calculateProgress(state).times(100).toInt()}% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress
                LinearProgressIndicator(
                    progress = calculateProgress(state),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                // Basic Info
                SectionCard(title = "Basic Information") {
                    BasicInfoSection(state, viewModel)
                }

                // Admin Account
                SectionCard(title = "Admin Account") {
                    AdminAccountSection(state, viewModel)
                }

                // Limits
                SectionCard(title = "Transaction Limits") {
                    LimitsSection(state, viewModel)
                }

                // Required Fields Preview
                SectionCard(title = "User Input Fields") {
                    RequiredFieldsPreview(state)
                }

                // Instructions
                SectionCard(title = "Instructions") {
                    InstructionsSection(state, viewModel)
                }

                // Actions
                ActionButtons(state, viewModel::savePaymentMethod, onNavigateBack)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun BasicInfoSection(
    state: AddEditAddMoneyPaymentMethodState,
    viewModel: AddEditAddMoneyPaymentMethodViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Name & Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Method Name") },
                placeholder = { Text("e.g., bKash") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = state.icon,
                onValueChange = viewModel::onIconChanged,
                label = { Text("Icon") },
                placeholder = { Text("📱") },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize
                )
            )
        }

        // Category
        var categoryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = AddMoneyPaymentCategory.fromValue(state.category).displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                AddMoneyPaymentCategory.values().forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.icon)
                                Text(category.displayName)
                            }
                        },
                        onClick = {
                            viewModel.onCategoryChanged(category.value)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Country
        var countryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = it }
        ) {
            OutlinedTextField(
                value = when (state.country) {
                    "BD" -> "🇧🇩 Bangladesh (${state.currency})"
                    "MY" -> "🇲🇾 Malaysia (${state.currency})"
                    else -> state.country
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Country & Currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(countryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = countryExpanded,
                onDismissRequest = { countryExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("🇧🇩 Bangladesh (BDT)") },
                    onClick = {
                        viewModel.onCountryChanged("BD")
                        countryExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("🇲🇾 Malaysia (MYR)") },
                    onClick = {
                        viewModel.onCountryChanged("MY")
                        countryExpanded = false
                    }
                )
            }
        }

        // Priority & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.priority.toString(),
                onValueChange = { viewModel.onPriorityChanged(it.toIntOrNull() ?: 0) },
                label = { Text("Priority") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = viewModel::onEnabledChanged
                    )
                    Text(
                        if (state.enabled) "Active" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAccountSection(
    state: AddEditAddMoneyPaymentMethodState,
    viewModel: AddEditAddMoneyPaymentMethodViewModel
) {
    val category = AddMoneyPaymentCategory.fromValue(state.category)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Users will send money to this account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        OutlinedTextField(
            value = state.accountNumber,
            onValueChange = viewModel::onAccountNumberChanged,
            label = {
                Text(
                    if (category == AddMoneyPaymentCategory.MOBILE_BANKING)
                        "Mobile Number"
                    else
                        "Account Number"
                )
            },
            placeholder = {
                Text(
                    if (category == AddMoneyPaymentCategory.MOBILE_BANKING)
                        "01XXXXXXXXX"
                    else
                        "Enter account number"
                )
            },
            isError = state.accountNumberError != null,
            supportingText = state.accountNumberError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.accountName,
            onValueChange = viewModel::onAccountNameChanged,
            label = { Text("Account Holder Name") },
            isError = state.accountNameError != null,
            supportingText = state.accountNameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (category == AddMoneyPaymentCategory.MOBILE_BANKING) {
            var accountTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = accountTypeExpanded,
                onExpandedChange = { accountTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.accountType.ifEmpty { "Select type" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = accountTypeExpanded,
                    onDismissRequest = { accountTypeExpanded = false }
                ) {
                    listOf("Personal", "Agent", "Merchant").forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.onAccountTypeChanged(type)
                                accountTypeExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = state.accountType,
                onValueChange = viewModel::onAccountTypeChanged,
                label = { Text("Bank Name") },
                placeholder = { Text("e.g., DBBL, Brac Bank") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun LimitsSection(
    state: AddEditAddMoneyPaymentMethodState,
    viewModel: AddEditAddMoneyPaymentMethodViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.minAmount,
                onValueChange = viewModel::onMinAmountChanged,
                label = { Text("Min Amount") },
                suffix = { Text(state.currency) },
                isError = state.minAmountError != null,
                supportingText = state.minAmountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = state.maxAmount,
                onValueChange = viewModel::onMaxAmountChanged,
                label = { Text("Max Amount") },
                suffix = { Text(state.currency) },
                isError = state.maxAmountError != null,
                supportingText = state.maxAmountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = state.dailyLimit,
            onValueChange = viewModel::onDailyLimitChanged,
            label = { Text("Daily Limit") },
            suffix = { Text(state.currency) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Preview
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LimitPreview("Min", state.minAmount.toDoubleOrNull() ?: 0.0, state.currency)
                LimitPreview("Max", state.maxAmount.toDoubleOrNull() ?: 0.0, state.currency)
                LimitPreview("Daily", state.dailyLimit.toDoubleOrNull() ?: 0.0, state.currency)
            }
        }
    }
}

@Composable
private fun LimitPreview(label: String, value: Double, currency: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatAmount(value, currency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RequiredFieldsPreview(state: AddEditAddMoneyPaymentMethodState) {
    if (state.requiredFields.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Auto-generated based on category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${state.requiredFields.size} field(s) will be shown to users",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.requiredFields.forEachIndexed { index, field ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}.",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            when (field.type) {
                                FieldType.PHONE -> Icons.Outlined.Phone
                                FieldType.NUMBER -> Icons.Outlined.Numbers
                                FieldType.DROPDOWN -> Icons.Outlined.ArrowDropDown
                                FieldType.TEXTAREA -> Icons.Outlined.Notes
                                else -> Icons.Outlined.Edit
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                field.label + if (field.required) " *" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (field.options.isNotEmpty()) {
                                Text(
                                    field.options.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionsSection(
    state: AddEditAddMoneyPaymentMethodState,
    viewModel: AddEditAddMoneyPaymentMethodViewModel
) {
    OutlinedTextField(
        value = state.instructions,
        onValueChange = viewModel::onInstructionsChanged,
        label = { Text("Step-by-step guide") },
        placeholder = {
            Text(
                "1. Open bKash app\n2. Select Send Money\n3. Enter number above\n4. Send amount\n5. Copy TrxID\n6. Submit here"
            )
        },
        modifier = Modifier.fillMaxWidth(),
        minLines = 5,
        maxLines = 10
    )
}

@Composable
private fun ActionButtons(
    state: AddEditAddMoneyPaymentMethodState,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Cancel")
        }

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Method")
            }
        }
    }
}

// Helper Functions
private fun calculateProgress(state: AddEditAddMoneyPaymentMethodState): Float {
    var filled = 0
    val total = 7

    if (state.name.isNotBlank()) filled++
    if (state.accountNumber.isNotBlank()) filled++
    if (state.accountName.isNotBlank()) filled++
    if (state.accountType.isNotBlank()) filled++
    if (state.minAmount.isNotBlank()) filled++
    if (state.maxAmount.isNotBlank()) filled++
    if (state.dailyLimit.isNotBlank()) filled++

    return filled.toFloat() / total.toFloat()
}

private fun formatAmount(amount: Double, currency: String): String {
    return when (currency) {
        "BDT" -> "৳${String.format("%.0f", amount)}"
        "MYR" -> "RM${String.format("%.2f", amount)}"
        else -> "$${String.format("%.2f", amount)}"
    }
}