package com.example.eventconnect

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventconnect.notification.NotificationHelper
import com.example.eventconnect.ui.CreateEventScreen
import com.example.eventconnect.ui.EditEventScreen
import com.example.eventconnect.ui.EventDetailScreen
import com.example.eventconnect.ui.HomeScreen
import com.example.eventconnect.ui.LoginScreen
import com.example.eventconnect.ui.RegisterScreen
import com.example.eventconnect.ui.MyReservationsScreen
import com.example.eventconnect.ui.ReservationsForOrganizerScreen
import com.example.eventconnect.ui.ForgotPasswordScreen
import com.example.eventconnect.ui.theme.EventConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Crear canal de notificaciones (Android 8+)
        NotificationHelper.createNotificationChannel(this)
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
    val context = LocalContext.current
    val hasSession = remember {
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.contains("email")
    }

    NavHost(navController = navController, startDestination = if (hasSession) "home" else "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable("forgotPassword") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
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
                },
                onCreateEvent = {
                    navController.navigate("createEvent")
                },
                onOpenEvent = { eventId -> navController.navigate("eventDetail/$eventId") },
                onEditEvent = { eventId -> navController.navigate("editEvent/$eventId") },
                onOpenReservations = { navController.navigate("reservations") },
                onOpenRequests = { navController.navigate("requests") }
            )
        }
        composable("createEvent") {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val storeName = prefs.getString("storeName", "") ?: ""
            CreateEventScreen(
                storeName = storeName,
                onEventCreated = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "editEvent/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: 0
            EditEventScreen(eventId = eventId, onBack = { navController.popBackStack() })
        }
        composable(
            route = "eventDetail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: 0
            EventDetailScreen(eventId = eventId, onBack = { navController.popBackStack() })
        }
        composable("reservations") {
            MyReservationsScreen(onBack = { navController.popBackStack() })
        }
        composable("requests") {
            ReservationsForOrganizerScreen(onBack = { navController.popBackStack() })
        }
    }
}
