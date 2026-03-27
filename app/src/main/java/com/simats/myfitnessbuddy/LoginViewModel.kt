package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Patterns

data class LoginUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isLoginSuccessful: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = if (validateEmail(email)) null else "Please enter a valid email address"
        )
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = if (password.isNotEmpty()) null else "Password cannot be empty"
        )
    }

    fun onPasswordVisibilityToggle() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun onSignInClicked() {
        val currentState = _uiState.value
        val isEmailValid = validateEmail(currentState.email)
        val isPasswordValid = currentState.password.isNotEmpty()

        if (isEmailValid && isPasswordValid) {
            _uiState.value = _uiState.value.copy(isLoading = true, loginError = null)
            // Simulated login logic
            // In a real app, this would be an API call
            _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccessful = true)
        } else {
            _uiState.value = _uiState.value.copy(
                emailError = if (isEmailValid) null else "Invalid email",
                passwordError = if (isPasswordValid) null else "Password required"
            )
        }
    }
}
