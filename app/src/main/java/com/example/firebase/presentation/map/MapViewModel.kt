package com.example.firebase.presentation.map

import androidx.lifecycle.ViewModel
import com.example.firebase.domain.model.MusicTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class MapViewModel : ViewModel() {


    private val _musicTags = MutableStateFlow<List<MusicTag>>(emptyList())
    val musicTags: StateFlow<List<MusicTag>> = _musicTags

    fun addMusicTag(songTitle: String, artist: String, lat: Double, lng: Double) {
        val newTag = MusicTag(
            id = UUID.randomUUID().toString(),
            songTitle = songTitle,
            artist = artist,
            latitude = lat,
            longitude = lng
        )
        _musicTags.value = _musicTags.value + newTag
    }
}