package com.droidnest.tech.pysadmin.presentation.screens.auth.signup

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidnest.tech.pysadmin.presentation.screens.auth.login.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSignUpScreen(
    viewModel: SignUpViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    // Success handler
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val (email, password) = state.createdCredentials!!
            Toast.makeText(
                context,
                "✅ Admin Created! Email: $email",
                Toast.LENGTH_LONG
            ).show()
            delay(2500)
            onSetupComplete()
        }
    }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
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
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFF6B8DD6),
                        Color(0xFF8E37D7)
                    ),
                    start = Offset(0f, gradientOffset * 2000),
                    end = Offset(1000f, 2000f - gradientOffset * 2000)
                )
            )
    ) {
        // Floating elements
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    tween(600, easing = FastOutSlowInEasing)
                ) { -it }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    // Header Logo
                    RegistrationLogo()

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Create Admin Account",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = Offset(0f, 4f),
                                blurRadius = 12f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "One-time setup for admin privileges",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Warning Card
                    PremiumWarningCard()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form Card
                    PremiumCard {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Name Field
                            PremiumTextField(
                                value = state.name,
                                onValueChange = { viewModel.onNameChange(it) },
                                label = "Full Name",
                                placeholder = "John Doe",
                                leadingIcon = Icons.Outlined.Person,
                                errorMessage = state.nameError,
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Email Field
                            PremiumTextField(
                                value = state.email,
                                onValueChange = { viewModel.onEmailChange(it) },
                                label = "Email Address",
                                placeholder = "admin@company.com",
                                leadingIcon = Icons.Outlined.Email,
                                errorMessage = state.emailError,
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Phone Field
                            PremiumTextField(
                                value = state.phone,
                                onValueChange = { viewModel.onPhoneChange(it) },
                                label = "Phone Number",
                                placeholder = "+60123456789",
                                leadingIcon = Icons.Outlined.Phone,
                                errorMessage = state.phoneError,
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Password Field
                            PremiumTextField(
                                value = state.password,
                                onValueChange = { viewModel.onPasswordChange(it) },
                                label = "Password",
                                placeholder = "Min 8 characters",
                                leadingIcon = Icons.Outlined.Lock,
                                trailingIcon = if (state.showPassword)
                                    Icons.Outlined.VisibilityOff
                                else
                                    Icons.Outlined.Visibility,
                                onTrailingIconClick = {
                                    viewModel.togglePasswordVisibility()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                visualTransformation = if (state.showPassword)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                errorMessage = state.passwordError,
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (state.isValid) {
                                            viewModel.createAdmin()
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Register Button
                            PremiumButton(
                                text = "Register Admin",
                                isLoading = state.isLoading,
                                loadingText = "Creating Account...",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    focusManager.clearFocus()
                                    viewModel.createAdmin()
                                }
                            )

                            // Login Link
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Already have an account? ",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Login",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6),
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToLogin()
                                    }
                                )
                            }

                            // Error Display
                            AnimatedVisibility(
                                visible = state.error != null,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))
                                ErrorCard(state.error ?: "")
                            }

                            // Success Display
                            AnimatedVisibility(
                                visible = state.isSuccess && state.createdCredentials != null,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                state.createdCredentials?.let { (email, password) ->
                                    Spacer(modifier = Modifier.height(20.dp))
                                    SuccessCard(email, password)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Instructions Card
                    InstructionsCard()

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// Registration Logo
@Composable
fun RegistrationLogo() {
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
        modifier = Modifier.size(120.dp)
    ) {
        // Glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .blur(40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Icon
        Surface(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .rotate(rotation),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF0F0F0)
                        )
                    )
                )
            ) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFF667eea)
                )
            }
        }

        // Ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
    }
}

// Warning Card
@Composable
fun PremiumWarningCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = Color(0xFFFF6B6B).copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFFF6B6B).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFE66D),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "⚠️ ONE-TIME SETUP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Comment out this route after creating admin",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Error Card
@Composable
fun ErrorCard(error: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color.Red.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Red.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Success Card
@Composable
fun SuccessCard(email: String, password: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF4CAF50).copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✅ Admin Created Successfully!",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("📧 Email: $email", fontSize = 13.sp, color = Color.White)
            Text("🔑 Password: $password", fontSize = 13.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ SAVE THESE CREDENTIALS!",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFE66D)
            )
        }
    }
}

// Instructions Card
@Composable
fun InstructionsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "📝 Setup Instructions",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            InstructionStep("1", "Fill all fields in the form")
            InstructionStep("2", "Click 'Register Admin'")
            InstructionStep("3", "Save displayed credentials")
            InstructionStep("4", "Test login with created account")
            InstructionStep("5", "Comment out OneTimeSetupRoute")
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = Color(0xFF64B5F6).copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )
    }
}