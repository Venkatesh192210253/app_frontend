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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.myfitnessbuddy.data.remote.CreateGroupRequest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- Models ---

data class InviteFriendModel(
    val id: String,
    val name: String,
    val username: String,
    val initials: String,
    val isInvited: Boolean = false
)

// --- ViewModel ---

class CreateGroupViewModel : ViewModel() {
    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _groupGoal = MutableStateFlow("")
    val groupGoal: StateFlow<String> = _groupGoal.asStateFlow()

    private val _isPublic = MutableStateFlow(true)
    val isPublic: StateFlow<Boolean> = _isPublic.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inviteList = MutableStateFlow<List<InviteFriendModel>>(emptyList())
    val inviteList: StateFlow<List<InviteFriendModel>> = _inviteList.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    _inviteList.value = response.body()?.friends?.map { friend ->
                        InviteFriendModel(
                            id = friend.id.toString(),
                            name = friend.profile?.full_name ?: friend.username,
                            username = "@${friend.username}",
                            initials = friend.profile?.full_name?.let { fn ->
                                if (fn.isNotEmpty()) {
                                    fn.split(" ").filter { it.isNotEmpty() }.take(2).map { it.take(1).uppercase() }.joinToString("")
                                } else null
                            } ?: friend.username.take(1).uppercase()
                        )
                    } ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onGroupNameChange(value: String) { _groupName.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onGroupGoalChange(value: String) { _groupGoal.value = value }
    fun onPrivacyChange(isPublic: Boolean) { _isPublic.value = isPublic }

    fun toggleInvite(id: String) {
        _inviteList.value = _inviteList.value.map {
            if (it.id == id) it.copy(isInvited = !it.isInvited) else it
        }
    }

    fun createGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            _isLoading.value = true

            val request = CreateGroupRequest(
                name = _groupName.value,
                description = _description.value,
                goal = _groupGoal.value,
                is_public = _isPublic.value,
                invited_users = _inviteList.value.filter { it.isInvited }.map { it.id }
            )

            try {
                val response = RetrofitClient.apiService.createGroup(request)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    // Handle error message
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = viewModel()
) {
    val groupName by viewModel.groupName.collectAsState()
    val description by viewModel.description.collectAsState()
    val groupGoal by viewModel.groupGoal.collectAsState()
    val isPublic by viewModel.isPublic.collectAsState()
    val inviteList by viewModel.inviteList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Start your fitness community", fontSize = 12.sp, color = Color.Gray)
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
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { 
                            if (groupName.isNotBlank()) {
                                viewModel.createGroup(onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Group '$groupName' created successfully!")
                                        onBack()
                                    }
                                })
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C896)),
                        shape = RoundedCornerShape(50.dp),
                        enabled = !isLoading && groupName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
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
            // 3. Group Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Group Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { viewModel.onGroupNameChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Mumbai Fitness Warriors") },
                            label = { Text("Group Name") },
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { viewModel.onDescriptionChange(it) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text("What’s your group about?") },
                            label = { Text("Description") },
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4
                        )

                        OutlinedTextField(
                            value = groupGoal,
                            onValueChange = { viewModel.onGroupGoalChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Build muscle, Lose weight") },
                            label = { Text("Group Goal") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 4. Privacy Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Privacy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrivacyOption(
                                label = "Public",
                                subtitle = "Anyone can join",
                                isSelected = isPublic,
                                onClick = { viewModel.onPrivacyChange(true) },
                                modifier = Modifier.weight(1f)
                            )
                            PrivacyOption(
                                label = "Private",
                                subtitle = "Invite only",
                                isSelected = !isPublic,
                                onClick = { viewModel.onPrivacyChange(false) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Invite Friends Section
            item {
                Text("Invite Friends", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }

            items(inviteList) { friend ->
                InviteFriendRow(
                    friend = friend,
                    onToggleInvite = { viewModel.toggleInvite(friend.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PrivacyOption(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF3F4F6)
    val borderColor = if (isSelected) Color(0xFF22C55E) else Color.Transparent

    Surface(
        modifier = modifier.clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF166534) else Color.DarkGray)
            Text(subtitle, fontSize = 10.sp, color = if (isSelected) Color(0xFF166534).copy(alpha = 0.7f) else Color.Gray)
        }
    }
}

@Composable
fun InviteFriendRow(
    friend: InviteFriendModel,
    onToggleInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(friend.initials, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(friend.username, color = Color.Gray, fontSize = 11.sp)
            }
            
            if (friend.isInvited) {
                Surface(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        "Invited",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            } else {
                Button(
                    onClick = onToggleInvite,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invite", color = Color(0xFF166534), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
