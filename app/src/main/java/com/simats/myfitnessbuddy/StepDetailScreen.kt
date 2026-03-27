package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.simats.myfitnessbuddy.StepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepDetailScreen(
    onBack: () -> Unit,
    appPadding: PaddingValues,
    viewModel: StepViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                viewModel.startPolling()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step Activity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF22C55E))
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
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Steps Today", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(String.format("%,d", viewModel.steps.value), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2937))
                        Text("Goal: ${viewModel.goal.value} steps", fontSize = 14.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Medium)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LinearProgressIndicator(
                            progress = { (viewModel.steps.value.toFloat() / viewModel.goal.value).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            color = Color(0xFF22C55E),
                            trackColor = Color(0xFFDCFCE7)
                        )
                    }
                }
            }

            // Metrics Grid
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepMetricCard("Calories", "${viewModel.calories.value} kcal", Icons.Default.ShowChart, Color(0xFFFEF3C7), Modifier.weight(1f))
                    StepMetricCard("Distance", String.format("%.2f km", viewModel.distance.value), Icons.Default.History, Color(0xFFDBEAFE), Modifier.weight(1f))
                }
            }

            // Insights
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF22C55E))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "You are in the top 30% of users today! Walk another 2k steps to reach the next tier.",
                            fontSize = 14.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepMetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        }
    }
}
