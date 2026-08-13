package com.flippy.walkietalkie.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flippy.walkietalkie.audio.*
import com.flippy.walkietalkie.data.model.*
import com.flippy.walkietalkie.network.SocketEvent
import com.flippy.walkietalkie.network.WebSocketClient
import com.flippy.walkietalkie.service.WalkieTalkieForegroundService
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FlippyUiState(
    val userProfile: User = User(
        id = "FLIP-" + Random.nextInt(1000, 9999) + "A",
        username = "Operador-" + Random.nextInt(100, 999),
        avatar = "📻"
    ),
    val activeChannel: Channel = Channel(
        id = "ch-general",
        name = "General Radio",
        frequency = "144.800 MHz",
        description = "Frecuencia abierta para todos los operadores Flippy",
        membersCount = 1
    ),
    val activeContact: User? = null,
    val isChannelMode: Boolean = true,
    val selectedFilter: VoiceFilter = VoiceFilter.RADIO,
    val isRecording: Boolean = false,
    val recordDurationSec: Float = 0f,
    val isIncoming: Boolean = false,
    val incomingSenderName: String = "",
    val pingMs: Long = 14L,
    val channels: List<Channel> = listOf(
        Channel("ch-general", "General Radio", "144.800 MHz", "Frecuencia abierta general", "Radio", true),
        Channel("ch-tactical", "Táctico & Emergencias", "433.920 MHz", "Canal prioritario de emergencias", "ShieldAlert", true),
        Channel("ch-chill", "Chill & Música", "88.500 MHz", "Charlas libres y audio", "Headphones", true),
        Channel("ch-gaming", "Squad / Gaming", "512.000 MHz", "Coordinación rápida de juego", "Gamepad2", true)
    ),
    val contacts: List<User> = listOf(
        User("FLIP-BOT", "Flippy AI Walkie", "🤖", UserStatus.ONLINE, isBot = true),
        User("FLIP-8921", "Alex Patrol", "🐺", UserStatus.ONLINE),
        User("FLIP-3142", "Echo Base", "📡", UserStatus.ONLINE)
    ),
    val messages: List<VoiceMessage> = emptyList(),
    val autoPlayIncoming: Boolean = true,
    val serverUrl: String = "ws://10.0.2.2:3000/ws"
)

class FlippyViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer(application)
    private val webSocketClient = WebSocketClient()

    private val prefs = application.getSharedPreferences("flippy_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        FlippyUiState(
            serverUrl = prefs.getString("server_url", "ws://10.0.2.2:3000/ws") ?: "ws://10.0.2.2:3000/ws"
        )
    )
    val uiState = _uiState.asStateFlow()

    val micLevel = audioRecorder.micLevel
    val waveformSamples = audioRecorder.waveformSamples
    val isAudioPlaying = audioPlayer.isPlaying
    val currentPlayingId = audioPlayer.currentPlayingId
    val isConnected = webSocketClient.isConnected

    private var durationTimerJob: Job? = null

    init {
        // Start Foreground Service
        WalkieTalkieForegroundService.startService(
            application,
            _uiState.value.activeChannel.frequency
        )

        // Connect WebSocket using saved or default server URL
        webSocketClient.connect(_uiState.value.serverUrl)

        // Listen for socket events
        viewModelScope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is SocketEvent.PttStarted -> {
                        _uiState.update { it.copy(isIncoming = true, incomingSenderName = event.senderName) }
                        SoundEffects.playIncomingAlert()
                    }
                    is SocketEvent.PttEnded -> {
                        _uiState.update { it.copy(isIncoming = false) }
                        SoundEffects.playRogerBeep()
                    }
                    is SocketEvent.MessageReceived -> {
                        _uiState.update {
                            it.copy(
                                isIncoming = false,
                                messages = listOf(event.message) + it.messages
                            )
                        }
                        if (_uiState.value.autoPlayIncoming) {
                            audioPlayer.playVoiceMessage(event.message.id, event.message.audioBase64)
                        }
                    }
                    is SocketEvent.LatencyUpdated -> {
                        _uiState.update { it.copy(pingMs = event.pingMs) }
                    }
                    is SocketEvent.Disconnected -> {
                        // Reset RX state to avoid getting stuck "receiving" after a drop
                        _uiState.update { it.copy(isIncoming = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun startPtt() {
        if (_uiState.value.isRecording) return
        _uiState.update { it.copy(isRecording = true, recordDurationSec = 0f) }

        audioRecorder.startRecording(_uiState.value.selectedFilter)

        val targetType = if (_uiState.value.isChannelMode) "channel" else "direct"
        val targetId = if (_uiState.value.isChannelMode) _uiState.value.activeChannel.id else _uiState.value.activeContact?.id ?: ""

        webSocketClient.sendPttStart(
            _uiState.value.userProfile.username,
            _uiState.value.userProfile.avatar,
            targetType,
            targetId
        )

        val startT = System.currentTimeMillis()
        durationTimerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                val elapsed = (System.currentTimeMillis() - startT) / 1000f
                _uiState.update { it.copy(recordDurationSec = elapsed) }
                if (elapsed >= 30f) {
                    stopPtt()
                    break
                }
                delay(100)
            }
        }
    }

    fun stopPtt() {
        if (!_uiState.value.isRecording) return
        durationTimerJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }

        val targetType = if (_uiState.value.isChannelMode) "channel" else "direct"
        val targetId = if (_uiState.value.isChannelMode) _uiState.value.activeChannel.id else _uiState.value.activeContact?.id ?: ""

        webSocketClient.sendPttEnd(targetType, targetId)

        viewModelScope.launch {
            val audioResult = audioRecorder.stopRecording(_uiState.value.selectedFilter)
            if (audioResult != null) {
                val newMsg = VoiceMessage(
                    id = "msg-${System.currentTimeMillis()}",
                    senderId = _uiState.value.userProfile.id,
                    senderName = _uiState.value.userProfile.username,
                    senderAvatar = _uiState.value.userProfile.avatar,
                    targetType = targetType,
                    targetId = targetId,
                    audioBase64 = audioResult.base64Audio,
                    duration = audioResult.duration,
                    filter = _uiState.value.selectedFilter.id,
                    transcription = "Transmisión de voz recibida. 10-4 cambio."
                )

                _uiState.update { it.copy(messages = listOf(newMsg) + it.messages) }
                webSocketClient.sendVoiceMessage(newMsg)

                // If target is bot or general channel, simulate bot reply after 1.5s
                if (targetId == "FLIP-BOT" || targetId == "ch-general") {
                    delay(1400)
                    simulateBotReply(targetType, targetId)
                }
            }
        }
    }

    private fun simulateBotReply(targetType: String, targetId: String) {
        val botResponses = listOf(
            "¡10-4! Transmisión recibida fuerte y claro en la frecuencia Flippy. Cambio.",
            "Flippy AI a la escucha. Mensaje 10-4 copiado sin interferencias. Adelante.",
            "¡Roger roger! Audio recibido en la red con latencia ultra baja. Cambio y fuera."
        )
        val botMsg = VoiceMessage(
            id = "bot-${System.currentTimeMillis()}",
            senderId = "FLIP-BOT",
            senderName = "Flippy AI Walkie",
            senderAvatar = "🤖",
            targetType = targetType,
            targetId = targetId,
            isBotAudio = true,
            botText = botResponses.random(),
            duration = 3.2f,
            transcription = botResponses.random(),
            translations = mapOf(
                "en" to "10-4 Roger! Transmission received loud and clear on Flippy radio.",
                "fr" to "10-4 Bien reçu ! Message reçu 5 sur 5. Terminé.",
                "pt" to "10-4 Positivo ! Transmissão recebida com sucesso. Câmbio."
            )
        )
        _uiState.update { it.copy(messages = listOf(botMsg) + it.messages) }
        SoundEffects.playIncomingAlert()
        SoundEffects.playRogerBeep()
    }

    fun selectFilter(filter: VoiceFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun selectChannel(channel: Channel) {
        _uiState.update {
            it.copy(
                activeChannel = channel,
                isChannelMode = true,
                activeContact = null
            )
        }
        WalkieTalkieForegroundService.startService(
            getApplication(),
            channel.frequency
        )
    }

    fun selectDirectContact(contact: User) {
        _uiState.update {
            it.copy(
                activeContact = contact,
                isChannelMode = false
            )
        }
    }

    fun createCustomChannel(name: String, frequency: String, description: String, pin: String?) {
        val newChan = Channel(
            id = "ch-${System.currentTimeMillis()}",
            name = name,
            frequency = if (frequency.contains("MHz")) frequency else "$frequency MHz",
            description = description.ifEmpty { "Canal privado Flippy" },
            pin = pin,
            isPublic = pin.isNullOrEmpty()
        )
        _uiState.update { it.copy(channels = it.channels + newChan) }
        selectChannel(newChan)
    }

    fun addContactById(id: String) {
        if (_uiState.value.contacts.any { it.id.equals(id, ignoreCase = true) }) return
        val newContact = User(
            id = id.uppercase(),
            username = "Operador-${id.takeLast(4)}",
            avatar = "🦊",
            status = UserStatus.ONLINE
        )
        _uiState.update { it.copy(contacts = it.contacts + newContact) }
        SoundEffects.playIncomingAlert()
    }

    fun updateServerUrl(newUrl: String) {
        val cleanUrl = newUrl.trim()
        if (cleanUrl.isNotEmpty()) {
            prefs.edit().putString("server_url", cleanUrl).apply()
            _uiState.update { it.copy(serverUrl = cleanUrl) }
            webSocketClient.connect(cleanUrl)
        }
    }

    fun playMessage(msg: VoiceMessage, speed: Float = 1.0f) {
        audioPlayer.playVoiceMessage(msg.id, msg.audioBase64, speed)
    }

    fun stopPlayback() {
        audioPlayer.stop()
    }

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        webSocketClient.disconnect()
    }
}
