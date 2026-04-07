package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

val PrimaryGreen = Color(0xFF22C55E)
val AccentGreen = Color(0xFF00C896)
val LightGrey = Color(0xFFF3F4F6)
val DarkGrey = Color(0xFF9CA3AF)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthFlow(
    onAuthSuccess: (Boolean) -> Unit,
    onSignUpClick: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthorized) {
        if (uiState.isAuthorized) {
            onAuthSuccess(uiState.onboardingCompleted)
        }
    }

    NavHost(
        navController = navController, 
        startDestination = "subscription",
        enterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { it / 2 }) },
        exitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(targetOffsetX = { -it / 2 }) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { -it / 2 }) },
        popExitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(targetOffsetX = { it / 2 }) }
    ) {
        composable("subscription") {
            SubscriptionScreen(
                onSignInClick = { navController.navigate("login") },
                onSkipClick = { navController.navigate("login") }
            )
        }
        composable("login") {
            LoginMainScreen(
                viewModel = viewModel,
                onForgotPasswordClick = { navController.navigate("forgot_password_email") },
                onSignUpClick = onSignUpClick
            )
        }
        composable("forgot_password_email") {
            ForgotPasswordEmailScreen(
                viewModel = viewModel,
                onOtpSent = { navController.navigate("forgot_password_otp") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("forgot_password_otp") {
            ForgotPasswordOtpScreen(
                viewModel = viewModel,
                onOtpVerified = { navController.navigate("forgot_password_reset") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("forgot_password_reset") {
            ForgotPasswordResetScreen(
                viewModel = viewModel,
                onResetSuccess = { 
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LoginMainScreen(
    viewModel: AuthViewModel,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "Log in to MyFitnessBuddy",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )


        
        LaunchedEffect(uiState.isOtpSent) {
            // Not used for login anymore, but could be used for password reset if needed
        }

        Spacer(modifier = Modifier.height(32.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // Removed SegmentedToggle as per user request (Email only login)
        
        Spacer(modifier = Modifier.height(8.dp))

        EmailLoginContent(viewModel, onForgotPasswordClick, onSignUpClick)
    }
}


@Composable
fun EmailLoginContent(
    viewModel: AuthViewModel,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Welcome Back",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Log in to your account",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        StyledTextField(
            value = uiState.email,
            onValueChange = { viewModel.onEmailChanged(it) },
            placeholder = "Email or Phone Number",
            label = "EMAIL",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(20.dp))

        StyledTextField(
            value = uiState.password,
            onValueChange = { viewModel.onPasswordChanged(it) },
            placeholder = "Password",
            label = "PASSWORD",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isPasswordVisible = uiState.isPasswordVisible,
            onPasswordToggle = { viewModel.togglePasswordVisibility() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Forgot Password?",
            color = PrimaryGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPasswordClick() }
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        ScaleButton(
            onClick = { viewModel.loginWithEmail() },
            modifier = Modifier.fillMaxWidth(),
            containerColor = AccentGreen,
            isLoading = uiState.isLoading
        ) {
            Text("Log In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("If you don't have an account, you can ", color = Color.Gray, fontSize = 12.sp)
            Text(
                "Create an account",
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
        Text(
            "on our Sign Up page",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}


@Composable
fun OTPInputRow(otp: String, onOtpChanged: (String) -> Unit) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0 until 6) {
            val char = otp.getOrNull(i)?.toString() ?: ""
            
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightGrey)
                    .border(
                        width = if (otp.length == i) 2.dp else 0.dp,
                        color = if (otp.length == i) PrimaryGreen else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = char,
                    onValueChange = { newVal ->
                        if (newVal.length <= 1) {
                            val updatedOtp = if (i < otp.length) {
                                otp.replaceRange(i, i + 1, newVal)
                            } else {
                                otp + newVal
                            }
                            onOtpChanged(updatedOtp.take(6))
                            
                            if (newVal.isNotEmpty() && i < 5) {
                                focusRequesters[i + 1].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequesters[i])
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && 
                                keyEvent.key == Key.Backspace && 
                                char.isEmpty() && i > 0
                            ) {
                                focusRequesters[i - 1].requestFocus()
                                true
                            } else {
                                false
                            }
                        }
                        .fillMaxSize(),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (isPassword) {
                    val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onPasswordToggle) {
                        Icon(icon, contentDescription = null, tint = DarkGrey)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = LightGrey
            ),
            singleLine = true
        )
    }
}

@Composable
fun ScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AccentGreen,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "ButtonScale")

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(50.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
            content()
        }
    }
}
