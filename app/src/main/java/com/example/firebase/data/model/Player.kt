package com.example.firebase.data.model

import com.example.firebase.domain.model.Song

data class Player(
    val song: Song? = null,
    val play: Boolean = false
)