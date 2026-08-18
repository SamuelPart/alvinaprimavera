package com.flippy.walkietalkie.data.model

data class VoiceMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "📻",
    val targetType: String = "channel", // "channel" | "direct"
    val targetId: String = "ch-general",
    val audioBase64: String? = null,
    val audioUrl: String? = null,
    val isBotAudio: Boolean = false,
    val botText: String? = null,
    val duration: Float = 2.5f,
    val filter: String = "radio",
    val timestamp: Long = System.currentTimeMillis(),
    val transcription: String = "",
    val translations: Map<String, String> = emptyMap()
)
