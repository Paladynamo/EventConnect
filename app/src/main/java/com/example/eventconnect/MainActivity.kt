package com.example.eventconnect

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eventconnect.ui.HomeScreen
import com.example.eventconnect.ui.LoginScreen
import com.example.eventconnect.ui.RegisterScreen
import com.example.eventconnect.ui.theme.EventConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventConnectTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable("home") {
            val context = LocalContext.current
            HomeScreen(
                onLogout = {
                    // Limpia sesión y regresa al login
                    context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
