@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidnest.tech.pysadmin.presentation.screens.withdrawpaymentmethod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnest.tech.pysadmin.domain.models.FeeRange
import com.droidnest.tech.pysadmin.domain.models.FieldType
import com.droidnest.tech.pysadmin.domain.models.PaymentCategory
import com.droidnest.tech.pysadmin.domain.models.RequiredField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPaymentMethodScreen(
    methodId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: AddEditPaymentMethodViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(methodId) {
        methodId?.let {
            viewModel.loadPaymentMethod(it)
        }
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
                    Text(
                        if (methodId == null) "Add Method" else "Edit Method",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BasicInfoCard(state, viewModel)
                    AmountRangeCard(state, viewModel)
                    RequiredFieldsCard(state, viewModel)
                    FeeRangesCard(state, viewModel)

                    Button(
                        onClick = viewModel::savePaymentMethod,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BasicInfoCard(
    state: AddEditPaymentMethodState,
    viewModel: AddEditPaymentMethodViewModel
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Basic Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = state.id,
                onValueChange = viewModel::onIdChanged,
                label = { Text("Method ID") },
                placeholder = { Text("bkash") },
                isError = state.idError != null,
                supportingText = state.idError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isEditMode
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Name") },
                placeholder = { Text("bKash") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.icon,
                onValueChange = viewModel::onIconChanged,
                label = { Text("Icon") },
                placeholder = { Text("💳") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = PaymentCategory.fromValue(state.category).displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        Text(PaymentCategory.fromValue(state.category).icon)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    PaymentCategory.values().forEach { category ->
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

            var countryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${state.country} (${state.currency})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country") },
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
        }
    }
}

@Composable
private fun AmountRangeCard(
    state: AddEditPaymentMethodState,
    viewModel: AddEditPaymentMethodViewModel
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Amount & Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.minAmount,
                    onValueChange = viewModel::onMinAmountChanged,
                    label = { Text("Min") },
                    isError = state.minAmountError != null,
                    supportingText = state.minAmountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.maxAmount,
                    onValueChange = viewModel::onMaxAmountChanged,
                    label = { Text("Max") },
                    isError = state.maxAmountError != null,
                    supportingText = state.maxAmountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state.processingTime,
                onValueChange = viewModel::onProcessingTimeChanged,
                label = { Text("Processing Time") },
                placeholder = { Text("1-2 hours") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::onEnabledChanged
                )
            }
        }
    }
}

@Composable
private fun RequiredFieldsCard(
    state: AddEditPaymentMethodState,
    viewModel: AddEditPaymentMethodViewModel
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Required Fields",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalIconButton(
                    onClick = viewModel::addRequiredField,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "Add", Modifier.size(18.dp))
                }
            }

            if (state.requiredFields.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No fields added",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                state.requiredFields.forEachIndexed { index, field ->
                    RequiredFieldItem(
                        field = field,
                        onUpdate = { viewModel.updateRequiredField(index, it) },
                        onRemove = { viewModel.removeRequiredField(index) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequiredFieldItem(
    field: RequiredField,
    onUpdate: (RequiredField) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Field",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp))
                }
            }

            OutlinedTextField(
                value = field.fieldName,
                onValueChange = { onUpdate(field.copy(fieldName = it)) },
                label = { Text("Field Name") },
                placeholder = { Text("phoneNumber") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = field.label,
                onValueChange = { onUpdate(field.copy(label = it)) },
                label = { Text("Label") },
                placeholder = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = field.placeholder,
                onValueChange = { onUpdate(field.copy(placeholder = it)) },
                label = { Text("Placeholder") },
                placeholder = { Text("01XXXXXXXXX") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = field.type.value.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    FieldType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.value.uppercase()) },
                            onClick = {
                                onUpdate(field.copy(type = type))
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            if (field.type == FieldType.DROPDOWN || field.type == FieldType.RADIO) {
                OutlinedTextField(
                    value = field.options.joinToString(", "),
                    onValueChange = {
                        val options = it.split(",").map { opt -> opt.trim() }.filter { opt -> opt.isNotBlank() }
                        onUpdate(field.copy(options = options))
                    },
                    label = { Text("Options") },
                    placeholder = { Text("Personal, Agent") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Required", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = field.required,
                    onCheckedChange = { onUpdate(field.copy(required = it)) }
                )
            }
        }
    }
}

@Composable
private fun FeeRangesCard(
    state: AddEditPaymentMethodState,
    viewModel: AddEditPaymentMethodViewModel
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fee Structure",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalIconButton(
                    onClick = viewModel::addFeeRange,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "Add", Modifier.size(18.dp))
                }
            }

            if (state.feeRanges.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No fee ranges added",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                state.feeRanges.forEachIndexed { index, feeRange ->
                    FeeRangeItem(
                        feeRange = feeRange,
                        onUpdate = { viewModel.updateFeeRange(index, it) },
                        onRemove = { viewModel.removeFeeRange(index) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeeRangeItem(
    feeRange: FeeRange,
    onUpdate: (FeeRange) -> Unit,
    onRemove: () -> Unit
) {
    // ✅ Convert Double to String properly (empty if 0)
    val minText = if (feeRange.min == 0.0) "" else feeRange.min.toString()
    val maxText = if (feeRange.max == 0.0) "" else feeRange.max.toString()
    val feeText = if (feeRange.fee == 0.0) "" else feeRange.fee.toString()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Range",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { input ->
                        // ✅ Allow empty or valid decimal
                        if (input.isEmpty()) {
                            onUpdate(feeRange.copy(min = 0.0))
                        } else {
                            input.toDoubleOrNull()?.let { value ->
                                onUpdate(feeRange.copy(min = value))
                            }
                        }
                    },
                    label = { Text("Min Amount") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = maxText,
                    onValueChange = { input ->
                        if (input.isEmpty()) {
                            onUpdate(feeRange.copy(max = 0.0))
                        } else {
                            input.toDoubleOrNull()?.let { value ->
                                onUpdate(feeRange.copy(max = value))
                            }
                        }
                    },
                    label = { Text("Max Amount") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { input ->
                        if (input.isEmpty()) {
                            onUpdate(feeRange.copy(fee = 0.0))
                        } else {
                            input.toDoubleOrNull()?.let { value ->
                                onUpdate(feeRange.copy(fee = value))
                            }
                        }
                    },
                    label = { Text("Fee Amount") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = feeRange.type.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fee Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Fixed") },
                            onClick = {
                                onUpdate(feeRange.copy(type = "fixed"))
                                typeExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Percentage") },
                            onClick = {
                                onUpdate(feeRange.copy(type = "percentage"))
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}