package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.simats.myfitnessbuddy.data.remote.GroupDetailResponse
import com.simats.myfitnessbuddy.data.remote.GroupMemberItem

// --- Models ---

data class MemberModel(
    val id: String,
    val name: String,
    val initials: String,
    val points: Int,
    val rank: Int,
    val isYou: Boolean = false
)

data class GroupActivityModel(
    val id: String,
    val text: String,
    val time: String
)

data class GroupDetailData(
    val id: String,
    val name: String,
    val memberCount: Int,
    val activeChallenge: String,
    val challengeEndDays: Int,
    val progress: Float,
    val totalWorkouts: Int,
    val totalCalories: String,
    val avgStreak: Int
)

// --- ViewModel ---

class GroupDetailViewModel : ViewModel() {
    private val _groupData = MutableStateFlow<GroupDetailData?>(null)
    val groupData: StateFlow<GroupDetailData?> = _groupData.asStateFlow()

    private val _members = MutableStateFlow<List<MemberModel>>(emptyList())
    val members: StateFlow<List<MemberModel>> = _members.asStateFlow()

    private val _activities = MutableStateFlow<List<GroupActivityModel>>(emptyList())
    val activities: StateFlow<List<GroupActivityModel>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadGroupDetail(groupId: String) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val res = RetrofitClient.apiService.getGroupDetail(groupId)
                if (res.isSuccessful) {
                    val data = res.body()
                    data?.let {
                        _groupData.value = it.toModel()
                        _members.value = it.members.map { member -> member.toModel() }
                    }
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val res = RetrofitClient.apiService.deleteGroup(groupId)
                if (res.isSuccessful) {
                    onSuccess()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Mapper Extensions
fun GroupDetailResponse.toModel() = GroupDetailData(
    id = id,
    name = name,
    memberCount = member_count,
    activeChallenge = active_challenge ?: "No active challenge",
    challengeEndDays = challenge_end_days,
    progress = progress,
    totalWorkouts = total_workouts,
    totalCalories = total_calories,
    avgStreak = avg_streak
)

fun GroupMemberItem.toModel() = MemberModel(
    id = id,
    name = name,
    initials = initials,
    points = points,
    rank = rank,
    isYou = is_you
)

// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    groupId: String,
    onBack: () -> Unit,
    onNavigateToCreateChallenge: (String) -> Unit,
    onNavigateToGroupChat: (String, String) -> Unit,
    onNavigateToGroupLeaderboard: (String) -> Unit,
    onNavigateToGroupChallenges: (String) -> Unit,
    onNavigateToInviteFriends: () -> Unit,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val groupData by viewModel.groupData.collectAsState()
    val members by viewModel.members.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            viewModel.loadGroupDetail(groupId)
        }
    }

    if (groupData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00C896))
        }
    } else {
        val data = groupData!!
        Scaffold(
            topBar = {
                Surface(shadowElevation = 2.dp) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(data.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("${data.memberCount} members", fontSize = 12.sp, color = Color.Gray)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { onNavigateToGroupChat(groupId, data.name) }) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chat", tint = Color.Gray)
                            }
                            
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Group", color = Color.Red) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deleteGroup(groupId, onSuccess = onBack)
                                        }
                                    )
                                }
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
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 3. Active Challenge Card
                item {
                    ActiveChallengeCard(
                        data = data,
                        onViewChallenge = { onNavigateToGroupChallenges(groupId) },
                        onViewLeaderboard = { onNavigateToGroupLeaderboard(groupId) }
                    )
                }
    
                // 4. Group Stats Row
                item {
                    GroupStatsCard(data)
                }
    
                // 5. Top Members Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Members", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text(
                            "View All",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToGroupLeaderboard(groupId) }
                        )
                    }
                }
    
                items(members) { member ->
                    MemberRankRow(member)
                }
    
                // 6. Recent Activity Section
                item {
                    Text("Recent Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }
    
                items(activities) { activity ->
                    ActivityItemCard(activity)
                }
    
                // 7. Bottom Action Buttons
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BottomActionCard(
                            icon = Icons.Default.AddCircleOutline, 
                            text = "New Challenge", 
                            onClick = { onNavigateToCreateChallenge(data.id) },
                            modifier = Modifier.weight(1f)
                        )
                        BottomActionCard(
                            icon = Icons.Default.PersonAddAlt, 
                            text = "Invite Friends", 
                            onClick = onNavigateToInviteFriends,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
    
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun ActiveChallengeCard(
    data: GroupDetailData,
    onViewChallenge: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(data.activeChallenge, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF166534))
                    Text("Ends in ${data.challengeEndDays} days", fontSize = 12.sp, color = Color(0xFF166534).copy(alpha = 0.7f))
                }
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Group Progress", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF166534))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { data.progress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = Color(0xFF22C55E),
                    trackColor = Color(0xFF166534).copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("${(data.progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onViewChallenge,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("View Challenge", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onViewLeaderboard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, Color(0xFF22C55E))
                ) {
                    Text("Leaderboard", color = Color(0xFF22C55E), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun GroupStatsCard(data: GroupDetailData) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GroupStatItem("${data.avgStreak}", "Group Streak")
            GroupStatItem(data.totalCalories, "Calories Burned")
        }
    }
}

@Composable
fun GroupStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1F2937))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun MemberRankRow(member: MemberModel) {
    val bgColor = if (member.isYou) Color(0xFFF3FBF5) else Color.White
    
    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${member.rank}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (member.rank <= 3) Color(0xFF22C55E) else Color.Gray,
                modifier = Modifier.width(32.dp)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(member.initials, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(member.name, fontWeight = if (member.isYou) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("${member.points} pts", fontWeight = FontWeight.Bold, color = Color(0xFF22C55E), fontSize = 14.sp)
        }
    }
}

@Composable
fun ActivityItemCard(activity: GroupActivityModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(activity.text, fontSize = 13.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
            Text(activity.time, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomActionCard(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00C896), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1F2937))
        }
    }
}
