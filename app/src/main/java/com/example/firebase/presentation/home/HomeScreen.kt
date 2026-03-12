package com.example.firebase.presentation.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.firebase.data.model.Artist
import com.example.firebase.data.model.Player
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
    val artists: State<List<Artist>> = viewmodel.artist.collectAsState()
    val player: Player? by viewmodel.player.collectAsState()

    // --- ACTIVAMOS EL SENSOR DE AGITAR ---
    // ¡Ojo! Llamamos a ShakeDetector directamente, sin el viewmodel.
    ShakeDetector(
        onShake = {
            Toast.makeText(context, "¡Agitado! Buscando recomendaciones...", Toast.LENGTH_SHORT).show()
            viewmodel.recommendRandomSong()
        }
    )
    // -------------------------------------

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // --- AQUÍ AÑADIMOS LOS BOTONES DE NAVEGACIÓN ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onNavigateToChat) {
                Text("Chat")
            }
            Button(onClick = onNavigateToMap) {
                Text("Mapa")
            }
            Button(onClick = onNavigateToProfile) {
                Text("Perfil")
            }
        }
        // -----------------------------------------------

        Text(
            "Popular artist",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            modifier = Modifier.padding(16.dp)
        )
        LazyRow {
            items(artists.value) { artist ->
                ArtistItem(artist = artist) { viewmodel.addPlayer(artist) }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        player?.let {
            PlayerComponent(it, { viewmodel.onPlaySelected() }, { viewmodel.onCancelSelected() })
        }
    }
}

@Composable
fun ArtistItem(artist: Artist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.image,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Text(
            text = artist.name ?: "Unknown",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = player.artist?.image,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = player.artist?.name ?: "Unknown",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (player.play == true) "Playing" else "Paused",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
        Row {
            Button(onClick = onPlayPause) {
                Text(if (player.play == true) "Pause" else "Play")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCancel) {
                Text("X")
            }
        }
    }
}

// --- ESTA ES LA FUNCIÓN DEL SENSOR DE AGITACIÓN QUE DEBE ESTAR AL FINAL DEL ARCHIVO ---
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