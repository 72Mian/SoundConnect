package com.example.firebase.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val context = LocalContext.current
    val musicTags by viewModel.musicTags.collectAsState()


    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }


    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 10f)
    }


    var hasLocationPermission by remember { mutableStateOf(false) }
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var inputSongTitle by remember { mutableStateOf("") }
    var inputArtist by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }


    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        currentLatLng = userLatLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        //
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
        ) {
            musicTags.forEach { tag ->
                Marker(
                    state = MarkerState(position = LatLng(tag.latitude, tag.longitude)),
                    title = tag.songTitle,
                    snippet = tag.artist
                )
            }
        }


        FloatingActionButton(
            onClick = {
                if (currentLatLng != null) {
                    showDialog = true
                } else {
                    Toast.makeText(context, "Buscando tu ubicación...", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+ Tag")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Guardar Music Tag") },
                text = {
                    Column {
                        Text("¿Qué estás escuchando aquí?")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputSongTitle,
                            onValueChange = { inputSongTitle = it },
                            label = { Text("Título de la canción") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputArtist,
                            onValueChange = { inputArtist = it },
                            label = { Text("Artista") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        currentLatLng?.let { latLng ->
                            viewModel.addMusicTag(inputSongTitle, inputArtist, latLng.latitude, latLng.longitude)
                        }
                        showDialog = false
                        inputSongTitle = ""
                        inputArtist = ""
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}