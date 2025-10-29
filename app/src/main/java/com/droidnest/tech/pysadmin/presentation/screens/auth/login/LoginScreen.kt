// admin_app/presentation/auth/login/AdminLoginScreen.kt
@file:OptIn(ExperimentalAnimationApi::class)

package com.droidnest.tech.pysadmin.presentation.screens.auth.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    viewModel: AdminLoginViewModel = hiltViewModel(), // ✅ Use ViewModel
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState() // ✅ Collect state

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    // ✅ Handle success navigation
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            delay(800)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLoginSuccess()
        }
    }

    // ✅ Handle errors
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when {
                error.contains("password", ignoreCase = true) -> {
                    passwordError = "Incorrect password"
                    "🔑 Invalid password. Please try again."
                }
                error.contains("user", ignoreCase = true) ||
                        error.contains("email", ignoreCase = true) -> {
                    emailError = "Account not found"
                    "👤 No account found with this email."
                }
                error.contains("network", ignoreCase = true) -> {
                    "🌐 Network error. Check your connection."
                }
                else -> "❌ $error"
            }

            snackbarHostState.showSnackbar(
                message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    ),
                    start = Offset(0f, gradientOffset * 2000),
                    end = Offset(1000f, 2000f - gradientOffset * 2000)
                )
            )
    ) {
        // Floating particles
        FloatingParticles()

        // Snackbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    PremiumSnackbar(data)
                }
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    tween(600, easing = FastOutSlowInEasing)
                ) { -it },
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Premium Logo
                    PremiumLogo()

                    Spacer(modifier = Modifier.height(48.dp))

                    // Login Card
                    PremiumCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            // Header
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )

                            Text(
                                text = "Login to your admin dashboard",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(36.dp))

                            // Email Field
                            PremiumTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = null
                                },
                                label = "Email Address",
                                placeholder = "admin@company.com",
                                leadingIcon = Icons.Outlined.Email,
                                errorMessage = emailError,
                                enabled = !state.isLoading, // ✅ Use ViewModel state
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Password Field
                            PremiumTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordError = null
                                },
                                label = "Password",
                                placeholder = "••••••••",
                                leadingIcon = Icons.Outlined.Lock,
                                trailingIcon = if (passwordVisible)
                                    Icons.Outlined.VisibilityOff
                                else
                                    Icons.Outlined.Visibility,
                                onTrailingIconClick = {
                                    passwordVisible = !passwordVisible
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                visualTransformation = if (passwordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                errorMessage = passwordError,
                                enabled = !state.isLoading, // ✅ Use ViewModel state
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (email.isNotBlank() && password.isNotBlank()) {
                                            viewModel.login(email.trim(), password)
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Forgot Password
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64B5F6),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Handle forgot password
                                    }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // ✅ Login Button - Use ViewModel
                            PremiumButton(
                                text = "Login",
                                isLoading = state.isLoading, // ✅ From ViewModel
                                loadingText = "Authenticating...",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    focusManager.clearFocus()

                                    // Validation
                                    var hasError = false

                                    if (email.isBlank()) {
                                        emailError = "Email is required"
                                        hasError = true
                                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        emailError = "Invalid email format"
                                        hasError = true
                                    }

                                    if (password.isBlank()) {
                                        passwordError = "Password is required"
                                        hasError = true
                                    }

                                    if (hasError) return@PremiumButton

                                    // ✅ Call ViewModel
                                    viewModel.login(email.trim(), password)
                                }
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Register Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Don't have an account? ",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Register",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToRegister()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .size(16.dp),
                                    tint = Color(0xFF64B5F6)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Footer
                    Text(
                        text = "🔐 Protected by enterprise-grade security",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Premium Logo Component
@Composable
fun PremiumLogo() {
    var scale by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(-180f) }

    LaunchedEffect(Unit) {
        launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) { value, _ -> scale = value }
        }
        launch {
            animate(
                initialValue = -180f,
                targetValue = 0f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            ) { value, _ -> rotation = value }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(130.dp)
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(scale)
                .blur(40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF64B5F6).copy(alpha = 0.8f),
                            Color(0xFF42A5F5).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Icon circle
        Surface(
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .rotate(rotation),
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 20.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
                        )
                    )
                )
            ) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    modifier = Modifier.size(55.dp),
                    tint = Color.White
                )
            }
        }

        // Inner ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

// Premium Card Component
@Composable
fun PremiumCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(32.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            content()
        }
    }
}

// Premium TextField Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorMessage: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isError = errorMessage != null

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = Color.White.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (isError)
                        Color(0xFFFF6B6B)
                    else
                        Color(0xFF64B5F6)
                )
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(
                            it,
                            contentDescription = "Toggle",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                disabledTextColor = Color.White.copy(alpha = 0.5f),
                focusedBorderColor = if (isError)
                    Color(0xFFFF6B6B)
                else
                    Color(0xFF64B5F6),
                unfocusedBorderColor = if (isError)
                    Color(0xFFFF6B6B).copy(alpha = 0.5f)
                else
                    Color.White.copy(alpha = 0.3f),
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = Color(0xFF64B5F6),
                errorBorderColor = Color(0xFFFF6B6B),
                errorContainerColor = Color(0xFFFF6B6B).copy(alpha = 0.05f)
            ),
            singleLine = true,
            enabled = enabled,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF6B6B),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// Premium Button Component
@Composable
fun PremiumButton(
    text: String,
    isLoading: Boolean,
    loadingText: String = "Loading...",
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isLoading) {
                            listOf(
                                Color(0xFF667eea).copy(alpha = 0.6f),
                                Color(0xFF764ba2).copy(alpha = 0.6f)
                            )
                        } else {
                            listOf(
                                Color(0xFF667eea),
                                Color(0xFF764ba2)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    fadeIn(tween(300)) with fadeOut(tween(300))
                },
                label = "buttonContent"
            ) { loading ->
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            loadingText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Premium Snackbar Component
@Composable
fun PremiumSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E2E),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

// Floating Particles Background
@Composable
fun FloatingParticles() {
    val particles = remember {
        List(15) {
            Particle(
                x = (0..1000).random().dp,
                y = (0..2000).random().dp,
                size = (20..80).random().dp,
                duration = (8000..15000).random()
            )
        }
    }

    particles.forEach { particle ->
        val infiniteTransition = rememberInfiniteTransition(label = "particle$particle")
        val offsetY by infiniteTransition.animateFloat(
            initialValue = particle.y.value,
            targetValue = particle.y.value - 300f,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particleY"
        )

        Box(
            modifier = Modifier
                .offset(x = particle.x, y = offsetY.dp)
                .size(particle.size)
                .alpha(0.05f)
                .blur(30.dp)
                .background(
                    Color.White,
                    shape = CircleShape
                )
        )
    }
}

data class Particle(
    val x: androidx.compose.ui.unit.Dp,
    val y: androidx.compose.ui.unit.Dp,
    val size: androidx.compose.ui.unit.Dp,
    val duration: Int
)