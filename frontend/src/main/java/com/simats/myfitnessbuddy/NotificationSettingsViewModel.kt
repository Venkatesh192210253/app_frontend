package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.simats.myfitnessbuddy.data.local.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationSettingsUiState(
    val masterPush: Boolean = true,
    
    // Activity Reminders
    val workoutReminders: Boolean = true,
    val mealLoggingReminders: Boolean = true,
    val waterIntakeReminders: Boolean = true,
    val bedtimeReminder: Boolean = false,
    
    // Social
    val friendRequests: Boolean = true,
    val groupInvites: Boolean = true,
    val challengeUpdates: Boolean = true,
    val messages: Boolean = true,
    
    // Progress
    val weeklySummary: Boolean = true,
    val goalAchieved: Boolean = true,
    
    // AI Coach
    val aiSuggestions: Boolean = true,
    val weeklyAdjustments: Boolean = true,
    val motivationalMessages: Boolean = true,
    
    // Quiet Hours
    val quietHoursFrom: String = "22:00",
    val quietHoursTo: String = "07:00",
    
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

class NotificationSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                masterPush = SettingsManager.masterPush,
                workoutReminders = SettingsManager.workoutReminders,
                quietHoursFrom = SettingsManager.quietHoursFrom ?: "22:00",
                quietHoursTo = SettingsManager.quietHoursTo ?: "07:00"
            )
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                
                // 1. Save Locally
                SettingsManager.masterPush = state.masterPush
                SettingsManager.workoutReminders = state.workoutReminders
                SettingsManager.quietHoursFrom = state.quietHoursFrom
                SettingsManager.quietHoursTo = state.quietHoursTo
                
                // 2. Save to Server Database
                val updateData = mapOf(
                    "quiet_hours_from" to state.quietHoursFrom,
                    "quiet_hours_to" to state.quietHoursTo
                )
                RetrofitClient.apiService.partialGoalUpdate(updateData)
                
                kotlinx.coroutines.delay(500)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsVM", "Failed to sync settings", e)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun updateMasterPush(value: Boolean) { 
        SettingsManager.masterPush = value
        _uiState.update { it.copy(masterPush = value) } 
    }
    fun updateWorkoutReminders(value: Boolean) { 
        SettingsManager.workoutReminders = value
        _uiState.update { it.copy(workoutReminders = value) } 
    }
    fun updateMealLoggingReminders(value: Boolean) { _uiState.update { it.copy(mealLoggingReminders = value) } }
    fun updateWaterIntakeReminders(value: Boolean) { _uiState.update { it.copy(waterIntakeReminders = value) } }
    fun updateBedtimeReminder(value: Boolean) { _uiState.update { it.copy(bedtimeReminder = value) } }
    fun updateFriendRequests(value: Boolean) { _uiState.update { it.copy(friendRequests = value) } }
    fun updateGroupInvites(value: Boolean) { _uiState.update { it.copy(groupInvites = value) } }
    fun updateChallengeUpdates(value: Boolean) { _uiState.update { it.copy(challengeUpdates = value) } }
    fun updateMessages(value: Boolean) { _uiState.update { it.copy(messages = value) } }
    fun updateWeeklySummary(value: Boolean) { _uiState.update { it.copy(weeklySummary = value) } }
    fun updateGoalAchieved(value: Boolean) { _uiState.update { it.copy(goalAchieved = value) } }
    fun updateAiSuggestions(value: Boolean) { _uiState.update { it.copy(aiSuggestions = value) } }
    fun updateWeeklyAdjustments(value: Boolean) { _uiState.update { it.copy(weeklyAdjustments = value) } }
    fun updateMotivationalMessages(value: Boolean) { _uiState.update { it.copy(motivationalMessages = value) } }
    fun updateQuietHoursFrom(value: String) { _uiState.update { it.copy(quietHoursFrom = value) } }
    fun updateQuietHoursTo(value: String) { _uiState.update { it.copy(quietHoursTo = value) } }
}
