package com.simats.myfitnessbuddy

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Simple data classes inside the file to avoid external dependency issues
data class AiSuggestion(
    val id: String,
    val message: String,
    val type: SuggestionType
)

enum class SuggestionType {
    RECOVERY, WORKOUT, CALORIE, REST, PREDICTION
}

data class PredictionData(
    val targetDate: String,
    val totalDays: Int,
    val confidence: Int,
    val trend: String
)

class AdaptiveAiManager : ViewModel() {

    // These are the methods causing resolution issues - moved to top and simplified
    fun ping() {
        android.util.Log.d("AdaptiveAiManager", "Ping captured")
    }

    fun syncFullData(data: com.simats.myfitnessbuddy.data.remote.DashboardDataResponse) {
        val stats = data.daily_stats
        val goals = data.goal_settings
        
        // 0. Update Adaptive Mode state
        _isAdaptiveModeEnabled.value = goals.is_adaptive_mode_enabled
        SettingsManager.isAdaptiveModeEnabled = goals.is_adaptive_mode_enabled

        // 1. Energy Balance Index (Today)
        // Deficit of ~500 is optimal (100%), 0 deficit is 50%, surplus drops it
        val todayConsumed = stats.calories_consumed
        val todayBurned = stats.calories_burned
        val net = todayBurned - todayConsumed
        
        // Use backend score as primary if available, otherwise calculate locally
        val backendScore = data.ai_metrics.energy_balance_score
        _energyBalanceScore.value = if (backendScore > 0) backendScore else (50 + (net / 10)).coerceIn(0, 100)
        _dailyDeficit.value = net

        // 2. Recovery & Readiness (Dynamic)
        var baseRecovery = data.ai_metrics.recovery_score
        
        // Fatigue Penalty: Look at yesterday's burned calories
        val yesterday = data.weekly_history.lastOrNull()
        if (yesterday != null && yesterday.calories_burned > 800) {
            baseRecovery -= 15 // High intensity yesterday reduces readiness
            _recoveryMessage.value = "Recovery slowed by yesterday's high intensity training. Focus on mobility today."
            _recommendedWorkout.value = "Active Recovery / Yoga"
        } else {
            _recoveryMessage.value = "You're rested and ready for a great session!"
            _recommendedWorkout.value = "High Intensity Interval Training"
        }
        
        // Nutrition Penalty: If today's intake is way below target relative to the time of day
        // (Simplified: if consumed < 40% of target by current check)
        if (todayConsumed < goals.daily_calorie_target * 0.4) {
             baseRecovery -= 10
             if (_recoveryMessage.value.contains("rested")) {
                 _recoveryMessage.value = "Energy levels are low due to calorie deficit. Fuel up before your workout!"
             }
        }

        _recoveryScore.value = baseRecovery.coerceIn(0, 100)
        _injuryRiskScore.value = data.ai_metrics.injury_risk_score
        
        // Update Goal Prediction
        data.weight_goal?.let { wg ->
            updateGoalPrediction(
                currentWeight = goals.current_weight.toFloat(),
                targetWeight = wg.target_weight ?: goals.target_weight.toFloat(),
                avgDeficit = maxOf(500, net),
                weeklyGoalWeight = wg.weekly_goal_weight,
                serverWeeks = wg.weeks_remaining
            )
        } ?: updateGoalPrediction(
            currentWeight = goals.current_weight.toFloat(),
            targetWeight = goals.target_weight.toFloat(),
            avgDeficit = maxOf(500, net),
            weeklyGoalWeight = goals.weekly_goal_weight.toFloat()
        )
    }

    fun updateGoalPrediction(
        currentWeight: Float, 
        targetWeight: Float, 
        avgDeficit: Int, 
        weeklyGoalWeight: Float = 0f,
        serverWeeks: Float? = null
    ) {
        if (kotlin.math.abs(currentWeight - targetWeight) < 0.1f) {
            _prediction.value = null
            return
        }

        val weightToLose = kotlin.math.abs(currentWeight - targetWeight)
        
        // Priority 1: User set weekly goal (kg/week)
        // Priority 2: Calculated from calorie deficit (7700 kcal per kg)
        val daysNeeded = if (serverWeeks != null && serverWeeks > 0) {
            (serverWeeks * 7).toInt()
        } else if (weeklyGoalWeight > 0.05f) {
            val weeksNeeded = weightToLose / weeklyGoalWeight
            (weeksNeeded * 7).toInt()
        } else {
            if (avgDeficit <= 0) {
                _prediction.value = null
                return
            }
            val totalCaloriesNeeded = weightToLose * 7700
            (totalCaloriesNeeded / avgDeficit).toInt()
        }
        
        if (daysNeeded <= 0) {
            _prediction.value = null
            return
        }

        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysNeeded)
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        
        val trendText = if (weeklyGoalWeight > 0.05f) {
            "Paced at ${String.format("%.2f", weeklyGoalWeight)}kg/week"
        } else {
            "Trending ${String.format("%.2f", (avgDeficit.toFloat() * 7 / 7700))}kg/week"
        }

        _prediction.value = PredictionData(
            targetDate = sdf.format(calendar.time),
            totalDays = daysNeeded,
            confidence = if (weeklyGoalWeight > 0.05f) 95 else 85,
            trend = trendText
        )
    }

    // --- State Properties ---

    private val _isAdaptiveModeEnabled = MutableStateFlow(SettingsManager.isAdaptiveModeEnabled)
    val isAdaptiveModeEnabled: StateFlow<Boolean> = _isAdaptiveModeEnabled.asStateFlow()

    private val _recoveryScore = mutableStateOf(82)
    val recoveryScore: State<Int> = _recoveryScore

    private val _energyBalanceScore = mutableStateOf(85)
    val energyBalanceScore: State<Int> = _energyBalanceScore

    private val _injuryRiskScore = mutableStateOf(15)
    val injuryRiskScore: State<Int> = _injuryRiskScore

    private val _recoveryMessage = mutableStateOf("You're ready to push your limits!")
    private val _recommendedWorkout = mutableStateOf("High Intensity Interval Training")

    private val _currentSuggestion = mutableStateOf<AiSuggestion?>(
        AiSuggestion("1", "Your recovery is high. Push your limits today!", SuggestionType.WORKOUT)
    )
    val currentSuggestion: State<AiSuggestion?> = _currentSuggestion

    private val _prediction = mutableStateOf<PredictionData?>(null)
    val prediction: State<PredictionData?> = _prediction

    private val _dailyDeficit = mutableStateOf(0)
    val dailyDeficit: State<Int> = _dailyDeficit

    val behaviorInsights = listOf(
        "Consistency is up 20%",
        "Weekend calorie intake is trending higher",
        "Higher step count correlates with better recovery"
    )

    // --- Helper Methods ---

    fun getInjuryRiskMessage(): String? = if (_injuryRiskScore.value > 30) "Risk of strain detected" else null

    fun getEnergyBalanceStatus(): String = when {
        _energyBalanceScore.value >= 80 -> "Optimal"
        _energyBalanceScore.value >= 50 -> "Balanced"
        else -> "Negative"
    }

    fun toggleAdaptiveMode(enabled: Boolean) {
        _isAdaptiveModeEnabled.value = enabled
        SettingsManager.isAdaptiveModeEnabled = enabled
    }

    fun dismissSuggestion() { _currentSuggestion.value = null }

    fun acceptSuggestion(onAdjustmentApplied: () -> Unit = {}) {
        _currentSuggestion.value = null
        onAdjustmentApplied()
    }

    fun getRecoveryStatus(): String = when {
        _recoveryScore.value >= 80 -> "High Readiness"
        _recoveryScore.value >= 50 -> "Moderate"
        else -> "Low / Rest Needed"
    }

    fun getRecoveryMessage(): String = _recoveryMessage.value
    
    fun getRecommendedWorkout(): String = _recommendedWorkout.value

    fun getEnergyBalanceScore(): Int = _energyBalanceScore.value
}
