package com.example.firebase.domain.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderEmail: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String? = null // Para cuando integremos la cámara más adelante
)