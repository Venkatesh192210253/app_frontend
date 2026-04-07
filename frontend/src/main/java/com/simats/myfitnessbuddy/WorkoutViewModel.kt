package com.simats.myfitnessbuddy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WorkoutType(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val metValue: Float
)

data class WorkoutProgram(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

data class WorkoutSession(
    val name: String,
    val calories: Int,
    val duration: String,
    val date: String = "Today"
)

data class WorkoutUiState(
    val selectedWorkout: WorkoutType? = null,
    val durationMinutes: Int = 30,
    val intensity: Float = 1f,
    val caloriesBurned: Int = 0,
    val workoutTypes: List<WorkoutType> = listOf(
        WorkoutType("run", "Running", Icons.Default.DirectionsRun, 8f),
        WorkoutType("gym", "Weightlifting", Icons.Default.FitnessCenter, 5f),
        WorkoutType("pool", "Swimming", Icons.Default.Pool, 7f),
        WorkoutType("yoga", "Yoga", Icons.Default.SelfImprovement, 3f)
    ),
    // Dashboard Specific State
    val thisWeekWorkouts: Int = 0,
    val totalTimeMinutes: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val currentWorkout: String = "Rest Day",
    val currentWorkoutTime: Int = 0,
    val exercisesCompleted: Int = 0,
    val totalExercises: Int = 0,
    val caloriesTarget: Int = 2000,
    val caloriesCurrent: Int = 0,
    val recentWorkouts: List<WorkoutSession> = emptyList(),
    val programs: List<WorkoutProgram> = listOf(
        WorkoutProgram("chest", "Chest & Triceps", Icons.Default.FitnessCenter, Color(0xFF22C55E)),
        WorkoutProgram("back", "Back & Biceps", Icons.Default.FitnessCenter, Color(0xFF22C55E)),
        WorkoutProgram("shoulders", "Shoulders", Icons.Default.FitnessCenter, Color(0xFF22C55E)),
        WorkoutProgram("legs", "Legs", Icons.Default.FitnessCenter, Color(0xFF22C55E)),
        WorkoutProgram("abs", "Abs", Icons.Default.FitnessCenter, Color(0xFF22C55E)),
        WorkoutProgram("fullbody", "Full Body", Icons.Default.FitnessCenter, Color(0xFF22C55E))
    ),
    val aiSuggestionMessage: String = "Analyzing your routine..."
)

class WorkoutViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        selectWorkout(_uiState.value.workoutTypes.first())
        fetchWorkoutStatus()
    }

    fun fetchWorkoutStatus() {
        viewModelScope.launch {
            try {
                val historyResponse = RetrofitClient.apiService.getWorkoutHistory()
                val templatesResponse = RetrofitClient.apiService.getWorkoutTemplates()
                val dashRes = RetrofitClient.apiService.getDashboardData() // Fetch dashboard data to extract AI Suggestion
                
                var suggestionMsg = "Keep pushing your limits! Logging your workouts accurately helps our AI tailor the perfect routine for you."
                
                if (dashRes.isSuccessful) {
                    dashRes.body()?.ai_metrics?.current_suggestion?.message?.let {
                        suggestionMsg = it
                    }
                }

                if (historyResponse.isSuccessful) {
                    val data = historyResponse.body()
                    
                    val recentLogs = data?.recentWorkouts?.take(3)?.map {
                        WorkoutSession(
                            name = it.workout_type,
                            calories = it.calories_burned,
                            duration = "${it.duration_minutes} min",
                            date = it.date
                        )
                    }
                    
                    val todayStr = LocalDate.now().toString()
                    val todayLog = data?.recentWorkouts?.find { it.date == todayStr || it.created_at?.startsWith(todayStr) == true }
                    
                    var totalExercisesCount = 8
                    val completedExercises = todayLog?.exercises?.count { it.is_completed } ?: 0
                    val workoutType = todayLog?.workout_type ?: "Chest"

                    if (templatesResponse.isSuccessful) {
                        val templates = templatesResponse.body()
                        val matchedTemplate = templates?.find { it.name?.equals(workoutType, ignoreCase = true) == true }
                        if (matchedTemplate != null && matchedTemplate.exercises != null) {
                            totalExercisesCount = matchedTemplate.exercises.size
                        }
                    }
                    
                    val todayCalories = todayLog?.calories_burned ?: 0
                    val dailyTarget = dashRes.isSuccessful.let { if(it) dashRes.body()?.goal_settings?.daily_calorie_target ?: 2000 else 2000 }
                    
                    // If no workout yet today, try to get today's scheduled workout name
                    var displayWorkout = workoutType
                    if (todayLog == null) {
                        try {
                            val todayTemplate = RetrofitClient.apiService.getTodayWorkout()
                            if (todayTemplate.isSuccessful) {
                                todayTemplate.body()?.name?.let { displayWorkout = it }
                            }
                        } catch(e: Exception) {}
                    }

                    _uiState.update { state -> 
                        state.copy(
                            currentWorkout = displayWorkout,
                            exercisesCompleted = completedExercises,
                            totalExercises = if (totalExercisesCount > 0) totalExercisesCount else 8,
                            caloriesCurrent = todayCalories,
                            caloriesTarget = dailyTarget,
                            recentWorkouts = recentLogs ?: state.recentWorkouts,
                            aiSuggestionMessage = suggestionMsg,
                            // Calculate weekly stats from logs
                            thisWeekWorkouts = data?.recentWorkouts?.filter { 
                                try { 
                                    val logDate = it.date ?: it.created_at?.split("T")?.get(0)
                                    java.time.LocalDate.parse(logDate).isAfter(java.time.LocalDate.now().minusDays(7)) 
                                } catch(e: Exception) { false }
                            }?.size ?: 0,
                            totalTimeMinutes = data?.recentWorkouts?.filter { 
                                try { 
                                    val logDate = it.date ?: it.created_at?.split("T")?.get(0)
                                    java.time.LocalDate.parse(logDate).isAfter(java.time.LocalDate.now().minusDays(7)) 
                                } catch(e: Exception) { false }
                            }?.sumOf { it.duration_minutes } ?: 0,
                            totalCaloriesBurned = data?.recentWorkouts?.filter { 
                                try { 
                                    val logDate = it.date ?: it.created_at?.split("T")?.get(0)
                                    java.time.LocalDate.parse(logDate).isAfter(java.time.LocalDate.now().minusDays(7)) 
                                } catch(e: Exception) { false }
                            }?.sumOf { it.calories_burned } ?: 0
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Error fetching workout status", e)
            }
        }
    }

    fun selectWorkout(workout: WorkoutType) {
        _uiState.update { it.copy(selectedWorkout = workout) }
        calculateCalories()
    }

    fun updateDuration(duration: Int) {
        _uiState.update { it.copy(durationMinutes = duration.coerceIn(5, 300)) }
        calculateCalories()
    }

    fun updateIntensity(intensity: Float) {
        _uiState.update { it.copy(intensity = intensity) }
        calculateCalories()
    }

    private fun calculateCalories() {
        val state = _uiState.value
        val workout = state.selectedWorkout ?: return
        val weight = com.simats.myfitnessbuddy.data.local.SettingsManager.currentWeight.toFloatOrNull() ?: 0f
        val burn = (workout.metValue * weight * (state.durationMinutes / 60f) * state.intensity).toInt()
        _uiState.update { it.copy(caloriesBurned = burn) }
    }
}
