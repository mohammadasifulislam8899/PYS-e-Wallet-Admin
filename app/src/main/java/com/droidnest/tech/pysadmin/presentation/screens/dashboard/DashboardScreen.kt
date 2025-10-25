@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidnest.tech.pysadmin.presentation.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidnest.tech.pysadmin.presentation.screens.dashboard.ratemanagement.RateManagementViewModel
import com.droidnest.tech.pysadmin.presentation.screens.dashboard.ratemanagement.UpdateRateBottomSheet
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToAddMoneyMethods: () -> Unit = {},
    onNavigateToWithdrawMethods: () -> Unit = {},
    rateManagementViewModel: RateManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val rateManagementState by rateManagementViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh()
            }
        }
    }

    if (rateManagementState.showBottomSheet) {
        UpdateRateBottomSheet(
            currentRate = rateManagementState.exchangeRate.rate,
            isUpdating = rateManagementState.isUpdating,
            onDismiss = rateManagementViewModel::hideBottomSheet,
            onUpdate = rateManagementViewModel::updateExchangeRate
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DashboardTopBar(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        actionColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading && state.stats.totalUsers == 0) {
            LoadingState(paddingValues)
        } else {
            DashboardContent(
                state = state,
                paddingValues = paddingValues,
                onNavigateToAddMoneyMethods = onNavigateToAddMoneyMethods,
                onNavigateToWithdrawMethods = onNavigateToWithdrawMethods,
                onNavigateToRateManagement = rateManagementViewModel::showBottomSheet
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Admin Control Panel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.then(
                        if (isRefreshing) Modifier.animateRotation()
                        else Modifier
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Loading dashboard...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    paddingValues: PaddingValues,
    onNavigateToAddMoneyMethods: () -> Unit,
    onNavigateToWithdrawMethods: () -> Unit,
    onNavigateToRateManagement: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 👥 User Stats Section
        item {
            AnimatedSection(delay = 0) {
                StatsSection(
                    title = "User Overview",
                    icon = Icons.Default.People
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModernStatCard(
                            title = "Total Users",
                            value = state.stats.totalUsers.toString(),
                            icon = Icons.Default.People,
                            gradient = listOf(
                                Color(0xFF667eea),
                                Color(0xFF764ba2)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatCard(
                            title = "Active Users",
                            value = state.stats.activeUsers.toString(),
                            icon = Icons.Default.CheckCircle,
                            gradient = listOf(
                                Color(0xFF06beb6),
                                Color(0xFF48b1bf)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 🔐 KYC Stats Section
        item {
            AnimatedSection(delay = 100) {
                StatsSection(
                    title = "KYC Verification",
                    icon = Icons.Default.VerifiedUser
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModernStatCard(
                            title = "Pending",
                            value = state.stats.pendingKyc.toString(),
                            icon = Icons.Default.PendingActions,
                            gradient = listOf(
                                Color(0xFFf093fb),
                                Color(0xFFf5576c)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatCard(
                            title = "Verified",
                            value = state.stats.verifiedKyc.toString(),
                            icon = Icons.Default.Verified,
                            gradient = listOf(
                                Color(0xFF4facfe),
                                Color(0xFF00f2fe)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 💳 Transaction Stats Section
        item {
            AnimatedSection(delay = 200) {
                StatsSection(
                    title = "Transactions",
                    icon = Icons.Default.SwapHoriz
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModernStatCard(
                            title = "Pending",
                            value = state.stats.pendingTransactions.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            gradient = listOf(
                                Color(0xFFfa709a),
                                Color(0xFFfee140)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatCard(
                            title = "Completed",
                            value = state.stats.successTransactions.toString(),
                            icon = Icons.Default.DoneAll,
                            gradient = listOf(
                                Color(0xFF30cfd0),
                                Color(0xFF330867)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 💰 Revenue & Profit Section
        item {
            AnimatedSection(delay = 300) {
                StatsSection(
                    title = "Revenue & Profit",
                    icon = Icons.Default.TrendingUp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Today's Performance
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "📊 Today's Performance",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ModernStatCard(
                                    title = "Gross Revenue",
                                    value = "৳${formatAmount(state.stats.todayGrossRevenue)}",
                                    icon = Icons.Default.TrendingUp,
                                    gradient = listOf(
                                        Color(0xFF11998e),
                                        Color(0xFF38ef7d)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                ModernStatCard(
                                    title = "Expenses",
                                    value = "৳${formatAmount(state.stats.todayExpenses)}",
                                    icon = Icons.Default.TrendingDown,
                                    gradient = listOf(
                                        Color(0xFFeb3349),
                                        Color(0xFFf45c43)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Highlighted Net Profit
                            HighlightedStatCard(
                                title = "💎 Today's Net Profit",
                                value = "৳${formatAmount(state.stats.todayNetRevenue)}",
                                icon = Icons.Default.MonetizationOn,
                                gradient = listOf(
                                    Color(0xFF8E2DE2),
                                    Color(0xFF4A00E0)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Monthly Performance
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "📈 Monthly Performance",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ModernStatCard(
                                    title = "Gross Revenue",
                                    value = "৳${formatAmount(state.stats.monthlyGrossRevenue)}",
                                    icon = Icons.Default.CalendarMonth,
                                    gradient = listOf(
                                        Color(0xFF2193b0),
                                        Color(0xFF6dd5ed)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                ModernStatCard(
                                    title = "Net Profit",
                                    value = "৳${formatAmount(state.stats.monthlyNetRevenue)}",
                                    icon = Icons.Default.AccountBalance,
                                    gradient = listOf(
                                        Color(0xFFee0979),
                                        Color(0xFFff6a00)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ⚙️ Management Section
        item {
            AnimatedSection(delay = 400) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Quick Actions",
                        icon = Icons.Default.Settings
                    )

                    ManagementCard {
                        ManagementItem(
                            icon = Icons.Default.CurrencyExchange,
                            title = "Exchange Rate",
                            subtitle = "Update MYR to BDT conversion rate",
                            onClick = onNavigateToRateManagement
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ManagementItem(
                            icon = Icons.Default.AddCard,
                            title = "Add Money Methods",
                            subtitle = "Configure payment gateway options",
                            onClick = onNavigateToAddMoneyMethods
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ManagementItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Withdraw Methods",
                            subtitle = "Manage withdrawal configurations",
                            onClick = onNavigateToWithdrawMethods
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Bottom Spacer
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AnimatedSection(
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
    ) {
        content()
    }
}

@Composable
private fun StatsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = title, icon = icon)
        content()
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModernStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(gradient))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
private fun HighlightedStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradient: List<Color>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.horizontalGradient(gradient))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ManagementItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 10000000 -> String.format("%.2f Cr", amount / 10000000)
        amount >= 100000 -> String.format("%.2f L", amount / 100000)
        amount >= 1000 -> String.format("%.1f K", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}

@Composable
private fun Modifier.animateRotation(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    return graphicsLayer { rotationZ = rotation }
}