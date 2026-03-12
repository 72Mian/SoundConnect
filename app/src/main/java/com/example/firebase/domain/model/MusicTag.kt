package com.example.firebase.domain.model

data class MusicTag(
    val id: String = "",
    val songTitle: String = "",
    val artist: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)