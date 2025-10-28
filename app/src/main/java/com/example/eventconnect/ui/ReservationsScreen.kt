package com.example.eventconnect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation
import com.example.eventconnect.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "APPROVED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(text = status, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

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
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var rejectPreset by remember { mutableStateOf("") }
    var selectedReservationId by remember { mutableStateOf<Int?>(null) }

    val presets = listOf(
        "Evento cancelado",
        "Evento sujeto a cambio de fecha/lugar",
        "Cupos agotados",
        "Mensaje personalizado"
    )

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

    AppBackground {
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
                                Text(evt?.title ?: "Evento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusChip(status = res.status)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Solicitante: ${res.userName} (${res.userEmail})")
                                if (res.reason != null && res.status == "REJECTED") {
                                    Text("Motivo: ${res.reason}", color = Color(0xFFB00020))
                                }
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
                                                loading = true
                                                reservations = withContext(Dispatchers.IO) { loadData() }
                                                loading = false
                                            }
                                        }) { Text("Aprobar") }
                                        OutlinedButton(onClick = {
                                            selectedReservationId = res.id
                                            rejectPreset = presets.first()
                                            rejectReason = ""
                                            showRejectDialog = true
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

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Motivo de rechazo") },
            text = {
                Column {
                    presets.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = rejectPreset == p, onClick = { rejectPreset = p })
                            Spacer(Modifier.width(8.dp))
                            Text(p)
                        }
                    }
                    if (rejectPreset == "Mensaje personalizado") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = rejectReason, onValueChange = { rejectReason = it }, label = { Text("Mensaje") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = selectedReservationId
                    if (id != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val current = reservationDao.getById(id) ?: return@withContext
                                val reason = if (rejectPreset == "Mensaje personalizado") rejectReason else rejectPreset
                                reservationDao.update(current.copy(status = "REJECTED", reason = reason))
                            }
                            loading = true
                            reservations = withContext(Dispatchers.IO) { loadData() }
                            loading = false
                        }
                    }
                    showRejectDialog = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancelar") }
            }
        )
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
    val scope = rememberCoroutineScope()

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(email) {
        loading = true
        reservations = withContext(Dispatchers.IO) { reservationDao.getForUser(email) }
        loading = false
    }

    AppBackground {
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
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Evento #${'$'}{r.eventId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    StatusChip(status = r.status)
                                }
                                if (r.reason != null && r.status == "REJECTED") {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Motivo: ${'$'}{r.reason}", color = Color(0xFFB00020))
                                }
                                Spacer(Modifier.height(6.dp))
                                val instructions = when (r.status) {
                                    "PENDING" -> "Tu solicitud está en revisión por el organizador."
                                    "APPROVED" -> "Tu reserva fue aprobada. ¡Prepárate para asistir!"
                                    "REJECTED" -> "Tu solicitud fue rechazada. Puedes intentar con otro evento."
                                    else -> ""
                                }
                                Text(instructions)
                                Spacer(Modifier.height(8.dp))
                                if (r.status == "PENDING" || r.status == "APPROVED") {
                                    OutlinedButton(onClick = {
                                        val resId = r.id
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val current = reservationDao.getById(resId) ?: return@withContext
                                                val event = db.eventDao().getEventById(current.eventId)
                                                if (event != null && event.hasCapacity && (current.status == "APPROVED")) {
                                                    db.eventDao().updateEvent(event.copy(capacityAvailable = (event.capacityAvailable ?: 0) + 1))
                                                }
                                                // Cambiar current.status a una copia modificada
                                                val updatedReservation = current.copy(status = "CANCELLED")
                                                reservationDao.update(updatedReservation)
                                            }
                                            val event = withContext(Dispatchers.IO) { db.eventDao().getEventById(r.eventId) }
                                            NotificationHelper.showCancellationNotification(context, organizerName = event?.store, eventTitle = event?.title ?: "", reserverName = r.userName)
                                            loading = true
                                            reservations = withContext(Dispatchers.IO) { reservationDao.getForUser(email) }
                                            loading = false
                                        }
                                    }) { Text("Cancelar reserva") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
