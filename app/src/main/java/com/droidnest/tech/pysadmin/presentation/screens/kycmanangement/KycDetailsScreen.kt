// presentation/kycdetails/KycDetailsScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.kycmanangement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.droidnest.tech.pysadmin.domain.models.KycRequest
import com.droidnest.tech.pysadmin.domain.models.KycStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycDetailsScreen(
    requestId: String,
    onNavigateBack: () -> Unit,
    viewModel: KycDetailsViewModel = hiltViewModel()
) {
    // Load KYC request
    LaunchedEffect(requestId) {
        viewModel.loadKycRequest(requestId)
    }

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showPassportImage by remember { mutableStateOf(false) }

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
                title = { Text("KYC Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            state.kycRequest?.let { request ->
                if (request.status == KycStatus.PENDING) {
                    KycActionButtons(
                        isUpdating = state.isUpdating,
                        onApprove = { showApproveDialog = true },
                        onReject = { showRejectDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.kycRequest == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "KYC request not found",
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    KycDetailsContent(
                        request = state.kycRequest!!,
                        onPassportImageClick = { showPassportImage = true }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showApproveDialog) {
        ApproveKycDialog(
            onConfirm = { notes ->
                viewModel.approveKyc(notes)
                showApproveDialog = false
            },
            onDismiss = { showApproveDialog = false }
        )
    }

    if (showRejectDialog) {
        RejectKycDialog(
            onConfirm = { reason ->
                viewModel.rejectKyc(reason)
                showRejectDialog = false
            },
            onDismiss = { showRejectDialog = false }
        )
    }

    if (showPassportImage && state.kycRequest != null) {
        FullScreenImageDialog(
            imageUrl = state.kycRequest!!.passportImageUrl,
            onDismiss = { showPassportImage = false }
        )
    }
}

// ========================================
// KYC DETAILS CONTENT
// ========================================

@Composable
fun KycDetailsContent(
    request: KycRequest,
    onPassportImageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Status Header
        KycStatusHeader(request)

        Spacer(modifier = Modifier.height(16.dp))

        // Passport Image Card
        PassportImageCard(
            imageUrl = request.passportImageUrl,
            onClick = onPassportImageClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Information
        UserInformationCard(request)

        Spacer(modifier = Modifier.height(16.dp))

        // Passport Details
        PassportDetailsCard(request)

        Spacer(modifier = Modifier.height(16.dp))

        // Review Information (if reviewed)
        if (request.status != KycStatus.PENDING && request.status != KycStatus.UNVERIFIED) {
            ReviewInformationCard(request)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(100.dp)) // Space for bottom buttons
    }
}

// ========================================
// STATUS HEADER
// ========================================

@Composable
fun KycStatusHeader(request: KycRequest) {
    val statusColor = when (request.status) {
        KycStatus.VERIFIED -> Color(0xFF4CAF50)
        KycStatus.PENDING -> Color(0xFFFFC107)
        KycStatus.REJECTED -> Color(0xFFF44336)
        KycStatus.UNVERIFIED -> Color.Gray
    }

    val statusIcon = when (request.status) {
        KycStatus.VERIFIED -> Icons.Default.CheckCircle
        KycStatus.PENDING -> Icons.Default.Schedule
        KycStatus.REJECTED -> Icons.Default.Cancel
        KycStatus.UNVERIFIED -> Icons.Default.PersonOff
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.1f),
                            statusColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = request.status.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KYC Verification Request",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========================================
// PASSPORT IMAGE CARD
// ========================================

@Composable
fun PassportImageCard(
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Passport Document",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Passport Image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clickable { onClick() },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Passport Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Zoom indicator
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap to enlarge",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================================
// USER INFORMATION CARD
// ========================================

@Composable
fun UserInformationCard(request: KycRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "User Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            KycDetailRow(
                icon = Icons.Default.Person,
                label = "Full Name",
                value = request.userName
            )

            Spacer(modifier = Modifier.height(12.dp))

            KycDetailRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = request.userEmail
            )

            Spacer(modifier = Modifier.height(12.dp))

            KycDetailRow(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = request.userPhone
            )

            Spacer(modifier = Modifier.height(12.dp))

            KycDetailRow(
                icon = Icons.Default.Tag,
                label = "User ID",
                value = request.userId
            )
        }
    }
}

// ========================================
// PASSPORT DETAILS CARD
// ========================================

@Composable
fun PassportDetailsCard(request: KycRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CardTravel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Passport Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            KycDetailRow(
                icon = Icons.Default.Numbers,
                label = "Passport Number",
                value = request.passportNumber,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            KycDetailRow(
                icon = Icons.Default.Public,
                label = "Nationality",
                value = request.nationality,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            KycDetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Submitted At",
                value = request.submittedAt,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ========================================
// REVIEW INFORMATION CARD
// ========================================

@Composable
fun ReviewInformationCard(request: KycRequest) {
    val cardColor = when (request.status) {
        KycStatus.VERIFIED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        KycStatus.REJECTED -> Color(0xFFF44336).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Review Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!request.reviewedAt.isNullOrEmpty()) {
                KycDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Reviewed At",
                    value = request.reviewedAt
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!request.reviewedBy.isNullOrEmpty()) {
                KycDetailRow(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Reviewed By",
                    value = request.reviewedBy
                )
            }

            // Rejection Reason
            if (!request.rejectionReason.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rejection Reason",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF44336).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.rejectionReason,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

// ========================================
// DETAIL ROW
// ========================================

@Composable
fun KycDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = textColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ========================================
// ACTION BUTTONS
// ========================================

@Composable
fun KycActionButtons(
    isUpdating: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Warning
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
                        text = "This action will update user's KYC status",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFC107)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Approve Button
                Button(
                    onClick = onApprove,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
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
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve")
                    }
                }

                // Reject Button
                Button(
                    onClick = onReject,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

// ========================================
// APPROVE DIALOG
// ========================================

@Composable
fun ApproveKycDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Approve KYC",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to approve this KYC request?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Admin Notes (Optional)") },
                    placeholder = { Text("Add notes...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ========================================
// REJECT DIALOG
// ========================================

@Composable
fun RejectKycDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Reject KYC",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Please provide a reason for rejection:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection Reason *") },
                    placeholder = { Text("e.g., Passport image is not clear") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = reason.isBlank()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ========================================
// FULL SCREEN IMAGE DIALOG
// ========================================

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Passport Full View",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}