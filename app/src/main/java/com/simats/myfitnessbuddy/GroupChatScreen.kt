package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.simats.myfitnessbuddy.data.remote.GroupMessageResponse
import com.simats.myfitnessbuddy.data.remote.SendGroupMessageRequest
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.WebSocketManager
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class GroupChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<GroupMessageResponse>>(emptyList())
    val messages: StateFlow<List<GroupMessageResponse>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Store current userId to differentiate sent/received messages
    var currentUserId: String = ""

    fun init(groupId: String) {
        viewModelScope.launch {
            // Get current userId from profile or assuming from somewhere
            // We'll rely on checking sender_id vs RetrofitClient stored user ID
            currentUserId = SettingsManager.userId
            loadMessages(groupId)
            listenForNewMessages(groupId)
        }
    }

    private fun loadMessages(groupId: String) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            _isLoading.value = true
            try {
                val res = RetrofitClient.apiService.getGroupMessages(groupId)
                if (res.isSuccessful) {
                    _messages.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun listenForNewMessages(groupId: String) {
        viewModelScope.launch {
            WebSocketManager.events.collect { rawEvent ->
                val event = rawEvent.optJSONObject("message") ?: rawEvent
                if (event.optString("event") == "new_group_message") {
                    val data = event.optJSONObject("data")
                    if (data != null && data.optString("group_id") == groupId) {
                        val newMsg = GroupMessageResponse(
                            id = data.optString("id"),
                            sender_id = data.optString("sender_id"),
                            sender_name = data.optString("sender_name"),
                            message = data.optString("message"),
                            created_at = data.optString("created_at")
                        )
                        // Append if not already existing
                        if (_messages.value.none { it.id == newMsg.id }) {
                            _messages.value = _messages.value + newMsg
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(groupId: String, text: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val token = com.simats.myfitnessbuddy.data.local.SettingsManager.authToken ?: ""
            try {
                val req = SendGroupMessageRequest(message = text)
                val res = RetrofitClient.apiService.sendGroupMessage(groupId, req)
                if (res.isSuccessful) {
                    onSuccess()
                    loadMessages(groupId)
                }
            } catch (e: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    groupName: String,
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: GroupChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            viewModel.init(groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = Modifier.padding(appPadding).navigationBarsPadding().imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAFAFA))
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                // Group messages by date
                val groupedMessages = messages
                    .sortedBy { it.created_at } // Sort oldest first for display
                    .groupBy { getDayHeader(it.created_at) }
                
                groupedMessages.forEach { (dateHeader, msgs) ->
                    item(key = dateHeader) {
                        DateHeaderItem(text = dateHeader)
                    }
                    items(msgs, key = { it.id }) { msg ->
                        val isMe = msg.sender_id == viewModel.currentUserId
                        MessageBubble(
                            message = msg,
                            isMe = isMe
                        )
                    }
                }
            }

            // Input Area
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp, max = 120.dp),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF00C896)
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(groupId, messageText.trim())
                                messageText = ""
                            }
                        },
                        containerColor = Color(0xFF00C896),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: GroupMessageResponse, isMe: Boolean) {
    val bubbleColor = if (isMe) Color(0xFF00C896) else Color.White
    val textColor = if (isMe) Color.White else Color.Black
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Text(
                text = message.sender_name,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
            )
        }
        
        Surface(
            shape = shape,
            color = bubbleColor,
            shadowElevation = if (isMe) 0.dp else 1.dp
        ) {
            Text(
                text = message.message,
                color = textColor,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        
        // Very basic time formatter for display
        val timeDisplay = try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
            val date = parser.parse(message.created_at.replace("Z", "+0000"))
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            date?.let { formatter.format(it) } ?: ""
        } catch (e: Exception) {
            ""
        }
        
        Text(
            text = timeDisplay,
            fontSize = 10.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 2.dp, end = 4.dp, start = 4.dp)
        )
    }
}

fun getDayHeader(dateString: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
        val date = parser.parse(dateString.replace("Z", "+0000")) ?: return ""
        
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.time = date
        val msgDay = calendar.get(Calendar.DAY_OF_YEAR)
        val msgYear = calendar.get(Calendar.YEAR)
        
        return if (currentYear == msgYear) {
            when {
                today == msgDay -> "Today"
                today - 1 == msgDay -> "Yesterday"
                else -> {
                    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                    formatter.format(date)
                }
            }
        } else {
             val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
             formatter.format(date)
        }
    } catch(e: Exception) {
        return ""
    }
}

@Composable
fun DateHeaderItem(text: String) {
    if (text.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = Color(0xFFE5E7EB),
            shape = RoundedCornerShape(50.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.DarkGray
            )
        }
    }
}
