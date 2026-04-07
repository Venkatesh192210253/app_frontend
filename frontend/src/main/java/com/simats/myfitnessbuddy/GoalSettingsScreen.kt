package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingsScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    viewModel: GoalSettingsViewModel = viewModel(),
    aiViewModel: AdaptiveAiManager = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdaptiveEnabled by aiViewModel.isAdaptiveModeEnabled.collectAsState()
    val backgroundColor = Color(0xFFF4F6FA)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = appPadding.calculateBottomPadding())
            ) 
        },
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Goal Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Define your targets", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + appPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 0. AI Adaptive Engine Section
            item {
                GoalSectionCard(title = "AI Adaptive Engine") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Adaptive Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                "Auto-adjust targets based on recovery and performance",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = uiState.isAdaptiveEnabled,
                            onCheckedChange = { 
                                viewModel.updateAdaptiveMode(it)
                                aiViewModel.toggleAdaptiveMode(it) 
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF22C55E)
                            )
                        )
                    }
                }
            }

            // 1. Primary Goal Section
            item {
                GoalSectionCard(title = "Primary Goal") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoalChip("Lose Weight", uiState.primaryGoal == UserGoal.LoseWeight, Modifier.weight(1f)) { viewModel.updatePrimaryGoal(UserGoal.LoseWeight) }
                                GoalChip("Build Muscle", uiState.primaryGoal == UserGoal.BuildMuscle, Modifier.weight(1f)) { viewModel.updatePrimaryGoal(UserGoal.BuildMuscle) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoalChip("Maintain", uiState.primaryGoal == UserGoal.Maintain, Modifier.weight(1f)) { viewModel.updatePrimaryGoal(UserGoal.Maintain) }
                                GoalChip("Athletic Performance", uiState.primaryGoal == UserGoal.AthleticPerformance, Modifier.weight(1f)) { viewModel.updatePrimaryGoal(UserGoal.AthleticPerformance) }
                            }
                        }
                    }
                }
            }

            // 2. Weight Goals Section
            item {
                GoalSectionCard(title = "Weight Goals") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) { 
                                GoalInputField(
                                    label = "Current Weight (kg)", 
                                    value = uiState.currentWeight,
                                    placeholder = "Enter current weight"
                                ) { viewModel.updateCurrentWeight(it) } 
                            }
                            Box(modifier = Modifier.weight(1f)) { 
                                GoalInputField(
                                    label = "Target Weight (kg)", 
                                    value = uiState.targetWeight,
                                    placeholder = "Enter target weight"
                                ) { viewModel.updateTargetWeight(it) } 
                            }
                        }
                        GoalInputField("Weekly Goal (kg per week)", uiState.weeklyGoal) { viewModel.updateWeeklyGoal(it) }
                        
                        // Dynamic Predictive Goal Completion (Phase 2)
                        val weeksRemaining = viewModel.calculateWeeksToGoal()
                        PredictedCompletionCard(weeksRemaining, uiState.consistencyScore)
                    }
                }
            }

            // 3. Body Composition Section
            item {
                GoalSectionCard(title = "Body Composition") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) { GoalInputField("Current Body Fat (%)", uiState.currentBodyFat) { viewModel.updateCurrentBodyFat(it) } }
                            Box(modifier = Modifier.weight(1f)) { GoalInputField("Target Body Fat (%)", uiState.targetBodyFat) { viewModel.updateTargetBodyFat(it) } }
                        }
                        GoalInputField("Muscle Mass Goal (kg)", uiState.muscleMassGoal) { viewModel.updateMuscleMassGoal(it) }
                    }
                }
            }

            // 4. Activity Goals Section
            item {
                GoalSectionCard(title = "Activity Goals") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GoalInputField("Workouts per Week", uiState.workoutsPerWeek) { viewModel.updateWorkoutsPerWeek(it) }
                        GoalInputField("Daily Step Goal", uiState.dailyStepGoal) { viewModel.updateDailyStepGoal(it) }
                        GoalInputField("Weekly Calories Burn Goal", uiState.weeklyCalorieBurnGoal) { viewModel.updateWeeklyCalorieBurnGoal(it) }
                    }
                }
            }

            // 5. Nutrition Goals Section
            item {
                GoalSectionCard(title = "Nutrition Goals") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GoalInputField("Daily Calorie Target", uiState.dailyCalorieTarget) { viewModel.updateDailyCalorieTarget(it) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) { GoalInputField("Protein (g)", uiState.proteinG) { viewModel.updateProtein(it) } }
                            Box(modifier = Modifier.weight(1f)) { GoalInputField("Carbs (g)", uiState.carbsG) { viewModel.updateCarbs(it) } }
                            Box(modifier = Modifier.weight(1f)) { GoalInputField("Fats (g)", uiState.fatsG) { viewModel.updateFats(it) } }
                        }
                    }
                }
            }

            // 6. Save Button
            item {
                LoadingButton(
                    text = "Save Goals",
                    isLoading = uiState.isSaving,
                    onClick = {
                        if (uiState.currentWeight.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter your current weight")
                            }
                        } else if (uiState.targetWeight.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter your goal weight")
                            }
                        } else {
                            viewModel.validateAndSave { success ->
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Goals saved successfully!")
                                        onBack()
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save goals. Please try again.")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, CircleShape),
                    containerColor = Color(0xFF00C896),
                    shape = CircleShape
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun PredictedCompletionCard(weeks: Int, consistency: Int) {
    Surface(
        color = Color(0xFFF0FDFA),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Predicted Completion", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF134E4A))
                    Text("Based on current consistency", fontSize = 11.sp, color = Color(0xFF0D9488))
                }
                Box(contentAlignment = Alignment.Center) {
                    Text("$consistency%", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0D9488))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$weeks", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF134E4A))
                Text(" weeks remaining", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF134E4A), modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Timeline Progress Bar
            val animatedProgress by animateFloatAsState(
                targetValue = if (weeks > 0) 0.3f else 1f, // Mock progress for timeline
                animationSpec = tween(1000),
                label = "timeline"
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFFCCFBF1)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Goal Estimate: ~ ${weeks} weeks from today", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun GoalSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray, modifier = Modifier.padding(bottom = 16.dp))
            content()
        }
    }
}

@Composable
fun GoalChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(45.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF22C55E) else Color(0xFFF3F4F6)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalInputField(
    label: String, 
    value: String, 
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = Color.LightGray) } } else null,
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF9FAFB),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFF22C55E).copy(alpha = 0.5f)
            )
        )
    }
}
