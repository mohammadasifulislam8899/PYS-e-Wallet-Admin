package com.droidnest.tech.pysadmin.presentation.screens.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidnest.tech.pysadmin.presentation.navigation.AdminLoginRoute
import com.droidnest.tech.pysadmin.presentation.navigation.OneTimeSetupRoute
import com.droidnest.tech.pysadmin.presentation.screens.auth.login.AdminLoginScreen
import com.droidnest.tech.pysadmin.presentation.screens.auth.signup.AdminSignUpScreen

@Composable
fun AuthNavHost(
    onNavigateToMain: () -> Unit
) {
    // ✅ Create a NEW NavController specifically for the Auth flow
    val authNavController = rememberNavController()

    NavHost(
        navController = authNavController,  // ✅ Use the auth-specific controller
        startDestination = AdminLoginRoute
    ) {
        // ========== ONE-TIME SETUP (Development Only) ==========
        composable<OneTimeSetupRoute> {
            AdminSignUpScreen(
                onSetupComplete = {
                    onNavigateToMain()
                },
                onNavigateToLogin = {
                    authNavController.navigate(AdminLoginRoute)
                }
            )
        }

        // ========== LOGIN ==========
        composable<AdminLoginRoute> {
            AdminLoginScreen(
                onLoginSuccess = {
                    onNavigateToMain()
                },
                onNavigateToRegister = {
                    authNavController.navigate(OneTimeSetupRoute)
                }
            )
        }
    }
}