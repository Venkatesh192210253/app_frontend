package com.simats.myfitnessbuddy

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import com.simats.myfitnessbuddy.data.remote.DashboardDataResponse
import com.simats.myfitnessbuddy.data.remote.FirebaseStatsManager
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.AdaptiveAiManager
import com.simats.myfitnessbuddy.AiSuggestion
import kotlinx.coroutines.async

data class StepStats(
    val steps: Int = 2961,
    val goal: Int = 10000
)
data class DiscoverItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)
class DashboardViewModel : ViewModel() {
    // Step Stats
    private val _stepStats = mutableStateOf(StepStats(
        steps = com.simats.myfitnessbuddy.data.local.SettingsManager.totalStepsToday,
        goal = com.simats.myfitnessbuddy.data.local.SettingsManager.stepGoal
    ))
    val stepStats: androidx.compose.runtime.State<StepStats> = _stepStats
    fun calculateDistance(steps: Int): Double = steps * 0.000762 // 1 step approx 0.762m
    fun calculateCalories(steps: Int): Double = steps * 0.04
    // Progress Stats
    val consumedCalories = mutableStateOf(1850)
    val calorieGoal = mutableStateOf(com.simats.myfitnessbuddy.data.local.SettingsManager.calorieGoal.toIntOrNull() ?: 2200)
    val currentWeight = mutableStateOf(com.simats.myfitnessbuddy.data.local.SettingsManager.currentWeight.toFloatOrNull() ?: 0f)
    val startWeight = mutableStateOf(com.simats.myfitnessbuddy.data.local.SettingsManager.startWeight.toFloatOrNull() ?: 0f)
    val targetWeight = mutableStateOf(com.simats.myfitnessbuddy.data.local.SettingsManager.targetWeight.toFloatOrNull() ?: 0f)
    val fatPercentage = mutableStateOf(18f)
    private val _dashboardData = MutableStateFlow<DashboardDataResponse?>(null)
    val dashboardData: StateFlow<DashboardDataResponse?> = _dashboardData.asStateFlow()
    // Stats Cards
    val caloriesBurned = mutableStateOf(450)
    val burnedVsLastWeek = mutableStateOf(12) // Percentage increase
    // Weekly Macro Data
    val weeklyData = androidx.compose.runtime.mutableStateListOf<com.simats.myfitnessbuddy.data.remote.WeeklyMacroResponse>()
    val selectedDayIndex = mutableStateOf(6)
    val discoverItems = listOf(
        DiscoverItem("Workouts", Icons.Default.FitnessCenter, Color(0xFFFFE0B2)),
        DiscoverItem("Diet Plans", Icons.Default.Restaurant, Color(0xFFC8E6C9)),
        DiscoverItem("Friends", Icons.Default.People, Color(0xFFBBDEFB)),
        DiscoverItem("Challenges", Icons.Default.EmojiEvents, Color(0xFFF8BBD0)),
        DiscoverItem("Community", Icons.Default.Groups, Color(0xFFD1C4E9)),
        DiscoverItem("Sync", Icons.Default.Sync, Color(0xFFB2EBF2))
    )
    // Gamification Stats
    val currentStreak = mutableStateOf(0)
    val totalWorkouts = mutableStateOf(0)
    val todayWorkouts = mutableStateOf(0)
    val userXp = mutableStateOf(0)
    val userLevel = mutableStateOf(1)
    private var pollingJob: kotlinx.coroutines.Job? = null
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                refreshSettings()
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
    fun refreshSettings() {
        calorieGoal.value = com.simats.myfitnessbuddy.data.local.SettingsManager.calorieGoal.toIntOrNull() ?: 2200
        currentWeight.value = com.simats.myfitnessbuddy.data.local.SettingsManager.currentWeight.toFloatOrNull() ?: 72f
        startWeight.value = com.simats.myfitnessbuddy.data.local.SettingsManager.startWeight.toFloatOrNull() ?: 75f
        targetWeight.value = com.simats.myfitnessbuddy.data.local.SettingsManager.targetWeight.toFloatOrNull() ?: 70f
        // Refresh steps from local storage (updated by service)
        val localSteps = com.simats.myfitnessbuddy.data.local.SettingsManager.totalStepsToday
        val goal = com.simats.myfitnessbuddy.data.local.SettingsManager.stepGoal
        _stepStats.value = StepStats(steps = localSteps, goal = goal)
        // Sync to backend if significantly different? Or just push
        syncStepsToBackend(localSteps)
    }
    private var lastSyncedSteps = -1
    private var lastSyncTime = 0L
    private fun syncStepsToBackend(steps: Int) {
        val currentTime = System.currentTimeMillis()
        // Only sync if steps have changed
        if (steps == lastSyncedSteps) return
        if (steps > 0 && steps < lastSyncedSteps) return // Safety check, don't sync if local count somehow decreased
        lastSyncedSteps = steps
        lastSyncTime = currentTime
        viewModelScope.launch {
            try {
                // 1. Sync to Django Backend (Permanent storage)
                com.simats.myfitnessbuddy.data.remote.RetrofitClient.apiService.updateDailyStats(
                    mapOf("steps" to steps)
                )
                Log.d("DashboardVM", "Synchronized $steps steps to backend")

                // 2. Sync to Firebase (Real-time friend comparison)
                FirebaseStatsManager.updateMyStats(
                    steps = steps,
                    workouts = todayWorkouts.value,
                    xp = userXp.value,
                    streak = currentStreak.value,
                    level = userLevel.value
                )
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed to sync steps: ${e.message}")
            }
        }
    }
    fun loadDashboardData() {
        startPolling() // Start polling when we load data
        viewModelScope.launch {
            try {
                val token = SettingsManager.authToken ?: return@launch
                val response = RetrofitClient.apiService.getDashboardData()
                if (response.isSuccessful) {
                    val dashData = response.body()
                    if (dashData != null) {
                        // 1. Daily Stats with improved local-first logic
                        val stats = dashData.daily_stats
                        val localSteps = com.simats.myfitnessbuddy.data.local.SettingsManager.totalStepsToday
                        // Only let the server overwrite local steps if local is 0 (new install/day)
                        // or if the server is significantly ahead (e.g. sync from another device)
                        val finalSteps = if (localSteps == 0 || stats.steps > localSteps + 500) {
                            Log.d("DashboardVM", "Using server steps: ${stats.steps} (Local was $localSteps)")
                            stats.steps
                        } else {
                            // Otherwise trust the local phone sensor as the primary source of truth
                            localSteps
                        }
                        _stepStats.value = StepStats(steps = finalSteps, goal = dashData.goal_settings.daily_step_goal)
                        com.simats.myfitnessbuddy.data.local.SettingsManager.totalStepsToday = finalSteps
                        com.simats.myfitnessbuddy.data.local.SettingsManager.stepGoal = dashData.goal_settings.daily_step_goal
                        consumedCalories.value = stats.calories_consumed
                        caloriesBurned.value = stats.calories_burned
                        todayWorkouts.value = stats.workouts_completed
                        // 2. Goal Settings
                        calorieGoal.value = dashData.goal_settings.daily_calorie_target
                        currentWeight.value = dashData.goal_settings.current_weight.toFloat()
                        // Sync SettingsManager for safety
                        SettingsManager.calorieGoal = dashData.goal_settings.daily_calorie_target.toString()
                        SettingsManager.currentWeight = dashData.goal_settings.current_weight.toString()
                        SettingsManager.targetWeight = dashData.goal_settings.target_weight.toString()
                        dashData.weight_goal?.let { wg ->
                            wg.start_weight?.let { startWeight.value = it }
                            wg.target_weight?.let { targetWeight.value = it }
                            SettingsManager.startWeight = (wg.start_weight ?: startWeight.value).toString()
                            SettingsManager.targetWeight = (wg.target_weight ?: targetWeight.value).toString()
                        }
                        SettingsManager.isAdaptiveModeEnabled = dashData.goal_settings.is_adaptive_mode_enabled
                        SettingsManager.quietHoursFrom = dashData.goal_settings.quiet_hours_from
                        SettingsManager.quietHoursTo = dashData.goal_settings.quiet_hours_to
                        // 3. Weekly History (Macros)
                        weeklyData.clear()
                        dashData.weekly_macros?.forEach { m ->
                            weeklyData.add(m)
                        }
                        val todayStr = java.time.LocalDate.now().toString()
                        val todayIdx = weeklyData.indexOfFirst { it.date == todayStr }
                        selectedDayIndex.value = if (todayIdx != -1) todayIdx else (if (weeklyData.isNotEmpty()) weeklyData.size - 1 else 0)
                        // 4. Update state for observers (like AI sync)
                        _dashboardData.value = dashData
                        // 5. Fetch Profile for real gamification stats and sync to Firebase
                        viewModelScope.launch {
                            try {
                                val profileResponse = RetrofitClient.apiService.getProfile()
                                if (profileResponse.isSuccessful) {
                                    profileResponse.body()?.let { profile ->
                                        val p = profile.profile
                                        currentStreak.value = p?.streak ?: 0
                                        totalWorkouts.value = p?.workouts_completed ?: 0
                                        userXp.value = p?.xp ?: 0
                                        userLevel.value = p?.level ?: 1
                                        
                                        // Persist for background service access
                                        SettingsManager.userStreak = p?.streak ?: 0
                                        SettingsManager.workoutsCompleted = p?.workouts_completed ?: 0
                                        SettingsManager.userXp = p?.xp ?: 0
                                        SettingsManager.userLevel = p?.level ?: 1

                                        // Update Firebase with ACTUAL user data from profile
                                        FirebaseStatsManager.updateMyStats(
                                            steps = finalSteps,
                                            workouts = todayWorkouts.value,
                                            xp = p?.xp ?: 0,
                                            streak = p?.streak ?: 0,
                                            level = p?.level ?: 1
                                        )
                                    }
                                } else {
                                    // Fallback sync if profile fails
                                    Log.w("DashboardVM", "Profile fetch failed for sync, using placeholders")
                                    FirebaseStatsManager.updateMyStats(
                                        steps = finalSteps,
                                        workouts = todayWorkouts.value,
                                        xp = 100,
                                        streak = stats.water_ml / 250 // Old placeholder as last resort
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("DashboardVM", "Error in background profile sync: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading dashboard data", e)
            }
        }
    }
}
// --- Main Dashboard Screen ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNutrition: () -> Unit = {},
    onNavigateToDiary: () -> Unit = {},
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToDailyCalories: () -> Unit = {},
    onNavigateToStepDetails: () -> Unit = {},
    onNavigateToWeightTracker: () -> Unit = {},
    onNavigateToCaloriesDetails: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    fAiVM: AdaptiveAiManager = viewModel()
) {
    val nVM: NotificationsViewModel = viewModel()
    val notifications by nVM.notifications.collectAsState()
    val hasUnread = notifications.any { !it.isRead }
    val isAdaptiveEnabled by fAiVM.isAdaptiveModeEnabled.collectAsState()
    val suggestion = fAiVM.currentSuggestion.value
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showQuickAdd by remember { mutableStateOf(false) }
    // Pager state for Steps/Weight cards
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val lifecycleOwner = LocalLifecycleOwner.current
    // Refresh data when screen is resumed (e.g., coming back from tracker)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSettings()
                viewModel.loadDashboardData()
                nVM.loadNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(viewModel.dashboardData) {
        viewModel.dashboardData.collect { dashData ->
            dashData?.let { data ->
                fAiVM.syncFullData(data)
            }
        }
    }
    Scaffold(
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Header Section
                item { DashboardHeader(onNavigateToNotifications, hasUnread, viewModel.currentStreak.value) }
                // 2. Progress Metrics (Steps & Weight) - MOVED UP
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StepsProgressMeterCard(viewModel, onNavigateToStepDetails)
                        WeightLogOptionCard(onNavigateToWeightTracker)
                    }
                }
                // 3. AI Adaptive Suggestion Card (Conditional)
                if (isAdaptiveEnabled && suggestion != null) {
                    item {
                        AIAdjustmentCard(
                            suggestion = suggestion,
                            onAccept = { 
                                fAiVM.acceptSuggestion {
                                    viewModel.refreshSettings()
                                }
                            },
                            onDismiss = { fAiVM.dismissSuggestion() }
                        )
                    }
                }
                // 3. Recovery & Readiness Score (New AI Feature)
                item { RecoveryReadinessCard(fAiVM) }
                // 3.5 Injury Risk Alert (Conditional Phase 4)
                val riskMessage = fAiVM.getInjuryRiskMessage()
                if (riskMessage != null) {
                    item { InjuryRiskAlert(riskMessage) }
                }
                // 3.6 Energy Balance Index (New AI Feature)
                item { EnergyBalanceCard(fAiVM) }
                // 3.7 Predictive Goal Completion (New AI Feature)
                item { PredictiveGoalCard(fAiVM) }
                // 3. Stats Cards Row
                item { StatsCardsRow(viewModel, onNavigateToDailyCalories, onNavigateToCaloriesDetails) }
                // 4. Weekly Chart Card
                item { WeeklyChartCard(viewModel, onNavigateToNutrition) }
                // 5. AI Insight Card
                item { AIInsightCard() }
                // 5.5 behavior Insights AI Dashboard (Phase 4)
                item { BehaviorInsightAISection(fAiVM) }
                // 7. Floating Action Section (Above bottom nav)
                item { BottomActionSection(onNavigateToDiary, onNavigateToWorkout, onNavigateToScan) }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
            if (showQuickAdd) {
                QuickAddBottomSheet(
                    onDismiss = { showQuickAdd = false },
                    sheetState = sheetState,
                    onActionClick = { action ->
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showQuickAdd = false
                                if (action == "Log Food") {
                                    onNavigateToDiary()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
// --- Component Implementations ---
@Composable
fun AIAdjustmentCard(
    suggestion: AiSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)) // Dark blue/slate
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Adaptive Suggestion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = suggestion.message,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Adjustment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Ignore", fontSize = 13.sp)
                }
            }
        }
    }
}
@Composable
fun WeeklyChartCard(viewModel: DashboardViewModel, onNavigateToNutrition: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val selectedIndex = viewModel.selectedDayIndex.value
            val selectedData = viewModel.weeklyData.getOrNull(selectedIndex)
            val todayStr = java.time.LocalDate.now().toString()
            val isToday = selectedData?.date == todayStr
            val titleText = if (isToday) "Today" else selectedData?.day_name ?: "This Week"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titleText, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Extract the 4 meal points for the selected day
            val proteinPoints = mutableListOf<Float>()
            val carbsPoints = mutableListOf<Float>()
            val fatsPoints = mutableListOf<Float>()
            if (selectedData != null) {
                val m = selectedData.meals
                proteinPoints.addAll(listOf(m.breakfast.protein, m.lunch.protein, m.dinner.protein, m.snacks.protein))
                carbsPoints.addAll(listOf(m.breakfast.carbs, m.lunch.carbs, m.dinner.carbs, m.snacks.carbs))
                fatsPoints.addAll(listOf(m.breakfast.fats, m.lunch.fats, m.dinner.fats, m.snacks.fats))
            }
            // Multi-Line Chart using Canvas
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                WeeklyLineChart(proteinPoints, carbsPoints, fatsPoints)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Macro Breakdown Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val pTotal = selectedData?.protein?.toInt() ?: 0
                val cTotal = selectedData?.carbs?.toInt() ?: 0
                val fTotal = selectedData?.fats?.toInt() ?: 0
                MacroLegendItem("Protein", "${pTotal}g", Color(0xFFEF4444))
                MacroLegendItem("Carbs", "${cTotal}g", Color(0xFF3B82F6))
                MacroLegendItem("Fats", "${fTotal}g", Color(0xFFF59E0B))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                viewModel.weeklyData.forEachIndexed { index, dayData ->
                    val isSelected = index == selectedIndex
                    Text(
                        text = dayData.day_name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF3B82F6) else Color.Gray, // Highlight selected text
                        modifier = Modifier
                            .clickable { viewModel.selectedDayIndex.value = index }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun MacroLegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            if (value.isNotEmpty()) {
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
        }
    }
}
@Composable
fun WeeklyLineChart(protein: List<Float>, carbs: List<Float>, fats: List<Float>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
        label = "chart"
    )
    LaunchedEffect(Unit) { animationPlayed = true }
    Box(modifier = Modifier.fillMaxSize()) {
        val maxP = protein.maxOrNull() ?: 0f
        val maxC = carbs.maxOrNull() ?: 0f
        val maxF = fats.maxOrNull() ?: 0f
        var absoluteMax = maxOf(maxP, maxC, maxF)
        if (absoluteMax < 50f) absoluteMax = 50f
        val maxVal = ((absoluteMax / 50).toInt() + 1) * 50f
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            val steps = 5
            for (i in 0..steps) {
                val labelVal = (maxVal - (maxVal / steps) * i).toInt()
                Text(text = labelVal.toString(), fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.width(35.dp))
            }
        }
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 40.dp)) {
            val width = size.width
            val height = size.height
            val count = maxOf(protein.size, 1)
            val spacing = if (count > 1) width / (count - 1) else width
            for (i in 0..5) {
                val y = height - (i * height / 5)
                drawLine(color = Color.LightGray.copy(alpha = 0.6f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 1.dp.toPx())
            }
            fun drawMacroLine(data: List<Float>, color: Color) {
                if (data.isEmpty()) return
                val path = androidx.compose.ui.graphics.Path()
                data.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = height - ((value / maxVal) * height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = color.copy(alpha = animatedProgress), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                data.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = height - ((value / maxVal) * height)
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                    drawCircle(color = color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                }
            }
            drawMacroLine(protein, Color(0xFFEF4444))
            drawMacroLine(carbs, Color(0xFF3B82F6))
            drawMacroLine(fats, Color(0xFFF59E0B))
        }
    }
}
@Composable
fun InjuryRiskAlert(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)) // Light red
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(message, fontSize = 14.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun EnergyBalanceCard(viewModel: AdaptiveAiManager) {
    val score = viewModel.getEnergyBalanceScore()
    val status = viewModel.getEnergyBalanceStatus()
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000),
        label = "EnergyBalanceProgress"
    )
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Energy Balance Index", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$score", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.width(8.dp))
                    PillBadge(status, Color(0xFFDCFCE7), Color(0xFF166534))
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF22C55E),
                    strokeWidth = 6.dp,
                    trackColor = Color.LightGray.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
@Composable
fun PredictiveGoalCard(viewModel: AdaptiveAiManager) {
    val prediction = viewModel.prediction.value ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)) // Deep dark navy
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Goal Prediction", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("At your current rate, you'll reach your goal weight by:", color = Color.LightGray.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(prediction.targetDate, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingDown, 
                    contentDescription = null, 
                    tint = Color(0xFF22C55E), 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(prediction.trend, color = Color(0xFF22C55E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = prediction.confidence / 100f, 
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), 
                color = Color(0xFF22C55E), 
                trackColor = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Prediction Confidence: ${prediction.confidence}%", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
@Composable
fun StepsProgressMeterCard(viewModel: DashboardViewModel, onClick: () -> Unit) {
    val stats = viewModel.stepStats.value
    val distance = viewModel.calculateDistance(stats.steps)
    val calories = viewModel.calculateCalories(stats.steps)
    var animationPlayed by remember { mutableStateOf(false) }
    val progress = stats.steps.toFloat() / stats.goal.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = if (animationPlayed) progress else 0f, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "stepsProgress")
    val animatedSteps by animateIntAsState(targetValue = if (animationPlayed) stats.steps else 0, animationSpec = tween(1500), label = "stepsCount")
    LaunchedEffect(Unit) { animationPlayed = true }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text("Steps", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    drawArc(Color.LightGray.copy(alpha = 0.2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(Color(0xFF22C55E), -90f, 360f * animatedProgress, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$animatedSteps", fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, color = Color.DarkGray)
                    Text("steps", fontSize = 16.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format("%.2f km", distance), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray.copy(alpha = 0.4f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format("%.0f kcal", calories), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
@Composable
fun WeightLogOptionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF22C55E).copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Weight Tracker", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                    Text("Log and track weight progress", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
@Composable
fun StatsCardsRow(viewModel: DashboardViewModel, onNavigateToDailyCalories: () -> Unit, onNavigateToCaloriesDetails: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard("Today's Goal", "${viewModel.calorieGoal.value}", "kcal target", null, Icons.Default.TrackChanges, Modifier.weight(1f).fillMaxHeight().clickable { onNavigateToDailyCalories() })
        StatCard("Calories Burned", "${viewModel.caloriesBurned.value}", "workout + BMR", "↑${viewModel.burnedVsLastWeek.value}% vs last week", Icons.Default.Whatshot, Modifier.weight(1f).fillMaxHeight().clickable { onNavigateToCaloriesDetails() })
    }
}
@Composable
fun StatCard(title: String, value: String, subtitle: String, trend: String? = null, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            if (trend != null) Text(trend, fontSize = 11.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
@Composable
fun RecoveryReadinessCard(viewModel: AdaptiveAiManager) {
    val score = viewModel.recoveryScore.value
    val status = viewModel.getRecoveryStatus()
    val recommendation = viewModel.getRecoveryMessage()
    val workout = viewModel.getRecommendedWorkout()
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000),
        label = "RecoveryProgress"
    )
    val statusColor = when (status) {
        "High Readiness" -> Color(0xFF22C55E)
        "Moderate" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Recovery & Readiness", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(90.dp),
                        color = statusColor,
                        strokeWidth = 10.dp,
                        trackColor = statusColor.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                    Text("$score%", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(status, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = statusColor)
                    Text(recommendation, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recommended Today: ", fontSize = 13.sp, color = Color.Gray)
                Text(workout, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
            }
        }
    }
}
@Composable
fun BehaviorInsightAISection(viewModel: AdaptiveAiManager) {
    Column {
        Text(
            "AI behavioral Insights",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        viewModel.behaviorInsights.forEach { insight ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(insight, fontSize = 14.sp, color = Color.DarkGray)
                }
            }
        }
    }
}
@Composable
fun AIInsightCard() {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = Color(0xFFBBF7D0),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text("AI Insight", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF166534))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "You're burning 15% more calories this week! Keep up the great work. Consider increasing protein intake by 20g daily.",
                    fontSize = 13.sp,
                    color = Color(0xFF166534).copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
@Composable
fun BottomActionSection(
    onNavigateToDiary: () -> Unit, 
    onNavigateToWorkout: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashboardActionCard(
            title = "Add Meal",
            subtitle = "Log your food",
            icon = Icons.Default.Restaurant,
            color = Color(0xFFBBF7D0),
            modifier = Modifier.weight(1f).clickable { onNavigateToDiary() },
            onScanClick = onNavigateToScan
        )
        DashboardActionCard(
            title = "Log Workout",
            subtitle = "Track exercise",
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFFDBEAFE),
            modifier = Modifier.weight(1f).clickable { onNavigateToWorkout() }
        )
    }
}
@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onScanClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.DarkGray.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            if (onScanClick != null) {
                IconButton(
                    onClick = onScanClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F6FA))
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt, 
                        contentDescription = "Scan Meal", 
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun DashboardHeader(
    onNavigateToNotifications: () -> Unit,
    hasUnread: Boolean = false,
    streak: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Hey, ${com.simats.myfitnessbuddy.data.local.SettingsManager.userName}! \uD83D\uDC4B",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Text(
                "Let's crush today's goals",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Streak Badge
            if (streak > 0) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment, 
                            contentDescription = null, 
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$streak", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 13.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }
            Box(
            modifier = Modifier
                .size(45.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onNavigateToNotifications() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.DarkGray)
            // Notification Dot (Real-time & Animated)
            if (hasUnread) {
                val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                        .size(12.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = alpha))
                        .shadow(2.dp, CircleShape)
                )
            }
        }
    }
}
@Composable
fun PillBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
}
