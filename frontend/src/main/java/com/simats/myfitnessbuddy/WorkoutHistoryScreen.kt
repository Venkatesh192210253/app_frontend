package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutHistoryUiState(
    val monthWorkouts: Int = 0,
    val monthMinutes: Int = 0,
    val monthCalories: Int = 0,
    val recentWorkouts: List<WorkoutSessionDetail> = listOf(),
    val filteredWorkouts: List<WorkoutSessionDetail> = listOf(),
    val selectedFilter: String = "All",
    val availableFilters: List<String> = listOf("All"),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class WorkoutSessionDetail(
    val name: String,
    val calories: Int,
    val duration: String,
    val exercisesCount: String,
    val totalVolume: String,
    val date: String
)

class WorkoutHistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    init {
        fetchHistory()
    }

    private fun fetchHistory() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.getWorkoutHistory()
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        val mappedSessions = data.recentWorkouts.map { log ->
                            WorkoutSessionDetail(
                                name = log.workout_type,
                                calories = log.calories_burned,
                                duration = "${log.duration_minutes} min",
                                exercisesCount = "${log.exercises.size} exercises",
                                totalVolume = "N/A", // Optional calc if weight is numeric
                                date = log.date
                            )
                        }
                        val filters = listOf("All") + mappedSessions.map { it.name }.distinct()
                        _uiState.update { it.copy(
                            monthWorkouts = data.summary.monthWorkouts,
                            monthMinutes = data.summary.monthMinutes,
                            monthCalories = data.summary.monthCalories,
                            recentWorkouts = mappedSessions,
                            filteredWorkouts = mappedSessions,
                            availableFilters = filters,
                            isLoading = false
                        ) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load history") }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { state ->
            val filtered = if (filter == "All") {
                state.recentWorkouts
            } else {
                state.recentWorkouts.filter { it.name == filter }
            }
            state.copy(selectedFilter = filter, filteredWorkouts = filtered)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF22C55E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Workout History", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Last 30 days", fontSize = 12.sp, color = Color.Gray)
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
            // This Month Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HistoryStatItem(uiState.monthWorkouts.toString(), "Workouts", Modifier.weight(1f))
                        HistoryStatItem(uiState.monthMinutes.toString(), "Minutes", Modifier.weight(1f))
                        HistoryStatItem(uiState.monthCalories.toString(), "Calories", Modifier.weight(1f))
                    }
                }
            }

            // Recent Workouts Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Workouts", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = if (uiState.selectedFilter != "All") primaryGreen else Color.Gray)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            uiState.availableFilters.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter) },
                                    onClick = {
                                        viewModel.setFilter(filter)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Workout Cards
            items(uiState.filteredWorkouts) { workout ->
                WorkoutHistoryCard(workout)
            }

            // Weekly Trend Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "You’re up 25% from last week! Keep pushing 💪",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryStatItem(value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF121418))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun WorkoutHistoryCard(workout: WorkoutSessionDetail) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(workout.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        "Completed",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color(0xFF22C55E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                workout.date,
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "${workout.calories} kcal",
                color = Color(0xFF22C55E),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WorkoutDetailItem(workout.exercisesCount, "exercises")
                WorkoutDetailItem(workout.duration, "duration")
                WorkoutDetailItem(workout.totalVolume, "total volume")
            }
        }
    }
}

@Composable
fun WorkoutDetailItem(value: String, label: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}
