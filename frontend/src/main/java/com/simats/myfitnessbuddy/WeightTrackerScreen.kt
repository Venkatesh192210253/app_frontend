package com.simats.myfitnessbuddy

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

import com.simats.myfitnessbuddy.data.remote.WeightGoalRequest
import com.simats.myfitnessbuddy.data.remote.WeightLogRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.simats.myfitnessbuddy.data.local.SettingsManager
import java.time.LocalDate

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class WeightEntry(val date: String, val weight: Float)

data class WeightTrackerUiState(
    val currentWeight: Float? = null,
    val startWeight: Float? = null,
    val targetWeight: Float? = null,
    val weeklyGoalWeight: Float = 0.5f,
    val history: List<WeightEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedToday: Boolean = false
)

class WeightTrackerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WeightTrackerUiState())
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()

    fun loadAll() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // Load goal
                val goalResp = RetrofitClient.apiService.getWeightGoal()
                if (goalResp.isSuccessful) {
                    goalResp.body()?.let { dto ->
                        _uiState.update { it.copy(
                            startWeight = dto.start_weight, 
                            targetWeight = dto.target_weight,
                            weeklyGoalWeight = dto.weekly_goal_weight
                        ) }
                    }
                }
                // Load history
                val historyResp = RetrofitClient.apiService.getWeightHistory()
                if (historyResp.isSuccessful) {
                    val entries = historyResp.body()?.map { WeightEntry(it.date, it.weight) } ?: emptyList()
                    // Sort entries by date to ensure last is truly current
                    val sortedEntries = entries.sortedBy { it.date }
                    val current = sortedEntries.lastOrNull()?.weight 
                        ?: SettingsManager.currentWeight.toFloatOrNull() 
                        ?: _uiState.value.startWeight
                    val today = LocalDate.now().toString()
                    _uiState.update { it.copy(
                        history = sortedEntries,
                        currentWeight = current,
                        savedToday = sortedEntries.any { e -> e.date == today }
                    )}
                }
            } catch (e: Exception) {
                Log.e("WeightTrackerVM", "Error loading", e)
                _uiState.update { it.copy(error = "Failed to load data") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun logWeight(weight: Float) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // Sync locally first for better UX
                SettingsManager.currentWeight = weight.toString()
                
                val today = LocalDate.now().toString()
                val resp = RetrofitClient.apiService.logWeight(WeightLogRequest(today, weight))
                if (resp.isSuccessful) {
                    loadAll()
                } else {
                    _uiState.update { it.copy(error = "Failed to save weight", isSaving = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Network error: ${e.localizedMessage}", isSaving = false) }
            }
        }
    }

    fun setGoal(start: Float, target: Float, weekly: Float = 0.5f) {
        viewModelScope.launch {
            try {
                // Sync locally
                SettingsManager.startWeight = start.toString()
                SettingsManager.targetWeight = target.toString()
                
                val resp = RetrofitClient.apiService.setWeightGoal(WeightGoalRequest(start, target, weekly))
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(startWeight = start, targetWeight = target, weeklyGoalWeight = weekly) }
                }
            } catch (e: Exception) {
                Log.e("WeightTrackerVM", "Set goal error", e)
            }
        }
    }

    fun calculateWeeksToGoal(): Float {
        val current = _uiState.value.currentWeight ?: 0f
        val target = _uiState.value.targetWeight ?: 0f
        val weekly = _uiState.value.weeklyGoalWeight
        
        if (weekly <= 0.05f || target == 0f || current == 0f) return 0f
        val isBulking = target > (_uiState.value.startWeight ?: current)
        
        // If we are already past the goal or at goal
        if (isBulking && current >= target) return 0f
        if (!isBulking && current <= target) return 0f
        
        val diff = kotlin.math.abs(target - current)
        val weeks = diff / weekly
        return if (weeks < 0.1f) 0f else weeks
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen(
    onBack: () -> Unit,
    appPadding: PaddingValues,
    viewModel: WeightTrackerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadAll() }

    var showGoalDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }

    // Progress: how far from start → target (Dynamic calculation as requested)
    val progress: Float = remember(uiState.startWeight, uiState.targetWeight, uiState.currentWeight) {
        val start = uiState.startWeight ?: SettingsManager.startWeight.toFloatOrNull() ?: 0f
        val target = uiState.targetWeight ?: SettingsManager.targetWeight.toFloatOrNull() ?: 0f
        val current = uiState.currentWeight ?: SettingsManager.currentWeight.toFloatOrNull() ?: 0f
        
        if (start != 0f && target != 0f && current != 0f) {
            if (java.lang.Math.abs(target - start) < 0.1f) {
                1f // Maintenance goal is always 100%
            } else {
                val isGaining = target > start
                val p = if (isGaining) {
                    (current - start) / (target - start)
                } else {
                    (start - current) / (start - target)
                }
                // We add a tiny 0.01 sliver minimum so the green color is visible at start
                p.coerceIn(0.01f, 1f)
            }
        } else 0.01f // Baseline sliver even if data is loading
    }

    var animPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animPlayed = true }
    
    val animProgress by animateFloatAsState(
        targetValue = if (animPlayed) progress else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "weightProgress"
    )
    
    val animWeight by animateFloatAsState(
        targetValue = if (animPlayed) (uiState.currentWeight ?: 0f) else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "weightValue"
    )

    if (showGoalDialog) {
        GoalSetupDialog(
            initialStart = uiState.startWeight?.toString() ?: "",
            initialTarget = uiState.targetWeight?.toString() ?: "",
            initialWeekly = uiState.weeklyGoalWeight,
            onDismiss = { showGoalDialog = false },
            onConfirm = { start, target, weekly ->
                viewModel.setGoal(start, target, weekly)
                showGoalDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Progress", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF3B82F6))
                    }
                },
                actions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Set Goal", tint = Color(0xFF3B82F6))
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
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = appPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circular Progress Card
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
                        Text("Weight Progress", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(16.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                            Canvas(modifier = Modifier.size(190.dp)) {
                                drawArc(
                                    color = Color(0xFF22C55E).copy(alpha = 0.1f),
                                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFF22C55E),
                                    startAngle = -90f, sweepAngle = 360f * animProgress, useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (animWeight > 0.1f) "%.1f".format(animWeight) else "--",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.DarkGray
                                )
                                Text("kg current", fontSize = 14.sp, color = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${(animProgress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            WeightStatItem("Start", uiState.startWeight?.let { "%.1f kg".format(it) } ?: "--", Color(0xFF6B7280))
                            Box(Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                            WeightStatItem("Current", uiState.currentWeight?.let { "%.1f kg".format(it) } ?: "--", Color(0xFF3B82F6))
                            Box(Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                            WeightStatItem("Target", uiState.targetWeight?.let { "%.1f kg".format(it) } ?: "--", Color(0xFF22C55E))
                        }
                    }
                }
            }

            // ── Set Your Weight Goal Card ──────────────────────────────────
            item {
                var startInput by remember { mutableStateOf(uiState.startWeight?.let { "%.1f".format(it) } ?: "") }
                var targetInput by remember { mutableStateOf(uiState.targetWeight?.let { "%.1f".format(it) } ?: "") }
                var editingGoal by remember { mutableStateOf(uiState.startWeight == null) }

                LaunchedEffect(uiState.startWeight, uiState.targetWeight) {
                    if (!editingGoal) {
                        startInput = uiState.startWeight?.let { "%.1f".format(it) } ?: ""
                        targetInput = uiState.targetWeight?.let { "%.1f".format(it) } ?: ""
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isBulking = (uiState.targetWeight ?: 0f) > (uiState.startWeight ?: 0f)
                            Text(if (isBulking) "Bulking Goal" else "Weight Goal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { editingGoal = !editingGoal }) {
                                Text(if (editingGoal) "Cancel" else "Edit", color = Color(0xFF3B82F6))
                            }
                        }
                        
                        if (editingGoal) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = startInput,
                                        onValueChange = { startInput = it },
                                        label = { Text("Start (kg)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = targetInput,
                                        onValueChange = { targetInput = it },
                                        label = { Text("Target (kg)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val s = startInput.toFloatOrNull()
                                        val t = targetInput.toFloatOrNull()
                                        if (s != null && t != null) {
                                            viewModel.setGoal(s, t, uiState.weeklyGoalWeight)
                                            editingGoal = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Update Range") }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("%.1f kg".format(uiState.startWeight ?: 0f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                    Text("Start", fontSize = 12.sp, color = Color.Gray)
                                }
                                val isBulking = (uiState.targetWeight ?: 0f) > (uiState.startWeight ?: 0f)
                                Icon(
                                    if (isBulking) Icons.Default.TrendingUp else Icons.Default.TrendingDown, 
                                    null, 
                                    tint = if (isBulking) Color(0xFF2563EB) else Color(0xFF22C55E), 
                                    modifier = Modifier.size(28.dp).align(Alignment.CenterVertically)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("%.1f kg".format(uiState.targetWeight ?: 0f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (isBulking) Color(0xFF2563EB) else Color(0xFF22C55E))
                                    Text("Target", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // ── Goal Strategy & Prediction Card ──────────────────────────────
            item {
                var editingStrategy by remember { mutableStateOf(false) }
                var innerPace by remember { mutableStateOf("") }
                var innerIsKg by remember { mutableStateOf(true) }

                LaunchedEffect(uiState.weeklyGoalWeight) {
                    innerIsKg = uiState.weeklyGoalWeight >= 1f || uiState.weeklyGoalWeight == 0f
                    innerPace = if (uiState.weeklyGoalWeight < 1f && uiState.weeklyGoalWeight > 0f) 
                        (uiState.weeklyGoalWeight * 1000).toInt().toString() else uiState.weeklyGoalWeight.toString()
                }

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Goal Intensity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { editingStrategy = !editingStrategy }) {
                                Text(if (editingStrategy) "Cancel" else "Adjust", color = Color(0xFF3B82F6))
                            }
                        }
                        
                        if (editingStrategy) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = innerPace,
                                        onValueChange = { innerPace = it },
                                        label = { Text(if (innerIsKg) "Pace (kg / week)" else "Pace (gm / week)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    FilterChip(selected = innerIsKg, onClick = { innerIsKg = true }, label = { Text("kg") })
                                    FilterChip(selected = !innerIsKg, onClick = { innerIsKg = false }, label = { Text("gm") })
                                }
                                Button(
                                    onClick = {
                                        var w = innerPace.toFloatOrNull() ?: 0.5f
                                        if (!innerIsKg) w /= 1000f
                                        viewModel.setGoal(uiState.startWeight ?: 0f, uiState.targetWeight ?: 0f, w)
                                        editingStrategy = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Save Strategy") }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Weekly Strategy", fontSize = 12.sp, color = Color.Gray)
                                    Text("%.2f kg / week".format(uiState.weeklyGoalWeight), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2937))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    val isBulking = (uiState.targetWeight ?: 0f) > (uiState.startWeight ?: 0f)
                                    Icon(
                                        if (isBulking) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Log Today's Weight Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Log Today's Weight", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (uiState.savedToday) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Logged ✓", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                placeholder = { Text("e.g. 0.0", color = Color.Gray) },
                                suffix = { Text("kg", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                            )
                            Button(
                                onClick = {
                                    weightInput.toFloatOrNull()?.let { w ->
                                        viewModel.logWeight(w)
                                        weightInput = ""
                                    }
                                },
                                enabled = !uiState.isSaving && weightInput.toFloatOrNull() != null,
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Log", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Weight History Graph
            if (uiState.history.size >= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Weight History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val change = uiState.history.lastOrNull()?.weight?.minus(uiState.history.first().weight)
                            if (change != null) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (change < 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (change < 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "${"%.1f".format(change)} kg overall",
                                        fontSize = 12.sp,
                                        color = if (change < 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            WeightHistoryChart(uiState.history)
                        }
                    }
                }
            }

            // History List
            if (uiState.history.isNotEmpty()) {
                item {
                    Text("Daily Log", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                }
                items(uiState.history.reversed()) { entry ->
                    WeightEntryRow(entry)
                }
            }
        }
    }
}

// ─── Sub-composables ─────────────────────────────────────────────────────────

@Composable
fun WeightStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun WeightHistoryChart(history: List<WeightEntry>) {
    var animPlayed by remember { mutableStateOf(false) }
    val animVal by animateFloatAsState(
        targetValue = if (animPlayed) 1f else 0f,
        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
        label = "chartAnim"
    )
    LaunchedEffect(Unit) { animPlayed = true }

    if (history.isEmpty()) return

    val weights = history.map { it.weight }
    val minWeight = (weights.minOrNull() ?: 0f) - 1.5f
    val maxWeight = (weights.maxOrNull() ?: 100f) + 1.5f
    val weightRange = maxWeight - minWeight

    Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp)) {
        // Y-axis Markers (on the left)
        Column(
            modifier = Modifier.fillMaxHeight().padding(bottom = 30.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val steps = 4
            for (i in 0..steps) {
                val value = maxWeight - (weightRange / steps) * i
                Text("%dkg".format(value.toInt()), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(35.dp))
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(start = 40.dp, bottom = 30.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                // Draw Horizontal Grid Lines
                val steps = 4
                for (i in 0..steps) {
                    val y = (height / steps) * i
                    drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, y), Offset(width, y), 1.dp.toPx())
                }

                val spacing = if (weights.size > 1) width / (weights.size - 1) else width
                val path = Path()
                weights.forEachIndexed { i, v ->
                    val x = i * spacing
                    val y = height - ((v - minWeight) / weightRange * height)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, Color(0xFF3B82F6).copy(alpha = animVal), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo((weights.size - 1) * spacing, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(fillPath, Brush.verticalGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.18f), Color.Transparent)))

                // Data points
                weights.forEachIndexed { i, v ->
                    val x = i * spacing
                    val y = height - ((v - minWeight) / weightRange * height)
                    drawCircle(Color.White, 4.dp.toPx(), Offset(x, y))
                    drawCircle(Color(0xFF3B82F6), 4.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                }
            }
        }

        // X-axis Markers (Dates at the bottom)
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(start = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (history.size > 0) {
                Text(history.first().date.takeLast(5), fontSize = 10.sp, color = Color.Gray)
                if (history.size > 2) {
                    Text(history[history.size / 2].date.takeLast(5), fontSize = 10.sp, color = Color.Gray)
                }
                Text(history.last().date.takeLast(5), fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun WeightEntryRow(entry: WeightEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MonitorWeight, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(entry.date, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color(0xFF6B7280))
            Text("%.1f kg".format(entry.weight), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1F2937))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSetupDialog(
    initialStart: String,
    initialTarget: String,
    initialWeekly: Float = 0.5f,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float, Float) -> Unit
) {
    var startInput by remember { mutableStateOf(initialStart) }
    var targetInput by remember { mutableStateOf(initialTarget) }
    var isKg by remember { mutableStateOf(true) }
    var weeklyVal by remember { mutableStateOf(if (initialWeekly < 1f) (initialWeekly * 1000).toString() else initialWeekly.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Weight Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weight Range (kg)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startInput,
                            onValueChange = { startInput = it },
                            label = { Text("Start") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("Target") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weekly Strategy", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weeklyVal,
                            onValueChange = { weeklyVal = it },
                            label = { Text(if (isKg) "kg / week" else "gm / week") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(selected = isKg, onClick = { isKg = true }, label = { Text("kg") })
                        FilterChip(selected = !isKg, onClick = { isKg = false }, label = { Text("gm") })
                    }
                    Text("Target pace of 0.3 - 0.7 kg is recommended.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startInput.toFloatOrNull()
                    val t = targetInput.toFloatOrNull()
                    var w = weeklyVal.toFloatOrNull() ?: 0.5f
                    if (!isKg) w /= 1000f
                    if (s != null && t != null) onConfirm(s, t, w)
                },
                enabled = startInput.toFloatOrNull() != null && targetInput.toFloatOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
