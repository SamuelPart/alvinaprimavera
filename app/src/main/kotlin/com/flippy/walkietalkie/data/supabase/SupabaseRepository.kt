package com.flippy.walkietalkie.data.supabase

import com.flippy.walkietalkie.BuildConfig
import com.flippy.walkietalkie.data.model.Channel
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.UserStatus
import com.flippy.walkietalkie.data.model.VoiceMessage
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================================
//  DTOs: coinciden con las tablas creadas en Supabase
//  (los campos con valor por defecto no se envÃ­an al insertar)
// ============================================================

@Serializable
data class ProfileDto(
    val flippy_id: String,
    val username: String,
    val avatar: String = "ðŸ“»",
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
    val id: String = "",
    val sender_flippy_id: String,
    val sender_name: String,
    val sender_avatar: String = "ðŸ“»",
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
    val sender_avatar: String = "ðŸ“»",
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
    id = id ?: "ch-${name.hashCode()}",
    name = name,
    frequency = frequency,
    description = description,
    icon = icon,
    isPublic = is_public,
    pin = pin
)

fun VoiceMessageDto.toVoiceMessage(): VoiceMessage = VoiceMessage(
    id = id.ifBlank { "msg-${created_at.hashCode()}" },
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
    id = id,
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
//  Repositorio: toda la comunicaciÃ³n con Supabase
// ============================================================

object SupabaseRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
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

    // ---------- ConexiÃ³n Realtime ----------

    suspend fun connectRealtime() {
        try {
            supabase.realtime.connect()
            messagesChannel.subscribe()
            pttChannel.subscribe()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------- Perfiles ----------

    suspend fun getOrCreateProfile(flippyId: String, username: String, avatar: String): ProfileDto {
        val existing = supabase.postgrest.from("profiles")
            .select()
            .decodeList<ProfileDto>()
            .firstOrNull { it.flippy_id == flippyId }
        if (existing != null) return existing
        val dto = ProfileDto(flippy_id = flippyId, username = username, avatar = avatar)
        supabase.postgrest.from("profiles").insert(dto)
        return dto
    }

    suspend fun fetchProfiles(): List<ProfileDto> =
        supabase.postgrest.from("profiles").select().decodeList()

    // ---------- Canales ----------

    suspend fun fetchChannels(): List<ChannelDto> =
        supabase.postgrest.from("channels").select().decodeList()

    suspend fun insertChannel(channel: ChannelDto) {
        supabase.postgrest.from("channels").insert(channel)
    }

    // ---------- Contactos ----------

    suspend fun insertContact(contact: ContactDto) {
        try {
            supabase.postgrest.from("contacts").insert(contact)
        } catch (e: Exception) {
            // ya existe o error de red: no es crÃ­tico
        }
    }

    // ---------- Mensajes ----------

    suspend fun fetchMessages(limit: Int = 50): List<VoiceMessageDto> =
        supabase.postgrest.from("voice_messages")
            .select()
            .decodeList<VoiceMessageDto>()
            .sortedByDescending { it.created_at ?: "" }
            .take(limit)

    suspend fun insertMessage(message: VoiceMessageDto) {
        supabase.postgrest.from("voice_messages").insert(message)
    }

    // ---------- Eventos PTT ----------

    suspend fun insertPttEvent(event: PttEventDto) {
        try {
            supabase.postgrest.from("ptt_events").insert(event)
        } catch (e: Exception) {
            // no crÃ­tico: es solo el aviso de TX/RX
        }
    }

    // ---------- Audio (Storage) ----------

    suspend fun uploadAudio(path: String, bytes: ByteArray) {
        supabase.storage["flippy-audio"].upload(path, bytes, upsert = true)
    }

    suspend fun downloadAudio(path: String): ByteArray =
        supabase.storage["flippy-audio"].downloadPublic(path) ?: byteArrayOf()

    fun publicAudioUrl(path: String): String =
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/flippy-audio/$path"

    // ---------- Observadores Realtime ----------

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
