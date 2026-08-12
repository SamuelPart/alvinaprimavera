package com.flippy.walkietalkie.network

import com.flippy.walkietalkie.data.model.VoiceMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.concurrent.TimeUnit

sealed class SocketEvent {
    data class Connected(val url: String) : SocketEvent()
    data class Disconnected(val reason: String) : SocketEvent()
    data class PttStarted(val senderName: String, val senderAvatar: String, val targetType: String, val targetId: String) : SocketEvent()
    data class PttEnded(val senderId: String) : SocketEvent()
    data class MessageReceived(val message: VoiceMessage) : SocketEvent()
    data class FriendAdded(val friendName: String, val friendId: String) : SocketEvent()
    data class LatencyUpdated(val pingMs: Long) : SocketEvent()
}

class WebSocketClient {

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<SocketEvent>()
    val events = _events.asSharedFlow()

    private val _pingMs = MutableStateFlow(14L)
    val pingMs = _pingMs.asStateFlow()

    private var pingStartTime = 0L

    fun connect(serverUrl: String = "ws://10.0.2.2:3000/ws") {
        disconnect()

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _isConnected.value = true
                scope.launch { _events.emit(SocketEvent.Connected(serverUrl)) }
                startPingLoop()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingJson(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                scope.launch { _events.emit(SocketEvent.Disconnected(reason)) }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                scope.launch { _events.emit(SocketEvent.Disconnected(t.message ?: "Error de red")) }
            }
        })
    }

    private fun handleIncomingJson(jsonStr: String) {
        try {
            val json = gson.fromJson(jsonStr, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return

            when (type) {
                "pong" -> {
                    val rtt = System.currentTimeMillis() - pingStartTime
                    _pingMs.value = Math.max(5L, rtt)
                    scope.launch { _events.emit(SocketEvent.LatencyUpdated(_pingMs.value)) }
                }
                "ptt_start" -> {
                    val name = json.get("senderName")?.asString ?: "Operador"
                    val avatar = json.get("senderAvatar")?.asString ?: "📻"
                    val targetType = json.get("targetType")?.asString ?: "channel"
                    val targetId = json.get("targetId")?.asString ?: "ch-general"
                    scope.launch { _events.emit(SocketEvent.PttStarted(name, avatar, targetType, targetId)) }
                }
                "ptt_end" -> {
                    val senderId = json.get("senderId")?.asString ?: ""
                    scope.launch { _events.emit(SocketEvent.PttEnded(senderId)) }
                }
                "voice_message" -> {
                    val msgObj = json.get("message")?.asJsonObject
                    if (msgObj != null) {
                        val message = gson.fromJson(msgObj, VoiceMessage::class.java)
                        scope.launch { _events.emit(SocketEvent.MessageReceived(message)) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendPttStart(senderName: String, senderAvatar: String, targetType: String, targetId: String) {
        val obj = JsonObject().apply {
            addProperty("type", "ptt_start")
            addProperty("senderName", senderName)
            addProperty("senderAvatar", senderAvatar)
            addProperty("targetType", targetType)
            addProperty("targetId", targetId)
            addProperty("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(obj.toString())
    }

    fun sendPttEnd(targetType: String, targetId: String) {
        val obj = JsonObject().apply {
            addProperty("type", "ptt_end")
            addProperty("targetType", targetType)
            addProperty("targetId", targetId)
            addProperty("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(obj.toString())
    }

    fun sendVoiceMessage(message: VoiceMessage) {
        val obj = JsonObject().apply {
            addProperty("type", "voice_message")
            add("message", gson.toJsonTree(message))
        }
        webSocket?.send(obj.toString())
    }

    fun measurePing() {
        pingStartTime = System.currentTimeMillis()
        val obj = JsonObject().apply {
            addProperty("type", "ping")
            addProperty("time", pingStartTime)
        }
        webSocket?.send(obj.toString())
    }

    private fun startPingLoop() {
        scope.launch {
            while (_isConnected.value) {
                measurePing()
                kotlinx.coroutines.delay(4000)
            }
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "App closed")
            webSocket = null
            _isConnected.value = false
        } catch (e: Exception) {}
    }
}
