package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToBarcode: () -> Unit,
    onNavigateToCustomFood: () -> Unit,
    onNavigateToAddFoodToMeal: (String) -> Unit,
    viewModel: DiaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onDateSelected(selectedDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = Color(0xFF4A6FFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp) // Lift above bottom nav
            ) 
        },
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = { Text("Food Diary", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = Color(0xFF4A6FFF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Date Selector ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onPreviousDay() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = Color(0xFF4A6FFF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Day View", fontSize = 12.sp, color = Color.Gray)
                        Text(uiState.dateDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { viewModel.onNextDay() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = Color(0xFF4A6FFF))
                    }
                }
            }

            // --- Calorie Summary Card ---
            item {
                CalorieSummaryCard(uiState)
            }

            // --- Visual Tracking Section (2x2 Uniform Grid) ---
            item {
                Text("Visual Tracking", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))
                
                val trackingOptions = listOf(
                    VisualTrackOption("Search Food", "Manual find", Icons.Default.Search, Color(0xFF22C55E), "search"),
                    VisualTrackOption("AI Photo", "Scan with AI", Icons.Default.CameraAlt, Color(0xFF4A6FFF), "ai_scan"),
                    VisualTrackOption("Nutrition Table", "Product facts", Icons.Default.QrCodeScanner, Color(0xFFFFA000), "barcode"),
                    VisualTrackOption("Custom", "Add manually", Icons.Default.EditNote, Color(0xFF6366F1), "custom")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        VisualTrackCardSimple(trackingOptions[0], Modifier.weight(1f)) { onNavigateToSearch() }
                        VisualTrackCardSimple(trackingOptions[1], Modifier.weight(1f)) { onNavigateToScan() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        VisualTrackCardSimple(trackingOptions[2], Modifier.weight(1f)) { onNavigateToBarcode() }
                        VisualTrackCardSimple(trackingOptions[3], Modifier.weight(1f)) { onNavigateToCustomFood() }
                    }
                }
            }

            // --- AI Food Swap Suggestions (Phase 5) ---
            if (uiState.swapSuggestions.isNotEmpty()) {
                item {
                    Text("AI Smart Swaps", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    AIFoodSwapCard(uiState.swapSuggestions) { suggestion ->
                        viewModel.applyFoodSwap(suggestion)
                        scope.launch {
                            snackbarHostState.showSnackbar("Applied ${suggestion.suggestedFood}!")
                        }
                    }
                }
            }

            // --- Meal Sections ---
            items(uiState.meals) { meal ->
                MealSectionCard(meal, onAddFood = { onNavigateToAddFoodToMeal(meal.name) })
            }

            // --- Water Intake Section ---
            item {
                WaterIntakeCard(
                    glasses = uiState.waterIntake,
                    onUpdate = { viewModel.updateWaterIntake(it) }
                )
            }

            item {
                LoadingButton(
                    text = "Save Changes",
                    isLoading = uiState.isSaving,
                    onClick = { 
                        viewModel.saveChanges()
                        scope.launch {
                            snackbarHostState.showSnackbar("Changes saved successfully!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    containerColor = Color(0xFF00C896),
                    icon = Icons.Default.Save,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun CalorieSummaryCard(state: DiaryState) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Calories Remaining", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                    Text("${state.caloriesLeft}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4A6FFF))
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(
                        progress = { 
                            val budget = (state.calorieGoal + state.exerciseCalories).toFloat()
                            if (budget > 0) state.totalCalories.toFloat() / budget else 0f 
                        },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF4A6FFF),
                        trackColor = Color.LightGray.copy(alpha = 0.3f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        strokeWidth = 6.dp
                    )
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric("Goal", "${state.calorieGoal}")
                SummaryMetric("Food", "${state.totalCalories}")
                SummaryMetric("Exercise", "${state.exerciseCalories}")
            }
        }
    }
}

@Composable
fun SummaryMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
    }
}

@Composable
fun MealSectionCard(meal: MealLog, onAddFood: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when(meal.name) {
                        "Breakfast" -> Icons.Default.WbSunny
                        "Lunch" -> Icons.Default.LightMode
                        "Dinner" -> Icons.Default.NightsStay
                        else -> Icons.Default.BakeryDining
                    }
                    Icon(icon, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(meal.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                }
                Text("${meal.calories} kcal", fontWeight = FontWeight.Medium, color = Color.Gray)
            }
            if (meal.foods.isEmpty()) {
                Text(
                    "No food logged yet",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            } else {
                meal.foods.forEach { food ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(food.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(food.macros, fontSize = 11.sp, color = Color.Gray)
                        }
                        Text("${food.calories} kcal", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    
                    if (food.aiSuggestion != null) {
                        Surface(
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = Color(0xFFF0F4FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb, 
                                    contentDescription = null, 
                                    tint = Color(0xFF4A6FFF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    food.aiSuggestion, 
                                    fontSize = 11.sp, 
                                    color = Color(0xFF312E81),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Divider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))
                }
            }

            Button(
                onClick = onAddFood,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F4FF)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Food", color = Color(0xFF4A6FFF), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VisualTrackCardSimple(
    option: VisualTrackOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp).shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(option.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(option.icon, contentDescription = null, tint = option.color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(option.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                Text(option.subtitle, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

data class VisualTrackOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun ScanActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AIFoodSwapCard(suggestions: List<FoodSwapSuggestion>, onApply: (FoodSwapSuggestion) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
                val suggestion = suggestions.firstOrNull() ?: return@Column
                val isGain = suggestion.caloriesSaved > 0
                val accentColor = if (isGain) Color(0xFFFFA000) else Color(0xFF6366F1)
                val statusText = if (isGain) "Fuel Up Suggestion" else "Better Alternative"
                val diffPrefix = if (isGain) "+" else ""
                val badgeBg = if (isGain) Color(0xFFFFF7ED) else Color(0xFFDCFCE7)
                val badgeText = if (isGain) Color(0xFF9A3412) else Color(0xFF166534)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(suggestion.originalFood, fontSize = 14.sp, color = Color.Gray)
                        Icon(if (isGain) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Text(suggestion.suggestedFood, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isGain) Color(0xFFF97316) else Color(0xFF22C55E))
                    }
                    
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$diffPrefix${suggestion.caloriesSaved} kcal",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = badgeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(suggestion.reason, fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onApply(suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Swap Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

@Composable
fun WaterIntakeCard(glasses: Int, onUpdate: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Water Intake", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                }
                
                val liters = (glasses * 250) / 1000f
                Text(String.format("%.2f L / 3.00 L", liters), fontWeight = FontWeight.Medium, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (glasses > 0) onUpdate(glasses - 1) },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF0F4FF))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color(0xFF4A6FFF))
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(12) { index ->
                        val isFilled = index < glasses
                        Box(
                            modifier = Modifier
                                .size(width = 10.dp, height = 30.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isFilled) Color(0xFF4A6FFF) else Color(0xFFE5E7EB))
                        )
                    }
                }
                
                IconButton(
                    onClick = { if (glasses < 12) onUpdate(glasses + 1) },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF0F4FF))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color(0xFF4A6FFF))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Goal: 12 glasses (3.0L) - 1 glass = 250ml",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
