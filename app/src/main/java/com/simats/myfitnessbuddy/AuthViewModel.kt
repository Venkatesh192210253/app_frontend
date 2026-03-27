package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.remote.LoginRequest
import com.simats.myfitnessbuddy.data.remote.OtpGenerateRequest
import com.simats.myfitnessbuddy.data.remote.OtpVerifyRequest
import com.simats.myfitnessbuddy.data.remote.RegisterRequest
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginMode {
    EMAIL
}

data class AuthUiState(
    val loginMode: LoginMode = LoginMode.EMAIL,
    
    // Auth Fields
    val username: String = "",
    val mobileNumber: String = "",
    val selectedCountryCode: String = "+91",
    val selectedCountryFlag: String = "🇮🇳",
    val email: String = "",
    val password: String = "",
    
    // OTP State (Keep for forgot password logic if needed)
    val otpCode: String = "",
    val isOtpSent: Boolean = false,
    val resendTimer: Int = 30,
    val canResendOtp: Boolean = false,
    
    // UI State
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthorized: Boolean = false,
    val onboardingCompleted: Boolean = false,
    
    // Forgot Password State
    val resetEmail: String = "",
    val isResetOtpSent: Boolean = false,
    val isResetOtpVerified: Boolean = false,
    val isResetSuccessful: Boolean = false,
    val newPasswordReset: String = "",
    val confirmPasswordReset: String = ""
)

sealed class AuthEvent {
    object NavigateToOtp : AuthEvent()
    object NavigateToDashboard : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
}

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        checkAuthStatus()
    }

    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validatePassword(password: String): String? {
        if (password.length <= 8) return "Password must be greater than 8 characters"
        if (!password.substring(0, 1).all { it.isUpperCase() }) return "Password must start with a Capital letter"
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?".toSet()
        if (!password.any { it in specialChars }) return "Password must contain a special character"
        return null
    }

    private fun validateMobile(mobile: String): Boolean {
        return mobile.length == 10 && mobile.all { it.isDigit() }
    }

    fun checkAuthStatus() {
        val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken
        
        if (!token.isNullOrEmpty()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val response = RetrofitClient.apiService.verifyToken()
                    if (response.isSuccessful) {
                        val body = response.body()
                        val completed = body?.goals_completed ?: false
                        com.simats.myfitnessbuddy.data.local.SettingsManager.goalsCompleted = completed
                        body?.user?.let { user ->
                            com.simats.myfitnessbuddy.data.local.SettingsManager.userId = user.id
                            com.simats.myfitnessbuddy.data.local.SettingsManager.userName = user.username
                            com.simats.myfitnessbuddy.data.local.SettingsManager.fullName = user.full_name ?: user.profile?.full_name ?: ""
                            com.simats.myfitnessbuddy.data.local.SettingsManager.userEmail = user.email
                        }
                        _uiState.update { it.copy(
                            isLoading = false,
                            isAuthorized = true,
                            onboardingCompleted = completed
                        ) }
                    } else {
                        com.simats.myfitnessbuddy.data.local.SettingsManager.authToken = null
                        com.simats.myfitnessbuddy.data.local.SettingsManager.refreshToken = null
                        _uiState.update { it.copy(isLoading = false, isAuthorized = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, isAuthorized = false) }
                }
            }
        }
    }

    fun setLoginMode(mode: LoginMode) {
        _uiState.update { it.copy(loginMode = mode, error = null, isOtpSent = false) }
    }

    fun onMobileNumberChanged(number: String) {
        if (number.length <= 10) {
            _uiState.update { it.copy(mobileNumber = number) }
        }
    }

    fun onCountryCodeChanged(code: String, flag: String) {
        _uiState.value = _uiState.value.copy(selectedCountryCode = code, selectedCountryFlag = flag)
    }

    fun onOtpChanged(otp: String) {
        if (otp.length <= 6) {
            _uiState.value = _uiState.value.copy(otpCode = otp)
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onResetEmailChanged(email: String) {
        _uiState.update { it.copy(resetEmail = email) }
    }

    fun onNewPasswordResetChanged(password: String) {
        _uiState.update { it.copy(newPasswordReset = password) }
    }

    fun onConfirmPasswordResetChanged(password: String) {
        _uiState.update { it.copy(confirmPasswordReset = password) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun generateOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isOtpSent = false) }
            try {
                val response = RetrofitClient.apiService.generateOtp(
                    OtpGenerateRequest(phone_number = _uiState.value.mobileNumber)
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, isOtpSent = true) }
                    startResendTimer()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Server Error: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Check internet or server status") }
            }
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = RetrofitClient.apiService.verifyOtp(
                    OtpVerifyRequest(
                        phone_number = _uiState.value.mobileNumber,
                        otp = _uiState.value.otpCode
                    )
                )
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val token = "Token ${authResponse?.access}"
                    val refreshToken = authResponse?.refresh
                    com.simats.myfitnessbuddy.data.local.SettingsManager.authToken = token
                    com.simats.myfitnessbuddy.data.local.SettingsManager.refreshToken = refreshToken
                    val completed = authResponse?.goals_completed ?: authResponse?.user?.goals_completed ?: false
                    com.simats.myfitnessbuddy.data.local.SettingsManager.goalsCompleted = completed
                    authResponse?.user?.let { user ->
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userId = user.id
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userName = user.username
                        com.simats.myfitnessbuddy.data.local.SettingsManager.fullName = user.full_name ?: user.profile?.full_name ?: ""
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userEmail = user.email
                    }
                    com.simats.myfitnessbuddy.data.local.SettingsManager.onboardingSeen = true
                    
                    _uiState.update { it.copy(
                        isLoading = false, 
                        isAuthorized = true,
                        onboardingCompleted = completed
                    ) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Invalid OTP") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Connection Error") }
            }
        }
    }

    fun loginWithEmail() {
        if (!validateEmail(_uiState.value.email)) {
            _uiState.update { it.copy(error = "Please enter a valid email address") }
            return
        }
        // Removed password validation here to allow existing accounts with old passwords to log in.
        // The new strict password rules will still be enforced during Registration and Password Change/Reset.

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(
                        identifier = _uiState.value.email,
                        password = _uiState.value.password
                    )
                )
                if (response.isSuccessful) {
                    val authResponse = response.body()

                    val token = "Token ${authResponse?.access}"
                    val refreshToken = authResponse?.refresh
                    com.simats.myfitnessbuddy.data.local.SettingsManager.authToken = token
                    com.simats.myfitnessbuddy.data.local.SettingsManager.refreshToken = refreshToken
                    val completed = authResponse?.goals_completed ?: authResponse?.user?.goals_completed ?: false
                    com.simats.myfitnessbuddy.data.local.SettingsManager.goalsCompleted = completed
                    authResponse?.user?.let { user ->
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userId = user.id
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userName = user.username
                        com.simats.myfitnessbuddy.data.local.SettingsManager.fullName = user.full_name ?: user.profile?.full_name ?: ""
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userEmail = user.email
                    }
                    com.simats.myfitnessbuddy.data.local.SettingsManager.onboardingSeen = true

                    _uiState.update { it.copy(
                        isLoading = false, 
                        isAuthorized = true,
                        onboardingCompleted = completed
                    ) }
                } else {
                    // Try to parse "error" field from JSON response: {"error": "..."}
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val json = com.google.gson.JsonParser().parse(errorBody).asJsonObject
                        if (json.has("error")) {
                            json.get("error").asString
                        } else {
                            // DRF error dictionary: {"field": ["msg", ...]} or {"non_field_errors": [...]}
                            json.entrySet().firstOrNull()?.let { entry ->
                                val msgs = entry.value.asJsonArray
                                "${entry.key}: ${msgs.firstOrNull()?.asString ?: "Error"}"
                            } ?: "Login failed (${response.code()})"
                        }
                    } catch (e: Exception) {
                        "Login failed (${response.code()})"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Unable to connect to server") }
            }
        }
    }


    fun register() {
        if (!validateEmail(_uiState.value.email)) {
            _uiState.update { it.copy(error = "Please enter a valid email address") }
            return
        }
        if (!validateMobile(_uiState.value.mobileNumber)) {
            _uiState.update { it.copy(error = "Mobile number should be 10 digits") }
            return
        }
        val pwdError = validatePassword(_uiState.value.password)
        if (pwdError != null) {
            _uiState.update { it.copy(error = pwdError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(
                        username = _uiState.value.username,
                        email = _uiState.value.email,
                        phone_number = _uiState.value.mobileNumber,
                        password = _uiState.value.password
                    )
                )
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val token = "Token ${authResponse?.access}"
                    val refreshToken = authResponse?.refresh
                    com.simats.myfitnessbuddy.data.local.SettingsManager.authToken = token
                    com.simats.myfitnessbuddy.data.local.SettingsManager.refreshToken = refreshToken
                    val completed = authResponse?.goals_completed ?: authResponse?.user?.goals_completed ?: false
                    com.simats.myfitnessbuddy.data.local.SettingsManager.goalsCompleted = completed
                    authResponse?.user?.let { user ->
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userId = user.id
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userName = user.username
                        com.simats.myfitnessbuddy.data.local.SettingsManager.fullName = user.full_name ?: user.profile?.full_name ?: ""
                        com.simats.myfitnessbuddy.data.local.SettingsManager.userEmail = user.email
                    }
                    com.simats.myfitnessbuddy.data.local.SettingsManager.onboardingSeen = true

                    _uiState.update { it.copy(
                        isLoading = false, 
                        isAuthorized = true,
                        onboardingCompleted = completed
                    ) }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val json = com.google.gson.JsonParser().parse(errorBody).asJsonObject
                        if (json.has("error")) {
                            json.get("error").asString
                        } else {
                            // DRF error dictionary: {"field": ["msg", ...]}
                            json.entrySet().firstOrNull()?.let { entry ->
                                val msgs = entry.value.asJsonArray
                                "${entry.key}: ${msgs.firstOrNull()?.asString ?: "Error"}"
                            } ?: "Registration failed (${response.code()})"
                        }
                    } catch (e: Exception) {
                        "Registration failed (${response.code()})"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Server connection error") }
            }
        }
    }

    fun resendOtp() {
        if (_uiState.value.canResendOtp) {
            generateOtp()
        }
    }

    fun requestPasswordReset() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.forgotPassword(
                    mapOf("email" to _uiState.value.resetEmail)
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, isResetOtpSent = true) }
                    startResendTimer()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Email not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun verifyResetOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.verifyResetOtp(
                    mapOf(
                        "email" to _uiState.value.resetEmail,
                        "otp" to _uiState.value.otpCode
                    )
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, isResetOtpVerified = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Invalid OTP") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Verification failed") }
            }
        }
    }

    fun resetPassword() {
        if (_uiState.value.newPasswordReset != _uiState.value.confirmPasswordReset) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        val pwdError = validatePassword(_uiState.value.newPasswordReset)
        if (pwdError != null) {
            _uiState.update { it.copy(error = pwdError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.resetPassword(
                    mapOf(
                        "email" to _uiState.value.resetEmail,
                        "otp" to _uiState.value.otpCode,
                        "new_password" to _uiState.value.newPasswordReset,
                        "confirm_password" to _uiState.value.confirmPasswordReset
                    )
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, isResetSuccessful = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Invalid OTP or error") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Reset failed") }
            }
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(resendTimer = 30, canResendOtp = false) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.resendTimer > 0) {
                delay(1000)
                _uiState.update { it.copy(resendTimer = it.resendTimer - 1) }
            }
            _uiState.update { it.copy(canResendOtp = true) }
        }
    }
}
