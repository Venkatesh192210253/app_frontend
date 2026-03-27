package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.myfitnessbuddy.data.local.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// --- Models & ViewModel ---

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val username: String = "",
    val bio: String = "",
    val initials: String = "",
    val level: Int = 1,
    val isPro: Boolean = false,
    val streak: Int = 0,
    val achievements: Int = 0,
    val xp: Int = 0,
    val profilePictureUri: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow(UserProfile(
        name = SettingsManager.fullName,
        username = SettingsManager.userName,
        email = SettingsManager.userEmail,
        initials = if (SettingsManager.fullName.isNotEmpty()) {
            SettingsManager.fullName.split(" ").filter { it.isNotEmpty() }.take(2).map { it.take(1).uppercase() }.joinToString("")
        } else if (SettingsManager.userName.isNotEmpty()) {
            SettingsManager.userName.take(1).uppercase()
        } else "U",
        profilePictureUri = SettingsManager.profilePictureUri
    ))
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getProfile()
                if (response.isSuccessful) {
                    val profile = response.body() ?: return@launch
                    _userProfile.update { current ->
                        val updatedFullName = profile.full_name ?: current.name
                        val updatedUsername = profile.username ?: current.username
                        val updatedEmail = profile.email ?: current.email
                        
                        // Persist to local storage
                        SettingsManager.fullName = updatedFullName
                        SettingsManager.userName = updatedUsername
                        SettingsManager.userEmail = updatedEmail
                        
                        current.copy(
                            name = updatedFullName,
                            username = updatedUsername,
                            age = profile.age?.toString() ?: current.age,
                            gender = profile.gender ?: current.gender,
                            height = profile.height_cm?.toString() ?: current.height,
                            weight = profile.current_weight?.toString() ?: current.weight,
                            bio = profile.profile?.bio ?: current.bio,
                            email = updatedEmail,
                            phone = profile.phone_number ?: current.phone,
                            achievements = profile.profile?.achievements_count ?: 0,
                            streak = profile.profile?.streak ?: 0,
                            level = profile.profile?.level ?: 1,
                            xp = profile.profile?.xp ?: 0,
                            initials = if (updatedFullName.isNotEmpty()) {
                                updatedFullName.split(" ").filter { it.isNotEmpty() }.take(2).map { it.take(1).uppercase() }.joinToString("")
                            } else if (updatedUsername.isNotEmpty()) {
                                updatedUsername.take(1).uppercase()
                            } else "U",
                            profilePictureUri = profile.profile_image ?: profile.profile?.profile_photo?.let { 
                                if (it.startsWith("http")) it else RetrofitClient.BASE_URL.removeSuffix("/") + it 
                            } ?: current.profilePictureUri
                        )
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(updated: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profileData = com.simats.myfitnessbuddy.data.remote.ProfileResponse(
                    full_name = updated.name,
                    username = updated.username,
                    bio = updated.bio,
                    age = updated.age.toIntOrNull(),
                    gender = updated.gender,
                    height_cm = updated.height.toFloatOrNull(),
                    current_weight = updated.weight.toFloatOrNull(),
                )
                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.updateProfile(profileData)
                if (response.isSuccessful) {
                    _userProfile.value = updated
                    // Persist to local storage
                    SettingsManager.fullName = updated.name
                    SettingsManager.userName = updated.username
                    SettingsManager.userEmail = updated.email
                    SettingsManager.profilePictureUri = updated.profilePictureUri
                } else {
                    _error.value = "Failed to save profile"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfileWithImage(context: Context, updated: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fullNameBody = updated.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val bioBody = updated.bio.toRequestBody("text/plain".toMediaTypeOrNull())
                
                var imagePart: MultipartBody.Part? = null
                updated.profilePictureUri?.let { uriString ->
                    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                        val uri = Uri.parse(uriString)
                        val file = getFileFromUri(context, uri)
                        if (file != null) {
                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            imagePart = MultipartBody.Part.createFormData("profile_photo", file.name, requestFile)
                        }
                    }
                }

                val response = RetrofitClient.apiService.updateProfileMultipart(
                    fullName = fullNameBody,
                    bio = bioBody,
                    profile_photo = imagePart
                )

                if (response.isSuccessful) {
                    val serverProfile = response.body()
                    val finalProfile = updated.copy(
                        profilePictureUri = serverProfile?.profile_image ?: serverProfile?.profile?.profile_photo ?: updated.profilePictureUri
                    )
                    _userProfile.value = finalProfile
                    SettingsManager.fullName = finalProfile.name
                    SettingsManager.userName = finalProfile.username
                    SettingsManager.profilePictureUri = finalProfile.profilePictureUri
                } else {
                    _error.value = "Failed to update profile image: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "temp_profile_photo.jpg")
        val outputStream = FileOutputStream(file)
        try {
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            return null
        }
    }
}

// --- Main Profile Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToGoalSettings: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel<ProfileViewModel>(),
    notificationsViewModel: NotificationsViewModel = viewModel<NotificationsViewModel>()
) {
    val userProfile by profileViewModel.userProfile.collectAsState()
    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        profileViewModel.fetchProfile()
        notificationsViewModel.loadNotifications()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout from My Fitness Buddy?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAF9)
    ) { innerPadding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1000)) + expandVertically(),
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { ProfileHeaderCard(userProfile) }
                item { StatsRow(userProfile, onNavigateToStats, onNavigateToAchievements) }
                item {
                    Text(
                        "Account Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                item { SettingsSection(if (unreadCount > 0) unreadCount.toString() else null, onNavigateToNotifications, onNavigateToEditProfile, onNavigateToGoalSettings, onNavigateToPrivacy, onNavigateToHelp) }
                item { BottomActionCards(onNavigateToStats, onNavigateToAchievements) }
                item { LogoutButton { showLogoutDialog = true } }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(user: UserProfile) {
    val nextLevelXp = (user.level * 1000) * 1.2f // Simple formula for now
    val progress = (user.xp.toFloat() / nextLevelXp).coerceIn(0f, 1f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color(0xFFC8E6C9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user.profilePictureUri.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = user.profilePictureUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = user.initials,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PillBadge("Level ${user.level}", Color(0xFF4CAF50), Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                    val nextLevelXp = (user.level * 1000)
                    val currentLevelXp = (user.level - 1) * 1000
                    val progress = ((user.xp - currentLevelXp).toFloat() / 1000f).coerceIn(0f, 1f)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("XP Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text("${user.xp}/${nextLevelXp} XP", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF22C55E),
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
            }
        }
    }
}

@Composable
fun PillBadge(text: String, bgColor: Color, textColor: Color, icon: ImageVector? = null) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(14.dp), tint = textColor) }
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun StatsRow(user: UserProfile, onNavigateToStats: () -> Unit, onNavigateToAchievements: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(Icons.Default.LocalFireDepartment, user.streak.toString(), "Day Streak", Color(0xFF4CAF50), onNavigateToStats)
        StatItem(Icons.Default.EmojiEvents, user.achievements.toString(), "Achievements", Color(0xFF2196F3), onNavigateToAchievements)
        StatItem(Icons.Default.Star, "Lvl ${user.level}", "Level", Color(0xFFFFB300), {})
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun SettingsSection(unreadBadge: String?, onNavigateToNotifications: () -> Unit, onNavigateToEditProfile: () -> Unit, onNavigateToGoalSettings: () -> Unit, onNavigateToPrivacy: () -> Unit, onNavigateToHelp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            SettingsRow(Icons.Default.Person, "Edit Profile", onClick = onNavigateToEditProfile) 
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            SettingsRow(Icons.Default.Settings, "Goal Settings", onClick = onNavigateToGoalSettings) 
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            SettingsRow(Icons.Default.Notifications, "Notifications", badge = unreadBadge, onClick = onNavigateToNotifications)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            SettingsRow(Icons.Default.Lock, "Privacy & Security", onClick = onNavigateToPrivacy) 
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            SettingsRow(Icons.Default.Help, "Help & Support", onClick = onNavigateToHelp) 
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, badge: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        if (badge != null) {
            Surface(
                color = if (badge == "Pro") Color(0xFFFFD700) else Color(0xFF4CAF50),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = badge,
                    color = if (badge == "Pro") Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun BottomActionCards(onNavigateToStats: () -> Unit, onNavigateToAchievements: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionCard(Icons.Default.BarChart, "View Stats", Modifier.weight(1f), onNavigateToStats)
        ActionCard(Icons.Default.EmojiEvents, "Achievements", Modifier.weight(1f), onNavigateToAchievements)
    }
}

@Composable
fun ActionCard(icon: ImageVector, text: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
        border = BorderStroke(1.dp, Color.Red)
    ) {
        Icon(Icons.Default.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Logout", fontWeight = FontWeight.Bold)
    }
}
