package com.flippy.walkietalkie.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flippy.walkietalkie.audio.*
import com.flippy.walkietalkie.data.model.*
import com.flippy.walkietalkie.data.supabase.*
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

    private companion object { const val TAG = "Flippy" }

    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer(application)
    private val webSocketClient = WebSocketClient()

    private val prefs = application.getSharedPreferences("flippy_prefs", Context.MODE_PRIVATE)

    // Supabase es el backend principal; el WebSocket local queda como
    // fallback cuando no hay credenciales configuradas en local.properties
    private val useSupabase: Boolean = SupabaseRepository.isConfigured()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _uiState = MutableStateFlow(
        FlippyUiState(
            userProfile = loadOrCreateLocalProfile(),
            serverUrl = prefs.getString("server_url", "ws://10.0.2.2:3000/ws") ?: "ws://10.0.2.2:3000/ws"
        )
    )
    val uiState = _uiState.asStateFlow()

    val micLevel = audioRecorder.micLevel
    val waveformSamples = audioRecorder.waveformSamples
    val isAudioPlaying = audioPlayer.isPlaying
    val currentPlayingId = audioPlayer.currentPlayingId

    private var durationTimerJob: Job? = null

    init {
        // Start Foreground Service
        WalkieTalkieForegroundService.startService(
            application,
            _uiState.value.activeChannel.frequency
        )

        if (useSupabase) {
            Log.i(TAG, "Backend: SUPABASE (nube) ✅")
            startSupabaseBackend()
        } else {
            Log.w(TAG, "Backend: WebSocket LOCAL — Supabase sin configurar. " +
                "Revisa supabase.url y supabase.anon_key en local.properties (raíz del proyecto)")
            startWebSocketFallback()
        }
    }

    /** ID Flippy estable por dispositivo: se genera una vez y se guarda en prefs. */
    private fun loadOrCreateLocalProfile(): User {
        val savedId = prefs.getString("flippy_id", null)
        val savedName = prefs.getString("flippy_username", null)
        if (savedId != null && savedName != null) {
            return User(
                id = savedId,
                username = savedName,
                avatar = prefs.getString("flippy_avatar", "📻") ?: "📻"
            )
        }
        val newId = "FLIP-" + Random.nextInt(1000, 9999) + ('A'..'Z').random()
        val newName = "Operador-" + Random.nextInt(100, 999)
        val newAvatar = "📻"
        prefs.edit()
            .putString("flippy_id", newId)
            .putString("flippy_username", newName)
            .putString("flippy_avatar", newAvatar)
            .apply()
        return User(id = newId, username = newName, avatar = newAvatar)
    }

    // ============================================================
    //  Backend principal: Supabase (Postgrest + Realtime + Storage)
    // ============================================================

    private fun startSupabaseBackend() {
        viewModelScope.launch {
            // Se crean los flujos (y sus canales) antes de conectar,
            // como recomienda supabase-kt para no perder eventos
            val incomingMessages = SupabaseRepository.observeMessages()
            val incomingPtt = SupabaseRepository.observePttEvents()

            _isConnected.value = SupabaseRepository.connectRealtime()
            Log.i(TAG, "Realtime conectado = ${_isConnected.value}")

            // Transmisiones entrantes de otros operadores en tiempo real
            launch {
                incomingMessages.collect { dto ->
                    if (dto.sender_flippy_id == _uiState.value.userProfile.id) return@collect
                    Log.i(TAG, "📥 Mensaje entrante de ${dto.sender_name} (${dto.target_id})")
                    val msg = dto.toVoiceMessage()
                    _uiState.update {
                        it.copy(isIncoming = false, messages = listOf(msg) + it.messages)
                    }
                    if (_uiState.value.autoPlayIncoming) {
                        playRemoteAudio(msg)
                    }
                }
            }

            // Señalización PTT: quién está transmitiendo / quién soltó el botón
            launch {
                incomingPtt.collect { event ->
                    if (event.sender_flippy_id == _uiState.value.userProfile.id) return@collect
                    Log.i(TAG, "🎙️ PTT ${event.event_type} de ${event.sender_name}")
                    when (event.event_type) {
                        "start" -> {
                            _uiState.update { it.copy(isIncoming = true, incomingSenderName = event.sender_name) }
                            SoundEffects.playIncomingAlert()
                        }
                        "end" -> {
                            _uiState.update { it.copy(isIncoming = false) }
                            SoundEffects.playRogerBeep()
                        }
                    }
                }
            }

            // Carga inicial de datos persistidos en la nube
            launch { registerProfileInSupabase() }
            launch { loadChannelsFromSupabase() }
            launch { loadContactsFromSupabase() }
            launch { loadMessageHistoryFromSupabase() }
        }
    }

    private suspend fun registerProfileInSupabase() {
        val p = _uiState.value.userProfile
        try {
            SupabaseRepository.getOrCreateProfile(p.id, p.username, p.avatar)
            Log.i(TAG, "Perfil registrado en Supabase: ${p.id} (${p.username})")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando perfil en Supabase (¿ejecutaste schema.sql?)", e)
        }
    }

    private suspend fun loadChannelsFromSupabase() {
        try {
            val remote = SupabaseRepository.fetchChannels().map { it.toChannel() }
            if (remote.isEmpty()) return
            _uiState.update { state ->
                val knownIds = state.channels.map { it.id }.toSet()
                state.copy(channels = state.channels + remote.filter { it.id !in knownIds })
            }
            Log.i(TAG, "Canales desde Supabase: ${remote.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando canales de Supabase", e)
        }
    }

    private suspend fun loadContactsFromSupabase() {
        try {
            val me = _uiState.value.userProfile.id
            val rows = SupabaseRepository.fetchContacts().filter { it.owner_flippy_id == me }
            if (rows.isEmpty()) return
            val profiles = SupabaseRepository.fetchProfiles()
            val users = rows.map { row ->
                profiles.firstOrNull { it.flippy_id == row.contact_flippy_id }?.toUser()
                    ?: User(
                        id = row.contact_flippy_id,
                        username = "Operador-${row.contact_flippy_id.takeLast(4)}",
                        avatar = "🦊"
                    )
            }
            _uiState.update { state ->
                val knownIds = state.contacts.map { it.id }.toSet()
                state.copy(contacts = state.contacts + users.filter { it.id !in knownIds })
            }
            Log.i(TAG, "Contactos desde Supabase: ${users.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando contactos de Supabase", e)
        }
    }

    private suspend fun loadMessageHistoryFromSupabase() {
        try {
            val history = SupabaseRepository.fetchMessages(50).map { it.toVoiceMessage() }
            if (history.isEmpty()) return
            _uiState.update { state ->
                val knownIds = state.messages.map { it.id }.toSet()
                state.copy(messages = state.messages + history.filter { it.id !in knownIds })
            }
            Log.i(TAG, "Historial desde Supabase: ${history.size} mensajes")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando historial de Supabase", e)
        }
    }

    // ============================================================
    //  Fallback: WebSocket local (backend/server.js)
    // ============================================================

    private fun startWebSocketFallback() {
        webSocketClient.connect(_uiState.value.serverUrl)

        viewModelScope.launch {
            webSocketClient.isConnected.collect { _isConnected.value = it }
        }

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

    // ============================================================
    //  PTT: transmitir voz (Supabase Storage + tabla, o WebSocket)
    // ============================================================

    fun startPtt() {
        if (_uiState.value.isRecording) return
        _uiState.update { it.copy(isRecording = true, recordDurationSec = 0f) }

        audioRecorder.startRecording(_uiState.value.selectedFilter)

        val targetType = currentTargetType()
        val targetId = currentTargetId()

        sendPttEvent("start", targetType, targetId)

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

        val targetType = currentTargetType()
        val targetId = currentTargetId()

        sendPttEvent("end", targetType, targetId)

        viewModelScope.launch {
            val audioResult = audioRecorder.stopRecording(_uiState.value.selectedFilter)
            if (audioResult == null) {
                Log.w(TAG, "Grabación muy corta (<0.3s) o sin permiso de micrófono: no se envía nada")
                return@launch
            }

            val profile = _uiState.value.userProfile
            val msgId = "msg-${System.currentTimeMillis()}"

            // Subir el audio a Supabase Storage (bucket público flippy-audio)
            var storagePath: String? = null
            if (useSupabase) {
                storagePath = try {
                    val path = "${profile.id}/$msgId.wav"
                    SupabaseRepository.uploadAudio(path, audioResult.wavBytes)
                    Log.i(TAG, "Audio subido a Storage: $path (${audioResult.wavBytes.size} bytes)")
                    path
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al subir audio (¿existe el bucket público 'flippy-audio'?)", e)
                    null
                }
            }

            val newMsg = VoiceMessage(
                id = msgId,
                senderId = profile.id,
                senderName = profile.username,
                senderAvatar = profile.avatar,
                targetType = targetType,
                targetId = targetId,
                audioBase64 = audioResult.base64Audio, // reproducción local inmediata
                audioUrl = storagePath,                // path en Storage para los demás dispositivos
                duration = audioResult.duration,
                filter = _uiState.value.selectedFilter.id,
                transcription = "Transmisión de voz recibida. 10-4 cambio."
            )

            _uiState.update { it.copy(messages = listOf(newMsg) + it.messages) }

            if (useSupabase) {
                try {
                    SupabaseRepository.insertMessage(newMsg.toDto())
                    Log.i(TAG, "Mensaje insertado en voice_messages ✅ (target=$targetId)")
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al insertar en voice_messages (¿ejecutaste schema.sql?)", e)
                }
            } else {
                webSocketClient.sendVoiceMessage(newMsg)
            }

            // If target is bot or general channel, simulate bot reply after 1.5s
            if (targetId == "FLIP-BOT" || targetId == "ch-general") {
                delay(1400)
                simulateBotReply(targetType, targetId)
            }
        }
    }

    private fun currentTargetType(): String =
        if (_uiState.value.isChannelMode) "channel" else "direct"

    private fun currentTargetId(): String =
        if (_uiState.value.isChannelMode) _uiState.value.activeChannel.id
        else _uiState.value.activeContact?.id ?: ""

    private fun sendPttEvent(eventType: String, targetType: String, targetId: String) {
        val profile = _uiState.value.userProfile
        if (useSupabase) {
            viewModelScope.launch {
                SupabaseRepository.insertPttEvent(
                    PttEventDto(
                        event_type = eventType,
                        sender_flippy_id = profile.id,
                        sender_name = profile.username,
                        sender_avatar = profile.avatar,
                        target_type = targetType,
                        target_id = targetId
                    )
                )
            }
        } else {
            if (eventType == "start") {
                webSocketClient.sendPttStart(profile.username, profile.avatar, targetType, targetId)
            } else {
                webSocketClient.sendPttEnd(targetType, targetId)
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

        if (useSupabase) {
            viewModelScope.launch {
                try {
                    SupabaseRepository.insertChannel(
                        ChannelDto(
                            id = newChan.id,
                            name = newChan.name,
                            frequency = newChan.frequency,
                            description = newChan.description,
                            icon = newChan.icon,
                            is_public = newChan.isPublic,
                            pin = newChan.pin
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        selectChannel(newChan)
    }

    fun addContactById(id: String) {
        val cleanId = id.trim().uppercase()
        if (cleanId.isEmpty()) return
        if (_uiState.value.contacts.any { it.id.equals(cleanId, ignoreCase = true) }) return

        viewModelScope.launch {
            var newContact = User(
                id = cleanId,
                username = "Operador-${cleanId.takeLast(4)}",
                avatar = "🦊",
                status = UserStatus.ONLINE
            )
            if (useSupabase) {
                try {
                    // Si el operador existe en la nube, usamos su perfil real
                    val profile = SupabaseRepository.fetchProfile(cleanId)
                    if (profile != null) newContact = profile.toUser()
                    SupabaseRepository.insertContact(
                        ContactDto(
                            owner_flippy_id = _uiState.value.userProfile.id,
                            contact_flippy_id = cleanId
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val contact = newContact
            _uiState.update { it.copy(contacts = it.contacts + contact) }
            SoundEffects.playIncomingAlert()
        }
    }

    fun updateServerUrl(newUrl: String) {
        val cleanUrl = newUrl.trim()
        if (cleanUrl.isNotEmpty()) {
            prefs.edit().putString("server_url", cleanUrl).apply()
            _uiState.update { it.copy(serverUrl = cleanUrl) }
            if (!useSupabase) {
                webSocketClient.connect(cleanUrl)
            }
        }
    }

    // ============================================================
    //  Reproducción (local en Base64 o remota desde Supabase Storage)
    // ============================================================

    fun playMessage(msg: VoiceMessage, speed: Float = 1.0f) {
        if (!msg.audioBase64.isNullOrEmpty()) {
            audioPlayer.playVoiceMessage(msg.id, msg.audioBase64, speed)
        } else {
            playRemoteAudio(msg, speed)
        }
    }

    private fun playRemoteAudio(msg: VoiceMessage, speed: Float = 1.0f) {
        val path = msg.audioUrl
        if (path.isNullOrBlank()) {
            audioPlayer.playVoiceMessage(msg.id, null, speed) // Roger beep fallback
            return
        }
        viewModelScope.launch {
            try {
                val bytes = SupabaseRepository.downloadAudio(storagePathFrom(path))
                val base64 = "data:audio/wav;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                audioPlayer.playVoiceMessage(msg.id, base64, speed)
            } catch (e: Exception) {
                e.printStackTrace()
                SoundEffects.playRogerBeep()
            }
        }
    }

    /** Acepta un path relativo del bucket o una URL pública completa. */
    private fun storagePathFrom(pathOrUrl: String): String =
        if (pathOrUrl.startsWith("http")) {
            pathOrUrl.substringAfter("/${SupabaseRepository.AUDIO_BUCKET}/")
        } else {
            pathOrUrl
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
        // La conexión Realtime de Supabase es un singleton a nivel app: se mantiene viva.
    }
}
