package com.example.firebase.presentation.home

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebase.data.api.RetrofitInstance
import com.example.firebase.data.local.SongDao
import com.example.firebase.data.local.SongEntity
import com.example.firebase.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.firebase.data.model.Artist
import com.example.firebase.data.model.Player
import kotlin.math.sqrt

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

    // --- ESTADO DEL REPRODUCTOR ---
    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player

    fun addPlayer(artist: Artist) {
        _player.value = Player(artist, true)
    }

    fun onPlaySelected() {
        _player.value = _player.value?.copy(play = !(_player.value?.play ?: false))
    }

    fun onCancelSelected() {
        _player.value = null
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

    private val recommendationKeywords = listOf(
        "Rock", "Pop", "Jazz", "The Beatles", "Dua Lipa", "Mozart", "Coldplay", "Rosalía"
    )

    fun recommendRandomSong() {
        val randomKeyword = recommendationKeywords.random()
        searchMusic(randomKeyword)
    }

    @Composable
    fun ShakeDetector(onShake: () -> Unit) {
        val context = LocalContext.current

        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val sensorEventListener = object : SensorEventListener {
                private var lastShakeTime: Long = 0

                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        val gX = x / SensorManager.GRAVITY_EARTH
                        val gY = y / SensorManager.GRAVITY_EARTH
                        val gZ = z / SensorManager.GRAVITY_EARTH

                        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                        if (gForce > 2.7f) {
                            val now = System.currentTimeMillis()
                            if (now - lastShakeTime > 2000) {
                                lastShakeTime = now
                                onShake()
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(
                sensorEventListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            onDispose {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
    }
}
