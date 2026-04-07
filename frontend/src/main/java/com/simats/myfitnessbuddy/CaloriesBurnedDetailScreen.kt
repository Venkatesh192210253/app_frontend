package com.simats.myfitnessbuddy

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.myfitnessbuddy.data.local.SettingsManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class CaloriesBurnedUiState(
    val totalBurned: Int = 0,
    val activeBurned: Int = 0,
    val burnGoal: Int = 500,
    val todayWorkouts: List<WorkoutSessionDetail> = emptyList(),
    val isLoading: Boolean = false
)

class CaloriesBurnedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CaloriesBurnedUiState())
    val uiState: StateFlow<CaloriesBurnedUiState> = _uiState.asStateFlow()

    fun loadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()

                // 1. Get workout history → filter today's sessions and sum calories
                val historyResponse = RetrofitClient.apiService.getWorkoutHistory()
                if (historyResponse.isSuccessful) {
                    val historyBody = historyResponse.body()
                    if (historyBody != null) {
                        val todaySessions = historyBody.recentWorkouts
                            .filter { it.date.take(10) == today }
                            .map { log ->
                                WorkoutSessionDetail(
                                    name = log.workout_type,
                                    calories = log.calories_burned,
                                    duration = "${log.duration_minutes} min",
                                    exercisesCount = "${log.exercises.size} exercises",
                                    totalVolume = "N/A",
                                    date = log.date
                                )
                            }
                        // Active burned = sum of today's workout calories
                        val activeBurned = todaySessions.sumOf { it.calories }
                        _uiState.update { it.copy(
                            todayWorkouts = todaySessions,
                            activeBurned = activeBurned
                        )}
                    }
                }

                // 2. Get diary summary → resting BMR and total burned
                val diaryResponse = RetrofitClient.apiService.getDiaryDaily(today)
                if (diaryResponse.isSuccessful) {
                    val body = diaryResponse.body()
                    if (body != null) {
                        // Resting BMR: ~60% of daily food calorie goal
                        val restingBmr = ((body.summary.goal * 0.6).toInt()).coerceAtLeast(1000)
                        val current = _uiState.value
                        _uiState.update { it.copy(
                            totalBurned = current.activeBurned + restingBmr,
                            burnGoal = 500  // Active burn target: 500 kcal
                        )}
                    }
                }
            } catch (e: Exception) {
                Log.e("CaloriesBurnedVM", "Error loading data", e)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloriesBurnedDetailScreen(
    onBack: () -> Unit,
    appPadding: PaddingValues,
    viewModel: CaloriesBurnedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadData() }

    var animationPlayed by remember { mutableStateOf(false) }
    val progress = if (uiState.burnGoal > 0)
        (uiState.activeBurned.toFloat() / uiState.burnGoal.toFloat()).coerceIn(0f, 1f)
    else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "burnProgress"
    )
    val animatedTotal by animateIntAsState(
        targetValue = if (animationPlayed) uiState.totalBurned else 0,
        animationSpec = tween(1500),
        label = "burnTotal"
    )

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.totalBurned > 0) animationPlayed = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calories Burned", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF59E0B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = appPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Summary Card with circular progress
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Burned Today", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(180.dp)) {
                                drawArc(
                                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.2f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 14.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawArc(
                                    color = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedProgress,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 14.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$animatedTotal",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1F2937)
                                )
                                Text("kcal", fontSize = 14.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${(animatedProgress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CalorieSplitItem("Active", "${uiState.activeBurned}", Color(0xFFF59E0B))
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                            CalorieSplitItem("Resting", "${uiState.totalBurned - uiState.activeBurned}", Color(0xFF3B82F6))
                        }
                    }
                }
            }

            // Today's Workouts Header
            item {
                Text("Today's Activity Log", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1F2937))
            }

            if (uiState.todayWorkouts.isEmpty() && !uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No workouts logged today", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            items(uiState.todayWorkouts) { workout ->
                BurnActivityCard(workout)
            }
        }
    }
}

@Composable
fun CalorieSplitItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun BurnActivityCard(workout: WorkoutSessionDetail) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF59E0B))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(workout.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Text(workout.duration, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${workout.calories} kcal burned • ${workout.exercisesCount}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
