package com.example.eventconnect.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation
import com.example.eventconnect.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EventDetailScreen(
    eventId: Int,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = remember { db.eventDao() }
    val reservationDao = remember { db.reservationDao() }
    val scope = rememberCoroutineScope()

    var event by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(eventId) {
        event = withContext(Dispatchers.IO) { eventDao.getEventById(eventId) }
    }

    event?.let { e ->
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            AsyncImage(model = e.imageUri, contentDescription = "Imagen del evento", modifier = Modifier
                .fillMaxWidth()
                .height(180.dp))
            Spacer(Modifier.height(12.dp))
            Text(e.title, style = MaterialTheme.typography.headlineSmall)
            Text("Categoría: ${e.category}")
            Text("Fecha: ${e.date}")
            Text("Organiza: ${e.store}")
            Spacer(Modifier.height(12.dp))
            Text(e.description)
            Spacer(Modifier.height(16.dp))

            if (e.hasCapacity) {
                val available = e.capacityAvailable ?: 0
                Text("Cupos disponibles: $available")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (available <= 0) {
                        Toast.makeText(context, "No quedan cupos disponibles", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
                    val userEmail = prefs.getString("email", "") ?: ""

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            reservationDao.insert(
                                Reservation(
                                    eventId = e.id,
                                    userEmail = userEmail,
                                    userName = userName,
                                    status = "PENDING"
                                )
                            )
                        }
                        NotificationHelper.showReservationNotification(
                            context = context,
                            organizerName = e.store,
                            eventTitle = e.title,
                            reserverName = userName
                        )
                        Toast.makeText(context, "Solicitud enviada a la tienda", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Solicitar cupo") }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text("Volver") }
        }
    } ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
