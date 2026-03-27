package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import android.util.Log

data class MonthlyLog(
    val month: String,
    val workouts: Int,
    val calories: Int
)

data class PersonalRecordUi(
    val title: String,
    val value: String,
    val date: String,
    val icon: ImageVector
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weightLost: Double = 0.0,
    val totalDaysTracked: Int = 0,
    val totalWorkouts: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val avgDailyCalories: Int = 0,
    val monthlyBreakdown: List<MonthlyLog> = emptyList(),
    val personalRecords: List<PersonalRecordUi> = emptyList(),
    val error: String? = null
)

class StatsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    
    init {
        fetchDetailedStats()
    }
    
    fun fetchDetailedStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.apiService.getDetailedStats()
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        val recordsUi = data.personalRecords.map { pr ->
                            val icon = when (pr.type) {
                                "calories" -> Icons.Filled.LocalFireDepartment
                                "steps" -> Icons.Filled.EventAvailable // Ideally we can add a Steps icon
                                else -> Icons.Filled.EventAvailable
                            }
                            PersonalRecordUi(
                                title = pr.title,
                                value = pr.value,
                                date = pr.date,
                                icon = icon
                            )
                        }
                        
                        val monthlyLogs = data.monthlyBreakdown.map { mb ->
                            MonthlyLog(
                                month = mb.month,
                                workouts = mb.workouts,
                                calories = mb.calories
                            )
                        }

                        _uiState.value = StatsUiState(
                            isLoading = false,
                            currentStreak = data.currentStreak,
                            longestStreak = data.longestStreak,
                            weightLost = data.weightLost.toDouble(),
                            totalDaysTracked = data.totalDaysTracked,
                            totalWorkouts = data.totalWorkouts,
                            totalCaloriesBurned = data.totalCaloriesBurned,
                            avgDailyCalories = data.avgDailyCalories,
                            monthlyBreakdown = monthlyLogs,
                            personalRecords = recordsUi
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Empty response")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Error fetching stats", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }
}
