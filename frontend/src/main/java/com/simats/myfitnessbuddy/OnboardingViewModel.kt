package com.simats.myfitnessbuddy

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.remote.ProfileResponse

import kotlinx.coroutines.launch
import kotlin.math.abs

enum class Gender { MALE, FEMALE, NONE }

data class OnboardingUiState(
    val currentStep: Int = 0,
    val goalsCompleted: Boolean = false,
    
    // Step 3-8 (Selection based)
    val selectedGoals: List<String> = emptyList(),
    val barriers: List<String> = emptyList(),
    val habits: List<String> = emptyList(),
    val mealPlanningFreq: String = "",
    val weeklyMealPlans: String = "",
    val activityLevel: String = "",
    
    // Step 9: Demographics
    val gender: Gender = Gender.NONE,
    val age: String = "",
    val country: String = "",
    
    // Step 10: Height & Weight
    val heightFeet: String = "",
    val heightInches: String = "",
    val currentWeight: String = "",
    val goalWeight: String = "",
    
    // Step 11: Weekly Goal
    val weeklyGoal: String = "",
    
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)



class OnboardingViewModel : ViewModel() {
    private val _uiState = mutableStateOf(OnboardingUiState())
    val uiState: State<OnboardingUiState> = _uiState

    // Weight Logic Derived States
    val weightDifference = derivedStateOf {
        val current = _uiState.value.currentWeight.toDoubleOrNull() ?: 0.0
        val goal = _uiState.value.goalWeight.toDoubleOrNull() ?: 0.0
        current - goal
    }

    val weightStatus = derivedStateOf {
        val diff = weightDifference.value
        val current = _uiState.value.currentWeight.toDoubleOrNull()
        val goal = _uiState.value.goalWeight.toDoubleOrNull()
        
        if (current == null || goal == null) ""
        else if (diff > 0) "lose"
        else if (diff < 0) "gain"
        else "maintain"
    }

    val weeksToGoal = derivedStateOf {
        val diff = abs(weightDifference.value)
        if (diff == 0.0) 0 else (diff / 0.5).toInt()
    }

    val isNextEnabled = derivedStateOf {
        if (_uiState.value.isLoading) return@derivedStateOf false
        when (_uiState.value.currentStep) {
            0 -> _uiState.value.selectedGoals.isNotEmpty()
            1 -> _uiState.value.barriers.isNotEmpty()
            2 -> _uiState.value.habits.isNotEmpty()
            3 -> _uiState.value.mealPlanningFreq.isNotEmpty()
            4 -> _uiState.value.weeklyMealPlans.isNotEmpty()
            5 -> _uiState.value.activityLevel.isNotEmpty()
            6 -> _uiState.value.gender != Gender.NONE && 
                 _uiState.value.age.isNotEmpty() && 
                 _uiState.value.country.isNotEmpty()
            7 -> _uiState.value.heightFeet.isNotEmpty() && 
                 _uiState.value.heightInches.isNotEmpty() && 
                 _uiState.value.currentWeight.isNotEmpty() && 
                 _uiState.value.goalWeight.isNotEmpty()
            8 -> _uiState.value.weeklyGoal.isNotEmpty()
            else -> false
        }
    }

    // Setters
    fun nextStep(onFinish: () -> Unit) {
        val state = _uiState.value
        if (state.currentStep < 8) {
            _uiState.value = _uiState.value.copy(currentStep = state.currentStep + 1)
        } else {
            onFinish()
        }
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun updateGoals(goals: List<String>) { _uiState.value = _uiState.value.copy(selectedGoals = goals) }
    fun updateBarriers(barriers: List<String>) { _uiState.value = _uiState.value.copy(barriers = barriers) }
    fun updateHabits(habits: List<String>) { _uiState.value = _uiState.value.copy(habits = habits) }
    fun updateMealFreq(freq: String) { _uiState.value = _uiState.value.copy(mealPlanningFreq = freq) }
    fun updateWeeklyMealPlans(ans: String) { _uiState.value = _uiState.value.copy(weeklyMealPlans = ans) }
    fun updateActivityLevel(level: String) { _uiState.value = _uiState.value.copy(activityLevel = level) }
    
    fun updateGender(gender: Gender) { _uiState.value = _uiState.value.copy(gender = gender) }
    fun updateAge(age: String) { _uiState.value = _uiState.value.copy(age = age) }
    fun updateCountry(country: String) { _uiState.value = _uiState.value.copy(country = country) }
    
    fun updateHeightFeet(ft: String) { _uiState.value = _uiState.value.copy(heightFeet = ft) }
    fun updateHeightInches(inch: String) { _uiState.value = _uiState.value.copy(heightInches = inch) }
    fun updateCurrentWeight(w: String) { _uiState.value = _uiState.value.copy(currentWeight = w) }
    fun updateGoalWeight(w: String) { _uiState.value = _uiState.value.copy(goalWeight = w) }
    
    fun updateWeeklyGoal(goal: String) { _uiState.value = _uiState.value.copy(weeklyGoal = goal) }

    fun saveProfile(onResult: (Boolean) -> Unit) {
        val state = _uiState.value
        val profile = ProfileResponse(
            goals_completed = true,
            gender = state.gender.name,
            age = state.age.toIntOrNull(),
            country = state.country,
            height_ft = state.heightFeet.toIntOrNull(),
            height_in = state.heightInches.toIntOrNull(),
            current_weight = state.currentWeight.toFloatOrNull(),
            goal_weight = state.goalWeight.toFloatOrNull(),
            activity_level = state.activityLevel,
            goals = state.selectedGoals,
            barriers = state.barriers,
            habits = state.habits,
            meal_planning_freq = state.mealPlanningFreq,
            weekly_meal_plans = state.weeklyMealPlans,
            weekly_goal = state.weeklyGoal
        )

        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = RetrofitClient.apiService.updateProfile(profile)
                if (response.isSuccessful) {
                    com.simats.myfitnessbuddy.data.local.SettingsManager.goalsCompleted = true
                    _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
                    onResult(true)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val json = com.google.gson.JsonParser().parse(errorBody).asJsonObject
                        json.entrySet().firstOrNull()?.let { entry ->
                            val msgs = entry.value.asJsonArray
                            "${entry.key}: ${msgs.firstOrNull()?.asString ?: "Error"}"
                        } ?: "Failed to save profile (${response.code()})"
                    } catch (e: Exception) {
                        "Failed to save profile (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
                    onResult(false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
                onResult(false)
            }
        }
    }
}
