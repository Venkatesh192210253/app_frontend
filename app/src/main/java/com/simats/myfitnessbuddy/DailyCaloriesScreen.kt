package com.simats.myfitnessbuddy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCaloriesScreen(
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: DailyCaloriesViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadDailyData()
    }

    Scaffold(
        containerColor = Color(0xFFF4F6FA), // Light background
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Daily Calories", 
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = appPadding.calculateBottomPadding()
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Circular Progress Ring
            item {
                CircularProgressCard(viewModel)
            }

            // 1b. Food Progress vs Goal Card
            item {
                val progressValue = viewModel.progress
                val animatedProg by animateFloatAsState(
                    targetValue = progressValue,
                    animationSpec = tween(1000),
                    label = "caloriesProgCard"
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Food Progress vs Goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${(animatedProg * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E),
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { animatedProg.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFF22C55E),
                            trackColor = Color.LightGray.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${viewModel.consumedCalories.value} of ${viewModel.calorieGoal.value} kcal consumed",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // 2. Meal Cards section title
            item {
                Text(
                    "Meals Today",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 3. Reusable Meal Cards
            items(viewModel.meals) { meal ->
                MealCard(meal)
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun CircularProgressCard(viewModel: DailyCaloriesViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                val progressValue = viewModel.progress
                val animatedProgress by animateFloatAsState(
                    targetValue = progressValue,
                    animationSpec = tween(1000),
                    label = "caloriesProgress"
                )

                Canvas(modifier = Modifier.size(200.dp)) {
                    // Gray Track
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Green Progress
                    drawArc(
                        color = Color(0xFF22C55E),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${viewModel.consumedCalories.value}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "of ${viewModel.calorieGoal.value} kcal",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E)
                    )
                }
            }
        }
    }
}

@Composable
fun MealCard(meal: MealItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meal Icon Placeholder (could be added if needed, using custom colors per meal)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF9FAFB)),
                contentAlignment = Alignment.Center
            ) {
                // We can put generic icons here if needed, or just calories
                Text("${meal.calories}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        meal.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        meal.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    "${meal.calories} kcal of ${meal.goal} kcal • ${(meal.progress * 100).toInt()}%",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = meal.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF22C55E),
                    trackColor = Color.LightGray.copy(alpha = 0.2f)
                )
            }
        }
    }
}
