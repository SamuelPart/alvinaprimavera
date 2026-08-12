package com.flippy.walkietalkie.data.model

data class Channel(
    val id: String,
    val name: String,
    val frequency: String,
    val description: String,
    val icon: String = "Radio",
    val isPublic: Boolean = true,
    val pin: String? = null,
    val membersCount: Int = 1
)
