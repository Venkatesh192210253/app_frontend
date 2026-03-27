package com.simats.myfitnessbuddy

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import com.simats.myfitnessbuddy.data.local.SettingsManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.util.Log

data class MealItem(
    val title: String,
    val calories: Int,
    val goal: Int,
    val time: String,
    val progress: Float
)

class DailyCaloriesViewModel : ViewModel() {
    val consumedCalories = mutableStateOf(0)
    val calorieGoal = mutableStateOf(2200)
    
    val meals = mutableStateListOf<MealItem>()
    
    val progress: Float
        get() = if (calorieGoal.value > 0) consumedCalories.value.toFloat() / calorieGoal.value.toFloat() else 0f

    fun loadDailyData() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()
                val response = RetrofitClient.apiService.getDiaryDaily(today)
                
                if (response.isSuccessful) {
                    val data = response.body()
                    data?.let { diaryData ->
                        consumedCalories.value = diaryData.summary.food
                        calorieGoal.value = diaryData.summary.goal
                        
                        // Calculate typical meal goals based on daily target
                        // e.g., Breakfast: 25%, Lunch: 35%, Dinner: 30%, Snacks: 10%
                        val bGoal = (calorieGoal.value * 0.25).toInt()
                        val lGoal = (calorieGoal.value * 0.35).toInt()
                        val dGoal = (calorieGoal.value * 0.30).toInt()
                        val sGoal = (calorieGoal.value * 0.10).toInt()
                        
                        meals.clear()
                        
                        // Map meals from response, grouping entries by meal type
                        val mealTypes = listOf("Breakfast", "Lunch", "Snacks", "Dinner")
                        val goalMap = mapOf("Breakfast" to bGoal, "Lunch" to lGoal, "Dinner" to dGoal, "Snacks" to sGoal)
                        val timeMap = mapOf("Breakfast" to "8:30 AM", "Lunch" to "1:00 PM", "Snacks" to "4:30 PM", "Dinner" to "8:00 PM")

                        mealTypes.forEach { type ->
                            // Case-insensitive lookup: API might return "breakfast" or "Breakfast"
                            val entries = diaryData.meals.entries
                                .firstOrNull { it.key.equals(type, ignoreCase = true) }
                                ?.value ?: emptyList()
                            val totalCals = entries.sumOf { it.calories }
                            val goal = goalMap[type] ?: 500
                            
                            meals.add(MealItem(
                                title = type,
                                calories = totalCals,
                                goal = goal,
                                time = timeMap[type] ?: "--:--",
                                progress = if (goal > 0) (totalCals.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DailyCaloriesViewModel", "Error loading daily data", e)
            }
        }
    }
}
