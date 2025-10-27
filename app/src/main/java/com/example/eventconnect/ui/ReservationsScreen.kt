package com.example.eventconnect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsForOrganizerScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = remember { db.eventDao() }
    val reservationDao = remember { db.reservationDao() }
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val storeName = prefs.getString("storeName", "") ?: ""
    val scope = rememberCoroutineScope()

    var reservations by remember { mutableStateOf<List<Pair<Reservation, Event?>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun loadData(): List<Pair<Reservation, Event?>> {
        val events = eventDao.getEventsByStore(storeName)
        val ids = events.map { it.id }
        val res = if (ids.isNotEmpty()) reservationDao.getForEvents(ids) else emptyList()
        return res.map { r -> r to events.find { it.id == r.eventId } }
    }

    LaunchedEffect(storeName) {
        loading = true
        reservations = withContext(Dispatchers.IO) { loadData() }
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Solicitudes de reserva") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Volver") }
        })
    }) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(reservations) { (res, evt) ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(evt?.title ?: "Evento", style = MaterialTheme.typography.titleMedium)
                            Text("Solicitante: ${res.userName} (${res.userEmail})")
                            Text("Estado: ${res.status}")
                            if (res.reason != null) Text("Motivo: ${res.reason}")
                            Spacer(Modifier.height(8.dp))
                            if (res.status == "PENDING") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val current = reservationDao.getById(res.id) ?: return@withContext
                                                val event = evt ?: return@withContext
                                                if (event.hasCapacity && (event.capacityAvailable ?: 0) > 0) {
                                                    reservationDao.update(current.copy(status = "APPROVED"))
                                                    eventDao.updateEvent(event.copy(capacityAvailable = (event.capacityAvailable ?: 0) - 1))
                                                }
                                            }
                                            // refrescar
                                            loading = true
                                            reservations = withContext(Dispatchers.IO) { loadData() }
                                            loading = false
                                        }
                                    }) { Text("Aprobar") }
                                    Button(onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val current = reservationDao.getById(res.id) ?: return@withContext
                                                reservationDao.update(current.copy(status = "REJECTED", reason = "Rechazado por el organizador"))
                                            }
                                            loading = true
                                            reservations = withContext(Dispatchers.IO) { loadData() }
                                            loading = false
                                        }
                                    }) { Text("Rechazar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val reservationDao = remember { db.reservationDao() }
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val email = prefs.getString("email", "") ?: ""

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(email) {
        loading = true
        reservations = withContext(Dispatchers.IO) { reservationDao.getForUser(email) }
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Mis reservas") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Volver") }
        })
    }) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(reservations) { r ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Evento #${r.eventId}")
                            Text("Estado: ${r.status}")
                            if (r.reason != null) Text("Motivo: ${r.reason}")
                        }
                    }
                }
            }
        }
    }
}
