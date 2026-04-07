package com.simats.myfitnessbuddy

import org.json.JSONObject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.remote.NotificationResponse

import com.simats.myfitnessbuddy.data.remote.WebSocketManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class NotificationType(val icon: ImageVector, val color: Color, val bgColor: Color) {
    CHALLENGE(Icons.Default.EmojiEvents, Color(0xFF4CAF50), Color(0xFFE8F5E9)),
    SOCIAL(Icons.Default.PersonAdd, Color(0xFF2196F3), Color(0xFFE3F2FD)),
    AI(Icons.Default.SmartToy, Color(0xFF9C27B0), Color(0xFFF3E5F5)),
    MILESTONE(Icons.Default.TrendingUp, Color(0xFFFFB300), Color(0xFFFFF8E1)),
    REMINDER(Icons.Default.Notifications, Color(0xFF757575), Color(0xFFF5F5F5)),
    OTHER(Icons.Default.Notifications, Color(0xFF757575), Color(0xFFF5F5F5));

    companion object {
        fun fromString(type: String): NotificationType {
            return when (type) {
                "FriendChallenge" -> CHALLENGE
                "GoalCompleted", "Milestone" -> MILESTONE
                "WorkoutReminder" -> REMINDER
                "AIRecommendation" -> AI
                "AchievementUnlocked" -> MILESTONE
                "FriendRequest", "Social" -> SOCIAL
                else -> OTHER
            }
        }
    }
}

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val description: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val isFriendRequest: Boolean = false,
    val originalId: Int = 0 // Database ID
)

class NotificationsViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _newNotificationEvent = MutableSharedFlow<Notification>(extraBufferCapacity = 1)
    val newNotificationEvent: SharedFlow<Notification> = _newNotificationEvent.asSharedFlow()

    init {
        loadNotifications()
        observeWebSocket()
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            WebSocketManager.events.collect { event ->
                val type = event.optString("type")
                if (type == "notification_message") {
                    val content = event.optJSONObject("content")
                    if (content != null) {
                        val newNotification = Notification(
                            id = content.optString("id"),
                            type = NotificationType.fromString(content.optString("type")),
                            title = content.optString("title"),
                            description = content.optString("message"),
                            timestamp = "Just now",
                            isRead = content.optBoolean("is_read"),
                            originalId = content.optInt("id")
                        )
                        _notifications.update { list ->
                            listOf(newNotification) + list.filter { it.id != newNotification.id }
                        }
                        _newNotificationEvent.emit(newNotification)
                    }
                }
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getNotifications()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val mapped = list.map { res ->
                        Notification(
                            id = res.id.toString(),
                            type = NotificationType.fromString(res.type),
                            title = res.title,
                            description = res.message,
                            timestamp = res.created_at, // You might want to format this
                            isRead = res.is_read,
                            originalId = res.id
                        )
                    }
                    _notifications.value = mapped
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(id: String) {
        val notification = _notifications.value.find { it.id == id } ?: return
        if (notification.isRead) return
        
        viewModelScope.launch {
            try {
                val dbId = notification.originalId
                if (dbId != 0) {
                    RetrofitClient.apiService.markNotificationRead(dbId)
                }
                _notifications.update { list ->
                    list.map { if (it.id == id) it.copy(isRead = true) else it }
                }
            } catch (e: Exception) {}
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.markAllNotificationsRead()
                _notifications.update { list ->
                    list.map { it.copy(isRead = true) }
                }
            } catch (e: Exception) {}
        }
    }

    fun deleteNotification(id: String) {
        // We don't have a delete endpoint yet based on prompt, but we can hide it locally
        _notifications.update { list ->
            list.filter { it.id != id }
        }
    }

    fun acceptFriendRequest(id: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.acceptFriendRequest(mapOf("request_id" to id))
                if (res.isSuccessful) {
                    deleteNotification(id)
                }
            } catch (e: Exception) {}
        }
    }

    fun declineFriendRequest(id: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.rejectFriendRequest(mapOf("request_id" to id))
                if (res.isSuccessful) {
                    deleteNotification(id)
                }
            } catch (e: Exception) {}
        }
    }
}
