package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class NutrientGoal(
    val name: String,
    val current: Float,
    val goal: Float,
    val unit: String,
    val color: Color,
    val iconColor: Color,
    val description: String
)

data class CalorieStat(
    val breakfast: Int = 0,
    val lunch: Int = 0,
    val dinner: Int = 0,
    val snacks: Int = 0,
    val total: Int = 0,
    val net: Int = 0,
    val goal: Int = 2840
)

data class HighCalorieFood(
    val name: String,
    val value: Int,
    val unit: String = "g"
)

data class NutrientRowData(
    val name: String,
    val total: Int,
    val goal: Int,
    val unit: String
) {
    val left: Int get() = (goal - total).coerceAtLeast(0)
}

data class MacroData(
    val name: String,
    val grams: Int,
    val percentage: Int,
    val targetPercentage: Int,
    val color: Color
)

data class NutritionState(
    val selectedTab: Int = 0,
    val foodsTracked: Int = 0,
    val mealsTracked: Int = 0,
    val overallProgress: Float = 0.35f,
    val calorieStat: CalorieStat = CalorieStat(),
    val highCalorieFoods: List<HighCalorieFood> = listOf(
        HighCalorieFood("Rice Bowl", 43),
        HighCalorieFood("Tomato Soup", 30)
    ),
    val nutrientGoals: List<NutrientGoal> = listOf(
        NutrientGoal(
            name = "Protein",
            current = 0f,
            goal = 142f,
            unit = "g",
            color = Color(0xFF4A6FFF),
            iconColor = Color(0xFFFFB74D),
            description = "Protein helps build and repair muscles. Aim for lean sources."
        ),
        NutrientGoal(
            name = "Fiber",
            current = 0f,
            goal = 38f,
            unit = "g",
            color = Color(0xFF00C896),
            iconColor = Color(0xFF81C784),
            description = "Fiber is essential for digestive health and keeps you full longer."
        ),
        NutrientGoal(
            name = "Carbohydrates",
            current = 0f,
            goal = 355f,
            unit = "g",
            color = Color(0xFF9C6CFF),
            iconColor = Color(0xFF4DB6AC),
            description = "Carbs are your body's main energy source. Focus on complex carbs."
        )
    ),
    val nutrientsList: List<NutrientRowData> = listOf(
        NutrientRowData("Protein", 0, 142, "g"),
        NutrientRowData("Carbohydrates", 0, 355, "g"),
        NutrientRowData("Fiber", 0, 38, "g"),
        NutrientRowData("Sugar", 0, 107, "g"),
        NutrientRowData("Fat", 0, 95, "g"),
        NutrientRowData("Saturated Fat", 0, 32, "g"),
        NutrientRowData("Polyunsaturated Fat", 0, 0, "g"),
        NutrientRowData("Monounsaturated Fat", 0, 0, "g"),
        NutrientRowData("Trans Fat", 0, 0, "g"),
        NutrientRowData("Cholesterol", 0, 300, "mg"),
        NutrientRowData("Sodium", 0, 2300, "mg"),
        NutrientRowData("Potassium", 0, 3500, "mg"),
        NutrientRowData("Vitamin A", 0, 100, "%"),
        NutrientRowData("Vitamin C", 0, 100, "%"),
        NutrientRowData("Calcium", 0, 100, "%"),
        NutrientRowData("Iron", 0, 100, "%")
    ),
    val macros: List<MacroData> = listOf(
        MacroData("Carbohydrates", 0, 0, 50, Color(0xFF22D3EE)),
        MacroData("Fat", 0, 0, 30, Color(0xFFA78BFA)),
        MacroData("Protein", 0, 0, 20, Color(0xFFF59E0B))
    ),
    val selectedDate: LocalDate = LocalDate.now()
) {
    val dateDisplay: String
        get() = when (selectedDate) {
            LocalDate.now() -> "Today"
            LocalDate.now().minusDays(1) -> "Yesterday"
            LocalDate.now().plusDays(1) -> "Tomorrow"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
}

class NutritionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NutritionState())
    val uiState: StateFlow<NutritionState> = _uiState.asStateFlow()

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun onPreviousDay() {
        _uiState.value = _uiState.value.copy(selectedDate = _uiState.value.selectedDate.minusDays(1))
    }

    fun onNextDay() {
        _uiState.value = _uiState.value.copy(selectedDate = _uiState.value.selectedDate.plusDays(1))
    }
}
