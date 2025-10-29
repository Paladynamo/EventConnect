package com.example.eventconnect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventconnect.R
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.data.Reservation
import com.example.eventconnect.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatusChip(status: String) {
    val (bg, fg, label) = when (status) {
        "APPROVED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "APROBADA")
        "REJECTED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "RECHAZADA")
        "CANCELLED" -> Triple(Color(0xFFECEFF1), Color(0xFF546E7A), "CANCELADA")
        else -> Triple(Color(0xFFFFF8E1), Color(0xFFEF6C00), "PENDIENTE")
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(text = label, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsForOrganizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = remember { db.eventDao() }
    val reservationDao = remember { db.reservationDao() }
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val storeName = prefs.getString("storeName", "") ?: ""
    val scope = rememberCoroutineScope()

    var reservations by remember { mutableStateOf<List<Pair<Reservation, Event?>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
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

    fun refresh() {
        scope.launch {
            val start = System.currentTimeMillis()
            refreshing = true
            reservations = withContext(Dispatchers.IO) { loadData() }
            val elapsed = System.currentTimeMillis() - start
            val min = 800L
            if (elapsed < min) withContext(Dispatchers.IO) { kotlinx.coroutines.delay(min - elapsed) }
            refreshing = false
        }
    }

    LaunchedEffect(storeName) {
        loading = true
        reservations = withContext(Dispatchers.IO) { loadData() }
        loading = false
    }

    AppBackground {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Solicitudes de reserva") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
                actions = {
                    TextButton(onClick = { refresh() }, enabled = !refreshing) { Text(if (refreshing) "Actualizando…" else "Actualizar") }
                }
            )
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                        items(reservations) { (res, evt) ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                // Imagen de cabecera del evento
                                val cover = evt?.imageUris?.firstOrNull()
                                if (cover != null) {
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = "Imagen del evento",
                                        modifier = Modifier.fillMaxWidth().height(140.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text(evt?.title ?: "Evento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusChip(status = res.status)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    if (evt != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.Gray)
                                            Spacer(Modifier.width(6.dp))
                                            Text(evt.date)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Store, contentDescription = null, tint = Color.Gray)
                                            Spacer(Modifier.width(6.dp))
                                            Text(evt.store)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Place, contentDescription = null, tint = Color.Gray)
                                            Spacer(Modifier.width(6.dp))
                                            Text(listOf(evt.region, evt.comuna).filter { it.isNotBlank() }.joinToString(" · "))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Solicitante: ${res.userName} (${res.userEmail})")
                                    if (res.reason != null && res.status == "REJECTED") {
                                        Spacer(Modifier.height(6.dp))
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
                                                            val payload = "{" + "\"reservationId\":${current.id}" + "}"
                                                            AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                                                PendingOperation(type = "RESERVATION_APPROVE", payload = payload)
                                                            )
                                                        }
                                                    }
                                                    refresh()
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
                if (refreshing) {
                    Box(
                        Modifier.fillMaxSize().background(Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GifImage(R.drawable.gatito_girando, modifier = Modifier.size(112.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Actualizando…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
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
                                // Encola op rechazo
                                val payload = "{" +
                                    "\"reservationId\":${current.id}," +
                                    "\"reason\":\"$reason\"" +
                                    "}"
                                AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                    PendingOperation(type = "RESERVATION_REJECT", payload = payload)
                                )
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
fun MyReservationsScreen(onBack: () -> Unit, onOpenEvent: (Int) -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val reservationDao = remember { db.reservationDao() }
    val eventDao = remember { db.eventDao() }
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val email = prefs.getString("email", "") ?: ""
    val scope = rememberCoroutineScope()

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var eventsById by remember { mutableStateOf<Map<Int, Event?>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Todas" to null,
        "Pendientes" to "PENDING",
        "Aprobadas" to "APPROVED",
        "Rechazadas" to "REJECTED",
        "Canceladas" to "CANCELLED",
    )
    var selectedTab by remember { mutableStateOf(0) }

    suspend fun loadData() {
        val res = reservationDao.getForUser(email)
        val evts = res.associate { it.eventId to eventDao.getEventById(it.eventId) }
        reservations = res
        eventsById = evts
    }

    LaunchedEffect(email) {
        loading = true
        withContext(Dispatchers.IO) { loadData() }
        loading = false
    }

    fun refresh() {
        scope.launch {
            val start = System.currentTimeMillis()
            refreshing = true
            withContext(Dispatchers.IO) { loadData() }
            val elapsed = System.currentTimeMillis() - start
            val min = 800L
            if (elapsed < min) withContext(Dispatchers.IO) { kotlinx.coroutines.delay(min - elapsed) }
            refreshing = false
        }
    }

    AppBackground {
        Scaffold(topBar = {
            TopAppBar(title = { Text("Mis reservas") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Volver") }
            }, actions = {
                TextButton(onClick = { refresh() }, enabled = !refreshing) { Text(if (refreshing) "Actualizando…" else "Actualizar") }
            })
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        // TabRow con contadores por estado
                        val counts by remember(reservations) {
                            mutableStateOf(
                                mapOf(
                                    null to reservations.size,
                                    "PENDING" to reservations.count { it.status == "PENDING" },
                                    "APPROVED" to reservations.count { it.status == "APPROVED" },
                                    "REJECTED" to reservations.count { it.status == "REJECTED" },
                                    "CANCELLED" to reservations.count { it.status == "CANCELLED" },
                                )
                            )
                        }
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, (label, code) ->
                                val count = counts[code] ?: 0
                                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text("$label ($count)") })
                            }
                        }
                        val filter = tabs[selectedTab].second
                        val list = if (filter == null) reservations else reservations.filter { it.status == filter }

                        if (list.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay reservas en esta categoría.") }
                        } else {
                            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                                items(list) { r ->
                                    val evt = eventsById[r.eventId]
                                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        val cover = evt?.imageUris?.firstOrNull()
                                        if (cover != null) {
                                            AsyncImage(model = cover, contentDescription = "Imagen del evento", modifier = Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Crop)
                                        }
                                        Column(Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                Text(evt?.title ?: "Evento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                StatusChip(status = r.status)
                                            }
                                            if (evt != null) {
                                                Spacer(Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.Gray)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(evt.date)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Store, contentDescription = null, tint = Color.Gray)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(evt.store)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color.Gray)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(listOf(evt.region, evt.comuna).filter { it.isNotBlank() }.joinToString(" · "))
                                                }
                                            }
                                            if (r.reason != null && r.status == "REJECTED") {
                                                Spacer(Modifier.height(6.dp))
                                                Text("Motivo: ${r.reason}", color = Color(0xFFB00020))
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            val instructions = when (r.status) {
                                                "PENDING" -> "Tu solicitud está en revisión por el organizador."
                                                "APPROVED" -> "Tu reserva fue aprobada. ¡Prepárate para asistir!"
                                                "REJECTED" -> "Tu solicitud fue rechazada. Puedes intentar con otro evento."
                                                "CANCELLED" -> "Has cancelado esta reserva."
                                                else -> ""
                                            }
                                            Text(instructions)
                                            Spacer(Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { onOpenEvent(r.eventId) }) { Text("Ver detalle") }
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
                                                                val updatedReservation = current.copy(status = "CANCELLED")
                                                                reservationDao.update(updatedReservation)
                                                                // Encola op cancelación
                                                                val payload = "{" + "\"reservationId\":${'$'}{current.id}" + "}"
                                                                AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                                                    PendingOperation(type = "RESERVATION_CANCEL", payload = payload)
                                                                )
                                                            }
                                                            val event = withContext(Dispatchers.IO) { db.eventDao().getEventById(r.eventId) }
                                                            NotificationHelper.showCancellationNotification(context, organizerName = event?.store, eventTitle = event?.title ?: "", reserverName = r.userName)
                                                            withContext(Dispatchers.IO) { loadData() }
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
                if (refreshing) {
                    Box(
                        Modifier.fillMaxSize().background(Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GifImage(R.drawable.gatito_girando, modifier = Modifier.size(112.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Actualizando…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
