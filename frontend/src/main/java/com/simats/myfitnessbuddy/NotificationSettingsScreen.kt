package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = Color(0xFFF4F6FA)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Manage your alerts", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + appPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Push Notifications Master Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    SwitchRow(
                        title = "Push Notifications",
                        subtitle = "Enable or disable all notifications",
                        checked = uiState.masterPush,
                        onCheckedChange = { viewModel.updateMasterPush(it) }
                    )
                }
            }

            // 2. Activity Reminders Section
            item {
                SettingsCategoryCard(title = "Activity Reminders") {
                    Column {
                        SwitchRow(
                            title = "Workout Reminders",
                            subtitle = "Daily alerts for your planned exercises",
                            checked = uiState.workoutReminders,
                            onCheckedChange = { viewModel.updateWorkoutReminders(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(
                            title = "Meal Logging Reminders",
                            subtitle = "Stay on track with your nutrition logging",
                            checked = uiState.mealLoggingReminders,
                            onCheckedChange = { viewModel.updateMealLoggingReminders(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(
                            title = "Water Intake Reminders",
                            subtitle = "Regular hydration alerts",
                            checked = uiState.waterIntakeReminders,
                            onCheckedChange = { viewModel.updateWaterIntakeReminders(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(
                            title = "Bedtime Reminder",
                            subtitle = "Optimize your recovery with sleep alerts",
                            checked = uiState.bedtimeReminder,
                            onCheckedChange = { viewModel.updateBedtimeReminder(it) }
                        )
                    }
                }
            }

            // 3. Social Section
            item {
                SettingsCategoryCard(title = "Social") {
                    Column {
                        SwitchRow(title = "Friend Requests", subtitle = "When someone wants to connect", checked = uiState.friendRequests, onCheckedChange = { viewModel.updateFriendRequests(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Group Invites", subtitle = "Invitations to fitness communities", checked = uiState.groupInvites, onCheckedChange = { viewModel.updateGroupInvites(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Challenge Updates", subtitle = "Progress in active competitions", checked = uiState.challengeUpdates, onCheckedChange = { viewModel.updateChallengeUpdates(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Messages", subtitle = "Direct messages from your friends", checked = uiState.messages, onCheckedChange = { viewModel.updateMessages(it) })
                    }
                }
            }

            // 4. Progress Section
            item {
                SettingsCategoryCard(title = "Progress & Achievements") {
                    Column {
                        SwitchRow(title = "Weekly Summary", subtitle = "Your weekly fitness performance report", checked = uiState.weeklySummary, onCheckedChange = { viewModel.updateWeeklySummary(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Goal Achieved", subtitle = "Celebration alerts for your success", checked = uiState.goalAchieved, onCheckedChange = { viewModel.updateGoalAchieved(it) })
                    }
                }
            }

            // 5. AI Coach Section
            item {
                SettingsCategoryCard(title = "AI Coach") {
                    Column {
                        SwitchRow(title = "AI Suggestions", subtitle = "Personalized tips based on your data", checked = uiState.aiSuggestions, onCheckedChange = { viewModel.updateAiSuggestions(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Weekly Adjustments", subtitle = "Calorie and macro goal optimizations", checked = uiState.weeklyAdjustments, onCheckedChange = { viewModel.updateWeeklyAdjustments(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Motivational Messages", subtitle = "Daily quotes to keep you moving", checked = uiState.motivationalMessages, onCheckedChange = { viewModel.updateMotivationalMessages(it) })
                    }
                }
            }

            // 6. Quiet Hours Section
            item {
                val context = LocalContext.current
                
                SettingsCategoryCard(title = "Quiet Hours") {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeInputBox(
                            label = "From", 
                            time = uiState.quietHoursFrom, 
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = uiState.quietHoursFrom.split(":")
                                TimePickerDialog(context, { _, h, m ->
                                    val formatted = String.format("%02d:%02d", h, m)
                                    viewModel.updateQuietHoursFrom(formatted)
                                }, current.getOrNull(0)?.toInt() ?: 22, current.getOrNull(1)?.toInt() ?: 0, true).show()
                            }
                        )
                        TimeInputBox(
                            label = "To", 
                            time = uiState.quietHoursTo, 
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = uiState.quietHoursTo.split(":")
                                TimePickerDialog(context, { _, h, m ->
                                    val formatted = String.format("%02d:%02d", h, m)
                                    viewModel.updateQuietHoursTo(formatted)
                                }, current.getOrNull(0)?.toInt() ?: 7, current.getOrNull(1)?.toInt() ?: 0, true).show()
                            }
                        )
                    }
                }
            }

            // Save Button (New)
            item {
                LoadingButton(
                    text = if (uiState.isSaved) "Changes Saved!" else "Save Settings",
                    isLoading = uiState.isSaving,
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                    containerColor = if (uiState.isSaved) Color(0xFF22C55E) else Color(0xFF4A6FFF),
                    icon = if (uiState.isSaved) Icons.Default.Check else null
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun SettingsCategoryCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF22C55E),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun TimeInputBox(label: String, time: String, modifier: Modifier, onClick: () -> Unit = {}) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Surface(
            color = Color(0xFFF9FAFB),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(time, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
