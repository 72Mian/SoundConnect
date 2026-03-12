package com.example.firebase.domain.model


data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val previewUrl: String?
)