package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GettingStartedScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Getting Started", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAF9)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Welcome to My Fitness Buddy!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    "Here is a quick guide to help you crush your fitness goals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            items(guideSteps) { step ->
                GuideStepCard(step)
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp).shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C896))
                ) {
                    Text("I'm Ready!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GuideStepCard(step: GuideStep) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(step.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(step.icon, contentDescription = null, tint = step.color)
            }
            Column {
                Text(step.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                Spacer(modifier = Modifier.height(4.dp))
                Text(step.description, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
            }
        }
    }
}

data class GuideStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val guideSteps = listOf(
    GuideStep(
        "Track Your Steps",
        "Keep your phone in your pocket. We automatically track your steps using sensors and calculate calories burned based on your weight.",
        Icons.Default.DirectionsRun,
        Color(0xFF4CAF50)
    ),
    GuideStep(
        "Log Your Meals",
        "Use the Food Diary to log what you eat. You can search for foods, scan barcodes, or even use AI to recognize food from photos!",
        Icons.Default.Restaurant,
        Color(0xFF2196F3)
    ),
    GuideStep(
        "Social & Challenges",
        "Add friends and join group challenges! Competing with others helps you stay motivated and reach milestones faster.",
        Icons.Default.Groups,
        Color(0xFFFF9800)
    ),
    GuideStep(
        "Personal AI Coach",
        "Need advice? Talk to your AI Coach. It analyzes your data to give personalized tips on workouts and nutrition.",
        Icons.Default.AutoAwesome,
        Color(0xFF9C27B0)
    ),
    GuideStep(
        "Earn XP & Level Up",
        "Every activity earns you XP. Complete challenges, log food, and track workouts to level up and earn achievements!",
        Icons.Default.EmojiEvents,
        Color(0xFFFFD700)
    )
)
