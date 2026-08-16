package com.flippy.walkietalkie.ui.viewmodel

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.provider.ContactsContract
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
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
    // Sin amigos de ejemplo: solo aparecen los que el usuario agrega de verdad
    val contacts: List<User> = emptyList(),
    val messages: List<VoiceMessage> = emptyList(),
    val autoPlayIncoming: Boolean = true,
    val dndSyncEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val profilePhotoPath: String? = null,
    val profileCreated: Boolean = false,
    val profilePhone: String? = null,
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
            autoPlayIncoming = prefs.getBoolean("autoplay_incoming", true),
            dndSyncEnabled = prefs.getBoolean("dnd_sync", false),
            quietHoursEnabled = prefs.getBoolean("quiet_hours", false),
            profilePhotoPath = prefs.getString("profile_photo", null),
            profileCreated = prefs.getBoolean("profile_done", false),
            profilePhone = prefs.getString("profile_phone", null),
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
                            // Silencio total mientras la otra persona graba 🤫
                            _uiState.update { it.copy(isIncoming = true, incomingSenderName = event.sender_name) }
                        }
                        "end" -> {
                            // Terminó de grabar → sonido de notificación 🔔 y el
                            // mensaje llega enseguida y se reproduce al instante
                            _uiState.update { it.copy(isIncoming = false) }
                            SoundEffects.playIncomingAlert()
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
        val email = SupabaseRepository.currentUserEmail()?.lowercase()
        try {
            // ¿Este correo ya tenía una cuenta Flippy? (p. ej. reinstalaste
            // la app) → RESTAURAMOS tu ID, nombre, avatar, número y foto;
            // no necesitas configurar tu perfil de nuevo.
            val existing = email?.let { SupabaseRepository.fetchProfileByEmail(it) }
            val local = _uiState.value.userProfile
            if (existing != null && !existing.flippy_id.equals(local.id, ignoreCase = true)) {
                restoreCloudProfile(existing)
            } else {
                SupabaseRepository.getOrCreateProfile(local.id, local.username, local.avatar)
            }
            val myId = _uiState.value.userProfile.id
            // Guarda el email de la cuenta para que tus amigos te encuentren por correo
            email?.let {
                try {
                    SupabaseRepository.updateAccountEmail(myId, it)
                } catch (_: Exception) {
                }
            }
            // Re-sube tu número si ya lo habías guardado (detección por teléfono)
            prefs.getString("profile_phone", null)?.let { phone ->
                try {
                    SupabaseRepository.updateProfilePhone(myId, phone)
                } catch (_: Exception) {
                }
            }
            Log.i(TAG, "Perfil sincronizado con Supabase: $myId")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando perfil en Supabase (¿ejecutaste schema.sql?)", e)
        }
    }

    /** Recupera tu identidad de la nube tras reinstalar la app. */
    private suspend fun restoreCloudProfile(p: ProfileDto) {
        // Foto (si tenías una): del bucket a un archivo local
        val photoFile = File(getApplication<Application>().filesDir, "profile_photo.jpg")
        val hasPhoto = try {
            val bytes = SupabaseRepository.downloadAudio("avatars/${p.flippy_id}.jpg")
            photoFile.writeBytes(bytes)
            true
        } catch (e: Exception) {
            false
        }

        prefs.edit()
            .putString("flippy_id", p.flippy_id)
            .putString("flippy_username", p.username)
            .putString("flippy_avatar", p.avatar)
            .putBoolean("profile_done", true)
            .putString("profile_photo", if (hasPhoto) photoFile.absolutePath else null)
            .apply()
        p.phone?.let { prefs.edit().putString("profile_phone", it).apply() }

        _uiState.update {
            it.copy(
                userProfile = User(id = p.flippy_id, username = p.username, avatar = p.avatar),
                profileCreated = true,
                profilePhotoPath = if (hasPhoto) photoFile.absolutePath else null,
                profilePhone = p.phone
            )
        }
        Log.i(TAG, "Perfil RESTAURADO desde la nube: ${p.flippy_id} (${p.username}) 📸✨")

        // Con tu ID recuperado, recargamos tus amigos y la detección de contactos
        loadContactsFromSupabase()
        refreshDeviceContacts()
    }

    // ============================================================
    //  Contactos del dispositivo → detectar quién ya usa Flippy
    //  Cruza por CORREO (perfiles.account_email) o por TELÉFONO
    //  (perfiles.phone, cuando esté disponible en el futuro).
    // ============================================================

    data class DeviceContactMatch(
        val name: String,
        val phone: String?,
        val email: String?,
        val matched: ProfileDto? // null = aún no tiene Flippy instalado/registrado
    )

    private val _deviceContacts = MutableStateFlow<List<DeviceContactMatch>>(emptyList())
    val deviceContacts = _deviceContacts.asStateFlow()

    fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(), Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    fun refreshDeviceContacts() {
        if (!hasContactsPermission()) {
            _deviceContacts.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val raw = readDeviceContacts()
            val profiles = if (useSupabase) {
                try {
                    SupabaseRepository.fetchProfiles()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
            val myId = _uiState.value.userProfile.id

            val result = raw.map { dc ->
                val hit = profiles.firstOrNull { p ->
                    p.flippy_id != myId && (
                        (!dc.email.isNullOrBlank() && !p.account_email.isNullOrBlank() &&
                            dc.email.equals(p.account_email, ignoreCase = true)) ||
                        (!dc.phone.isNullOrBlank() && !p.phone.isNullOrBlank() &&
                            phoneDigitsMatch(dc.phone, p.phone))
                        )
                }
                DeviceContactMatch(dc.name, dc.phone, dc.email, hit)
            }.sortedWith(
                compareByDescending<DeviceContactMatch> { it.matched != null }
                    .thenBy { it.name.lowercase() }
            )
            _deviceContacts.value = result
        }
    }

    private data class RawContact(val name: String, val phone: String?, val email: String?)

    private fun readDeviceContacts(): List<RawContact> {
        val resolver = getApplication<Application>().contentResolver
        val out = LinkedHashMap<String, RawContact>()

        // Teléfonos
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )?.use { c ->
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val name = c.getString(nameIdx) ?: continue
                val num = c.getString(numIdx) ?: continue
                val key = num.filter(Char::isDigit).takeLast(9)
                if (key.length >= 8 && !out.containsKey(key)) {
                    out[key] = RawContact(name, num, null)
                }
            }
        }

        // Emails (se enganchan al contacto del mismo nombre o van aparte)
        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null, null, null
        )?.use { c ->
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
            val addrIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (c.moveToNext()) {
                val name = c.getString(nameIdx) ?: continue
                val addr = c.getString(addrIdx)?.trim()?.lowercase() ?: continue
                val existing = out.entries.firstOrNull { it.value.name.equals(name, true) }
                if (existing != null) {
                    out[existing.key] = existing.value.copy(email = addr)
                } else {
                    val key = "em:$addr"
                    if (!out.containsKey(key)) out[key] = RawContact(name, null, addr)
                }
            }
        }
        return out.values.toList()
    }

    /** Coincidencia de teléfonos por sufijo de dígitos (ignora prefijos de país). */
    private fun phoneDigitsMatch(a: String, b: String): Boolean {
        val da = a.filter(Char::isDigit)
        val db = b.filter(Char::isDigit)
        if (da.length < 8 || db.length < 8) return false
        return da.takeLast(9) == db.takeLast(9)
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
                        // Silencio total mientras la otra persona graba 🤫
                        _uiState.update { it.copy(isIncoming = true, incomingSenderName = event.senderName) }
                    }
                    is SocketEvent.PttEnded -> {
                        // Terminó de grabar → sonido de notificación 🔔
                        _uiState.update { it.copy(isIncoming = false) }
                        SoundEffects.playIncomingAlert()
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
            // Sin límite automático: graba hasta que SUELTES el botón 🎙️
            while (_uiState.value.isRecording) {
                val elapsed = (System.currentTimeMillis() - startT) / 1000f
                _uiState.update { it.copy(recordDurationSec = elapsed) }
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
    //  Modos de reproducción (persisten en SharedPreferences)
    // ============================================================

    /** Activa/desactiva la reproducción automática de mensajes entrantes. */
    fun setAutoPlayIncoming(enabled: Boolean) {
        _uiState.update { it.copy(autoPlayIncoming = enabled) }
        prefs.edit().putBoolean("autoplay_incoming", enabled).apply()
    }

    /** Botón rápido del encabezado: alterna reproducción ↔ silencio. */
    fun toggleAutoPlayIncoming() = setAutoPlayIncoming(!_uiState.value.autoPlayIncoming)

    /** Guarda tu número (opcional) para que te encuentren por teléfono. */
    fun saveAccountPhone(phone: String) {
        val clean = phone.trim()
        if (clean.isEmpty()) return
        prefs.edit().putString("profile_phone", clean).apply()
        _uiState.update { it.copy(profilePhone = clean) }
        if (useSupabase) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    SupabaseRepository.updateProfilePhone(_uiState.value.userProfile.id, clean)
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo guardar tu número en Supabase", e)
                }
            }
        }
        // Recalcula coincidencias: quizá algún contacto ya te tenía con ese número
        refreshDeviceContacts()
    }

    /** Sincronizar con No molestar del sistema. */
    fun setDndSync(enabled: Boolean) {
        _uiState.update { it.copy(dndSyncEnabled = enabled) }
        prefs.edit().putBoolean("dnd_sync", enabled).apply()
    }

    /** Configurar horario de silencio automático. */
    fun setQuietHours(enabled: Boolean) {
        _uiState.update { it.copy(quietHoursEnabled = enabled) }
        prefs.edit().putBoolean("quiet_hours", enabled).apply()
    }

    /** Guarda el perfil creado en la configuración (nombre visible + foto local). */
    fun saveUserProfile(firstName: String, lastName: String, photoPath: String?) {
        val fullName = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { _uiState.value.userProfile.username }
        val current = _uiState.value.userProfile
        _uiState.update {
            it.copy(
                userProfile = current.copy(username = fullName),
                profilePhotoPath = photoPath,
                profileCreated = true
            )
        }
        prefs.edit()
            .putString("flippy_username", fullName)
            .putString("profile_photo", photoPath)
            .putBoolean("profile_done", true)
            .apply()

        // Sube la foto a Supabase Storage para que los amigos la vean
        // (ruta por convención: flippy-audio/avatars/{FLIP-ID}.jpg)
        if (photoPath != null && useSupabase) {
            viewModelScope.launch {
                try {
                    val bytes = File(photoPath).readBytes()
                    SupabaseRepository.uploadAvatar(current.id, bytes)
                    Log.i(TAG, "Foto de perfil subida a Supabase ✅ (avatars/${current.id}.jpg)")
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo subir la foto de perfil", e)
                }
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
