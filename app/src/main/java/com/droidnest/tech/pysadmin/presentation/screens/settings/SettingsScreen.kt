// admin_app/presentation/settings/SettingsScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.droidnest.tech.pysadmin.domain.models.Admin
import com.droidnest.tech.pysadmin.domain.models.AdminRole
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Section
            item {
                ProfileCard(
                    admin = state.admin,
                    onClick = onNavigateToProfile
                )
            }

            // Help & Support Section
            item {
                SectionTitle("Help & Support")
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.Help,
                    title = "Admin FAQ",
                    description = "Common admin questions",
                    onClick = { showFaqDialog = true }
                )
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.Support,
                    title = "Technical Support",
                    description = "Contact developer team",
                    onClick = { openDeveloperSupport(context) }
                )
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.MenuBook,
                    title = "Admin Guide",
                    description = "How to manage the system",
                    onClick = { /* Open guide */ }
                )
            }

            // Legal Section
            item {
                SectionTitle("Legal & Policies")
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.Description,
                    title = "Terms & Conditions",
                    description = "Admin terms of service",
                    onClick = { showTermsDialog = true }
                )
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    description = "How we protect data",
                    onClick = { showPrivacyDialog = true }
                )
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.Shield,
                    title = "Admin Responsibilities",
                    description = "Rules and guidelines",
                    onClick = { /* Open responsibilities */ }
                )
            }

            // About Section
            item {
                SectionTitle("About")
            }

            item {
                InfoItemCard(
                    icon = Icons.Outlined.Info,
                    title = "About Admin Panel",
                    description = "Learn about this system",
                    onClick = { showAboutDialog = true }
                )
            }

            // Version Info
            item {
                AdminVersionInfoCard()
            }

            // Logout Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Logout from Admin Panel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ✅ Dialogs
    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showFaqDialog) {
        AdminFaqDialog(onDismiss = { showFaqDialog = false })
    }

    if (showTermsDialog) {
        AdminTermsDialog(onDismiss = { showTermsDialog = false })
    }

    if (showPrivacyDialog) {
        AdminPrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showAboutDialog) {
        AdminAboutDialog(onDismiss = { showAboutDialog = false })
    }
}

// ========================================
// ✅ Developer Support
// ========================================

private fun openDeveloperSupport(context: Context) {
    val phoneNumber = "+8801768773889"
    val message = "Hi DroidNest Team,\n\nI'm an admin of PYS e-Wallet Admin Panel.\n\nI need technical support regarding: "

    try {
        val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://send?phone=$phoneNumber&text=${Uri.encode(message)}")
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }
}

// ========================================
// ✅ Logout Dialog
// ========================================

@Composable
private fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Logout from Admin Panel?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Are you sure you want to logout?",
                    style = MaterialTheme.typography.bodyLarge
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "You'll need to login again to access admin panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Yes, Logout")
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
// ✅ Admin FAQ Dialog
// ========================================

@Composable
private fun AdminFaqDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Admin FAQ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FaqItem(
                    question = "How do I approve Add Money requests?",
                    answer = "Go to Add Money tab → View pending requests → Review payment proof → Verify transaction → Tap Approve or Reject."
                )

                FaqItem(
                    question = "How do I process withdrawals?",
                    answer = "Go to Withdraw tab → View pending requests → Verify user account details → Process payment manually → Mark as Approved."
                )

                FaqItem(
                    question = "How do I verify KYC documents?",
                    answer = "Go to Users tab → View KYC pending → Review documents → Verify identity → Approve or Reject with reason."
                )

                FaqItem(
                    question = "How do I lock/unlock user accounts?",
                    answer = "Go to Users tab → Select user → Tap Lock Account → Provide reason → Account will be locked immediately."
                )

                FaqItem(
                    question = "What if I see suspicious activity?",
                    answer = "Lock the account immediately, review all transactions, contact the user, and report to main admin if fraud is suspected."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Q: $question",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "A: $answer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

// ========================================
// ✅ Terms Dialog
// ========================================

@Composable
private fun AdminTermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Admin Terms & Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TermsSection(
                    title = "1. Admin Responsibilities",
                    content = "As an admin, you are responsible for managing user accounts, approving transactions, verifying KYC, and maintaining system security."
                )

                TermsSection(
                    title = "2. Data Privacy",
                    content = "You must protect all user data. Never share user information, passwords, or transaction details with unauthorized parties."
                )

                TermsSection(
                    title = "3. Fair Processing",
                    content = "All users must be treated equally. Approve/reject requests based on validity only. No discrimination or favoritism."
                )

                TermsSection(
                    title = "4. Timely Response",
                    content = "Process add money and withdrawal requests within 24-48 hours. Respond to user queries promptly."
                )

                TermsSection(
                    title = "5. Security",
                    content = "Keep your admin credentials secure. Never share your login details. Report any security issues immediately."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

// ========================================
// ✅ Privacy Dialog
// ========================================

@Composable
private fun AdminPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Admin Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TermsSection(
                    title = "Admin Data Collection",
                    content = "We collect admin login credentials, activity logs, and IP addresses for security purposes."
                )

                TermsSection(
                    title = "User Data Access",
                    content = "As an admin, you have access to user data for management purposes only. This data must never be shared or misused."
                )

                TermsSection(
                    title = "Activity Monitoring",
                    content = "All admin actions are logged and monitored for security and audit purposes."
                )

                TermsSection(
                    title = "Data Security",
                    content = "All data is encrypted and stored securely. Access is restricted to authorized admins only."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ========================================
// ✅ About Dialog
// ========================================

@Composable
private fun AdminAboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "About Admin Panel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PYS e-Wallet Admin Panel is a comprehensive management system for handling user transactions, KYC verification, and system administration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                TermsSection(
                    title = "Key Features",
                    content = "• Dashboard with statistics\n• Add money management\n• Withdrawal processing\n• User account management\n• KYC verification\n• Transaction history\n• Activity logs"
                )

                TermsSection(
                    title = "Developer Contact",
                    content = "WhatsApp: +8801768773889\nEmail: droidnest.tech@gmail.com\nCompany: DroidNest Technologies"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ========================================
// ✅ UI Components
// ========================================

@Composable
private fun ProfileCard(
    admin: Admin?,
    onClick: () -> Unit
) {
    val roleColor = when (admin?.role) {
        AdminRole.SUPER_ADMIN -> Color(0xFFE91E63) // Pink
        AdminRole.ADMIN -> Color(0xFF7C4DFF) // Purple
        AdminRole.MODERATOR -> Color(0xFF00BCD4) // Cyan
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Pattern
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val pattern = Path().apply {
                    moveTo(0f, size.height * 0.6f)
                    cubicTo(
                        size.width * 0.3f, size.height * 0.4f,
                        size.width * 0.7f, size.height * 0.8f,
                        size.width, size.height * 0.5f
                    )
                    lineTo(size.width, 0f)
                    lineTo(0f, 0f)
                    close()
                }

                drawPath(
                    path = pattern,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            roleColor.copy(alpha = 0.12f),
                            roleColor.copy(alpha = 0.04f)
                        )
                    )
                )

                // Decorative elements
                drawCircle(
                    color = roleColor.copy(alpha = 0.08f),
                    radius = 45f,
                    center = Offset(size.width * 0.85f, size.height * 0.3f)
                )
                drawCircle(
                    color = roleColor.copy(alpha = 0.05f),
                    radius = 30f,
                    center = Offset(size.width * 0.15f, size.height * 0.7f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Picture with Hexagon
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
                            color = roleColor.copy(alpha = 0.15f)
                        )
                    }

                    // Profile content
                    if (admin?.profilePictureUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = admin.profilePictureUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(roleColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = admin?.name?.split(" ")?.take(2)
                                ?.joinToString("") { it.first().uppercase() } ?: "A"

                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineMedium,
                                color = roleColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Role icon badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(26.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = roleColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (admin?.role) {
                                    AdminRole.SUPER_ADMIN -> Icons.Default.Shield
                                    AdminRole.ADMIN -> Icons.Default.AdminPanelSettings
                                    AdminRole.MODERATOR -> Icons.Default.Visibility
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Admin Information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Name
                    Text(
                        text = admin?.name ?: "Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Email
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = roleColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = roleColor
                            )
                        }
                        Text(
                            text = admin?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Phone (if available)
                    if (!admin?.phone.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        color = roleColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(5.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = roleColor
                                )
                            }
                            Text(
                                text = admin?.phone ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = roleColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, roleColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when (admin?.role) {
                                    AdminRole.SUPER_ADMIN -> "★"
                                    AdminRole.ADMIN -> "⚡"
                                    AdminRole.MODERATOR -> "👁"
                                    else -> "•"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                            Text(
                                text = admin?.role?.name?.replace("_", " ") ?: "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = roleColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Chevron
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AdminVersionInfoCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "PYS Admin Panel",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = "Build 001 (Admin)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Developed by DroidNest Technologies",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = "© 2024 PYS e-Wallet. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Surface(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "⚠️ Admin Access Only - Unauthorized use prohibited",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}