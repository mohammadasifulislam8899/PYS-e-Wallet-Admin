// admin_app/presentation/navigation/AdminNavGraph.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.droidnest.tech.pysadmin.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.droidnest.tech.pysadmin.presentation.screens.auth.AuthNavHost
import com.droidnest.tech.pysadmin.presentation.screens.auth.login.AdminLoginScreen
import com.droidnest.tech.pysadmin.presentation.screens.auth.signup.AdminSignUpScreen
import com.droidnest.tech.pysadmin.presentation.screens.dashboard.DashboardScreen
import com.droidnest.tech.pysadmin.presentation.screens.kycmanangement.KycDetailsScreen
import com.droidnest.tech.pysadmin.presentation.screens.kycmanangement.KycManagementScreen
import com.droidnest.tech.pysadmin.presentation.screens.paymentmethods.AddMoneyPaymentMethodsScreen
import com.droidnest.tech.pysadmin.presentation.screens.paymentmethods.add_edit.AddEditAddMoneyPaymentMethodScreen
import com.droidnest.tech.pysadmin.presentation.screens.settings.SettingsScreen
import com.droidnest.tech.pysadmin.presentation.screens.splash.SplashScreen
import com.droidnest.tech.pysadmin.presentation.screens.transaction.AdminTransactionScreen
import com.droidnest.tech.pysadmin.presentation.screens.transactiondetails.TransactionDetailsScreen
import com.droidnest.tech.pysadmin.presentation.screens.usermanagement.UserManagementScreen
import com.droidnest.tech.pysadmin.presentation.screens.usermanagement.UserDetailsScreen
import com.droidnest.tech.pysadmin.presentation.screens.withdrawpaymentmethod.AddEditPaymentMethodScreen
import com.droidnest.tech.pysadmin.presentation.screens.withdrawpaymentmethod.WithdrawPaymentMethodsScreen
import com.google.firebase.auth.FirebaseAuth

// ========================================
// MAIN NAVIGATION GRAPH
// ========================================

@Composable
fun AdminApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        // ========== SPLASH ==========
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(AuthRoute) {  // ✅ Navigate to AuthRoute
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(AdminDashboardRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<AuthRoute> {
            AuthNavHost(
                // ✅ Don't pass the parent navController anymore
                onNavigateToMain = {
                    navController.navigate(AdminDashboardRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                }
            )
        }


        // ========== ADDITIONAL ROUTES ==========
        composable<AdminRateManagementRoute> {
            RateManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<AddMoneyPaymentMethodsRoute> {
            AddMoneyPaymentMethodsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdd = {
                    navController.navigate(AddAddMoneyPaymentMethodScreenRoute)
                },
                onNavigateToEdit = { methodId ->
                    navController.navigate(AddEditAddMoneyPaymentMethodScreenRoute(methodId))
                }
            )
        }

        composable<AddEditAddMoneyPaymentMethodScreenRoute> {
            val route = it.toRoute<AddEditAddMoneyPaymentMethodScreenRoute>()
            AddEditAddMoneyPaymentMethodScreen(
                methodId = route.id,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable<AddAddMoneyPaymentMethodScreenRoute> {
            AddEditAddMoneyPaymentMethodScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable<ReportsRoute> {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // ========== PAYMENT METHODS ROUTES ==========

        // Withdraw Payment Methods List
        composable<WithdrawPaymentMethodsRoute> {
            WithdrawPaymentMethodsScreen(
                onNavigateToAdd = {
                    navController.navigate(AddPaymentMethodRoute)
                },
                onNavigateToEdit = { methodId ->
                    navController.navigate(EditPaymentMethodRoute(methodId))
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Add New Payment Method
        composable<AddPaymentMethodRoute> {
            AddEditPaymentMethodScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Existing Payment Method
        composable<EditPaymentMethodRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditPaymentMethodRoute>()
            AddEditPaymentMethodScreen(
                methodId = route.methodId,  // Pass this to ViewModel
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<ActivityLogsRoute> {
            ActivityLogsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NotificationsRoute> {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ========== MAIN APP ==========
        composable<AdminDashboardRoute> {
            AdminMainScreen(
                mainNavHostController = navController,
                onUserClick = { userId ->
                    navController.navigate(UserDetailsRoute(userId))
                },
                onNavigateToDetails = {
                    navController.navigate(KycDetailsRoute(it))
                },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(AuthRoute) {  // ✅ Navigate to AuthRoute, not AdminLoginRoute
                        popUpTo(AdminDashboardRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<TransactionDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TransactionDetailsRoute>()
            TransactionDetailsScreen(
                transactionId = route.transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<UserDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<UserDetailsRoute>()
            UserDetailsScreen(
                userId = route.userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<KycDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<KycDetailsRoute>()
            KycDetailsScreen(
                requestId = route.kycRequestId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// ========================================
// MAIN SCREEN WITH BOTTOM NAVIGATION
// ========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMainScreen(
    mainNavHostController: NavHostController,
    onUserClick: (String) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            AdminBottomNavigation(navController = innerNavController)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = AdminDashboardRoute,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<AdminDashboardRoute> {
                DashboardScreen(
                    onNavigateToAddMoneyMethods = {
                        mainNavHostController.navigate(AddMoneyPaymentMethodsRoute)
                    },
                    onNavigateToWithdrawMethods = {
                        mainNavHostController.navigate(WithdrawPaymentMethodsRoute)
                    }
                )
            }

            composable<AdminKycRoute> {
                KycManagementScreen(
                    onNavigateToDetails = { kycRequestId ->
                        onNavigateToDetails(kycRequestId)
                    }
                )
            }

            composable<AdminUsersRoute> {
                UserManagementScreen(
                    onNavigateToUserDetails = { userId ->
                        onUserClick(userId)
                    }
                )
            }

            composable<AdminSettingsRoute> {
                SettingsScreen(
                    onLogout = {
                        onLogout()
                    },
                    onNavigateToProfile = {
                        // TODO: Implement profile screen
                    }
                )
            }

            composable<AdminTransactionsRoute> {
                AdminTransactionScreen(
                    onNavigateToDetails = { transactionId ->
                        mainNavHostController.navigate(TransactionDetailsRoute(transactionId))
                    }
                )
            }
        }
    }
}

// ========================================
// BOTTOM NAVIGATION
// ========================================

@Composable
private fun AdminBottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Dashboard", Icons.Default.Dashboard, AdminDashboardRoute),
        BottomNavItem("Transactions", Icons.Default.Receipt, AdminTransactionsRoute),
        BottomNavItem("KYC", Icons.Default.VerifiedUser, AdminKycRoute),
        BottomNavItem("Users", Icons.Default.People, AdminUsersRoute),
        BottomNavItem("Settings", Icons.Default.Settings, AdminSettingsRoute)
    )

    NavigationBar {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route::class.qualifiedName } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

// ========================================
// DASHBOARD SCREEN
// ========================================


@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

// ========================================
// PLACEHOLDER SCREENS
// ========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateManagementScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Text("Rate Management - Coming Soon")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Text("Reports - Coming Soon")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityLogsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Text("Activity Logs - Coming Soon")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Text("Notifications - Coming Soon")
        }
    }
}