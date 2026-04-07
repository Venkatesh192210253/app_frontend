package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- Models ---

data class FriendRequestModel(
    val id: String,
    val name: String,
    val username: String,
    val initials: String,
    val mutualFriends: Int,
    val level: Int
)

data class SuggestedFriendModel(
    val id: String,
    val name: String,
    val username: String,
    val initials: String,
    val mutualFriends: Int,
    val level: Int,
    val isRequested: Boolean = false
)

// --- ViewModel ---

class AddFriendsViewModel : ViewModel() {
    private val _requests = MutableStateFlow<List<FriendRequestModel>>(emptyList())
    val requests: StateFlow<List<FriendRequestModel>> = _requests.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SuggestedFriendModel>>(emptyList())
    val suggestions: StateFlow<List<SuggestedFriendModel>> = _suggestions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadRequests()
    }

    private fun loadRequests() {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            try {
                val res = RetrofitClient.apiService.getFriendRequests()
                if (res.isSuccessful) {
                    _requests.value = res.body()?.received_requests?.map { it.toModel() } ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 3) {
            searchUsers(query)
        } else {
            _suggestions.value = emptyList()
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            try {
                val res = RetrofitClient.apiService.searchUsers(query)
                if (res.isSuccessful) {
                    _suggestions.value = res.body()?.map { it.toSuggestedModel() } ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun acceptRequest(id: String) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            try {
                val res = RetrofitClient.apiService.acceptFriendRequest(mapOf("request_id" to id))
                if (res.isSuccessful) {
                    _requests.value = _requests.value.filter { it.id != id }
                }
            } catch (e: Exception) {}
        }
    }

    fun declineRequest(id: String) {
         _requests.value = _requests.value.filter { it.id != id }
    }

    fun addFriend(id: String) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            try {
                android.util.Log.d("AddFriends", "Sending friend request to $id")
                val res = RetrofitClient.apiService.sendFriendRequest(mapOf("receiver_id" to id))
                if (res.isSuccessful) {
                    android.util.Log.d("AddFriends", "Request sent successfully")
                    _suggestions.value = _suggestions.value.map {
                        if (it.id == id) it.copy(isRequested = true) else it
                    }
                } else {
                    android.util.Log.e("AddFriends", "Failed to send request: ${res.code()} ${res.message()} body: ${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AddFriends", "Exception sending request", e)
            }
        }
    }
}

// Mapper Extensions
fun com.simats.myfitnessbuddy.data.remote.FriendRequestItem.toModel() = FriendRequestModel(
    id = id,
    name = sender.profile?.full_name ?: sender.username,
    username = "@${sender.username}",
    initials = (sender.profile?.full_name ?: sender.username).take(1).uppercase(),
    mutualFriends = 0,
    level = sender.profile?.level ?: 1
)

fun com.simats.myfitnessbuddy.data.remote.UserData.toSuggestedModel() = SuggestedFriendModel(
    id = id.toString(),
    name = profile?.full_name ?: username,
    username = "@$username",
    initials = (profile?.full_name ?: username).take(1).uppercase(),
    mutualFriends = 0,
    level = profile?.level ?: 1
)

// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendsScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    viewModel: AddFriendsViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Add Friends", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Grow your fitness network", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = Color(0xFFF4F6FA),
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = appPadding.calculateBottomPadding())
            ) 
        }
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
            // 3. Search Section
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(50.dp))
                        .background(Color.White, RoundedCornerShape(50.dp)),
                    placeholder = { Text("Search by username...", color = Color.Gray) },
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

            // 4. Friend Requests Section
            if (requests.isNotEmpty()) {
                item {
                    Text("Friend Requests (${requests.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }
                items(requests) { request ->
                    RequestCard(
                        request = request,
                        onAccept = { viewModel.acceptRequest(request.id) },
                        onDecline = { viewModel.declineRequest(request.id) }
                    )
                }
            }

            // 5. Suggested For You Section
            item {
                Text("Suggested for You", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }
            items(suggestions) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onAdd = { viewModel.addFriend(suggestion.id) }
                )
            }

            // 6. Share Your Profile Card
            item {
                ShareProfileCard(onShare = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Hey! Join me on MyFitnessBuddy to track our fitness goals together! My profile: https://myfitnessbuddy.com/profile/user")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Profile via"))
                })
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun RequestCard(
    request: FriendRequestModel,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(request.initials, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(request.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(request.username, color = Color.Gray, fontSize = 12.sp)
                    Text("${request.mutualFriends} mutual friends • Level ${request.level}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Decline", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: SuggestedFriendModel,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(suggestion.initials, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(suggestion.username, color = Color.Gray, fontSize = 12.sp)
                Text("ID: ${suggestion.id}", color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                Text("${suggestion.mutualFriends} mutual friends • Level ${suggestion.level}", color = Color.Gray, fontSize = 11.sp)
            }
            
            if (suggestion.isRequested) {
                Surface(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        "Requested",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            } else {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", color = Color(0xFF166534), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShareProfileCard(onShare: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Share Your Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF166534))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Invite friends to join you on your fitness journey",
                fontSize = 13.sp,
                color = Color(0xFF166534).copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, tint = Color(0xFF22C55E))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Profile Link", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
            }
        }
    }
}
