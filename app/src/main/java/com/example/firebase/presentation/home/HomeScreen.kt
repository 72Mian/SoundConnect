package com.example.firebase.presentation.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firebase.data.model.Player
import com.example.firebase.domain.model.Song
import com.example.firebase.ui.theme.Black
import kotlin.math.sqrt

@Composable
fun HomeScreen(
    viewmodel: HomeViewmodel,
    onNavigateToChat: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val songs by viewmodel.songs.collectAsState() // Obtenemos las canciones reales de la API
    val player by viewmodel.player.collectAsState()
    var searchQuery by remember { mutableStateOf("") } // Estado para la barra de búsqueda

    ShakeDetector(
        onShake = {
            Toast.makeText(context, "¡Agitado! Buscando recomendaciones...", Toast.LENGTH_SHORT).show()
            viewmodel.recommendRandomSong()
        }
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // --- BOTONES DE NAVEGACIÓN ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onNavigateToChat) { Text("Chat") }
            Button(onClick = onNavigateToMap) { Text("Mapa") }
            Button(onClick = onNavigateToProfile) { Text("Perfil") }
        }

        // --- BARRA DE BÚSQUEDA ---
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                label = { Text("Buscar canción...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewmodel.searchMusic(searchQuery) }) {
                Text("Buscar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LISTA DE CANCIONES (API) ---
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(songs) { song ->
                SongItem(song = song) {
                    viewmodel.selectSongToPlay(song) // ¡Pulsar reproduce la canción!
                }
            }
        }

        // --- REPRODUCTOR ---
        player?.let {
            PlayerComponent(it, { viewmodel.onPlaySelected() }, { viewmodel.onCancelSelected() })
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = song.artist,
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PlayerComponent(player: Player, onPlayPause: () -> Unit, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            AsyncImage(
                model = player.song?.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = player.song?.title ?: "Unknown",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (player.play) "Reproduciendo..." else "Pausado",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
        Row {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = Icons.Default.PlayArrow, // Usamos icono de Play de Compose
                    contentDescription = "Play/Pause",
                    tint = if (player.play) Color.Green else Color.White
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.Red
                )
            }
        }
    }
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