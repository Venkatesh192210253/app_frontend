package com.simats.myfitnessbuddy.data.remote

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import com.simats.myfitnessbuddy.RetrofitClient
import com.simats.myfitnessbuddy.data.local.SettingsManager

import java.util.concurrent.TimeUnit

object WebSocketManager {
    private const val TAG = "WebSocketManager"
    private var webSocket: WebSocket? = null
    
    // Ensure RetrofitClient.BASE_URL exists
    private val baseWsUrl = RetrofitClient.BASE_URL.replace("http://", "ws://").replace("https://", "wss://")
    
    private val _events = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val events: SharedFlow<JSONObject> = _events.asSharedFlow()

    enum class WebSocketStatus { CONNECTING, CONNECTED, DISCONNECTED, ERROR }
    private val _status = MutableStateFlow(WebSocketStatus.DISCONNECTED)
    val status: StateFlow<WebSocketStatus> = _status.asStateFlow()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Important for WebSockets
        .build()

    fun connect() {
        if (webSocket != null) return
        
        _status.value = WebSocketStatus.CONNECTING
        val token = SettingsManager.authToken ?: return
        val userId = SettingsManager.userId
        if (userId.isEmpty()) return
        
        val baseUrl = baseWsUrl.removeSuffix("/")
        val url = "$baseUrl/ws/notifications/$userId/?token=${token.replace("Token ", "").replace("Bearer ", "").trim()}"
        
        Log.d(TAG, "Connecting to WebSocket: $url")
        
        val request = Request.Builder()
            .url(url)
            .addHeader("ngrok-skip-browser-warning", "true")
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened: ${response.message}")
                _status.value = WebSocketStatus.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket Message: $text")
                try {
                    val json = JSONObject(text)
                    _events.tryEmit(json)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed: $reason")
                _status.value = WebSocketStatus.DISCONNECTED
                this@WebSocketManager.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code ?: 0
                val message = response?.message ?: t.message ?: "Unknown error"
                
                if (code == 101) {
                    Log.d(TAG, "WebSocket handshake successful (101), but connection was lost: $message")
                } else {
                    Log.e(TAG, "WebSocket Failure: $message (Code: $code)", t)
                }
                
                _status.value = WebSocketStatus.ERROR
                this@WebSocketManager.webSocket = null
                
                // Attempt to reconnect after a delay should be handled by the caller or a specialized job
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
    }
}
