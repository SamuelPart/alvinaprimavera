package com.flippy.walkietalkie.data.supabase

import android.util.Log
import com.flippy.walkietalkie.BuildConfig
import com.flippy.walkietalkie.data.model.Channel
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.UserStatus
import com.flippy.walkietalkie.data.model.VoiceMessage
import io.github.jan.supabase.gotrue.Auth
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
    val account_email: String? = null,
    val phone: String? = null,
    // Un solo dispositivo activo por cuenta: guarda la instancia actual
    val active_device: String? = null,
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
    // "pending" = solicitud enviada · "accepted" = ya son amigos
    // (las filas antiguas sin estado cuentan como aceptadas)
    val status: String? = null,
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
    // Jamás mostrar el viejo texto de relleno (mensajes antiguos en la nube)
    transcription = transcription.takeUnless {
        it.startsWith("Transmisión de voz recibida", ignoreCase = true) ||
            it.startsWith("Transmision de voz recibida", ignoreCase = true)
    } ?: "",
    // Hora REAL del mensaje (para «14:32» / «2 días atrás» en la consola)
    timestamp = created_at?.let {
        runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    } ?: System.currentTimeMillis()
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

    // Vigila cambios en perfiles (para la regla «un solo dispositivo»)
    private val profilesChannel by lazy {
        supabase.realtime.channel("realtime:profiles_watch")
    }

    // Vigila la tabla de contactos (solicitudes de amistad en tiempo real)
    private val contactsChannel by lazy {
        supabase.realtime.channel("realtime:contacts_watch")
    }

    fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    // ---------- Autenticación (Supabase Auth) ----------

    /** Estado de la sesión (se persiste solo en el dispositivo entre arranques). */
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    /** Crea una cuenta nueva con correo + contraseña (inicia sesión al instante
     *  si "Confirm email" está desactivado en el dashboard). */
    suspend fun signUpWithEmail(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Entra con correo + contraseña. No envía correos: login directo. */
    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    // ---------- Conexión Realtime ----------

    // Una vez conectado y suscrito, nadie más debe volver a suscribirse
    // (el servicio y la app comparten los mismos canales).
    @Volatile
    private var realtimeReady = false

    @Synchronized
    suspend fun connectRealtime(): Boolean {
        if (realtimeReady) return true
        return try {
            supabase.realtime.connect()
            // Suscripción tolerante: si un canal ya estaba suscrito
            // (por el servicio o la app), NO es un error, seguimos ✅
            listOf(messagesChannel, pttChannel, profilesChannel, contactsChannel).forEach { ch ->
                runCatching { ch.subscribe() }
            }
            realtimeReady = true
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

    /** Perfil asociado a un correo de cuenta (para restaurar tu identidad
        si reinstalas la app). */
    suspend fun fetchProfileByEmail(email: String): ProfileDto? =
        supabase.postgrest.from("profiles")
            .select { filter { eq("account_email", email) } }
            .decodeList<ProfileDto>()
            .firstOrNull()

    /** Email de la sesión actual (sirve para encontrar amigos por correo). */
    fun currentUserEmail(): String? = supabase.auth.currentUserOrNull()?.email

    /** Guarda el email de la cuenta en tu perfil (detección de amigos). */
    suspend fun updateAccountEmail(flippyId: String, email: String) {
        supabase.postgrest.from("profiles").update(
            { set("account_email", email) }
        ) { filter { eq("flippy_id", flippyId) } }
    }

    /** Guarda tu número en tu perfil (detección de amigos por teléfono). */
    suspend fun updateProfilePhone(flippyId: String, phone: String) {
        supabase.postgrest.from("profiles").update(
            { set("phone", phone) }
        ) { filter { eq("flippy_id", flippyId) } }
    }

    /** Mantiene tu nombre en la nube (para restaurarlo si reinstalas). */
    suspend fun updateProfileName(flippyId: String, username: String) {
        supabase.postgrest.from("profiles").update(
            { set("username", username) }
        ) { filter { eq("flippy_id", flippyId) } }
    }

    /** Registra QUÉ dispositivo tiene la sesión activa de esta cuenta. */
    suspend fun updateActiveDevice(flippyId: String, deviceId: String) {
        supabase.postgrest.from("profiles").update(
            { set("active_device", deviceId) }
        ) { filter { eq("flippy_id", flippyId) } }
    }

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
                put("status", contact.status ?: "accepted")
            }
            supabase.postgrest.from("contacts").insert(payload)
        } catch (e: Exception) {
            // Ya existe o error de red: no es crítico
        }
    }

    /** Cambia el estado de una relación (p. ej. solicitud → "accepted"). */
    suspend fun updateContactStatus(ownerId: String, contactId: String, status: String) {
        try {
            supabase.postgrest.from("contacts").update({ set("status", status) }) {
                filter {
                    eq("owner_flippy_id", ownerId)
                    eq("contact_flippy_id", contactId)
                }
            }
        } catch (e: Exception) {
            Log.e("Flippy", "No se pudo actualizar el contacto", e)
        }
    }

    /** Borra la relación owner→contact (se usa al rechazar una solicitud). */
    suspend fun deleteContact(ownerId: String, contactId: String) {
        try {
            supabase.postgrest.from("contacts").delete {
                filter {
                    eq("owner_flippy_id", ownerId)
                    eq("contact_flippy_id", contactId)
                }
            }
        } catch (e: Exception) {
            Log.e("Flippy", "No se pudo borrar el contacto", e)
        }
    }

    /** Emite CUALQUIER cambio en la tabla contacts (solicitudes en vivo). */
    fun observeContactChanges(): Flow<PostgresAction> =
        contactsChannel
            .postgresChangeFlow<PostgresAction>(schema = "public") { table = "contacts" }

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

    /** Rellena la transcripción de un mensaje ya enviado (voz → texto ✍️).
     *  Se busca por audio_url, que es único (FLIP-ID/msg-XXXX). */
    suspend fun updateMessageTranscription(audioPath: String, text: String) {
        try {
            supabase.postgrest.from("voice_messages")
                .update({ set("transcription", text) }) {
                    filter { eq("audio_url", audioPath) }
                }
        } catch (e: Exception) {
            Log.e("Flippy", "No se pudo guardar la transcripción", e)
        }
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

    // ---------- Avatares (Storage: avatars/{FLIP-ID}.jpg) ----------

    /** Sube la foto de perfil del usuario (se sobrescribe si ya existía). */
    suspend fun uploadAvatar(flippyId: String, bytes: ByteArray) {
        supabase.storage[AUDIO_BUCKET].upload("avatars/$flippyId.jpg", bytes, upsert = true)
    }

    /** URL pública de la foto de perfil de cualquier usuario (convención por ID).
     *  Si aún no ha subido foto, la URL da 404 y la UI muestra el emoji/iniciales. */
    fun avatarPublicUrl(flippyId: String): String? =
        if (isConfigured())
            "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$AUDIO_BUCKET/avatars/$flippyId.jpg"
        else null

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

    /** Emite cada UPDATE de mensajes (p. ej. cuándo llega la transcripción ✍️). */
    fun observeMessageUpdates(): Flow<VoiceMessageDto> =
        messagesChannel
            .postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "voice_messages" }
            .filterIsInstance<PostgresAction.Update>()
            .map { action ->
                json.decodeFromString<VoiceMessageDto>(action.record.toString())
            }

    /** Emite cada UPDATE de cualquier perfil (el ViewModel filtra por el tuyo). */
    fun observeProfileChanges(): Flow<ProfileDto> =
        profilesChannel
            .postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "profiles" }
            .filterIsInstance<PostgresAction.Update>()
            .map { action ->
                json.decodeFromString<ProfileDto>(action.record.toString())
            }
}
