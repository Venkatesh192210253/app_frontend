package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeeklyWorkout(
    val day: String,
    val workoutName: String,
    val duration: String,
    val exercises: Int,
    val calories: Int,
    val isCompleted: Boolean = false,
    val workoutCode: String,
    val type: String // e.g., "chest", "back", "rest"
)

data class WeeklyScheduleUiState(
    val completedDays: Int = 0,
    val totalDays: Int = 7,
    val totalWorkouts: Int = 0,
    val completedWorkouts: Int = 0,
    val weeklyCalories: Int = 0,
    val schedule: List<WeeklyWorkout> = listOf(),
    val isLoading: Boolean = false
)

class WeeklyScheduleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    fun loadSchedule() {
        fetchSchedule()
    }

    private fun fetchSchedule() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val scheduleRes = RetrofitClient.apiService.getWeeklySchedule()
                val historyRes = RetrofitClient.apiService.getWorkoutHistory()
                
                if (scheduleRes.isSuccessful && historyRes.isSuccessful) {
                    val backendSchedules = scheduleRes.body() ?: emptyList()
                    val history = historyRes.body()?.recentWorkouts ?: emptyList()
                    
                    // Calculate the start of the current week (Monday)
                    val today = java.time.LocalDate.now()
                    val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    
                    // Filter history for THIS calendar week only
                    val currentWeekLogs = history.filter {
                        try {
                            val logDate = java.time.LocalDate.parse(it.date)
                            !logDate.isBefore(startOfWeek) && !logDate.isAfter(today)
                        } catch (e: Exception) { false }
                    }

                    val mappedSchedule = backendSchedules.map { s ->
                        // A day is completed if it's a rest day OR if there's a log matching the day name OR workout type in the current week
                        val isRest = s.is_rest_day
                        val templateName = s.template?.name
                        val dayName = s.day_name
                        
                        // We also check if the log date corresponds to the day of the week
                        val isDone = if (isRest) {
                            true // Automatically complete rest days (e.g., Sunday)
                        } else {
                            currentWeekLogs.any { log -> 
                                try {
                                    val logDay = java.time.LocalDate.parse(log.date).dayOfWeek.name
                                    logDay.equals(dayName, ignoreCase = true) ||
                                    (templateName?.contains(log.workout_type, ignoreCase = true) == true)
                                } catch(e: Exception) {
                                    templateName?.contains(log.workout_type, ignoreCase = true) == true
                                }
                            }
                        }

                        WeeklyWorkout(
                            day = s.day_name,
                            workoutName = if (isRest) "Rest Day" else (templateName ?: "No Workout"),
                            duration = "${s.template?.exercises?.size?.times(5) ?: 45} min", // Dynamic estimate
                            exercises = s.template?.exercises?.size ?: 0,
                            calories = if (isRest) 0 else 300,
                            isCompleted = isDone,
                            workoutCode = if (isRest) "RD" else (templateName?.take(1)?.uppercase() ?: "W"),
                            type = if (isRest) "rest" else "workout"
                        )
                    }

                    val totalWorkoutsCount = mappedSchedule.count { it.type != "rest" }
                    val completedWorkoutsCount = mappedSchedule.count { it.type != "rest" && it.isCompleted }
                    val completedDaysCount = mappedSchedule.count { it.isCompleted }
                    val weeklyCals = currentWeekLogs.sumOf { it.calories_burned }

                    _uiState.update { state -> 
                        state.copy(
                            schedule = mappedSchedule,
                            completedDays = completedDaysCount,
                            totalWorkouts = totalWorkoutsCount,
                            completedWorkouts = completedWorkoutsCount,
                            weeklyCalories = weeklyCals,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WeeklySchedule", "Error loading schedule", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleWorkoutCompletion(day: String) {
        _uiState.update { state ->
            val newSchedule = state.schedule.map {
                if (it.day == day) it.copy(isCompleted = !it.isCompleted) else it
            }
            state.copy(
                schedule = newSchedule,
                completedDays = newSchedule.count { it.isCompleted },
                completedWorkouts = newSchedule.count { it.type != "rest" && it.isCompleted }
            )
        }
    }
}
