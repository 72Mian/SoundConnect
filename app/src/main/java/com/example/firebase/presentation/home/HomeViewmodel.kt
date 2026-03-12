package com.example.firebase.presentation.home

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebase.data.api.RetrofitInstance
import com.example.firebase.data.local.SongDao
import com.example.firebase.data.local.SongEntity
import com.example.firebase.data.model.Artist
import com.example.firebase.data.model.Player
import com.example.firebase.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewmodel(private val songDao: SongDao) : ViewModel() {

    // --- ESTADO DE ARTISTAS ---
    private val _artist = MutableStateFlow<List<Artist>>(
        listOf(
            Artist("The Beatles", "Legendary Rock Band", "https://example.com/beatles.jpg"),
            Artist("Dua Lipa", "Pop Star", "https://example.com/dualipa.jpg"),
            Artist("Coldplay", "Alternative Rock", "https://example.com/coldplay.jpg")
        )
    )
    val artist: StateFlow<List<Artist>> = _artist

    // --- ESTADO DEL REPRODUCTOR (UI) ---
    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player

    // --- MEDIA PLAYER (Nativo para audio) ---
    private var mediaPlayer: MediaPlayer? = null

    fun addPlayer(artist: Artist) {
        _player.value = Player(artist, true)
        // Opcional: Si tus artistas tuvieran una URL de audio, llamarías a playSong(url) aquí
    }

    fun onPlaySelected() {
        val isPlaying = _player.value?.play ?: false
        // Cambiamos el estado visual de la UI (Play/Pause)
        _player.value = _player.value?.copy(play = !isPlaying)

        // Pausamos o reanudamos la música real
        if (isPlaying) {
            mediaPlayer?.pause()
        } else {
            mediaPlayer?.start()
        }
    }

    fun onCancelSelected() {
        _player.value = null
        stopSong()
    }

    // Función para reproducir el audio real desde una URL de la API
    fun playSong(previewUrl: String?) {
        if (previewUrl == null) return

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(previewUrl)
            prepareAsync()
            setOnPreparedListener {
                start()
            }
        }
    }

    fun stopSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Limpiamos la memoria si se destruye el ViewModel
    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }

    // --- ESTADO DE LA API (Búsqueda) ---
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- ESTADO DE ROOM (Favoritos) ---
    val favoriteSongs: StateFlow<List<Song>> = songDao.getAllFavorites()
        .map { entities ->
            entities.map {
                Song(it.id, it.title, it.artist, it.coverUrl, it.previewUrl)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Función para buscar en la API
    fun searchMusic(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitInstance.api.searchSongs(query)
                val domainSongs = response.results.map { track ->
                    Song(
                        id = track.trackId.toString(),
                        title = track.trackName ?: "Unknown",
                        artist = track.artistName ?: "Unknown",
                        coverUrl = track.artworkUrl100 ?: "",
                        previewUrl = track.previewUrl
                    )
                }
                _songs.value = domainSongs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Funciones para Favoritos
    fun toggleFavorite(song: Song, isFavorite: Boolean) {
        viewModelScope.launch {
            val entity = SongEntity(song.id, song.title, song.artist, song.coverUrl, song.previewUrl)
            if (isFavorite) {
                songDao.deleteFavorite(entity)
            } else {
                songDao.insertFavorite(entity)
            }
        }
    }

    // --- SENSORES: Recomendación ---
    private val recommendationKeywords = listOf(
        "Rock", "Pop", "Jazz", "The Beatles", "Dua Lipa", "Mozart", "Coldplay", "Rosalía"
    )

    fun recommendRandomSong() {
        val randomKeyword = recommendationKeywords.random()
        searchMusic(randomKeyword)
    }
}