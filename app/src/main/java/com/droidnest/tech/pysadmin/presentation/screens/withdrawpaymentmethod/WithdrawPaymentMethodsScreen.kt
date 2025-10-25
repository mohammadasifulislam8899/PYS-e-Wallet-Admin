package com.droidnest.tech.pysadmin.presentation.screens.withdrawpaymentmethod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnest.tech.pysadmin.domain.models.FieldType
import com.droidnest.tech.pysadmin.domain.models.PaymentCategory
import com.droidnest.tech.pysadmin.domain.models.WithdrawPaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawPaymentMethodsScreen(
    viewModel: WithdrawPaymentMethodsViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var methodToDelete by remember { mutableStateOf<WithdrawPaymentMethod?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filteredMethods = if (selectedCategory == null) {
        state.withdrawPaymentMethods
    } else {
        state.withdrawPaymentMethods.filter { it.category == selectedCategory }
    }

    val groupedMethods = filteredMethods.groupBy { it.category }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showDeleteDialog && methodToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Method?") },
            text = { Text("Delete ${methodToDelete?.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMethod(methodToDelete!!.id)
                        showDeleteDialog = false
                        methodToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.withdrawPaymentMethods.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No payment methods",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNavigateToAdd) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Method")
                        }
                    }
                }

                else -> {
                    Column(Modifier.fillMaxSize()) {
                        CategoryFilterRow(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            categories = state.withdrawPaymentMethods
                                .map { it.category }
                                .distinct()
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedMethods.forEach { (category, methods) ->
                                item {
                                    CategoryHeader(category = category)
                                }

                                items(methods) { method ->
                                    PaymentMethodCard(
                                        method = method,
                                        onEdit = { onNavigateToEdit(method.id) },
                                        onDelete = {
                                            methodToDelete = method
                                            showDeleteDialog = true
                                        },
                                        onToggleStatus = { enabled ->
                                            viewModel.toggleMethodStatus(method.id, enabled)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    categories: List<String>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                leadingIcon = {
                    Icon(Icons.Default.FilterList, null, Modifier.size(16.dp))
                }
            )
        }

        items(categories) { category ->
            val categoryEnum = PaymentCategory.fromValue(category)
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                label = { Text(categoryEnum.displayName) },
                leadingIcon = {
                    Text(categoryEnum.icon)
                }
            )
        }
    }
}

@Composable
private fun CategoryHeader(category: String) {
    val categoryEnum = PaymentCategory.fromValue(category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(categoryEnum.icon, style = MaterialTheme.typography.titleMedium)
        Text(
            categoryEnum.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PaymentMethodCard(
    method: WithdrawPaymentMethod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(method.icon, style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(
                            method.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            method.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = method.enabled,
                    onCheckedChange = onToggleStatus
                )
            }

            HorizontalDivider()

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailItem("Range", "${method.minAmount.toInt()}-${method.maxAmount.toInt()}")
                DetailItem("Processing", method.processingTime)
                DetailItem("Country", method.currency)
            }

            // Required Fields
            if (method.requiredFields.isNotEmpty()) {
                Text(
                    "User Fields:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    method.requiredFields.take(3).forEach { field ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    field.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (field.type) {
                                        FieldType.PHONE -> Icons.Default.Phone
                                        FieldType.NUMBER -> Icons.Default.Numbers
                                        FieldType.DROPDOWN -> Icons.Default.ArrowDropDown
                                        else -> Icons.Default.Edit
                                    },
                                    null,
                                    Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                    if (method.requiredFields.size > 3) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "+${method.requiredFields.size - 3}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            // Fee ranges
            if (method.fees.isNotEmpty()) {
                Text(
                    "Fees:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                method.fees.take(2).forEach { fee ->
                    Text(
                        "${fee.min.toInt()}-${fee.max.toInt()}: ${fee.fee.toInt()} ${if (fee.type == "percentage") "%" else method.currency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (method.fees.size > 2) {
                    Text(
                        "+${method.fees.size - 2} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}