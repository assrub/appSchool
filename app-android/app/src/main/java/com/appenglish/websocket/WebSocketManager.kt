package com.appenglish.websocket

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class WebSocketEvent(
    val type: String,
    val userId: Int? = null,
    val scope: String? = null,
    val topicId: String? = null,
    val unitId: String? = null,
    val blockIndex: Int? = null
)

@Singleton
class WebSocketManager @Inject constructor(
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private val baseReconnectDelay = 2000L

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    fun connect(wsUrl: String, userId: Int) {
        if (isConnected) return

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MINUTES)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
                isConnected = true
                reconnectAttempts = 0
                scope.launch { _connectionState.emit(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = gson.fromJson(text, WebSocketEvent::class.java)
                    if (event.userId == null || event.userId == userId) {
                        scope.launch { _events.emit(event) }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message: ${e.message}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
                isConnected = false
                scope.launch { _connectionState.emit(false) }
                scheduleReconnect(wsUrl, userId)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Failure: ${t.message}")
                isConnected = false
                scope.launch { _connectionState.emit(false) }
                scheduleReconnect(wsUrl, userId)
            }
        })
    }

    private fun scheduleReconnect(wsUrl: String, userId: Int) {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w("WebSocket", "Max reconnect attempts reached")
            return
        }

        val delay = baseReconnectDelay * (1L shl reconnectAttempts.coerceAtMost(5))
        reconnectAttempts++

        scope.launch {
            delay(delay)
            connect(wsUrl, userId)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        isConnected = false
    }
}
