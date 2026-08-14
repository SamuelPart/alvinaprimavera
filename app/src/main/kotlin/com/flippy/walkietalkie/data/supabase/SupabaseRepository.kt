package com.flippy.walkietalkie.data.supabase

import android.util.Log
import com.flippy.walkietalkie.BuildConfig
import com.flippy.walkietalkie.data.model.Channel
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.UserStatus
import com.flippy.walkietalkie.data.model.VoiceMessage
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ============================================================
//  DTOs: coinciden con las tablas de Supabase (ver supabase/schema.sql)
// ============================================================

@Serializable
data class ProfileDto(
    val flippy_id: String,
    val username: String,
    val avatar: String = "📻",
    val created_at: String? = null
)

@Serializable
data class ChannelDto(
    val id: String? = null,
    val name: String,
    val frequency: String = "",
    val description: String = "",
    val icon: String = "Radio",
    val is_public: Boolean = true,
    val pin: String? = null,
    val created_at: String? = null
)

@Serializable
data class ContactDto(
    val owner_flippy_id: String,
    val contact_flippy_id: String,
    val created_at: String? = null
)

@Serializable
data class VoiceMessageDto(
    val id: String? = null,
    val sender_flippy_id: String,
    val sender_name: String,
    val sender_avatar: String = "📻",
    val target_type: String = "channel",
    val target_id: String = "ch-general",
    val audio_url: String? = null,
    val duration: Float = 0f,
    val filter: String = "radio",
    val transcription: String = "",
    val created_at: String? = null
)

@Serializable
data class PttEventDto(
    val event_type: String, // "start" | "end"
    val sender_flippy_id: String,
    val sender_name: String,
    val sender_avatar: String = "📻",
    val target_type: String = "channel",
    val target_id: String = "ch-general",
    val created_at: String? = null
)

// ============================================================
//  Conversiones entre DTOs y los modelos de la app
// ============================================================

fun ProfileDto.toUser(): User = User(
    id = flippy_id,
    username = username,
    avatar = avatar,
    status = UserStatus.ONLINE
)

fun ChannelDto.toChannel(): Channel = Channel(
    id = id?.takeIf { it.isNotBlank() } ?: "ch-${name.hashCode()}",
    name = name,
    frequency = frequency,
    description = description,
    icon = icon,
    isPublic = is_public,
    pin = pin
)

fun VoiceMessageDto.toVoiceMessage(): VoiceMessage = VoiceMessage(
    id = id?.takeIf { it.isNotBlank() } ?: "msg-${(created_at ?: "").hashCode()}",
    senderId = sender_flippy_id,
    senderName = sender_name,
    senderAvatar = sender_avatar,
    targetType = target_type,
    targetId = target_id,
    audioUrl = audio_url,
    duration = duration,
    filter = filter,
    transcription = transcription
)

fun VoiceMessage.toDto(): VoiceMessageDto = VoiceMessageDto(
    sender_flippy_id = senderId,
    sender_name = senderName,
    sender_avatar = senderAvatar,
    target_type = targetType,
    target_id = targetId,
    audio_url = audioUrl,
    duration = duration,
    filter = filter,
    transcription = transcription
)

// ============================================================
//  Repositorio: toda la comunicación con Supabase
// ============================================================

object SupabaseRepository {

    const val AUDIO_BUCKET = "flippy-audio"

    private val json = Json { ignoreUnknownKeys = true }

    private val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    private val messagesChannel by lazy {
        supabase.realtime.channel("realtime:voice_messages")
    }

    private val pttChannel by lazy {
        supabase.realtime.channel("realtime:ptt_events")
    }

    fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    // ---------- Autenticación (Supabase Auth) ----------

    /** Estado de la sesión (se persiste solo en el dispositivo entre arranques). */
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    /** Envía un código de 6 dígitos al correo (Email OTP). */
    suspend fun sendEmailCode(email: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
        }
    }

    suspend fun verifyEmailCode(email: String, code: String) {
        try {
            // Flujo normal: OTP sin confirmación (tipo magiclink/email)
            supabase.auth.verifyEmailOtp(OtpType.Email.EMAIL, email, code)
        } catch (e: Exception) {
            // Si el proyecto tiene "Confirm email" activado, el token llega
            // como tipo signup; un intento fallido no consume el token
            supabase.auth.verifyEmailOtp(OtpType.Email.SIGNUP, email, code)
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    // ---------- Conexión Realtime ----------

    suspend fun connectRealtime(): Boolean {
        return try {
            supabase.realtime.connect()
            messagesChannel.subscribe()
            pttChannel.subscribe()
            true
        } catch (e: Exception) {
            Log.e("Flippy", "No se pudo conectar Realtime de Supabase", e)
            false
        }
    }

    // ---------- Perfiles ----------

    suspend fun getOrCreateProfile(flippyId: String, username: String, avatar: String): ProfileDto {
        val existing = fetchProfile(flippyId)
        if (existing != null) return existing
        val payload = buildJsonObject {
            put("flippy_id", flippyId)
            put("username", username)
            put("avatar", avatar)
        }
        supabase.postgrest.from("profiles").insert(payload)
        return ProfileDto(flippy_id = flippyId, username = username, avatar = avatar)
    }

    suspend fun fetchProfile(flippyId: String): ProfileDto? =
        supabase.postgrest.from("profiles")
            .select()
            .decodeList<ProfileDto>()
            .firstOrNull { it.flippy_id == flippyId }

    suspend fun fetchProfiles(): List<ProfileDto> =
        supabase.postgrest.from("profiles").select().decodeList()

    // ---------- Canales ----------

    suspend fun fetchChannels(): List<ChannelDto> =
        supabase.postgrest.from("channels").select().decodeList()

    suspend fun insertChannel(dto: ChannelDto) {
        val payload = buildJsonObject {
            put("id", dto.id ?: "ch-${System.currentTimeMillis()}")
            put("name", dto.name)
            put("frequency", dto.frequency)
            put("description", dto.description)
            put("icon", dto.icon)
            put("is_public", dto.is_public)
            dto.pin?.let { put("pin", it) }
        }
        supabase.postgrest.from("channels").insert(payload)
    }

    // ---------- Contactos ----------

    suspend fun fetchContacts(): List<ContactDto> =
        supabase.postgrest.from("contacts").select().decodeList()

    suspend fun insertContact(contact: ContactDto) {
        try {
            val payload = buildJsonObject {
                put("owner_flippy_id", contact.owner_flippy_id)
                put("contact_flippy_id", contact.contact_flippy_id)
            }
            supabase.postgrest.from("contacts").insert(payload)
        } catch (e: Exception) {
            // Ya existe o error de red: no es crítico
        }
    }

    // ---------- Mensajes de voz ----------

    suspend fun fetchMessages(limit: Int = 50): List<VoiceMessageDto> =
        supabase.postgrest.from("voice_messages")
            .select()
            .decodeList<VoiceMessageDto>()
            .sortedByDescending { it.created_at ?: "" }
            .take(limit)

    suspend fun insertMessage(message: VoiceMessageDto) {
        // Solo se envían los campos propios del cliente; id y created_at los genera la BD
        val payload = buildJsonObject {
            put("sender_flippy_id", message.sender_flippy_id)
            put("sender_name", message.sender_name)
            put("sender_avatar", message.sender_avatar)
            put("target_type", message.target_type)
            put("target_id", message.target_id)
            message.audio_url?.let { put("audio_url", it) }
            put("duration", message.duration)
            put("filter", message.filter)
            put("transcription", message.transcription)
        }
        supabase.postgrest.from("voice_messages").insert(payload)
    }

    // ---------- Eventos PTT (señalización TX/RX) ----------

    suspend fun insertPttEvent(event: PttEventDto) {
        try {
            val payload = buildJsonObject {
                put("event_type", event.event_type)
                put("sender_flippy_id", event.sender_flippy_id)
                put("sender_name", event.sender_name)
                put("sender_avatar", event.sender_avatar)
                put("target_type", event.target_type)
                put("target_id", event.target_id)
            }
            supabase.postgrest.from("ptt_events").insert(payload)
        } catch (e: Exception) {
            // No crítico: es solo el aviso de transmisión entrante
        }
    }

    // ---------- Audio (Storage) ----------

    suspend fun uploadAudio(path: String, bytes: ByteArray) {
        supabase.storage[AUDIO_BUCKET].upload(path, bytes, upsert = true)
    }

    suspend fun downloadAudio(path: String): ByteArray =
        supabase.storage[AUDIO_BUCKET].downloadPublic(path) ?: byteArrayOf()

    fun publicAudioUrl(path: String): String =
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$AUDIO_BUCKET/$path"

    // ---------- Observadores Realtime (INSERT en tablas) ----------

    fun observeMessages(): Flow<VoiceMessageDto> =
        messagesChannel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "voice_messages" }
            .filterIsInstance<PostgresAction.Insert>()
            .map { action ->
                json.decodeFromString<VoiceMessageDto>(action.record.toString())
            }

    fun observePttEvents(): Flow<PttEventDto> =
        pttChannel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "ptt_events" }
            .filterIsInstance<PostgresAction.Insert>()
            .map { action ->
                json.decodeFromString<PttEventDto>(action.record.toString())
            }
}
