package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForgotPasswordEmailScreen(
    viewModel: AuthViewModel,
    onOtpSent: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isResetOtpSent) {
        if (uiState.isResetOtpSent) {
            onOtpSent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Forgot Password",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            text = "Enter your registered email to receive an OTP",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        StyledTextField(
            value = uiState.resetEmail,
            onValueChange = { viewModel.onResetEmailChanged(it) },
            placeholder = "Email Address",
            label = "EMAIL",
            keyboardType = KeyboardType.Email
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        ScaleButton(
            onClick = { viewModel.requestPasswordReset() },
            modifier = Modifier.fillMaxWidth(),
            containerColor = AccentGreen,
            isLoading = uiState.isLoading
        ) {
            Text("Send OTP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ForgotPasswordOtpScreen(
    viewModel: AuthViewModel,
    onOtpVerified: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isResetOtpVerified) {
        if (uiState.isResetOtpVerified) {
            onOtpVerified()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verify Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            text = "Enter the code sent to ${uiState.resetEmail}",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        OTPInputRow(
            otp = uiState.otpCode,
            onOtpChanged = { viewModel.onOtpChanged(it) }
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (uiState.canResendOtp) {
                Text(
                    text = "Resend OTP",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.requestPasswordReset() }
                )
            } else {
                Text(
                    text = "Resend OTP in ${uiState.resendTimer}s",
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        ScaleButton(
            onClick = { viewModel.verifyResetOtp() },
            modifier = Modifier.fillMaxWidth(),
            containerColor = AccentGreen,
            isLoading = uiState.isLoading
        ) {
            Text("Verify OTP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ForgotPasswordResetScreen(
    viewModel: AuthViewModel,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isResetSuccessful) {
        if (uiState.isResetSuccessful) {
            onResetSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Reset Password",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            text = "Create a strong new password for your account",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        StyledTextField(
            value = uiState.newPasswordReset,
            onValueChange = { viewModel.onNewPasswordResetChanged(it) },
            placeholder = "New Password",
            label = "NEW PASSWORD",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isPasswordVisible = uiState.isPasswordVisible,
            onPasswordToggle = { viewModel.togglePasswordVisibility() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        StyledTextField(
            value = uiState.confirmPasswordReset,
            onValueChange = { viewModel.onConfirmPasswordResetChanged(it) },
            placeholder = "Confirm New Password",
            label = "CONFIRM NEW PASSWORD",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isPasswordVisible = uiState.isPasswordVisible,
            onPasswordToggle = { viewModel.togglePasswordVisibility() }
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        ScaleButton(
            onClick = { viewModel.resetPassword() },
            modifier = Modifier.fillMaxWidth(),
            containerColor = AccentGreen,
            isLoading = uiState.isLoading
        ) {
            Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
