package com.simats.myfitnessbuddy

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

import com.simats.myfitnessbuddy.data.remote.InviteToGroupRequest
import com.simats.myfitnessbuddy.data.local.SettingsManager

class InviteFriendsViewModel : ViewModel() {
    private val _friends = MutableStateFlow<List<FriendResponseModel>>(emptyList())
    val friends: StateFlow<List<FriendResponseModel>> = _friends.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Add search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Track who has been invited successfully
    private val _invitedIds = MutableStateFlow<Set<String>>(emptySet())
    val invitedIds: StateFlow<Set<String>> = _invitedIds.asStateFlow()
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadFriends() {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val res = RetrofitClient.apiService.getFriends()
                if (res.isSuccessful) {
                    val friendsList = res.body()?.friends ?: emptyList()
                    _friends.value = friendsList.map { f ->
                        FriendResponseModel(
                            id = f.id,
                            name = f.profile?.full_name ?: f.username,
                            username = f.username,
                            initials = f.profile?.full_name?.let { fn ->
                                if (fn.isNotEmpty()) {
                                    fn.split(" ").filter { it.isNotEmpty() }.take(2).map { it.take(1).uppercase() }.joinToString("")
                                } else null
                            } ?: f.username.take(1).uppercase()
                        )
                    }
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inviteFriend(groupId: String, friendId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val req = InviteToGroupRequest(group_id = groupId, user_id = friendId)
                val res = RetrofitClient.apiService.inviteToGroup(req)
                if (res.isSuccessful) {
                    _invitedIds.value = _invitedIds.value + friendId
                    onResult(true, "Invited successfully")
                } else {
                    onResult(false, "Failed: Already in group or invited")
                }
            } catch (e: Exception) {
                onResult(false, "Network error")
            }
        }
    }
}

data class FriendResponseModel(
    val id: String,
    val name: String,
    val username: String,
    val initials: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsScreen(
    groupId: String,
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: InviteFriendsViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val invitedIds by viewModel.invitedIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val filteredFriends = friends.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            viewModel.loadFriends()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Friends", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = Modifier.padding(appPadding)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAFAFA))
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(50.dp)),
                    placeholder = { Text("Search friends...", color = Color.Gray) },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C896),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
            }

            if (isLoading && friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00C896))
                }
            } else if (friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No friends available to invite.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredFriends) { friend ->
                        val isInvited = invitedIds.contains(friend.id)
                        FriendInviteCard(
                            friend = friend,
                            isInvited = isInvited,
                            onInviteClick = {
                                viewModel.inviteFriend(groupId, friend.id) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendInviteCard(
    friend: FriendResponseModel,
    isInvited: Boolean,
    onInviteClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.initials,
                    color = Color(0xFF00C896),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "@${friend.username}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Button(
                onClick = onInviteClick,
                enabled = !isInvited,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInvited) Color.LightGray else Color(0xFF00C896)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isInvited) {
                    Icon(Icons.Default.Check, contentDescription = "Invited", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invited")
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Invite", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invite")
                }
            }
        }
    }
}
