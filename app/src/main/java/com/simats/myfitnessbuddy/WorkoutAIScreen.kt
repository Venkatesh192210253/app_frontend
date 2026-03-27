package com.simats.myfitnessbuddy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.simats.myfitnessbuddy.data.remote.AiChatRequest
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AIImprovementArea(
    val title: String,
    val reason: String,
    val recommendation: String
)

data class RecommendedWorkout(
    val name: String,
    val level: String
)

data class AIProgressPrediction(
    val label: String,
    val value: String
)

data class AiDynamicSuggestions(
    val improvements: List<AIImprovementArea>,
    val recommendedWorkouts: List<RecommendedWorkout>,
    val progressPredictions: List<AIProgressPrediction>
)

data class AIUiState(
    val analysisMessage: String = "Based on your last 5 workouts, you're performing well. However, we’ve identified areas to improve.",
    val improvements: List<AIImprovementArea> = listOf(
        AIImprovementArea("Add More Leg Training", "Your back workouts are great but leg sessions lag behind.", "Add 1 extra leg session this week."),
        AIImprovementArea("Increase Rest Between Sets", "Your heart rate remains high. Better recovery will boost performance.", "Increase rest to 90s for heavy lifts."),
        AIImprovementArea("Progressive Overload", "You've used the same weight for Bench Press for 3 sessions.", "Increase weight by 2.5kg next session."),
        AIImprovementArea("Add Core Training", "Core stability helps in compound movements.", "Add 10 mins of core after each session.")
    ),
    val recommendedWorkouts: List<RecommendedWorkout> = listOf(
        RecommendedWorkout("Leg Power Builder", "Intermediate"),
        RecommendedWorkout("Core Stability Flow", "Beginner")
    ),
    val progressPredictions: List<AIProgressPrediction> = listOf(
        AIProgressPrediction("Muscle Balance", "+10%"),
        AIProgressPrediction("Strength Gains", "+12%"),
        AIProgressPrediction("Injury Risk", "-8%")
    ),
    val appliedSuggestions: Set<String> = emptySet(),
    val addedWorkouts: Set<String> = emptySet(),
    val actionMessage: String? = null
)

class WorkoutAIViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AIUiState(
        appliedSuggestions = SettingsManager.appliedAiSuggestions,
        addedWorkouts = SettingsManager.addedAiWorkouts
    ))
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()
    
    init {
        fetchAIAnalysis()
    }
    
    private fun fetchAIAnalysis() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            if (SettingsManager.lastAiDate == today && SettingsManager.cachedAiImprovements != "[]" && SettingsManager.cachedAiProgress != "[]") {
                try {
                    val gson = Gson()
                    val type1 = object : TypeToken<List<AIImprovementArea>>() {}.type
                    val type2 = object : TypeToken<List<RecommendedWorkout>>() {}.type
                    val type3 = object : TypeToken<List<AIProgressPrediction>>() {}.type
                    
                    val imp = gson.fromJson<List<AIImprovementArea>>(SettingsManager.cachedAiImprovements, type1)
                    val wrk = gson.fromJson<List<RecommendedWorkout>>(SettingsManager.cachedAiWorkouts, type2)
                    val prog = gson.fromJson<List<AIProgressPrediction>>(SettingsManager.cachedAiProgress, type3)
                    val insight = SettingsManager.cachedAiInsight
                    
                    _uiState.update { it.copy(
                        improvements = imp ?: it.improvements,
                        recommendedWorkouts = wrk ?: it.recommendedWorkouts,
                        progressPredictions = prog ?: it.progressPredictions,
                        analysisMessage = if (insight.isNotEmpty()) insight else it.analysisMessage
                    )}
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            try {
                val dashRes = RetrofitClient.apiService.getDashboardData()
                if (dashRes.isSuccessful) {
                    dashRes.body()?.ai_metrics?.current_suggestion?.message?.let { msg ->
                        val insight = "AI Insight: $msg"
                        _uiState.update { it.copy(analysisMessage = insight) }
                        SettingsManager.cachedAiInsight = insight
                    }
                }
                
                val aiPrompt = """
                    You are an expert AI Fitness Coach.
                    Provide 2 workout improvement suggestions, 2 recommended workouts customized for fitness progress, and 3 expected progress outcome predictions.
                    Respond ONLY in valid JSON format with no markdown wrappers or extra text.
                    Format:
                    {
                        "improvements": [
                            {"title": "string", "reason": "string", "recommendation": "string"}
                        ],
                        "recommendedWorkouts": [
                            {"name": "string", "level": "string"}
                        ],
                        "progressPredictions": [
                            {"label": "string", "value": "string"}
                        ]
                    }
                """.trimIndent()
                
                val aiRes = RetrofitClient.apiService.chatWithAi(AiChatRequest(message = aiPrompt))
                if (aiRes.isSuccessful) {
                    val reply = aiRes.body()?.reply ?: ""
                    try {
                        val gson = Gson()
                        val cleanReply = reply.replace("```json", "").replace("```", "").trim()
                        val parsed = gson.fromJson(cleanReply, AiDynamicSuggestions::class.java)
                        
                        SettingsManager.cachedAiImprovements = gson.toJson(parsed.improvements)
                        SettingsManager.cachedAiWorkouts = gson.toJson(parsed.recommendedWorkouts)
                        SettingsManager.cachedAiProgress = gson.toJson(parsed.progressPredictions)
                        SettingsManager.lastAiDate = today
                        SettingsManager.appliedAiSuggestions = emptySet()
                        SettingsManager.addedAiWorkouts = emptySet()
                        
                        _uiState.update { it.copy(
                            improvements = parsed.improvements,
                            recommendedWorkouts = parsed.recommendedWorkouts,
                            progressPredictions = parsed.progressPredictions,
                            appliedSuggestions = emptySet(),
                            addedWorkouts = emptySet()
                        )}
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun applySuggestion(title: String) {
        viewModelScope.launch {
            val updatedSet = _uiState.value.appliedSuggestions + title
            SettingsManager.appliedAiSuggestions = updatedSet
            _uiState.update { 
                it.copy(
                    appliedSuggestions = updatedSet,
                    actionMessage = "Suggestion Applied: $title"
                )
            }
            try {
                RetrofitClient.apiService.partialGoalUpdate(
                    mapOf("workouts_per_week" to 5)
                )
            } catch (e: Exception) {}
        }
    }

    fun addRecommendedWorkout(name: String) {
        viewModelScope.launch {
            val updatedSet = _uiState.value.addedWorkouts + name
            SettingsManager.addedAiWorkouts = updatedSet
            _uiState.update { 
                it.copy(
                    addedWorkouts = updatedSet,
                    actionMessage = "Added to Schedule: $name"
                )
            }
            try {
                RetrofitClient.apiService.updateWeeklySchedule(
                    mapOf("day_of_week" to 1, "is_rest_day" to false)
                )
            } catch (e: Exception) {}
        }
    }
    
    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutAIScreen(
    onBack: () -> Unit,
    viewModel: WorkoutAIViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF22C55E)
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    
    androidx.compose.runtime.LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Suggestions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        },
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp) // Lift above nav bar
            ) 
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // AI Analysis Complete Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Text(
                        uiState.analysisMessage,
                        modifier = Modifier.padding(20.dp),
                        fontSize = 16.sp,
                        color = Color(0xFF166534)
                    )
                }
            }

            // Improvement Areas Section
            item {
                Text("Improvement Areas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            items(uiState.improvements) { area ->
                ImprovementCard(
                    area = area, 
                    isApplied = uiState.appliedSuggestions.contains(area.title),
                    onApply = { viewModel.applySuggestion(area.title) },
                    primaryGreen = primaryGreen
                )
            }

            // Recommended Workouts Section
            item {
                Text("Recommended Workouts", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            items(uiState.recommendedWorkouts) { workout ->
                RecommendedWorkoutCard(
                    workout = workout, 
                    isAdded = uiState.addedWorkouts.contains(workout.name),
                    onAdd = { viewModel.addRecommendedWorkout(workout.name) },
                    primaryGreen = primaryGreen
                )
            }

            // Expected Progress Section
            item {
                Text("Expected Progress", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.progressPredictions.forEach { prediction ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prediction.label, color = Color.Gray)
                                Text(prediction.value, fontWeight = FontWeight.Bold, color = primaryGreen)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ImprovementCard(
    area: AIImprovementArea, 
    isApplied: Boolean,
    onApply: () -> Unit,
    primaryGreen: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(area.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Reason: ${area.reason}", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Recommendation: ${area.recommendation}", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApplied) Color.LightGray else primaryGreen
                ),
                shape = RoundedCornerShape(50.dp),
                enabled = !isApplied
            ) {
                Text(if (isApplied) "Applied" else "Apply Suggestion", fontWeight = FontWeight.Bold, color = if (isApplied) Color.DarkGray else Color.White)
            }
        }
    }
}

@Composable
fun RecommendedWorkoutCard(
    workout: RecommendedWorkout, 
    isAdded: Boolean,
    onAdd: () -> Unit,
    primaryGreen: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(workout.level, fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdded) Color.LightGray else primaryGreen
                ),
                shape = RoundedCornerShape(50.dp),
                enabled = !isAdded
            ) {
                Text(if (isAdded) "Added" else "Add to Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isAdded) Color.DarkGray else Color.White)
            }
        }
    }
}
