package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.ChangePasswordRequest
import com.simats.myfitnessbuddy.data.remote.PrivacySettingsResponse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacySecurityUiState(
    // Account Privacy
    val privateAccount: Boolean = false,
    val showProfileInSearch: Boolean = true,
    val showActivityStatus: Boolean = true,
    
    // Data Sharing
    val shareWorkoutData: Boolean = true,
    val shareDietData: Boolean = true,
    val shareProgressPhotos: Boolean = false,
    val appearOnLeaderboards: Boolean = true,
    
    // Security info
    val connectedDevicesCount: Int = 0
)

class PrivacySecurityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PrivacySecurityUiState())
    val uiState: StateFlow<PrivacySecurityUiState> = _uiState.asStateFlow()

    init {
        fetchPrivacySettings()
    }

    private fun fetchPrivacySettings() {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val res = RetrofitClient.apiService.getPrivacySettings()
                if (res.isSuccessful && res.body() != null) {
                    val settings = res.body()!!
                    _uiState.update {
                        it.copy(
                            privateAccount = settings.private_account,
                            showProfileInSearch = settings.show_profile_in_search,
                            showActivityStatus = settings.show_activity_status,
                            shareWorkoutData = settings.share_workout_data,
                            shareDietData = settings.share_diet_data,
                            shareProgressPhotos = settings.share_progress_photos,
                            appearOnLeaderboards = settings.appear_on_leaderboards
                        )
                    }
                    
                    // Backup locally
                    SettingsManager.privateAccount = settings.private_account
                    SettingsManager.showInSearch = settings.show_profile_in_search
                    SettingsManager.showActivity = settings.show_activity_status
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveSettings(newState: PrivacySecurityUiState) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: return@launch
            try {
                val settingsResponse = PrivacySettingsResponse(
                    private_account = newState.privateAccount,
                    show_profile_in_search = newState.showProfileInSearch,
                    show_activity_status = newState.showActivityStatus,
                    share_workout_data = newState.shareWorkoutData,
                    share_diet_data = newState.shareDietData,
                    share_progress_photos = newState.shareProgressPhotos,
                    appear_on_leaderboards = newState.appearOnLeaderboards
                )
                RetrofitClient.apiService.updatePrivacySettings(settingsResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePrivateAccount(value: Boolean) { 
        val newState = _uiState.value.copy(privateAccount = value)
        _uiState.value = newState
        SettingsManager.privateAccount = value
        saveSettings(newState)
    }
    fun updateShowProfileInSearch(value: Boolean) { 
        val newState = _uiState.value.copy(showProfileInSearch = value)
        _uiState.value = newState
        SettingsManager.showInSearch = value
        saveSettings(newState)
    }
    fun updateShowActivityStatus(value: Boolean) { 
        val newState = _uiState.value.copy(showActivityStatus = value)
        _uiState.value = newState
        SettingsManager.showActivity = value
        saveSettings(newState)
    }
    
    fun updateShareWorkoutData(value: Boolean) { 
        val newState = _uiState.value.copy(shareWorkoutData = value)
        _uiState.value = newState
        saveSettings(newState)
    }
    fun updateShareDietData(value: Boolean) { 
        val newState = _uiState.value.copy(shareDietData = value)
        _uiState.value = newState
        saveSettings(newState)
    }
    fun updateShareProgressPhotos(value: Boolean) { 
        val newState = _uiState.value.copy(shareProgressPhotos = value)
        _uiState.value = newState
        saveSettings(newState)
    }
    fun updateAppearOnLeaderboards(value: Boolean) { 
        val newState = _uiState.value.copy(appearOnLeaderboards = value)
        _uiState.value = newState
        saveSettings(newState)
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Validation rules
        if (newPass.length <= 8) {
            onError("New password must be greater than 8 characters")
            return
        }
        if (!newPass.substring(0, 1).all { it.isUpperCase() }) {
            onError("New password must start with a Capital letter")
            return
        }
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?".toSet()
        if (!newPass.any { it in specialChars }) {
            onError("New password must contain a special character")
            return
        }

        viewModelScope.launch {
            val token = SettingsManager.authToken
            if (token == null) {
                onError("No authorization token found")
                return@launch
            }
            try {
                val request = ChangePasswordRequest(oldPass, newPass)
                val response = RetrofitClient.apiService.changePassword(request)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    var errorMessage = "Failed to change password."
                    try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            val json = org.json.JSONObject(errorBody)
                            if (json.has("old_password")) {
                                errorMessage = json.getJSONArray("old_password").getString(0)
                            } else if (json.has("new_password")) {
                                errorMessage = json.getJSONArray("new_password").getString(0)
                            } else if (json.has("non_field_errors")) {
                                errorMessage = json.getJSONArray("non_field_errors").getString(0)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Network error occurred.")
            }
        }
    }


    fun downloadData(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val token = SettingsManager.authToken
            if (token == null) {
                onError("No authorization token found")
                return@launch
            }
            try {
                val response = RetrofitClient.apiService.downloadData()
                if (response.isSuccessful) {
                    onSuccess("Check your console or email for the download link.")
                } else {
                    onError("Failed to aggregate data.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Network error occurred.")
            }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val token = SettingsManager.authToken
            if (token == null) {
                onError("No authorization token found")
                return@launch
            }
            try {
                val response = RetrofitClient.apiService.deleteAccount(mapOf("password" to password))
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    var errorMessage = "Failed to delete account."
                    try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            val json = org.json.JSONObject(errorBody)
                            if (json.has("error")) {
                                errorMessage = json.getString("error")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Network error occurred.")
            }
        }
    }
}
