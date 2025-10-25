package com.droidnest.tech.pysadmin.presentation.screens.usermanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.droidnest.tech.pysadmin.domain.models.KycStatus
import com.droidnest.tech.pysadmin.domain.models.User
import com.droidnest.tech.pysadmin.presentation.screens.userdetails.UserDetailsViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: UserDetailsViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ REPLACED Block/Unblock with Lock/Unlock
    var showLockDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var showKycStatusDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },

        ) { padding ->
        Box(modifier = Modifier.padding(padding)) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 200.dp)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    state.user == null -> {
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
                                text = "User not found",
                                color = Color.Gray
                            )
                        }
                    }

                    else -> {
                        UserDetailsContent(
                            user = state.user!!,
                            onChangeKycStatus = { showKycStatusDialog = true }
                        )
                    }
                }
            }
            state.user?.let { user ->
                UserActionButtons(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp),
                    user = user,
                    isUpdating = state.isUpdating,
                    onLockAccount = { showLockDialog = true },      // ✅ CHANGED
                    onUnlockAccount = { showUnlockDialog = true },  // ✅ CHANGED
                    onUpdateBalance = { showBalanceDialog = true },
                    onResetPin = { showResetPinDialog = true }
                )
            }
        }
    }

    // ✅ REPLACED Block/Unblock Dialogs with Lock/Unlock

    // Lock Dialog
    if (showLockDialog) {
        AccountLockDialog(
            onConfirm = { duration, reason ->
                viewModel.lockUserAccount(duration, reason)
                showLockDialog = false
            },
            onDismiss = { showLockDialog = false }
        )
    }

    // Unlock Dialog
    if (showUnlockDialog) {
        ConfirmationDialog(
            title = "Unlock Account",
            message = "Are you sure you want to unlock this account? The user will be able to login immediately.",
            confirmText = "Unlock",
            confirmColor = Color(0xFF4CAF50),
            onConfirm = {
                viewModel.unlockUserAccount()
                showUnlockDialog = false
            },
            onDismiss = { showUnlockDialog = false }
        )
    }

    if (showBalanceDialog) {
        UpdateBalanceDialog(
            currentBalance = state.user?.balance ?: emptyMap(),
            onConfirm = { currency, newBalance ->
                viewModel.updateBalance(currency, newBalance)
                showBalanceDialog = false
            },
            onDismiss = { showBalanceDialog = false }
        )
    }

    if (showKycStatusDialog) {
        ChangeKycStatusDialog(
            currentStatus = state.user?.kycStatus ?: KycStatus.UNVERIFIED,
            onConfirm = { newStatus, rejectionReason ->
                viewModel.updateKycStatus(newStatus, rejectionReason)
                showKycStatusDialog = false
            },
            onDismiss = { showKycStatusDialog = false }
        )
    }

    if (showResetPinDialog) {
        ResetPinDialog(
            onConfirm = { newPin ->
                viewModel.resetUserPin(newPin)
                showResetPinDialog = false
            },
            onDismiss = { showResetPinDialog = false }
        )
    }
}

// ✅ NEW: Account Lock Dialog
@Composable
fun AccountLockDialog(
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDuration by remember { mutableStateOf(30L) }
    var customDuration by remember { mutableStateOf("") }
    var useCustomDuration by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Lock User Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select lock duration",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Duration Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationOption(
                        duration = 30,
                        label = "30 Minutes",
                        isSelected = !useCustomDuration && selectedDuration == 30L,
                        onClick = {
                            selectedDuration = 30
                            useCustomDuration = false
                        }
                    )
                    DurationOption(
                        duration = 60,
                        label = "1 Hour",
                        isSelected = !useCustomDuration && selectedDuration == 60L,
                        onClick = {
                            selectedDuration = 60
                            useCustomDuration = false
                        }
                    )
                    DurationOption(
                        duration = 1440,
                        label = "24 Hours",
                        isSelected = !useCustomDuration && selectedDuration == 1440L,
                        onClick = {
                            selectedDuration = 1440
                            useCustomDuration = false
                        }
                    )
                    DurationOption(
                        duration = 10080,
                        label = "7 Days",
                        isSelected = !useCustomDuration && selectedDuration == 10080L,
                        onClick = {
                            selectedDuration = 10080
                            useCustomDuration = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useCustomDuration,
                        onCheckedChange = { useCustomDuration = it }
                    )
                    Text("Custom duration")
                }

                if (useCustomDuration) {
                    OutlinedTextField(
                        value = customDuration,
                        onValueChange = { if (it.all { c -> c.isDigit() }) customDuration = it },
                        label = { Text("Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lock Reason
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Lock Reason *") },
                    placeholder = { Text("e.g., Suspicious activity, Multiple failed attempts") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Warning
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User won't be able to login until unlocked or time expires.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = if (useCustomDuration) {
                        customDuration.toLongOrNull() ?: 30
                    } else {
                        selectedDuration
                    }
                    if (reason.isNotBlank()) {
                        onConfirm(duration, reason)
                    }
                },
                enabled = reason.isNotBlank() &&
                        (!useCustomDuration || customDuration.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Lock Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DurationOption(
    duration: Long,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Gray.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label)
        }
    }
}

// User Details Content
@Composable
fun UserDetailsContent(
    user: User,
    onChangeKycStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        UserProfileHeader(user)

        // ✅ ADD Account Lock Status Card
        UserAccountLockCard(user)
        Spacer(modifier = Modifier.height(8.dp))

        UserBalanceCard(user)
        Spacer(modifier = Modifier.height(8.dp))
        UserPersonalInfoCard(user)
        Spacer(modifier = Modifier.height(8.dp))
        UserAccountStatusCard(user)
        Spacer(modifier = Modifier.height(8.dp))
        UserKycStatusCard(
            user = user,
            onChangeKycStatus = onChangeKycStatus
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (user.refCode.isNotEmpty()) {
            UserReferralCard(user)
            Spacer(modifier = Modifier.height(8.dp))
        }
        UserAccountInfoCard(user)
        Spacer(modifier = Modifier.height(120.dp))
    }
}

// ✅ NEW: Account Lock Status Card
@Composable
fun UserAccountLockCard(user: User) {
    val isLocked = user.accountLock?.isLocked == true &&
            user.accountLock.unlockTime > System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                Color(0xFFF44336).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Lock Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isLocked) Color(0xFFF44336) else Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLocked) {
                UserDetailRow(
                    icon = Icons.Default.Lock,
                    label = "Status",
                    value = "LOCKED",
                    valueColor = Color(0xFFF44336)
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserDetailRow(
                    icon = Icons.Default.Timer,
                    label = "Remaining Time",
                    value = user.accountLock!!.getRemainingTime()
                )

                Spacer(modifier = Modifier.height(12.dp))

                UserDetailRow(
                    icon = Icons.Default.Info,
                    label = "Lock Reason",
                    value = user.accountLock.reason
                )
            } else {
                UserDetailRow(
                    icon = Icons.Default.LockOpen,
                    label = "Status",
                    value = "ACTIVE",
                    valueColor = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun UserProfileHeader(user: User) {
    val isLocked = user.accountLock?.isLocked == true &&
            user.accountLock.unlockTime > System.currentTimeMillis()

    // Dynamic theme based on status
    val headerColor = when {
        isLocked -> Color(0xFFFF5252)
        user.kycStatus == KycStatus.VERIFIED -> Color(0xFF00C853)
        user.isAdmin -> Color(0xFF7C4DFF)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Animated Background Pattern
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val pattern = Path().apply {
                    moveTo(0f, size.height * 0.7f)
                    cubicTo(
                        size.width * 0.25f, size.height * 0.5f,
                        size.width * 0.75f, size.height * 0.9f,
                        size.width, size.height * 0.6f
                    )
                    lineTo(size.width, 0f)
                    lineTo(0f, 0f)
                    close()
                }

                drawPath(
                    path = pattern,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            headerColor.copy(alpha = 0.15f),
                            headerColor.copy(alpha = 0.05f)
                        )
                    )
                )

                // Decorative circles
                drawCircle(
                    color = headerColor.copy(alpha = 0.08f),
                    radius = 60f,
                    center = Offset(size.width * 0.85f, size.height * 0.3f)
                )
                drawCircle(
                    color = headerColor.copy(alpha = 0.06f),
                    radius = 40f,
                    center = Offset(size.width * 0.15f, size.height * 0.7f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side: Profile Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Picture with Hexagon Shape
                        Box(contentAlignment = Alignment.Center) {
                            // Hexagon background
                            Canvas(modifier = Modifier.size(90.dp)) {
                                val path = Path().apply {
                                    val radius = size.minDimension / 2
                                    val centerX = size.width / 2
                                    val centerY = size.height / 2

                                    for (i in 0..5) {
                                        val angle = (60 * i - 30) * Math.PI / 180
                                        val x = centerX + radius * cos(angle).toFloat()
                                        val y = centerY + radius * sin(angle).toFloat()
                                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                                    }
                                    close()
                                }

                                drawPath(
                                    path = path,
                                    color = headerColor.copy(alpha = 0.15f)
                                )
                            }

                            // Profile content
                            if (user.profilePictureUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(75.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(75.dp)
                                        .clip(CircleShape)
                                        .background(headerColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.split(" ").take(2)
                                            .joinToString("") { it.first().uppercase() },
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = headerColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Name & Email
                        Column {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (user.kycStatus == KycStatus.VERIFIED) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right Side: Quick Status Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = headerColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isLocked -> Icons.Default.Lock
                                user.isAdmin -> Icons.Default.AdminPanelSettings
                                user.kycStatus == KycStatus.VERIFIED -> Icons.Default.Shield
                                else -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = headerColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status Tags with unique pill design
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Account Status
                    UniquePillTag(
                        text = if (isLocked) "Locked" else "Active",
                        color = if (isLocked) Color(0xFFFF5252) else Color(0xFF00C853),
                        icon = if (isLocked) "🔒" else "✓"
                    )

                    // KYC Status
                    when (user.kycStatus) {
                        KycStatus.VERIFIED -> UniquePillTag(
                            text = "Verified",
                            color = Color(0xFF2196F3),
                            icon = "✓"
                        )

                        KycStatus.PENDING -> UniquePillTag(
                            text = "KYC Pending",
                            color = Color(0xFFFFA726),
                            icon = "⏱"
                        )

                        KycStatus.REJECTED -> UniquePillTag(
                            text = "Rejected",
                            color = Color(0xFFEF5350),
                            icon = "✕"
                        )

                        KycStatus.UNVERIFIED -> UniquePillTag(
                            text = "Unverified",
                            color = Color(0xFF78909C),
                            icon = "?"
                        )
                    }

                    // Admin Badge
                    if (user.isAdmin) {
                        UniquePillTag(
                            text = "Admin",
                            color = Color(0xFF7C4DFF),
                            icon = "★"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UniquePillTag(
    text: String,
    color: Color,
    icon: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Account Status Card
@Composable
fun UserAccountStatusCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Account Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ REPLACED isBlocked check with accountLock
            val isLocked = user.accountLock?.isLocked == true &&
                    user.accountLock.unlockTime > System.currentTimeMillis()

            UserDetailRow(
                icon = if (isLocked) Icons.Default.Lock else Icons.Default.CheckCircle,
                label = "Account Status",
                value = if (isLocked) "Locked" else "Active",
                valueColor = if (isLocked) Color(0xFFF44336) else Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(12.dp))

            UserDetailRow(
                icon = if (user.profileCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                label = "Profile Status",
                value = if (user.profileCompleted) "Completed" else "Incomplete",
                valueColor = if (user.profileCompleted) Color(0xFF4CAF50) else Color(0xFFFFC107)
            )

            if (user.lastActive.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.AccessTime,
                    label = "Last Active",
                    value = user.lastActive
                )
            }
        }
    }
}

// Action Buttons
@Composable
fun UserActionButtons(
    modifier: Modifier = Modifier,
    user: User,
    isUpdating: Boolean,
    onLockAccount: () -> Unit,    // ✅ CHANGED
    onUnlockAccount: () -> Unit,  // ✅ CHANGED
    onUpdateBalance: () -> Unit,
    onResetPin: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onUpdateBalance,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Balance")
            }

            Button(
                onClick = onResetPin,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                )
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset PIN")
            }

            // ✅ REPLACED Block/Unblock with Lock/Unlock
            val isLocked = user.accountLock?.isLocked == true &&
                    user.accountLock.unlockTime > System.currentTimeMillis()

            if (isLocked) {
                Button(
                    onClick = onUnlockAccount,
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock Account")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onLockAccount,
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFF44336)
                        )
                    } else {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Account")
                    }
                }
            }
        }
    }
}


@Composable
fun ResetPinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Reset User PIN",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Enter new 6-digit PIN for this user",  // ✅ CHANGED: 4-digit to 6-digit
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New PIN
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it
                    },  // ✅ CHANGED: 4 to 6
                    label = { Text("New PIN") },
                    placeholder = { Text("000000") },  // ✅ CHANGED: 0000 to 000000
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm PIN
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it
                    },  // ✅ CHANGED: 4 to 6
                    label = { Text("Confirm PIN") },
                    placeholder = { Text("000000") },  // ✅ CHANGED: 0000 to 000000
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Validation Messages
                when {
                    newPin.isEmpty() -> {
                        Text(
                            text = "⚠ PIN cannot be empty",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }

                    newPin.length < 6 -> {  // ✅ CHANGED: 4 to 6
                        Text(
                            text = "⚠ PIN must be 6 digits (${newPin.length}/6)",  // ✅ CHANGED: 4 to 6
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }

                    confirmPin.isNotEmpty() && newPin != confirmPin -> {
                        Text(
                            text = "⚠ PINs do not match",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }

                    newPin.length == 6 && newPin == confirmPin -> {  // ✅ CHANGED: 4 to 6
                        Text(
                            text = "✓ PINs match",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // Warning Card
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User's PIN will be reset. Share it securely with the user.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length == 6 && newPin == confirmPin) {  // ✅ CHANGED: 4 to 6
                        onConfirm(newPin)
                    }
                },
                enabled = newPin.length == 6 && newPin == confirmPin && newPin.isNotBlank()  // ✅ CHANGED: 4 to 6
            ) {
                Text("Reset PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangeKycStatusDialog(
    currentStatus: KycStatus,
    onConfirm: (KycStatus, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var rejectionReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Change KYC Status",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Current Status: ${currentStatus.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select New Status:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Options
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KycStatusOption(
                        status = KycStatus.UNVERIFIED,
                        isSelected = selectedStatus == KycStatus.UNVERIFIED,
                        onClick = { selectedStatus = KycStatus.UNVERIFIED },
                        icon = Icons.Default.PersonOff,
                        color = Color.Gray
                    )

                    KycStatusOption(
                        status = KycStatus.PENDING,
                        isSelected = selectedStatus == KycStatus.PENDING,
                        onClick = { selectedStatus = KycStatus.PENDING },
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFFFC107)
                    )

                    KycStatusOption(
                        status = KycStatus.VERIFIED,
                        isSelected = selectedStatus == KycStatus.VERIFIED,
                        onClick = { selectedStatus = KycStatus.VERIFIED },
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )

                    KycStatusOption(
                        status = KycStatus.REJECTED,
                        isSelected = selectedStatus == KycStatus.REJECTED,
                        onClick = { selectedStatus = KycStatus.REJECTED },
                        icon = Icons.Default.Cancel,
                        color = Color(0xFFF44336)
                    )
                }

                // Rejection Reason Input (only for REJECTED status)
                if (selectedStatus == KycStatus.REJECTED) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        placeholder = { Text("Enter reason...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }

                // Warning
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This is a development feature. Use carefully!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason =
                        if (selectedStatus == KycStatus.REJECTED && rejectionReason.isNotBlank()) {
                            rejectionReason
                        } else null

                    onConfirm(selectedStatus, reason)
                },
                enabled = selectedStatus != currentStatus &&
                        (selectedStatus != KycStatus.REJECTED || rejectionReason.isNotBlank())
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun KycStatusOption(
    status: KycStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    color: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) {
            BorderStroke(2.dp, color)
        } else {
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = status.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else Color.Gray
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun UserBalanceCard(
    user: User
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Balance Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Total Balance
            Text(
                text = "Total Balance (BDT)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "৳${String.format("%.2f", user.totalBalanceBDT)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Individual Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BalanceItem(
                    label = "BDT Balance",
                    amount = "৳${String.format("%.2f", user.balance["BDT"] ?: 0.0)}"
                )
                BalanceItem(
                    label = "MYR Balance",
                    amount = "RM ${String.format("%.2f", user.balance["MYR"] ?: 0.0)}"
                )
            }
        }
    }
}

@Composable
fun UserPersonalInfoCard(
    user: User
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            UserDetailRow(
                icon = Icons.Default.Person,
                label = "Full Name",
                value = user.name
            )

            Spacer(modifier = Modifier.height(12.dp))

            UserDetailRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email
            )

            Spacer(modifier = Modifier.height(12.dp))

            UserDetailRow(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = user.phone
            )

            if (user.country.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.Public,
                    label = "Country",
                    value = user.country
                )
            }

            if (user.address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.LocationOn,
                    label = "Address",
                    value = user.address
                )
            }

            if (user.dateOfBirth.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.Cake,
                    label = "Date of Birth",
                    value = user.dateOfBirth
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            UserDetailRow(
                icon = Icons.Default.Tag,
                label = "User ID",
                value = user.userId
            )
        }
    }
}

@Composable
fun UserKycStatusCard(
    user: User,
    onChangeKycStatus: () -> Unit  // ✅ ADD THIS PARAMETER
) {
    val (statusColor, statusText) = when (user.kycStatus) {
        KycStatus.VERIFIED -> Color(0xFF4CAF50) to "Verified"
        KycStatus.PENDING -> Color(0xFFFFC107) to "Pending"
        KycStatus.REJECTED -> Color(0xFFF44336) to "Rejected"
        KycStatus.UNVERIFIED -> Color.Gray to "Unverified"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KYC Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // ✅ ADD THIS BUTTON
                FilledTonalButton(
                    onClick = onChangeKycStatus,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Status", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            UserDetailRow(
                icon = Icons.Default.VerifiedUser,
                label = "KYC Status",
                value = statusText,
                valueColor = statusColor
            )

            if (!user.kycRequestId.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.Receipt,
                    label = "KYC Request ID",
                    value = user.kycRequestId
                )
            }

            if (!user.kycVerifiedAt.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Verified At",
                    value = user.kycVerifiedAt
                )
            }

            if (!user.kycRejectionReason.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Rejection Reason",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF44336).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = user.kycRejectionReason,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserReferralCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Referral Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            UserDetailRow(
                icon = Icons.Default.QrCode,
                label = "Referral Code",
                value = user.refCode
            )

            Spacer(modifier = Modifier.height(12.dp))

            UserDetailRow(
                icon = Icons.Default.People,
                label = "Total Referrals",
                value = user.referralCount.toString()
            )

            Spacer(modifier = Modifier.height(12.dp))

            UserDetailRow(
                icon = Icons.Default.MonetizationOn,
                label = "Referral Earnings",
                value = "৳${String.format("%.2f", user.referralEarnings)}"
            )

            if (user.referredBy.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.PersonAdd,
                    label = "Referred By",
                    value = user.referredBy
                )
            }
        }
    }
}

@Composable
fun UserAccountInfoCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Account Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (user.createdAt.isNotEmpty()) {
                UserDetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Account Created",
                    value = user.createdAt
                )
            }

            if (user.pin != null) {
                Spacer(modifier = Modifier.height(12.dp))
                UserDetailRow(
                    icon = Icons.Default.Lock,
                    label = "PIN Status",
                    value = "Set",
                    valueColor = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun UserDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
fun UserStatusBadge(
    label: String,
    color: Color,
    icon: ImageVector
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BalanceItem(label: String, amount: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = confirmColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpdateBalanceDialog(
    currentBalance: Map<String, Double>,
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCurrency by remember { mutableStateOf("BDT") }
    var newBalanceText by remember {
        mutableStateOf(currentBalance[selectedCurrency]?.toString() ?: "0.0")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update User Balance",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Current Balance: ${selectedCurrency} ${currentBalance[selectedCurrency] ?: 0.0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Currency Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCurrency == "BDT",
                        onClick = {
                            selectedCurrency = "BDT"
                            newBalanceText = currentBalance["BDT"]?.toString() ?: "0.0"
                        },
                        label = { Text("BDT") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCurrency == "MYR",
                        onClick = {
                            selectedCurrency = "MYR"
                            newBalanceText = currentBalance["MYR"]?.toString() ?: "0.0"
                        },
                        label = { Text("MYR") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Balance Input
                OutlinedTextField(
                    value = newBalanceText,
                    onValueChange = { newBalanceText = it },
                    label = { Text("New Balance") },
                    placeholder = { Text("Enter amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newBalance = newBalanceText.toDoubleOrNull()
                    if (newBalance != null && newBalance >= 0) {
                        onConfirm(selectedCurrency, newBalance)
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}











