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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import com.simats.myfitnessbuddy.data.remote.FriendResponse
import com.simats.myfitnessbuddy.data.remote.GroupResponse
import com.simats.myfitnessbuddy.data.local.SettingsManager

// --- Models ---

data class FriendModel(
    val id: String,
    val name: String,
    val username: String,
    val initials: String,
    val dayStreak: Int,
    val level: Int,
    val workouts: Int,
    val status: String // "Ahead" or "Behind"
)

data class GroupModel(
    val id: String,
    val name: String,
    val members: Int,
    val activeMembers: Int,
    val unreadCount: Int,
    val activeChallenge: String
)


// --- ViewModel ---

class FriendsViewModel : ViewModel() {
    private val _friends = MutableStateFlow<List<FriendModel>>(emptyList())
    val friends: StateFlow<List<FriendModel>> = _friends.asStateFlow()

    private val _userRank = MutableStateFlow(0)
    val userRank: StateFlow<Int> = _userRank.asStateFlow()

    private val _groups = MutableStateFlow<List<GroupModel>>(emptyList())
    val groups: StateFlow<List<GroupModel>> = _groups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendRequestModel>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequestModel>> = _pendingRequests.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SuggestedFriendModel>>(emptyList())
    val suggestions: StateFlow<List<SuggestedFriendModel>> = _suggestions.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                // Fetch Profile
                val profileRes = RetrofitClient.apiService.getProfile()
                var userProfile: com.simats.myfitnessbuddy.data.remote.UserProfileResponse? = null
                if (profileRes.isSuccessful) {
                    userProfile = profileRes.body()
                }

                // Fetch Friends
                val friendsRes = RetrofitClient.apiService.getFriends()
                if (friendsRes.isSuccessful) {
                    val data = friendsRes.body()
                    var friendsList = data?.friends?.map { it.toModel() } ?: emptyList()

                    if (userProfile != null) {
                        val profile = userProfile.profile
                        val userScore = ((profile?.level ?: 1) * 100) + ((profile?.workouts_completed ?: 0) * 10) + (profile?.streak ?: 0)
                        
                        friendsList = friendsList.map { friend ->
                            val friendScore = (friend.level * 100) + (friend.workouts * 10) + friend.dayStreak
                            val status = if (userScore > friendScore) "Behind" else "Ahead"
                            friend.copy(status = status)
                        }.sortedByDescending { (it.level * 100) + (it.workouts * 10) + it.dayStreak }
                        
                        val allScores = friendsList.map { (it.level * 100) + (it.workouts * 10) + it.dayStreak } + userScore
                        val sortedScores = allScores.sortedDescending()
                        _userRank.value = sortedScores.indexOf(userScore) + 1
                    } else {
                        _userRank.value = 0
                    }
                    _friends.value = friendsList
                }

                // Fetch Groups
                val groupsRes = RetrofitClient.apiService.getGroups()
                if (groupsRes.isSuccessful) {
                    _groups.value = groupsRes.body()?.map { it.toModel() } ?: emptyList()
                }

                // Fetch Friend Requests
                val requestsRes = RetrofitClient.apiService.getFriendRequests()
                if (requestsRes.isSuccessful) {
                    _pendingRequests.value = requestsRes.body()?.received_requests?.map { it.toModel() } ?: emptyList()
                }

                // Fetch Suggestions
                val suggestionsRes = RetrofitClient.apiService.getSuggestedFriends()
                if (suggestionsRes.isSuccessful) {
                    _suggestions.value = suggestionsRes.body()?.map { it.toSuggestedModel() } ?: emptyList()
                }
            } catch (e: Exception) {
                // Error handled gracefully
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun startBattle(friend: FriendModel) {
        viewModelScope.launch {
             val token = SettingsManager.authToken ?: ""
             RetrofitClient.apiService.sendFriendRequest(mapOf("receiver_id" to friend.id))
             loadData()
             _isLoading.value = true
             try {
                 RetrofitClient.apiService.sendFriendRequest(mapOf("receiver_id" to friend.id))
                 loadData()
             } catch (e: Exception) {
                 // Error handled gracefully
             } finally {
                 _isLoading.value = false
             }
        }
    }

    fun acceptRequest(id: String) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val res = RetrofitClient.apiService.acceptFriendRequest(mapOf("request_id" to id))
                if (res.isSuccessful) {
                    loadData()
                }
            } catch (e: Exception) {
                // Error handled gracefully
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun declineRequest(id: String) {
        _pendingRequests.value = _pendingRequests.value.filter { it.id != id }
    }

    fun sendRequest(userId: String) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                android.util.Log.d("FriendsScreen", "Sending request to $userId")
                val res = RetrofitClient.apiService.sendFriendRequest(mapOf("receiver_id" to userId))
                if (res.isSuccessful) {
                    android.util.Log.d("FriendsScreen", "Request sent successfully")
                    _suggestions.value = _suggestions.value.map {
                        if (it.id == userId) it.copy(isRequested = true) else it
                    }
                } else {
                    android.util.Log.e("FriendsScreen", "Failed to send request: ${res.code()} ${res.message()} body: ${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FriendsScreen", "Exception sending request", e)
            }
        }
    }
}

// Mapper Extensions
fun FriendResponse.toModel() = FriendModel(
    id = id.toString(),
    name = profile?.full_name ?: username,
    username = "@$username",
    initials = profile?.full_name?.let { fn ->
        if (fn.isNotEmpty()) {
            fn.split(" ").filter { it.isNotEmpty() }.take(2).map { it.take(1).uppercase() }.joinToString("")
        } else null
    } ?: username.take(1).uppercase(),
    dayStreak = profile?.streak ?: 0,
    level = profile?.level ?: 1,
    workouts = profile?.workouts_completed ?: 0,
    status = "Tied"
)

fun GroupResponse.toModel() = GroupModel(
    id = id.toString(),
    name = name,
    members = member_count,
    activeMembers = member_count,
    unreadCount = unread_count,
    activeChallenge = active_challenge ?: "No active challenge"
)


// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onNavigateToAddFriends: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToCompareStats: (String, String) -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(state) {
        if (state == Lifecycle.State.RESUMED) {
            viewModel.loadData()
        }
    }

    val friends by viewModel.friends.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userRank by viewModel.userRank.collectAsState()

    val filteredFriends = friends.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = { 
                        Text("Friends", fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF22C55E))
                        }
                    },
                    actions = {
                        Button(
                            onClick = onNavigateToAddFriends,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            // 0. Pending Requests Notification
            if (pendingRequests.isNotEmpty()) {
                item {
                    Text(
                        "Pending Requests (${pendingRequests.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
                items(pendingRequests) { request ->
                    RequestCard(
                        request = request,
                        onAccept = { viewModel.acceptRequest(request.id) },
                        onDecline = { viewModel.declineRequest(request.id) }
                    )
                }
            }

            // 2. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(50.dp))
                        .background(Color.White, RoundedCornerShape(50.dp)),
                    placeholder = { Text("Search friends...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // 3. Your Rank Card
            item {
                RankCard(userRank)
            }

            // 4. Your Friends Section
            item {
                Text("Your Friends", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }

            items(filteredFriends) { friend ->
                FriendCard(friend, onCompare = { onNavigateToCompareStats(friend.id, friend.name) })
            }

            // 4.5 Suggested Friends Section
            if (suggestions.isNotEmpty()) {
                item {
                   Text("Friend Suggestions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }
                
                items(suggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAdd = { viewModel.sendRequest(suggestion.id) }
                    )
                }
            }

            // 5. Your Groups Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Groups", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Text(
                        "Create",
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToCreateGroup() }
                    )
                }
            }

            items(groups) { group ->
                GroupCard(
                    group = group,
                    onClick = { onNavigateToGroupDetail(group.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RankCard(rank: Int) {
    var rankVisible by remember { mutableStateOf(false) }
    val animatedRank by animateIntAsState(
        targetValue = if (rankVisible) rank else 0,
        animationSpec = tween(durationMillis = 1000),
        label = "rankAnimation"
    )

    LaunchedEffect(rank) { 
        if (rank > 0) rankVisible = true 
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Your Rank", fontSize = 14.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                Text("#$animatedRank", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                Text("Among friends", fontSize = 12.sp, color = Color(0xFF166534).copy(alpha = 0.7f))
            }
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendCard(friend: FriendModel, onCompare: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "pressEffect")

    Card(
        onClick = { /* Friend Detail */ },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(friend.initials, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(friend.username, color = Color.Gray, fontSize = 12.sp)
                }
                
                // Status Badge
                StatusBadge(friend.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FriendStatItem("${friend.dayStreak}", "Day Streak")
                FriendStatItem("Lvl ${friend.level}", "Level")
                FriendStatItem("${friend.workouts}", "Workouts")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCompare,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = Color(0xFF22C55E))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compare Stats", color = Color(0xFF1F2937))
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val isAhead = status == "Ahead"
    val bgColor = if (isAhead) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val textColor = if (isAhead) Color(0xFF166534) else Color(0xFF991B1B)
    val label = if (status == "Tied") "Tied" else if (isAhead) "Ahead" else "Behind"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            label,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FriendStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(group: GroupModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${group.members} members • ${group.activeMembers} active", color = Color.Gray, fontSize = 12.sp)
                }
                
                if (group.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (group.unreadCount > 99) "99+" else group.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Active Challenge: ${group.activeChallenge}",
                        color = Color(0xFF166534),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

