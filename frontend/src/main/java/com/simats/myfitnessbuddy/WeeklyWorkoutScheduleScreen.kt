package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyWorkoutScheduleScreen(
    navController: NavController,
    viewModel: WeeklyScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF22C55E)
    val accentGreen = Color(0xFF00C896)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Weekly Workout Schedule", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Your 7-day training plan", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Progress Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("This Week’s Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${uiState.completedDays}/${uiState.totalDays} days completed", color = Color.Gray, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SummaryItem("Total Workouts", "${uiState.completedWorkouts}/${uiState.totalWorkouts}", Modifier.weight(1f))
                                SummaryItem("Weekly Calories", "${uiState.weeklyCalories} kcal", Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 2. Week Schedule Title
                item {
                    Text("Week Schedule", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                // 3. Day Cards
                items(uiState.schedule) { workout ->
                    DayCard(workout) {
                        if (workout.type != "rest" && workout.type != "recovery") {
                            // Go directly to logging/active workout mode
                            navController.navigate("log_workout/${workout.workoutName}")
                        }
                    }
                }

                // 4. Training Tips Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFCE8)) // Light yellow/green tint
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Weekly Training Tips", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            TipBullet("Rest days are crucial for muscle recovery")
                            TipBullet("Stay hydrated throughout your workouts")
                            TipBullet("Focus on proper form over heavy weights")
                            TipBullet("Progressively overload to see results")
                            TipBullet("Listen to your body and adjust as needed")
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        
        // Bottom Button overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(size = 50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
            ) {
                Text("Back to Workouts", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF121418))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun DayCard(workout: WeeklyWorkout, onClick: () -> Unit) {
    val isRestDay = workout.type == "rest"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(if (isRestDay) 0.dp else 2.dp, RoundedCornerShape(size = 20.dp)),
        shape = RoundedCornerShape(size = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRestDay) Color.LightGray.copy(alpha = 0.2f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (isRestDay) Color.Gray.copy(alpha = 0.2f) else Color(0xFFDCFCE7)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        workout.workoutCode,
                        fontWeight = FontWeight.Bold,
                        color = if (isRestDay) Color.Gray else Color(0xFF22C55E),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Center: Info
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.day, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(workout.workoutName, fontSize = 14.sp, color = if (isRestDay) Color.Gray else Color.Black)
                
                if (!isRestDay) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${workout.duration} • ${workout.exercises} exercises • ${workout.calories} cal",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Right: Chevron/Badge
            if (workout.isCompleted) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(size = 50.dp)
                ) {
                    Text(
                        "Completed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TipBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color.DarkGray)
    }
}
