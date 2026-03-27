package com.simats.myfitnessbuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.simats.myfitnessbuddy.data.local.StepEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class StepsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.simats.myfitnessbuddy.data.local.SettingsManager.init(this)
        setContent {
            MaterialTheme {
                StepsHistoryScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsHistoryScreen(
    onBack: () -> Unit,
    viewModel: StepViewModel = viewModel()
) {
    val history by viewModel.history.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startPolling()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps History", fontWeight = FontWeight.Bold) },
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
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Big Card
            item {
                TodayStepCard(viewModel.steps.value, viewModel.goal.value)
            }

            // 7-Day Chart
            item {
                StepChartCard(history)
            }

            // Stats Summary
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStatCard("Weekly Total", "${viewModel.weeklyTotal.value}", Icons.Default.TrendingUp, Color(0xFFDBEAFE).copy(alpha = 0.5f), Modifier.weight(1f))
                    MiniStatCard("Avg Daily", "%.0f".format(viewModel.averageSteps.value), Icons.Default.History, Color(0xFFDCFCE7).copy(alpha = 0.5f), Modifier.weight(1f))
                }
            }
            
            // History List Header
            item {
                Text(
                    "Previous Days",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // History Items
            items(history.filter { it.date != LocalDate.now().toString() }) { entry ->
                HistoryItem(entry)
            }
            
            if (history.size <= 1) {
                item {
                    EmptyHistoryView()
                }
            }
        }
    }
}

@Composable
fun StepChartCard(history: List<StepEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Last 7 Days", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(20.dp))
            
            // Simple Bar Chart
            val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()).toString() }.reversed()
            val data = last7Days.map { date ->
                history.find { it.date == date }?.steps ?: 0
            }
            val maxSteps = (data.maxOrNull() ?: 10000).coerceAtLeast(5000)

            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { steps ->
                    val heightFactor = steps.toFloat() / maxSteps
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .fillMaxHeight(heightFactor.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (steps >= 10000) Color(0xFF22C55E) else Color(0xFF3B82F6))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                last7Days.forEach { date ->
                    val dayName = LocalDate.parse(date).dayOfWeek.name.take(1)
                    Text(dayName, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TodayStepCard(steps: Int, goal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Today", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(String.format("%,d", steps), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2937))
            Text("Goal: $goal steps", fontSize = 14.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = { (steps.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFFDCFCE7)
            )
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(bgColor),
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

@Composable
fun HistoryItem(entry: StepEntry) {
    val date = try {
        val parsed = LocalDate.parse(entry.date)
        if (parsed == LocalDate.now().minusDays(1)) "Yesterday"
        else parsed.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH))
    } catch (e: Exception) {
        entry.date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(date, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1F2937))
                Text("Completed", fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format("%,d", entry.steps), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF22C55E))
                Text("steps", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EmptyHistoryView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No history yet", color = Color.Gray)
        Text("Keep walking to see your progress!", fontSize = 12.sp, color = Color.LightGray)
    }
}
