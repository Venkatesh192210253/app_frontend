package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    navController: NavController,
    viewModel: WorkoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = Color(0xFFF4F6FA)
    val primaryGreen = Color(0xFF22C55E)
    val accentGreen = Color(0xFF00C896)

    LaunchedEffect(Unit) {
        viewModel.fetchWorkoutStatus()
    }

    Scaffold(
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 120.dp // Added extra space for a better scroll-to-bottom feel
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Top Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Workout",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF121418)
                        )
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM dd")),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = { navController.navigate("log_workout/${uiState.currentWorkout.lowercase()}") },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                        shape = RoundedCornerShape(size = 50.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Log Workout", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Current Workout Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeText = when {
                                uiState.exercisesCompleted == 0 -> "Not Started"
                                uiState.exercisesCompleted >= uiState.totalExercises -> "Completed"
                                else -> "In Progress"
                            }

                            val titleText = uiState.currentWorkout

                            Text(
                                titleText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF121418)
                            )
                            Surface(
                                color = primaryGreen,
                                shape = RoundedCornerShape(size = 50.dp)
                            ) {
                                Text(
                                    badgeText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text("${uiState.currentWorkoutTime} min", color = Color.Gray, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Progress Section
                        ProgressSection(
                            label = "Exercises",
                            progress = uiState.exercisesCompleted.toFloat() / uiState.totalExercises,
                            currentText = "${uiState.exercisesCompleted}/${uiState.totalExercises}",
                            activeColor = primaryGreen
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ProgressSection(
                            label = "Calories Burned",
                            progress = uiState.caloriesCurrent.toFloat() / uiState.caloriesTarget,
                            currentText = "${uiState.caloriesCurrent} / ${uiState.caloriesTarget}",
                            activeColor = Color(0xFF4A6FFF)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val buttonText = when {
                            uiState.exercisesCompleted == 0 -> "Start Workout"
                            uiState.exercisesCompleted >= uiState.totalExercises -> "Completed"
                            else -> "Continue Workout"
                        }
                        
                        Button(
                            onClick = { navController.navigate("log_workout/${uiState.currentWorkout.lowercase()}") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            shape = RoundedCornerShape(size = 50.dp)
                        ) {
                            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Weekly Summary Stats Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(uiState.thisWeekWorkouts.toString(), "Workouts", Modifier.weight(1f))
                    SummaryStatCard(uiState.totalTimeMinutes.toString(), "Total Time", Modifier.weight(1f))
                    SummaryStatCard(uiState.totalCaloriesBurned.toString(), "Kcal Burned", Modifier.weight(1f))
                }
            }

            // 4. Quick Options Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickOptionCard(Icons.Default.CalendarMonth, "Day Schedule", Modifier.weight(1f)) {
                        navController.navigate("weekly_schedule")
                    }
                    QuickOptionCard(Icons.Default.History, "History", Modifier.weight(1f)) {
                        navController.navigate("workout_history")
                    }
                }
            }

            // 5. Workout Programs Section
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Workout Programs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Since it's inside a LazyColumn, we use a simple loop or a fixed height grid
                    // For performance and layout simplicity in a demo, we'll use a manually built grid
                    uiState.programs.chunked(2).forEach { rowPrograms ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowPrograms.forEach { program ->
                                ProgramCard(program, Modifier.weight(1f)) {
                                    navController.navigate("log_workout/${program.id}")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // 6. Recent Workouts Section
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Workouts", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "View All",
                            color = primaryGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.navigate("workout_history") }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    uiState.recentWorkouts.forEach { session ->
                        RecentWorkoutCard(session)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // 7. AI Suggestion Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = primaryGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Suggestion", fontWeight = FontWeight.Bold, color = Color(0xFF121418))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            uiState.aiSuggestionMessage,
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { navController.navigate("workout_ai") },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGreen)
                        ) {
                            Text("View Suggestions", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressSection(label: String, progress: Float, currentText: String, activeColor: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, color = Color.DarkGray)
            Text(currentText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF121418))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = activeColor,
            trackColor = Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun SummaryStatCard(value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF121418))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun QuickOptionCard(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF121418))
        }
    }
}

@Composable
fun ProgramCard(program: WorkoutProgram, modifier: Modifier, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .heightIn(min = 120.dp)
            .clickable { 
                isPressed = !isPressed
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                program.icon,
                contentDescription = null,
                tint = program.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(program.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun RecentWorkoutCard(session: WorkoutSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFFF0F4FF)
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFF4A6FFF),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(session.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(session.duration, color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                "${session.calories} kcal",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22C55E),
                fontSize = 14.sp
            )
        }
    }
}
