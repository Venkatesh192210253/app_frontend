package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthorized) {
        if (uiState.isAuthorized) {
            onSignupSuccess()
        }
    }

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
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        
        Text(
            text = "Join MyFitnessBuddy today",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        StyledTextField(
            value = uiState.username,
            onValueChange = { viewModel.onUsernameChanged(it) },
            placeholder = "Username",
            label = "USERNAME"
        )

        Spacer(modifier = Modifier.height(20.dp))

        StyledTextField(
            value = uiState.email,
            onValueChange = { viewModel.onEmailChanged(it) },
            placeholder = "Email Address",
            label = "EMAIL",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(20.dp))

        StyledTextField(
            value = uiState.mobileNumber,
            onValueChange = { viewModel.onMobileNumberChanged(it) },
            placeholder = "Phone Number",
            label = "PHONE NUMBER",
            keyboardType = KeyboardType.Phone
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

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        ScaleButton(
            onClick = { viewModel.register() },
            modifier = Modifier.fillMaxWidth(),
            containerColor = AccentGreen,
            isLoading = uiState.isLoading
        ) {
            Text("Create Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text("Already have an account? ", color = Color.Gray)
            Text(
                "Log In",
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}
