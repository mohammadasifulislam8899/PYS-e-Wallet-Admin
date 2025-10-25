// admin_app/presentation/splash/SplashScreen.kt
package com.droidnest.tech.pysadmin.presentation.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    // Animation states
    var startAnimation by remember { mutableStateOf(false) }

    // Logo animations
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "logoAlpha"
    )

    // Text animations
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "textAlpha"
    )

    // Infinite pulse animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Start animation on launch
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Handle navigation
    LaunchedEffect(state.isLoggedIn) {
        when {
            state.isLoggedIn == true -> {
                delay(500)
                onNavigateToDashboard()
            }
            state.isLoggedIn == false -> {
                delay(500)
                onNavigateToAuth()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),  // Purple
                        Color(0xFF764ba2),  // Deep Purple
                        Color(0xFF6B8DD6),  // Blue
                        Color(0xFF8E37D7)   // Violet
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Animated circles in background
        FloatingCircles()

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo container with shadow
            Surface(
                modifier = Modifier
                    .scale(logoScale * pulse)
                    .alpha(logoAlpha)
                    .size(140.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        spotColor = Color.White.copy(alpha = 0.3f)
                    ),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Replace with your logo
                    // If you have a logo image:
                    /*
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "PYS Logo",
                        modifier = Modifier.size(80.dp)
                    )
                    */

                    // Or use icon:
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "PYS Admin",
                        modifier = Modifier.size(80.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App name with shimmer effect
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 800,
                        delayMillis = 300
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 800,
                        delayMillis = 300
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PYS",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 4.sp,
                        style = MaterialTheme.typography.displayLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                                blurRadius = 8f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ADMIN PANEL",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 6.sp,
                        modifier = Modifier.alpha(textAlpha)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tagline
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 800,
                        delayMillis = 600
                    )
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "Money Transfer Management",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Loading indicator
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 800
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom loading indicator
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            color = Color.White,
                            strokeWidth = 3.dp,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Loading...",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // Error message
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Red.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Bottom section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Features
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                FeatureChip(
                    icon = Icons.Default.AdminPanelSettings,
                    text = "Secure"
                )
                FeatureChip(
                    icon = Icons.Default.AdminPanelSettings,
                    text = "Fast"
                )
                FeatureChip(
                    icon = Icons.Default.AdminPanelSettings,
                    text = "Reliable"
                )
            }

            // Version
            Text(
                text = "Version 1.0.0",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Copyright
            Text(
                text = "© 2024 PYS. All rights reserved.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Light
            )
        }
    }
}

// Feature chip component
@Composable
private fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Floating circles animation
@Composable
private fun FloatingCircles() {
    val infiniteTransition = rememberInfiniteTransition(label = "circles")

    // Circle 1
    val circle1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle1"
    )

    // Circle 2
    val circle2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle2"
    )

    // Circle 3
    val circle3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle3"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Circle 1 - Top Right
        Surface(
            modifier = Modifier
                .offset(x = 200.dp, y = (-100).dp + circle1Offset.dp)
                .size(200.dp)
                .alpha(0.1f),
            shape = CircleShape,
            color = Color.White
        ) {}

        // Circle 2 - Bottom Left
        Surface(
            modifier = Modifier
                .offset(x = (-50).dp, y = 500.dp + circle2Offset.dp)
                .size(150.dp)
                .alpha(0.08f),
            shape = CircleShape,
            color = Color.White
        ) {}

        // Circle 3 - Middle
        Surface(
            modifier = Modifier
                .offset(x = 100.dp, y = 300.dp + circle3Offset.dp)
                .size(120.dp)
                .alpha(0.06f),
            shape = CircleShape,
            color = Color.White
        ) {}
    }
}