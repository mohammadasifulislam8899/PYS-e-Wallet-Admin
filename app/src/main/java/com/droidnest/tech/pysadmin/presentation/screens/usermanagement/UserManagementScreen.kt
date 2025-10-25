// presentation/usermanagement/UserManagementScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.usermanagement

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
import coil.compose.AsyncImage
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel = hiltViewModel(),
    onNavigateToUserDetails: (String) -> Unit = {}
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

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("User Management")
                        Text(
                            text = "${state.filteredUsers.size} users",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllUsers() }) {
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
            // Search Bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.searchUsers(it) }
            )

            // Filter Section
            UserFilterSection(
                selectedFilter = state.selectedFilter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            // User List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.filteredUsers.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PersonOff,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No users found",
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
                                items = state.filteredUsers,
                                key = { it.userId }
                            ) { user ->
                                UserItem(
                                    user = user,
                                    onClick = { onNavigateToUserDetails(user.userId) }
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
// SEARCH BAR
// ========================================

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search by name, email, phone, or ref code...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

// ========================================
// FILTER SECTION (COMPACT DESIGN)
// ========================================

@Composable
fun UserFilterSection(
    selectedFilter: UserFilter,
    onFilterChange: (UserFilter) -> Unit
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
                    imageVector = getFilterIcon(selectedFilter),
                    contentDescription = null,
                    tint = getFilterColor(selectedFilter),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Filter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getFilterLabel(selectedFilter),
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

                // Status Filters
                FilterChipGroup(
                    title = "Status",
                    filters = listOf(
                        FilterItem(UserFilter.ALL, "All", Icons.Default.People, MaterialTheme.colorScheme.primary),
                        FilterItem(UserFilter.ACTIVE, "Active", Icons.Default.CheckCircle, Color(0xFF4CAF50)),
                    ),
                    selectedFilter = selectedFilter,
                    onFilterChange = {
                        onFilterChange(it)
                        expanded = false
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // KYC Filters
                FilterChipGroup(
                    title = "KYC Status",
                    filters = listOf(
                        FilterItem(UserFilter.KYC_UNVERIFIED, "Unverified", Icons.Default.PersonOff, Color.Gray),
                        FilterItem(UserFilter.KYC_PENDING, "Pending", Icons.Default.Schedule, Color(0xFFFFC107)),
                        FilterItem(UserFilter.KYC_VERIFIED, "Verified", Icons.Default.VerifiedUser, Color(0xFF2196F3)),
                        FilterItem(UserFilter.KYC_REJECTED, "Rejected", Icons.Default.Cancel, Color(0xFFF44336))
                    ),
                    selectedFilter = selectedFilter,
                    onFilterChange = {
                        onFilterChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChipGroup(
    title: String,
    filters: List<FilterItem>,
    selectedFilter: UserFilter,
    onFilterChange: (UserFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filters.forEach { filter ->
                CompactFilterChip(
                    label = filter.label,
                    icon = filter.icon,
                    color = filter.color,
                    isSelected = selectedFilter == filter.type,
                    onClick = { onFilterChange(filter.type) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactFilterChip(
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

// Helper data class
private data class FilterItem(
    val type: UserFilter,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

// Helper functions
private fun getFilterLabel(filter: UserFilter): String {
    return when (filter) {
        UserFilter.ALL -> "All Users"
        UserFilter.ACTIVE -> "Active Users"
        UserFilter.KYC_UNVERIFIED -> "Unverified"
        UserFilter.KYC_PENDING -> "Pending KYC"
        UserFilter.KYC_VERIFIED -> "Verified"
        UserFilter.KYC_REJECTED -> "Rejected KYC"
        UserFilter.LOCKED -> "Locked Users"
    }
}

private fun getFilterIcon(filter: UserFilter): ImageVector {
    return when (filter) {
        UserFilter.ALL -> Icons.Default.People
        UserFilter.ACTIVE -> Icons.Default.CheckCircle
        UserFilter.KYC_UNVERIFIED -> Icons.Default.PersonOff
        UserFilter.KYC_PENDING -> Icons.Default.Schedule
        UserFilter.KYC_VERIFIED -> Icons.Default.VerifiedUser
        UserFilter.KYC_REJECTED -> Icons.Default.Cancel
        UserFilter.LOCKED -> Icons.Default.Lock
    }
}

private fun getFilterColor(filter: UserFilter): Color {
    return when (filter) {
        UserFilter.ALL -> Color(0xFF6200EE)
        UserFilter.ACTIVE -> Color(0xFF4CAF50)
        UserFilter.KYC_UNVERIFIED -> Color.Gray
        UserFilter.KYC_PENDING -> Color(0xFFFFC107)
        UserFilter.KYC_VERIFIED -> Color(0xFF2196F3)
        UserFilter.KYC_REJECTED -> Color(0xFFF44336)
        UserFilter.LOCKED -> Color(0xFFF44336)
    }
}

// ========================================
// USER ITEM
// ========================================

@Composable
fun UserItem(
    user: User,
    onClick: () -> Unit
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            if (user.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = user.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // KYC Status
                    when (user.kycStatus) {
                        KycStatus.VERIFIED -> StatusChip("KYC ✓", Color(0xFF2196F3))
                        KycStatus.PENDING -> StatusChip("Pending", Color(0xFFFFC107))
                        KycStatus.REJECTED -> StatusChip("Rejected", Color(0xFFF44336))
                        KycStatus.UNVERIFIED -> StatusChip("Unverified", Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳${String.format("%.2f", user.totalBalanceBDT)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}