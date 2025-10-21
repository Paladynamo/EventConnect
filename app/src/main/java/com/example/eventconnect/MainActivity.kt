package com.example.eventconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.example.eventconnect.ui.theme.EventConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventConnectTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    var currentScreen by remember { mutableStateOf("login") }
    var userType by remember { mutableStateOf("Usuario común") }
    var storeName by remember { mutableStateOf("") }

    when (currentScreen) {
        "login" -> LoginScreen(
            onLoginSuccess = { tipoUsuario ->
                userType = tipoUsuario
                currentScreen = "home"
            },
            onRegisterClick = { currentScreen = "register" }
        )

        "register" -> RegisterScreen(
            onBackToLogin = { currentScreen = "login" }
        )

        "home" -> HomeScreen(
            userType = userType,
            onLogout = { currentScreen = "login" },
            onCreateEvent = { currentScreen = "createEvent" }
        )

        "createEvent" -> CreateEventScreen(
            storeName = storeName.ifBlank { "Mi Tienda" },
            onEventCreated = { currentScreen = "home" },
            onCancel = { currentScreen = "home" }
        )
    }
}
