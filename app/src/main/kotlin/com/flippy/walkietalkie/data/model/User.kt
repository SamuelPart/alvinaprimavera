package com.flippy.walkietalkie.data.model

import com.google.gson.annotations.SerializedName

enum class UserStatus {
    @SerializedName("online") ONLINE,
    @SerializedName("offline") OFFLINE,
    @SerializedName("transmitting") TRANSMITTING,
    @SerializedName("listening") LISTENING
}

data class User(
    val id: String,
    val username: String,
    val avatar: String = "📻",
    val status: UserStatus = UserStatus.ONLINE,
    val currentChannel: String = "ch-general",
    val isBot: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
