package com.simats.myfitnessbuddy

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager

import com.simats.myfitnessbuddy.data.remote.GoalSettingsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserGoal {
    LoseWeight, BuildMuscle, Maintain, AthleticPerformance
}

data class GoalSettingsUiState(
    val primaryGoal: UserGoal = UserGoal.BuildMuscle,
    val currentWeight: String = "0",
    val targetWeight: String = "0",
    val weeklyGoal: String = "0.5",
    val currentBodyFat: String = "18",
    val targetBodyFat: String = "15",
    val muscleMassGoal: String = "35",
    val workoutsPerWeek: String = "4",
    val dailyStepGoal: String = "10000",
    val weeklyCalorieBurnGoal: String = "2000",
    val dailyCalorieTarget: String = "2500",
    val proteinG: String = "160",
    val carbsG: String = "250",
    val fatsG: String = "70",
    val consistencyScore: Int = 85,
    val isAdaptiveEnabled: Boolean = false,
    val isSaving: Boolean = false
)

class GoalSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GoalSettingsUiState())
    val uiState: StateFlow<GoalSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.getGoalSettings()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            primaryGoal = UserGoal.valueOf(body.primary_goal),
                            currentWeight = body.current_weight.toString(),
                            targetWeight = body.target_weight.toString(),
                            weeklyGoal = body.weekly_goal_weight.toString(),
                            currentBodyFat = body.current_body_fat.toString(),
                            targetBodyFat = body.target_body_fat.toString(),
                            muscleMassGoal = body.muscle_mass_goal.toString(),
                            workoutsPerWeek = body.workouts_per_week.toString(),
                            dailyStepGoal = body.daily_step_goal.toString(),
                            weeklyCalorieBurnGoal = body.weekly_calorie_burn_goal.toString(),
                            dailyCalorieTarget = body.daily_calorie_target.toString(),
                            proteinG = body.protein_g.toString(),
                            carbsG = body.carbs_g.toString(),
                            fatsG = body.fats_g.toString(),
                            isAdaptiveEnabled = body.is_adaptive_mode_enabled
                        )
                    }
                    SettingsManager.isAdaptiveModeEnabled = body.is_adaptive_mode_enabled
                } else {
                    // Fallback to local settings if API fails
                    loadFromLocal()
                }
            } catch (e: Exception) {
                loadFromLocal()
            }
        }
    }

    private fun loadFromLocal() {
        _uiState.update {
            it.copy(
                dailyCalorieTarget = SettingsManager.calorieGoal,
                currentWeight = SettingsManager.currentWeight,
                targetWeight = SettingsManager.targetWeight,
                weeklyGoal = SettingsManager.weeklyGoal,
                proteinG = SettingsManager.protein,
                carbsG = SettingsManager.carbs,
                fatsG = SettingsManager.fats,
                isAdaptiveEnabled = SettingsManager.isAdaptiveModeEnabled
            )
        }
    }

    fun calculateWeeksToGoal(): Int {
        val current = _uiState.value.currentWeight.toDoubleOrNull() ?: 0.0
        val target = _uiState.value.targetWeight.toDoubleOrNull() ?: 0.0
        val weekly = _uiState.value.weeklyGoal.toDoubleOrNull() ?: 0.5
        
        if (weekly <= 0.0) return 0
        val diff = kotlin.math.abs(target - current)
        return (diff / weekly).toInt()
    }

    fun updatePrimaryGoal(goal: UserGoal) {
        _uiState.update { it.copy(primaryGoal = goal) }
    }

    fun updateCurrentWeight(value: String) { _uiState.update { it.copy(currentWeight = value) } }
    fun updateTargetWeight(value: String) { _uiState.update { it.copy(targetWeight = value) } }
    fun updateWeeklyGoal(value: String) { _uiState.update { it.copy(weeklyGoal = value) } }
    fun updateCurrentBodyFat(value: String) { _uiState.update { it.copy(currentBodyFat = value) } }
    fun updateTargetBodyFat(value: String) { _uiState.update { it.copy(targetBodyFat = value) } }
    fun updateMuscleMassGoal(value: String) { _uiState.update { it.copy(muscleMassGoal = value) } }
    fun updateWorkoutsPerWeek(value: String) { _uiState.update { it.copy(workoutsPerWeek = value) } }
    fun updateDailyStepGoal(value: String) { _uiState.update { it.copy(dailyStepGoal = value) } }
    fun updateWeeklyCalorieBurnGoal(value: String) { _uiState.update { it.copy(weeklyCalorieBurnGoal = value) } }
    fun updateDailyCalorieTarget(value: String) { _uiState.update { it.copy(dailyCalorieTarget = value) } }
    fun updateProtein(value: String) { _uiState.update { it.copy(proteinG = value) } }
    fun updateCarbs(value: String) { _uiState.update { it.copy(carbsG = value) } }
    fun updateFats(value: String) { _uiState.update { it.copy(fatsG = value) } }
    fun updateAdaptiveMode(value: Boolean) { _uiState.update { it.copy(isAdaptiveEnabled = value) } }

    fun validateAndSave(onComplete: (Boolean) -> Unit) {
        // Simple persistence logic fallback
        SettingsManager.calorieGoal = _uiState.value.dailyCalorieTarget
        SettingsManager.currentWeight = _uiState.value.currentWeight
        SettingsManager.targetWeight = _uiState.value.targetWeight
        SettingsManager.weeklyGoal = _uiState.value.weeklyGoal
        SettingsManager.protein = _uiState.value.proteinG
        SettingsManager.carbs = _uiState.value.carbsG
        SettingsManager.fats = _uiState.value.fatsG
        SettingsManager.isAdaptiveModeEnabled = _uiState.value.isAdaptiveEnabled

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val requestBody = GoalSettingsResponse(
                    primary_goal = _uiState.value.primaryGoal.name,
                    current_weight = _uiState.value.currentWeight.toDoubleOrNull() ?: 0.0,
                    target_weight = _uiState.value.targetWeight.toDoubleOrNull() ?: 0.0,
                    weekly_goal_weight = _uiState.value.weeklyGoal.toDoubleOrNull() ?: 0.5,
                    current_body_fat = _uiState.value.currentBodyFat.toDoubleOrNull() ?: 18.0,
                    target_body_fat = _uiState.value.targetBodyFat.toDoubleOrNull() ?: 15.0,
                    muscle_mass_goal = _uiState.value.muscleMassGoal.toDoubleOrNull() ?: 35.0,
                    workouts_per_week = _uiState.value.workoutsPerWeek.toIntOrNull() ?: 4,
                    daily_step_goal = _uiState.value.dailyStepGoal.toIntOrNull() ?: 10000,
                    weekly_calorie_burn_goal = _uiState.value.weeklyCalorieBurnGoal.toIntOrNull() ?: 2000,
                    daily_calorie_target = _uiState.value.dailyCalorieTarget.trim().toIntOrNull() ?: 2500,
                    protein_g = _uiState.value.proteinG.trim().toIntOrNull() ?: 160,
                    carbs_g = _uiState.value.carbsG.trim().toIntOrNull() ?: 250,
                    fats_g = _uiState.value.fatsG.trim().toIntOrNull() ?: 70,
                    is_adaptive_mode_enabled = _uiState.value.isAdaptiveEnabled
                )
                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.updateGoalSettings(requestBody)
                onComplete(response.isSuccessful)
            } catch (e: Exception) {
                onComplete(false)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
