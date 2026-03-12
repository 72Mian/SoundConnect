package com.example.firebase

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.firebase.presentation.chat.ChatScreen
import com.example.firebase.presentation.chat.ChatViewModel
import com.example.firebase.presentation.home.HomeScreen
import com.example.firebase.presentation.home.HomeViewmodel
import com.example.firebase.presentation.initial.InitialScreen
import com.example.firebase.presentation.login.LoginScreen
import com.example.firebase.presentation.signup.SignupScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    homeViewmodel: HomeViewmodel
) {
    val startDestination = if (auth.currentUser != null) "home" else "Initial"

    NavHost(navHostController, startDestination = startDestination) {
        composable("Initial") {
            InitialScreen(
                navigateToLogin = { navHostController.navigate("LogIn") },
                navigateToSignUp = { navHostController.navigate("Signup") }
            )
        }
        composable("LogIn") {
            LoginScreen(auth, navigateToHome = {
                navHostController.navigate("home") {
                    popUpTo("Initial") { inclusive = true }
                }
            })
        }
        composable("Signup") {
            SignupScreen(auth)
        }
        composable("home") {
            HomeScreen(
                viewmodel = homeViewmodel,
                onNavigateToChat = { navHostController.navigate("chat") },
                onNavigateToMap = { navHostController.navigate("map") },
                onNavigateToProfile = { navHostController.navigate("profile") }
            )
        }
        composable("chat") {
            // Instanciamos el ChatViewModel pasándole la autenticación
            val chatViewModel = ChatViewModel(auth)
            ChatScreen(chatViewModel)
        }
        composable("profile") {
            com.example.firebase.presentation.profile.ProfileScreen(auth, onSignOut = {
                navHostController.navigate("LogIn") {
                    popUpTo(0) // Limpiamos toda la pila de navegación
                }
            })
        }
        composable("map") {
            // Instanciamos el MapViewModel
            val mapViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.firebase.presentation.map.MapViewModel>()
            com.example.firebase.presentation.map.MapScreen(mapViewModel)
        }
    }
}