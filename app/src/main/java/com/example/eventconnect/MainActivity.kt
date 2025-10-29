package com.example.eventconnect

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventconnect.ui.*
import com.example.eventconnect.ui.theme.EventConnectTheme
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventConnectTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    RequestNotificationPermissionOnce()
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun RequestNotificationPermissionOnce() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
        composable("login") { LoginScreen(navController = navController) }
        composable("register") { RegisterScreen(onBackToLogin = { navController.popBackStack() }) }
        composable("forgotPassword") { ForgotPasswordScreen(onBack = { navController.popBackStack() }) }
        composable("home") {
            val context = LocalContext.current
            HomeScreen(
                onLogout = {
                    context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit { clear() }
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                },
                onCreateEvent = { navController.navigate("createEvent") },
                onOpenEvent = { eventId -> navController.navigate("eventDetail/$eventId") },
                onEditEvent = { eventId -> navController.navigate("editEvent/$eventId") },
                onOpenReservations = { navController.navigate("reservations") },
                onOpenRequests = { navController.navigate("requests") },
                onOpenProfile = { navController.navigate("profile") }
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
            MyReservationsScreen(
                { navController.popBackStack() },
                { eventId -> navController.navigate("eventDetail/$eventId") }
            )
        }
        composable("requests") { ReservationsForOrganizerScreen(onBack = { navController.popBackStack() }) }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
