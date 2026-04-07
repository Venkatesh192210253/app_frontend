package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.border
import com.simats.myfitnessbuddy.data.remote.ExerciseLogRequest
import com.simats.myfitnessbuddy.data.remote.WorkoutLogRequest
import java.time.LocalDate

data class ExerciseLog(
    val id: String,
    val name: String,
    val setsReps: String,
    val weight: String,
    val isCompleted: Boolean = false
)

data class LogWorkoutUiState(
    val workoutType: String = "",
    val exercises: List<ExerciseLog> = listOf(),
    val isTrackingMode: Boolean = false,
    val caloriesBurned: Int = 0,
    val manualWorkouts: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

class LogWorkoutViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LogWorkoutUiState())
    val uiState: StateFlow<LogWorkoutUiState> = _uiState.asStateFlow()

    private var startTimeMillis: Long = 0
    private var userWeightKg: Float = 0f // Default fallback

    init {
        fetchUserWeight()
    }

    private fun fetchUserWeight() {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getGoalSettings()
                if (res.isSuccessful) {
                    res.body()?.current_weight?.let { 
                        userWeightKg = it.toFloat()
                    }
                }
            } catch (e: Exception) {
                // Ignore, use fallback (0kg)
            }
        }
    }

    private fun getMetValueForWorkout(type: String): Float {
        val lowerType = type.lowercase()
        return when {
            lowerType.contains("run") -> 8f
            lowerType.contains("pool") || lowerType.contains("swim") -> 7f
            lowerType.contains("yoga") -> 3f
            lowerType.contains("chest") || lowerType.contains("back") ||
            lowerType.contains("legs") || lowerType.contains("shoulders") ||
            lowerType.contains("abs") || lowerType.contains("gym") || 
            lowerType.contains("lifting") || lowerType.contains("power") -> 5f
            else -> 5f // Default to moderate resistance training
        }
    }

    fun initWorkout(type: String) {
        val typeFormatted = type.replaceFirstChar { it.uppercase() }
        _uiState.update { it.copy(workoutType = typeFormatted) }
        loadWorkoutTemplate(typeFormatted)
    }

    private fun loadWorkoutTemplate(requestedType: String) {
        viewModelScope.launch {
            try {
                // Fetch template and history in parallel for performance
                val templatesResponse = RetrofitClient.apiService.getWorkoutTemplates()
                val historyResponse = RetrofitClient.apiService.getWorkoutHistory()
                
                var matched = false
                var completedExerciseIds = setOf<String>()
                var todayBurned = 0
                
                if (historyResponse.isSuccessful) {
                    val data = historyResponse.body()
                    val todayStr = java.time.LocalDate.now().toString()
                    val todayLog = data?.recentWorkouts?.firstOrNull { 
                        (it.date == todayStr || it.created_at?.startsWith(todayStr) == true) &&
                        it.workout_type.equals(requestedType, ignoreCase = true)
                    }
                    if (todayLog != null) {
                        completedExerciseIds = todayLog.exercises.filter { it.is_completed }.map { it.name }.toSet()
                        todayBurned = todayLog.calories_burned
                    }
                }
                
                if (templatesResponse.isSuccessful) {
                    val templates = templatesResponse.body() ?: emptyList()
                    val template = templates.find { it.name?.equals(requestedType, ignoreCase = true) == true }
                        ?: templates.find { it.name?.contains(requestedType, ignoreCase = true) == true }

                    if (template != null && !template.is_rest_day) {
                        matched = true
                        val exercises = template.exercises?.map {
                            ExerciseLog(
                                id = it.id.toString(),
                                name = it.name,
                                setsReps = it.sets_reps,
                                weight = it.weight,
                                isCompleted = completedExerciseIds.contains(it.name)
                            )
                        } ?: emptyList()
                        
                        _uiState.update { it.copy(
                            workoutType = template.name ?: requestedType,
                            exercises = exercises,
                            caloriesBurned = todayBurned
                        ) }
                    }
                }
                
                // Fallback: If no template matched, try today's workout
                if (!matched && (requestedType.equals("Today", ignoreCase = true) || requestedType.equals("chest", ignoreCase = true))) {
                    val todayRes = RetrofitClient.apiService.getTodayWorkout()
                    if (todayRes.isSuccessful) {
                        todayRes.body()?.let { template ->
                            if (!template.is_rest_day) {
                                val exercises = template.exercises?.map {
                                    ExerciseLog(
                                        id = it.id.toString(),
                                        name = it.name,
                                        setsReps = it.sets_reps,
                                        weight = it.weight,
                                        isCompleted = completedExerciseIds.contains(it.name)
                                    )
                                } ?: emptyList()
                                
                                _uiState.update { it.copy(
                                    workoutType = template.name ?: requestedType,
                                    exercises = exercises,
                                    caloriesBurned = todayBurned
                                ) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleExercise(id: String) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.map {
                if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
            }
            
            // Recalculate accurately based on proportion of workout completed over a standard 45 min duration
            // This offers a "realtime" feeling of calories building up as they check off exercises
            val completedCount = updatedExercises.count { it.isCompleted }
            val totalCount = updatedExercises.size
            val fractionCompleted = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            
            val metValue = getMetValueForWorkout(state.workoutType)
            val fullWorkoutDurationMins = 45f 
            val totalPossibleCalories = metValue * userWeightKg * (fullWorkoutDurationMins / 60f)
            
            val activeCaloriesBurned = (totalPossibleCalories * fractionCompleted).toInt()

            state.copy(
                exercises = updatedExercises,
                caloriesBurned = activeCaloriesBurned
            )
        }
    }

    fun startWorkout() {
        startTimeMillis = System.currentTimeMillis()
        _uiState.update { it.copy(isTrackingMode = true) }
    }

    fun updateExerciseWeight(id: String, weight: String) {
        _uiState.update { state ->
            val updated = state.exercises.map {
                if (it.id == id) it.copy(weight = weight) else it
            }
            state.copy(exercises = updated)
        }
    }

    fun updateManualWorkouts(workouts: String) {
        _uiState.update { it.copy(manualWorkouts = workouts) }
        val workoutCount = workouts.toIntOrNull() ?: 0
        
        // Use true MET calculation for manual workouts based on generic 45m average per workout session
        val metValue = getMetValueForWorkout(_uiState.value.workoutType)
        val durationH = (workoutCount * 45) / 60f
        val calculatedBurn = (metValue * userWeightKg * durationH).toInt()
        
        _uiState.update { it.copy(caloriesBurned = calculatedBurn) }
    }

    fun completeWorkout() {
        val state = _uiState.value
        if (state.exercises.isEmpty()) return

        _uiState.update { it.copy(isSaving = true, isSaved = false) }
        viewModelScope.launch {
            try {
                // If tracking was properly started, use real actual duration
                var durationMins = 45
                if (startTimeMillis > 0) {
                    val elapsedMins = ((System.currentTimeMillis() - startTimeMillis) / 60000).toInt()
                    if (elapsedMins > 0) {
                        durationMins = elapsedMins
                    }
                }
                
                val body = WorkoutLogRequest(
                    date = java.time.LocalDate.now().toString(),
                    workout_type = state.workoutType,
                    calories_burned = state.caloriesBurned,
                    duration_minutes = durationMins,
                    exercises = state.exercises.map {
                        ExerciseLogRequest(
                            name = it.name,
                            sets_reps = it.setsReps,
                            weight = it.weight,
                            is_completed = it.isCompleted
                        )
                    }
                )
                val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
                val res = RetrofitClient.apiService.logWorkout(body)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(isSaved = true) }
                }
            } catch (e: Exception) {
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveManualLogs() {
        val workouts = _uiState.value.manualWorkouts.toIntOrNull() ?: 0
        val calories = _uiState.value.caloriesBurned
        if (workouts <= 0) return

        _uiState.update { it.copy(isSaving = true, isSaved = false) }
        viewModelScope.launch {
            try {
                val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
                val body = WorkoutLogRequest(
                    date = java.time.LocalDate.now().toString(),
                    workout_type = "Manual Workout",
                    calories_burned = calories,
                    duration_minutes = workouts * 45, // Use accurate total duration
                    exercises = emptyList()
                )
                val res = RetrofitClient.apiService.logWorkout(body)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(isSaved = true, manualWorkouts = "", caloriesBurned = 0) }
                }
            } catch (e: Exception) {
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutScreen(
    workoutType: String,
    onBack: () -> Unit,
    viewModel: LogWorkoutViewModel = viewModel()
) {
    LaunchedEffect(workoutType) {
        viewModel.initWorkout(workoutType)
    }

    val uiState by viewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF22C55E)
    var infoDialogExercise by remember { mutableStateOf<ExerciseLog?>(null) }
    
    val completedCount = uiState.exercises.count { it.isCompleted }
    val totalCount = uiState.exercises.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    
    val animatedProgress by animateFloatAsState(targetValue = progress)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Log Workout", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(uiState.workoutType, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("$completedCount/$totalCount exercises completed", fontWeight = FontWeight.Bold)
                                Text("${(progress * 100).toInt()}%", color = Color.Gray, fontSize = 14.sp)
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = primaryGreen,
                                    trackColor = Color.White.copy(alpha = 0.5f),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        LoadingButton(
                            text = if (uiState.isTrackingMode) "Complete Workout" else "Start Workout",
                            isLoading = uiState.isSaving,
                            onClick = { 
                                if (uiState.isTrackingMode) viewModel.completeWorkout() 
                                else viewModel.startWorkout() 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = primaryGreen,
                            shape = RoundedCornerShape(50.dp)
                        )
                    }
                }
            }

            // Manual Logging Section Header
            item {
                Text("Manual Logging", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Workouts Completed", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = uiState.manualWorkouts,
                            onValueChange = { viewModel.updateManualWorkouts(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("e.g. 1") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryGreen,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Estimated Calories Burned: ${uiState.caloriesBurned} kcal",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LoadingButton(
                            text = if (uiState.isSaved) "Saved Successfully!" else "Save Manual Logs",
                            isLoading = uiState.isSaving,
                            onClick = { viewModel.saveManualLogs() },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = primaryGreen,
                            shape = RoundedCornerShape(50.dp),
                            enabled = uiState.manualWorkouts.isNotEmpty()
                        )
                    }
                }
            }

            // Exercises Section Header
            item {
                Text("Exercises Plan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // Exercise Cards
            items(uiState.exercises) { exercise ->
                ExerciseLogCard(
                    exercise = exercise, 
                    onToggle = { viewModel.toggleExercise(exercise.id) }, 
                    onWeightChange = { newWeight -> viewModel.updateExerciseWeight(exercise.id, newWeight) },
                    onInfoClick = { infoDialogExercise = exercise },
                    primaryGreen = primaryGreen
                )
            }
            
            // Finish Workout Button
            if (uiState.exercises.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    LoadingButton(
                        text = if (uiState.isSaved) "Workout Saved!" else "Finish & Save Workout",
                        isLoading = uiState.isSaving,
                        onClick = { viewModel.completeWorkout() },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = primaryGreen,
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Info Dialog
    infoDialogExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = { infoDialogExercise = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = primaryGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column {
                    Text("Exercise Details", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Target Sets & Reps: ${exercise.setsReps}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Logged Weight: ${if(exercise.weight.isBlank()) "None" else exercise.weight}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Maintain proper form and control throughout all repetitions to maximize muscle engagement and minimize injury risk.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogExercise = null }) {
                    Text("Got it", color = primaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ExerciseLogCard(
    exercise: ExerciseLog, 
    onToggle: () -> Unit, 
    onWeightChange: (String) -> Unit,
    onInfoClick: () -> Unit,
    primaryGreen: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (exercise.isCompleted) primaryGreen else Color.LightGray.copy(alpha = 0.3f))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (exercise.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(exercise.setsReps, fontSize = 14.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = exercise.weight,
                    onValueChange = onWeightChange,
                    modifier = Modifier.width(120.dp),
                    label = { Text("Weight", fontSize = 10.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryGreen,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
            }
            
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.Gray)
            }
        }
    }
}
