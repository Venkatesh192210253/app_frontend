package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutType: String?,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF22C55E)
    val accentGreen = Color(0xFF00C896)

    LaunchedEffect(workoutType) {
        viewModel.loadWorkout(workoutType)
    }

    val workout = uiState.workout

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(workout?.title ?: "Workout Detail", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("${workout?.level} • ${workout?.duration}", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Workout Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(
                                        "${uiState.exercisesCompleted}/${workout?.exercises?.size ?: 0} exercises completed",
                                        color = Color.DarkGray,
                                        fontSize = 14.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Calories Burned", color = Color.Gray, fontSize = 12.sp)
                                    Text("${uiState.caloriesBurned}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = primaryGreen)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val progress = if (workout != null && workout.exercises.isNotEmpty()) {
                                uiState.exercisesCompleted.toFloat() / workout.exercises.size
                            } else 0f

                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                            )

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = primaryGreen,
                                trackColor = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // 2. Exercises Title
                item {
                    Text("Exercises", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                // 3. Exercise List
                workout?.let {
                    itemsIndexed(it.exercises) { index, exercise ->
                        ExerciseRow(index + 1, exercise) {
                            viewModel.completeExercise(exercise.name)
                        }
                    }
                }

                // 4. Training Tips
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("${workout?.title?.split(" ")?.firstOrNull() ?: ""} Training Tips", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            workout?.tips?.forEach { tip ->
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("•", color = primaryGreen, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tip, fontSize = 14.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Bottom Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
            ) {
                Text("Back to Workouts", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ExerciseRow(number: Int, exercise: Exercise, onComplete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise Number
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Color(0xFFF4F6FA)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$number", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(exercise.sets, fontSize = 13.sp, color = Color.DarkGray)
                Text("Rest: ${exercise.rest}", fontSize = 12.sp, color = Color.Gray)
            }
            
            // Start/Complete Button
            if (exercise.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(28.dp))
            } else {
                OutlinedButton(
                    onClick = onComplete,
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF22C55E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E))
                ) {
                    Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
