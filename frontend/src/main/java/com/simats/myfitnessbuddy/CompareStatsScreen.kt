package com.simats.myfitnessbuddy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

import com.simats.myfitnessbuddy.data.remote.FirebaseStatsManager
import com.simats.myfitnessbuddy.data.remote.UserStats
import com.simats.myfitnessbuddy.data.local.SettingsManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareStatsScreen(
    friendId: String,
    friendName: String,
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRemoveDialog by remember { mutableStateOf(false) }

    // Observe stats (Firebase)
    val myFirebaseStats by FirebaseStatsManager.observeUserStats(SettingsManager.userId).collectAsState(initial = UserStats())
    val friendFirebaseStats by FirebaseStatsManager.observeUserStats(friendId).collectAsState(initial = UserStats())

    // API Stats (Fallback/Primary if Firebase missing)
    var myApiStats by remember { mutableStateOf<UserStats?>(null) }
    var friendApiStats by remember { mutableStateOf<UserStats?>(null) }
    var isApiLoading by remember { mutableStateOf(true) }

    LaunchedEffect(friendId) {
        isApiLoading = true
        try {
            val response = RetrofitClient.apiService.compareStats(friendId)
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    myApiStats = UserStats(
                        userId = SettingsManager.userId,
                        username = SettingsManager.userName,
                        steps = data.me.steps,
                        workouts = data.me.workouts,
                        xp = data.me.xp,
                        streak = data.me.streak,
                        level = data.me.level
                    )
                    friendApiStats = UserStats(
                        userId = friendId,
                        username = friendName,
                        steps = data.friend.steps,
                        workouts = data.friend.workouts,
                        xp = data.friend.xp,
                        streak = data.friend.streak,
                        level = data.friend.level
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CompareStatsScreen", "API Fetch failed: ${e.message}")
        } finally {
            isApiLoading = false
        }
    }

    // Merge Stats: Use Firebase if available, otherwise use API
    // Local polling for immediate UI feedback as the user walks
    var localUserSteps by remember { mutableStateOf(SettingsManager.totalStepsToday) }
    LaunchedEffect(Unit) {
        while (true) {
            localUserSteps = SettingsManager.totalStepsToday
            kotlinx.coroutines.delay(2000) // Fast refresh for active users
        }
    }

    // Merge Stats: Combine Firebase, API, and immediate Local stats for maximum accuracy
    val finalMyStats = remember(myFirebaseStats, myApiStats, localUserSteps) {
        val fb = myFirebaseStats
        val api = myApiStats ?: UserStats()
        UserStats(
            userId = SettingsManager.userId,
            username = SettingsManager.userName,
            steps = maxOf(localUserSteps, fb?.steps ?: 0, api.steps),
            workouts = maxOf(fb?.workouts ?: 0, api.workouts),
            xp = maxOf(fb?.xp ?: 0, api.xp),
            streak = maxOf(fb?.streak ?: 0, api.streak),
            level = maxOf(api.level, fb?.level ?: 1)
        )
    }

    val finalFriendStats = remember(friendFirebaseStats, friendApiStats) {
        val fb = friendFirebaseStats
        val api = friendApiStats ?: UserStats()
        UserStats(
            userId = friendId,
            username = friendName,
            steps = maxOf(fb?.steps ?: 0, api.steps),
            workouts = maxOf(fb?.workouts ?: 0, api.workouts),
            xp = maxOf(fb?.xp ?: 0, api.xp),
            streak = maxOf(fb?.streak ?: 0, api.streak),
            level = maxOf(fb?.level ?: api.level, api.level)
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Unfriend $friendName?") },
            text = { Text("Are you sure you want to remove this friend? They will be removed from your friends list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
                                val response = RetrofitClient.apiService.removeFriend(
                                    body = mapOf("friend_id" to friendId)
                                )
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            showRemoveDialog = false
                        }
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = { Text("Compare Stats", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isApiLoading = true
                                try {
                                    val response = RetrofitClient.apiService.compareStats(friendId)
                                    if (response.isSuccessful) {
                                        val data = response.body()
                                        if (data != null) {
                                            myApiStats = UserStats(
                                                userId = SettingsManager.userId,
                                                username = SettingsManager.userName,
                                                steps = data.me.steps,
                                                workouts = data.me.workouts,
                                                xp = data.me.xp,
                                                streak = data.me.streak,
                                                level = data.me.level
                                            )
                                            friendApiStats = UserStats(
                                                userId = friendId,
                                                username = friendName,
                                                steps = data.friend.steps,
                                                workouts = data.friend.workouts,
                                                xp = data.friend.xp,
                                                streak = data.friend.streak,
                                                level = data.friend.level
                                            )
                                        }
                                    }
                                } finally {
                                    isApiLoading = false
                                }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF4A6FFF))
                        }
                        TextButton(onClick = { showRemoveDialog = true }) {
                            Text("Unfriend", color = Color.Red, fontWeight = FontWeight.Medium)
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
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. VS Header
            item {
                ComparisonHeader(SettingsManager.userName, friendName)
            }

            // 2. Main Stats Grid
            item {
                Text("Core Metrics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }

            item {
                ComparisonStatsGrid(finalMyStats ?: UserStats(), finalFriendStats ?: UserStats())
            }

            // 3. Ranking Insight
            item {
                InsightCard(
                    friendName,
                    (finalMyStats ?: UserStats()).workouts,
                    (finalFriendStats ?: UserStats()).workouts
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ComparisonHeader(userName: String, friendName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF4A6FFF), Color(0xFF6366F1))))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User side
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(userName.take(1).uppercase() + (userName.split(" ").getOrNull(1)?.take(1)?.uppercase() ?: ""), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(userName, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            // VS Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("VS", color = Color(0xFF4A6FFF), fontWeight = FontWeight.Black, fontSize = 18.sp)
            }

            // Friend side
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(friendName.take(1).uppercase() + (friendName.split(" ").getOrNull(1)?.take(1)?.uppercase() ?: ""), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(friendName, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ComparisonStatsGrid(my: UserStats, friend: UserStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val maxSteps = maxOf(my.steps, friend.steps)
        val stepUserProgress = if (maxSteps > 0) my.steps.toFloat() / maxSteps else 0.0f
        val stepFriendProgress = if (maxSteps > 0) friend.steps.toFloat() / maxSteps else 0.0f

        val maxWorkouts = maxOf(my.workouts, friend.workouts)
        val workoutUserProgress = if (maxWorkouts > 0) my.workouts.toFloat() / maxWorkouts else 0.0f
        val workoutFriendProgress = if (maxWorkouts > 0) friend.workouts.toFloat() / maxWorkouts else 0.0f

        StatCompareRow(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            label = "Daily Steps",
            userVal = "${my.steps} steps",
            friendVal = "${friend.steps} steps",
            userProgress = stepUserProgress,
            friendProgress = stepFriendProgress,
            friendName = friend.username
        )
        
        StatCompareRow(
            icon = Icons.Default.FitnessCenter,
            label = "Workouts Done",
            userVal = "${my.workouts}",
            friendVal = "${friend.workouts}",
            userProgress = workoutUserProgress,
            friendProgress = workoutFriendProgress,
            friendName = friend.username
        )
        
        StatCompareRow(
            icon = Icons.Default.Bolt,
            label = "XP Level",
            userVal = "Lvl ${my.level}",
            friendVal = "Lvl ${friend.level}",
            userProgress = if(maxOf(my.xp, friend.xp) > 0) my.xp.toFloat()/maxOf(my.xp, friend.xp) else 0f,
            friendProgress = if(maxOf(my.xp, friend.xp) > 0) friend.xp.toFloat()/maxOf(my.xp, friend.xp) else 0f,
            friendName = friend.username
        )

        val maxStreak = maxOf(my.streak, friend.streak)
        val streakUserProgress = if (maxStreak > 0) my.streak.toFloat() / maxStreak else 0.0f
        val streakFriendProgress = if (maxStreak > 0) friend.streak.toFloat() / maxStreak else 0.0f
        
        StatCompareRow(
            icon = Icons.Default.Whatshot,
            label = "Day Streak",
            userVal = "${my.streak} days",
            friendVal = "${friend.streak} days",
            userProgress = streakUserProgress,
            friendProgress = streakFriendProgress,
            friendName = friend.username
        )
    }
}

@Composable
fun StatCompareRow(
    icon: ImageVector, 
    label: String, 
    userVal: String, 
    friendVal: String, 
    userProgress: Float,
    friendProgress: Float,
    friendName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Side-by-side comparison
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // User Side
                Column(modifier = Modifier.weight(1f)) {
                    Text("You", fontSize = 11.sp, color = Color.Gray)
                    Text(userVal, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { userProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF22C55E),
                        trackColor = Color(0xFFF3F4F6),
                        strokeCap = StrokeCap.Round
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // Friend Side
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(friendName, fontSize = 11.sp, color = Color.Gray)
                    Text(friendVal, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF4A6FFF))
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { friendProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF4A6FFF),
                        trackColor = Color(0xFFF3F4F6),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            
            // Winning Status
            val message = when {
                userProgress > friendProgress -> "You're ahead! 🚀"
                userProgress < friendProgress -> "$friendName is taking the lead!"
                userProgress > 0f -> "It's a tie! 🤝"
                else -> "No progress yet"
            }
            
            Text(
                message,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if(userProgress >= friendProgress && userProgress > 0) Color(0xFF059669) else Color.Gray,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun InsightCard(friendName: String, myWorkouts: Int, friendWorkouts: Int) {
    val diff = friendWorkouts - myWorkouts
    val message = when {
        diff > 0 -> "You need $diff more workouts to match $friendName's rank! Keep pushing! 🔥"
        diff < 0 -> "You are leading $friendName by ${-diff} workouts! You're a beast! 🏆"
        else -> "You and $friendName are neck and neck in workouts! Who will take the lead? 🤔"
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF4A6FFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E40AF)
            )
        }
    }
}
