package com.simats.myfitnessbuddy

import android.content.Intent
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import android.os.Build

class DashboardActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
        } else true

        if (activityRecognitionGranted) {
            startStepCounterService()
        } else {
            Log.e("DashboardActivity", "Activity Recognition permission denied")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Only show toast if permission is actually missing
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                    android.widget.Toast.makeText(this, "Step tracking requires Activity Recognition permission. Please enable it in Settings.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.simats.myfitnessbuddy.data.local.SettingsManager.init(this)
        
        checkPermissionsAndStartService()
        
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val profileViewModel: ProfileViewModel = viewModel<ProfileViewModel>()
            val diaryViewModel: DiaryViewModel = viewModel<DiaryViewModel>()

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val coroutineScope = rememberCoroutineScope()
            var friendRequestDialog by remember { mutableStateOf<org.json.JSONObject?>(null) }
            var groupInviteDialog by remember { mutableStateOf<org.json.JSONObject?>(null) }

            LaunchedEffect(Unit) {
                com.simats.myfitnessbuddy.data.remote.WebSocketManager.connect()
                com.simats.myfitnessbuddy.data.remote.WebSocketManager.events.collect { rawEvent ->
                    val event = rawEvent.optJSONObject("message") ?: rawEvent
                    if (event.optString("event") == "friend_request_received") {
                        val data = event.optJSONObject("data")
                        if (data != null) {
                            friendRequestDialog = data
                        }
                    } else if (event.optString("event") == "group_invite_received") {
                        val data = event.optJSONObject("data")
                        if (data != null) {
                            groupInviteDialog = data
                        }
                    }
                }
            }

            friendRequestDialog?.let { dialogData ->
                AlertDialog(
                    onDismissRequest = { friendRequestDialog = null },
                    title = { Text("New Friend Request") },
                    text = { Text("${dialogData.optString("from_username")} sent you a friend request.") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
                                    com.simats.myfitnessbuddy.data.remote.RetrofitClient.apiService.acceptFriendRequest(mapOf("request_id" to dialogData.optString("request_id"))
                                    )
                                } catch (e: Exception) {}
                                friendRequestDialog = null
                            }
                        }) {
                            Text("Accept")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
                                    com.simats.myfitnessbuddy.data.remote.RetrofitClient.apiService.rejectFriendRequest(mapOf("request_id" to dialogData.optString("request_id"))
                                    )
                                } catch (e: Exception) {}
                                friendRequestDialog = null
                            }
                        }) {
                            Text("Decline")
                        }
                    }
                )
            }

            groupInviteDialog?.let { dialogData ->
                AlertDialog(
                    onDismissRequest = { groupInviteDialog = null },
                    title = { Text("Added to Group") },
                    text = { Text("${dialogData.optString("invited_by_name")} added you to the group '${dialogData.optString("group_name")}'.") },
                    confirmButton = {
                        TextButton(onClick = { groupInviteDialog = null }) {
                            Text("OK")
                        }
                    }
                )
            }
            val notificationsViewModel: NotificationsViewModel = viewModel<NotificationsViewModel>()
            val snackbarHostState = remember { SnackbarHostState() }

                // Global Notification Popup Logic
                LaunchedEffect(Unit) {
                    notificationsViewModel.newNotificationEvent.collect { notification ->
                        snackbarHostState.showSnackbar(
                            message = "${notification.title}: ${notification.description}",
                            actionLabel = "View",
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = Color(0xFF1F2937),
                                contentColor = Color.White,
                                actionColor = Color(0xFF22C55E),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    },
                    bottomBar = {
                        val showBottomBar = currentRoute in listOf(
                            "dashboard", "diary", "profile", "stats", "achievements", 
                            "nutrition", "notifications", "edit_profile", "ai_coach",
                            "goal_settings", "notifications_settings", "privacy_security", "help_support", "getting_started", "webview/{title}/{url}",
                            "friends", "add_friends", "group_detail/{groupId}",
                            "weekly_schedule", "workout_detail/{workoutType}",
                            "workout", "workout_history", "workout_ai", "log_workout/{workoutType}", "daily_calories",
                            "add_food_to_meal/{mealType}", "group_challenges/{groupId}", "challenge_participants/{challengeId}/{challengeName}"
                        )
                        if (showBottomBar) {
                            AppBottomNavigationBar(navController, currentRoute ?: "dashboard")
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                    composable("dashboard") {
                        DashboardScreen(
                            appPadding = innerPadding,
                            onNavigateToNotifications = { navController.navigate("notifications") },
                            onNavigateToProfile = { navController.navigate("profile") },
                            onNavigateToNutrition = { navController.navigate("nutrition") },
                            onNavigateToDiary = { navController.navigate("diary") },
                            onNavigateToWorkout = { navController.navigate("workout") },
                            onNavigateToDailyCalories = { navController.navigate("daily_calories") },
                            onNavigateToStepDetails = { 
                                val intent = Intent(this@DashboardActivity, StepsActivity::class.java)
                                startActivity(intent)
                            },
                            onNavigateToWeightTracker = { navController.navigate("weight_tracker") },
                            onNavigateToCaloriesDetails = { navController.navigate("calories_burned_details") },
                            onNavigateToScan = { navController.navigate("ai_food_scan") },
                            fAiVM = viewModel<AdaptiveAiManager>()
                        )
                    }
                    composable("calories_burned_details") {
                        CaloriesBurnedDetailScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("step_details") {
                        StepDetailScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("weight_tracker") {
                        WeightTrackerScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("nutrition") {
                        NutritionScreen(
                            onBack = { navController.popBackStack() }, 
                            appPadding = innerPadding,
                            onNavigateToProgress = { navController.navigate("stats") },
                            onNavigateToSettings = { navController.navigate("goal_settings") }
                        )
                    }
                    composable("notifications") {
                        NotificationsScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("diary") {
                        DiaryScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToSearch = { navController.navigate("search_food/Today") },
                            onNavigateToScan = { navController.navigate("ai_food_scan") },
                            onNavigateToBarcode = { navController.navigate("barcode_scan") },
                            onNavigateToCustomFood = { navController.navigate("custom_food") },
                            onNavigateToAddFoodToMeal = { mealType -> navController.navigate("search_food/$mealType") },
                            appPadding = innerPadding,
                            viewModel = diaryViewModel
                        )
                    }
                    composable("workout") {
                        WorkoutScreen(
                            navController = navController,
                            appPadding = innerPadding
                        )
                    }
                    composable("workout_history") {
                        WorkoutHistoryScreen(onBack = { navController.popBackStack() })
                    }
                    composable("workout_ai") {
                        WorkoutAIScreen(onBack = { navController.popBackStack() })
                    }
                    composable("log_workout/{workoutType}") { backStackEntry ->
                        val workoutType = backStackEntry.arguments?.getString("workoutType") ?: "chest"
                        LogWorkoutScreen(
                            workoutType = workoutType,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("stats") {
                        StatsScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("achievements") {
                        AchievementsScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("profile") {
                        ProfileScreen(
                            appPadding = innerPadding,
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                com.simats.myfitnessbuddy.data.local.SettingsManager.authToken = null
                                com.simats.myfitnessbuddy.data.local.SettingsManager.userId = ""
                                com.simats.myfitnessbuddy.data.local.SettingsManager.refreshToken = null
                                val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            },
                            profileViewModel = profileViewModel,
                            onNavigateToNotifications = { navController.navigate("notifications_settings") },
                            onNavigateToEditProfile = { navController.navigate("edit_profile") },
                            onNavigateToGoalSettings = { navController.navigate("goal_settings") },
                            onNavigateToPrivacy = { navController.navigate("privacy_security") },
                            onNavigateToHelp = { navController.navigate("help_support") },
                            onNavigateToStats = { navController.navigate("stats") },
                            onNavigateToAchievements = { navController.navigate("achievements") }
                        )
                    }
                    composable("edit_profile") {
                        EditProfileScreen(
                            appPadding = innerPadding,
                            onBack = { navController.popBackStack() },
                            viewModel = profileViewModel
                        )
                    }
                    composable("goal_settings") {
                        GoalSettingsScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("notifications_settings") {
                        NotificationSettingsScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("privacy_security") {
                        PrivacySecurityScreen(
                            onBack = { navController.popBackStack() }, 
                            onAccountDeleted = {
                                com.simats.myfitnessbuddy.data.local.SettingsManager.authToken = null
                                val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            },
                            appPadding = innerPadding
                        )
                    }
                    composable("help_support") {
                        HelpSupportScreen(
                            onBack = { navController.popBackStack() }, 
                            appPadding = innerPadding,
                            onNavigateToGuide = { navController.navigate("getting_started") },
                            onNavigateToWebView = { title, url ->
                                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                navController.navigate("webview/$title/$encodedUrl")
                            }
                        )
                    }
                    composable("getting_started") {
                        GettingStartedScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        "webview/{title}/{url}",
                        arguments = listOf(
                            navArgument("title") { type = NavType.StringType },
                            navArgument("url") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val title = backStackEntry.arguments?.getString("title") ?: "Page"
                        val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                        val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
                        WebViewScreen(
                            title = title,
                            url = url,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("friends") {
                        FriendsScreen(
                            appPadding = innerPadding,
                            onBack = { navController.popBackStack() },
                            onNavigateToAddFriends = { navController.navigate("add_friends") },
                            onNavigateToCreateGroup = { navController.navigate("create_group") },
                            onNavigateToGroupDetail = { groupId -> navController.navigate("group_detail/$groupId") },
                            onNavigateToCompareStats = { id, name -> navController.navigate("compare_stats/$id/$name") }
                        )
                    }
                    composable("add_friends") {
                        AddFriendsScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("create_group") {
                        CreateGroupScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("group_detail/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        GroupDetailScreen(
                            appPadding = innerPadding,
                            groupId = groupId, 
                            onBack = { navController.popBackStack() },
                            onNavigateToCreateChallenge = { id -> navController.navigate("create_challenge/$id") },
                            onNavigateToGroupChat = { id, name -> navController.navigate("group_chat/$id/$name") },
                            onNavigateToGroupLeaderboard = { id -> navController.navigate("group_leaderboard/$id") },
                            onNavigateToGroupChallenges = { id -> navController.navigate("group_challenges/$id") },
                            onNavigateToInviteFriends = { navController.navigate("invite_to_group/$groupId") }
                        )
                    }
                    composable("compare_stats/{friendId}/{friendName}") { backStackEntry ->
                        val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                        val friendName = backStackEntry.arguments?.getString("friendName") ?: ""
                        CompareStatsScreen(
                            friendId = friendId,
                            friendName = friendName,
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding
                        )
                    }
                    composable("group_leaderboard/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        GroupLeaderboardScreen(
                            groupId = groupId,
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding
                        )
                    }
                    composable("group_challenges/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        GroupChallengesScreen(
                            groupId = groupId,
                            onBack = { navController.popBackStack() },
                            onNavigateToParticipants = { challengeId -> 
                                // Ideally we'd pass the name here too, but for now we'll fetch or pass placeholder
                                navController.navigate("challenge_participants/$challengeId/Challenge")
                            },
                            appPadding = innerPadding
                        )
                    }
                    composable("challenge_participants/{challengeId}/{challengeName}") { backStackEntry ->
                        val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
                        val challengeName = backStackEntry.arguments?.getString("challengeName") ?: "Challenge"
                        ChallengeParticipantsScreen(
                            challengeId = challengeId,
                            challengeName = challengeName,
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding
                        )
                    }
                    composable("group_chat/{groupId}/{groupName}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
                        GroupChatScreen(
                            groupId = groupId,
                            groupName = groupName,
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding
                        )
                    }
                    composable("invite_to_group/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        InviteFriendsScreen(
                            groupId = groupId,
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding
                        )
                    }
                    composable("create_challenge/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        CreateChallengeScreen(appPadding = innerPadding, groupId = groupId, onBack = { navController.popBackStack() })
                    }
                    composable("ai_coach") {
                        AICoachScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("search_food/{mealType}") { backStackEntry ->
                        val mealType = backStackEntry.arguments?.getString("mealType") ?: "Breakfast"
                        SearchFoodScreen(
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding,
                            initialMealType = mealType,
                            viewModel = diaryViewModel
                        )
                    }
                    composable("barcode_scan") {
                        BarcodeScanScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = diaryViewModel
                        )
                    }
                    composable("custom_food") {
                        CustomFoodScreen(
                            onBack = { navController.popBackStack() },
                            onFoodAdded = { mealType, name, cals, macros ->
                                diaryViewModel.addFoodToMeal(mealType, FoodEntry(name, cals, macros))
                            }
                        )
                    }
                    composable("weekly_schedule") {
                        WeeklyWorkoutScheduleScreen(navController = navController)
                    }
                    composable("workout_detail/{workoutType}") { backStackEntry ->
                        val workoutType = backStackEntry.arguments?.getString("workoutType")
                        WorkoutDetailScreen(
                            workoutType = workoutType,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("ai_food_scan") {
                        AIFoodScanScreen(onBack = { navController.popBackStack() }, appPadding = innerPadding)
                    }
                    composable("daily_calories") {
                        DailyCaloriesScreen(
                            onBack = { navController.popBackStack() },
                            appPadding = innerPadding
                        )
                    }
                }
            }
        }
    }
    
    private fun checkPermissionsAndStartService() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            startStepCounterService()
        } else {
            // Only toast if we haven't requested before and it's not for nothing
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            // Toast removed to avoid clutter
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Failed to start step tracking: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            Log.e("DashboardActivity", "Service start error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.simats.myfitnessbuddy.data.remote.WebSocketManager.disconnect()
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController, currentRoute: String) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier.height(80.dp)
        ) {
            NavigationBarItem(
                icon = { Icon(imageVector = Icons.Outlined.Home, contentDescription = "Home") },
                label = { Text("Home", fontSize = 11.sp) },
                selected = currentRoute == "dashboard",
                onClick = {
                    if (currentRoute != "dashboard") {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF22C55E),
                    selectedTextColor = Color(0xFF22C55E),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(imageVector = Icons.Outlined.RestaurantMenu, contentDescription = "Food") },
                label = { Text("Food", fontSize = 11.sp) },
                selected = currentRoute == "diary",
                onClick = {
                    if (currentRoute != "diary") {
                        navController.navigate("diary") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF22C55E),
                    selectedTextColor = Color(0xFF22C55E),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = if (currentRoute == "friends" || currentRoute == "add_friends" || currentRoute == "create_group" || currentRoute.startsWith("group_detail")) Icons.Default.People else Icons.Outlined.PeopleOutline, 
                        contentDescription = "Friends"
                    ) 
                },
                label = { Text("Friends", fontSize = 11.sp) },
                selected = currentRoute == "friends" || currentRoute == "add_friends" || currentRoute == "create_group" || currentRoute.startsWith("group_detail"),
                onClick = {
                    if (currentRoute != "friends") {
                        navController.navigate("friends") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF22C55E),
                    selectedTextColor = Color(0xFF22C55E),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = if (currentRoute == "ai_coach") Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                        contentDescription = "AI Coach"
                    ) 
                },
                label = { Text("AI", fontSize = 11.sp) },
                selected = currentRoute == "ai_coach",
                onClick = {
                    if (currentRoute != "ai_coach") {
                        navController.navigate("ai_coach") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF22C55E),
                    selectedTextColor = Color(0xFF22C55E),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.PersonOutline, contentDescription = "Profile") },
                label = { Text("Profile", fontSize = 11.sp) },
                selected = currentRoute in listOf("profile", "stats", "achievements", "edit_profile", "goal_settings", "notifications_settings", "privacy_security", "help_support"),
                onClick = {
                    if (currentRoute != "profile") {
                        navController.navigate("profile") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF22C55E),
                    selectedTextColor = Color(0xFF22C55E),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
