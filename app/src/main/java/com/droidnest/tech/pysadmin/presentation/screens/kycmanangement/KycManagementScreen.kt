// presentation/kycmanagement/KycManagementScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.kycmanangement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.droidnest.tech.pysadmin.domain.models.KycRequest
import com.droidnest.tech.pysadmin.domain.models.KycStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycManagementScreen(
    viewModel: KycManagementViewModel = hiltViewModel(),
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

    // ✅ Sort requests - PENDING first
    val sortedRequests = remember(state.filteredRequests) {
        state.filteredRequests.sortedWith(
            compareBy { request ->
                when (request.status) {
                    KycStatus.PENDING -> 0      // Pending সবার আগে
                    KycStatus.VERIFIED -> 1     // তারপর Verified
                    KycStatus.REJECTED -> 2     // তারপর Rejected
                    KycStatus.UNVERIFIED -> 3   // সবশেষে Unverified
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("KYC Management")
                        Text(
                            text = "${state.filteredRequests.size} requests",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    // Pending count badge
                    if (state.pendingCount > 0) {
                        Badge(
                            containerColor = Color(0xFFFFC107),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${state.pendingCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.loadAllKycRequests() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Section
            KycFilterSection(
                selectedFilter = state.selectedFilter,
                onFilterChange = { viewModel.setFilter(it) },
                pendingCount = state.pendingCount
            )

            // KYC Request List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    sortedRequests.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No KYC requests found",
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
                            items(
                                items = sortedRequests, // ✅ Sorted list ব্যবহার করা হচ্ছে
                                key = { it.id }
                            ) { request ->
                                KycRequestItem(
                                    request = request,
                                    onClick = { onNavigateToDetails(request.id) }
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
// FILTER SECTION
// ========================================

@Composable
fun KycFilterSection(
    selectedFilter: KycFilter,
    onFilterChange: (KycFilter) -> Unit,
    pendingCount: Int
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
                    imageVector = getKycFilterIcon(selectedFilter),
                    contentDescription = null,
                    tint = getKycFilterColor(selectedFilter),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Filter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getKycFilterLabel(selectedFilter),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Pending badge
                        if (selectedFilter == KycFilter.PENDING && pendingCount > 0) {
                            Badge(
                                containerColor = Color(0xFFFFC107)
                            ) {
                                Text(
                                    text = "$pendingCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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

                Text(
                    text = "KYC Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactKycFilterChip(
                        label = "All",
                        icon = Icons.Default.List,
                        color = MaterialTheme.colorScheme.primary,
                        isSelected = selectedFilter == KycFilter.ALL,
                        onClick = {
                            onFilterChange(KycFilter.ALL)
                            expanded = false
                        }
                    )

                    CompactKycFilterChip(
                        label = "Pending",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFFFC107),
                        isSelected = selectedFilter == KycFilter.PENDING,
                        onClick = {
                            onFilterChange(KycFilter.PENDING)
                            expanded = false
                        },
                        badge = if (pendingCount > 0) pendingCount else null
                    )

                    CompactKycFilterChip(
                        label = "Verified",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50),
                        isSelected = selectedFilter == KycFilter.VERIFIED,
                        onClick = {
                            onFilterChange(KycFilter.VERIFIED)
                            expanded = false
                        }
                    )

                    CompactKycFilterChip(
                        label = "Rejected",
                        icon = Icons.Default.Cancel,
                        color = Color(0xFFF44336),
                        isSelected = selectedFilter == KycFilter.REJECTED,
                        onClick = {
                            onFilterChange(KycFilter.REJECTED)
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
private fun CompactKycFilterChip(
    label: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: Int? = null
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (badge != null && badge > 0) {
                    Badge(
                        containerColor = color,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "$badge",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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

// Helper functions
private fun getKycFilterLabel(filter: KycFilter): String {
    return when (filter) {
        KycFilter.ALL -> "All Requests"
        KycFilter.PENDING -> "Pending"
        KycFilter.VERIFIED -> "Verified"
        KycFilter.REJECTED -> "Rejected"
    }
}

private fun getKycFilterIcon(filter: KycFilter): ImageVector {
    return when (filter) {
        KycFilter.ALL -> Icons.Default.List
        KycFilter.PENDING -> Icons.Default.Schedule
        KycFilter.VERIFIED -> Icons.Default.CheckCircle
        KycFilter.REJECTED -> Icons.Default.Cancel
    }
}

private fun getKycFilterColor(filter: KycFilter): Color {
    return when (filter) {
        KycFilter.ALL -> Color(0xFF6200EE)
        KycFilter.PENDING -> Color(0xFFFFC107)
        KycFilter.VERIFIED -> Color(0xFF4CAF50)
        KycFilter.REJECTED -> Color(0xFFF44336)
    }
}

// ========================================
// KYC REQUEST ITEM
// ========================================

@Composable
fun KycRequestItem(
    request: KycRequest,
    onClick: () -> Unit
) {
    val statusColor = when (request.status) {
        KycStatus.VERIFIED -> Color(0xFF4CAF50)
        KycStatus.PENDING -> Color(0xFFFFC107)
        KycStatus.REJECTED -> Color(0xFFF44336)
        KycStatus.UNVERIFIED -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (request.status) {
                        KycStatus.VERIFIED -> Icons.Default.CheckCircle
                        KycStatus.PENDING -> Icons.Default.Schedule
                        KycStatus.REJECTED -> Icons.Default.Cancel
                        KycStatus.UNVERIFIED -> Icons.Default.PersonOff
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = request.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = request.userPhone,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Passport Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pass: ${request.passportNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status & Date
            Column(horizontalAlignment = Alignment.End) {
                KycStatusBadge(status = request.status)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = request.submittedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun KycStatusBadge(status: KycStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        KycStatus.VERIFIED -> Triple(Color(0xFF4CAF50), Color.White, "VERIFIED")
        KycStatus.PENDING -> Triple(Color(0xFFFFC107), Color.Black, "PENDING")
        KycStatus.REJECTED -> Triple(Color(0xFFF44336), Color.White, "REJECTED")
        KycStatus.UNVERIFIED -> Triple(Color.Gray, Color.White, "UNVERIFIED")
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}