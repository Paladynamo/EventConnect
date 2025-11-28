package com.example.eventconnect.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale
import androidx.core.content.edit
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
import com.example.eventconnect.util.dateToMillis
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.eventconnect.remote.sync.RemoteReservationSync
import com.example.eventconnect.remote.sync.RemoteReviewSync
import com.example.eventconnect.remote.service.UserReviewDto
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.ReservationService
import com.example.eventconnect.remote.dto.RejectReservationRequest
import com.example.eventconnect.remote.dto.toReservation
import com.example.eventconnect.util.SessionHelper
import retrofit2.HttpException
import android.util.Log
import android.widget.Toast

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
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
        allEvents = events
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

    // Filtros y búsqueda
    val tabs = listOf("Todas" to null, "Pendientes" to "PENDING", "Aprobadas" to "APPROVED", "Rechazadas" to "REJECTED", "Canceladas" to "CANCELLED")
    var selectedTab by remember { mutableStateOf(0) }
    var selectedEventId by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(reservations, selectedTab, selectedEventId, query) {
        val code = tabs[selectedTab].second
        reservations.filter { (r, e) ->
            (code == null || r.status == code) &&
            (selectedEventId == null || r.eventId == selectedEventId) &&
            (query.isBlank() || r.userName.contains(query, true) || r.userEmail.contains(query, true) || (e?.title?.contains(query, true) == true))
        }
    }

    // === Perfil / Reseñas (declaraciones únicas antes de uso en tarjetas) ===
    val reviewDao = remember { db.userReviewDao() }
    var showProfile by remember { mutableStateOf(false) }
    var profileUserEmail by remember { mutableStateOf<String?>(null) }
    var profileData by remember { mutableStateOf<Triple<Double, Int, List<com.example.eventconnect.data.UserReview>>?>(null) }
    var profileNotes by remember { mutableStateOf("") }
    val notesPrefs = remember { context.getSharedPreferences("user_notes", Context.MODE_PRIVATE) }
    var userStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var favorite by remember { mutableStateOf(false) }
    suspend fun loadProfile(email: String) {
        val avg = reviewDao.getAverageRating(email) ?: 0.0
        val count = reviewDao.getReviewsCount(email)
        val list = reviewDao.getReviewsForUser(email)
        profileData = Triple(avg, count, list)
    }
    // === Fin perfil ===

    val remoteReservationSync = remember { RemoteReservationSync(context) }
    val remoteReviewSync = remember { RemoteReviewSync(context) }

    AppBackground {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Solicitudes de reserva") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
                actions = {
                    if (selectedEventId != null) {
                        val evt = allEvents.firstOrNull { it.id == selectedEventId }
                        val pendingCount = filtered.count { it.first.status == "PENDING" && it.second?.id == selectedEventId }
                        val canBulk = pendingCount > 0 && (evt?.hasCapacity == true) && ((evt.capacityAvailable ?: 0) > 0)
                        TextButton(onClick = {
                            // Aprobar en lote hasta agotar cupos - PRIMERO a la API
                            scope.launch {
                                val userId = SessionHelper.getUserId(context)
                                if (userId == null) {
                                    Toast.makeText(context, "Error: No se encontró sesión de usuario", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                
                                try {
                                    val event = withContext(Dispatchers.IO) { eventDao.getEventById(selectedEventId!!) }
                                    if (event != null && event.hasCapacity) {
                                        var available = event.capacityAvailable ?: 0
                                        val pendings = withContext(Dispatchers.IO) { 
                                            reservationDao.getForEvents(listOf(event.id)).filter { it.status == "PENDING" } 
                                        }
                                        
                                        val reservationService = RetrofitProvider.retrofit(context).create(ReservationService::class.java)
                                        
                                        for (r in pendings) {
                                            if (available <= 0) break
                                            
                                            try {
                                                // Llamar primero a la API
                                                val response = withContext(Dispatchers.IO) {
                                                    reservationService.approve(r.id.toLong(), userId)
                                                }
                                                
                                                // Si tiene éxito, actualizar local
                                                withContext(Dispatchers.IO) {
                                                    val updatedReservation = response.toReservation()
                                                    reservationDao.update(updatedReservation)
                                                    available -= 1
                                                    eventDao.updateEvent(event.copy(capacityAvailable = available))
                                                }
                                                
                                                Log.d("ReservationsScreen", "Reserva ${r.id} aprobada en API")
                                            } catch (e: HttpException) {
                                                Log.e("ReservationsScreen", "Error al aprobar reserva ${r.id}: HTTP ${e.code()}", e)
                                                // Continuar con la siguiente reserva
                                            } catch (e: Exception) {
                                                Log.e("ReservationsScreen", "Error al aprobar reserva ${r.id}: ${e.message}", e)
                                                // Continuar con la siguiente reserva
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ReservationsScreen", "Error al aprobar en lote: ${e.message}", e)
                                    Toast.makeText(context, "Error al aprobar reservas: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                
                                refresh()
                            }
                        }, enabled = canBulk) { Text("Aprobar lote") }
                    }
                    TextButton(onClick = { refresh() }, enabled = !refreshing) { Text(if (refreshing) "Actualizando…" else "Actualizar") }
                }
            )
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                        // Controles de filtro
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { idx, (label, _) ->
                                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Buscar: nombre, email o evento") }
                            )
                            Spacer(Modifier.width(8.dp))
                            ExposedDropdownMenuBox(expanded = false, onExpandedChange = { }) {}
                            // Dropdown simple manual
                            var eventMenu by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { eventMenu = true }) {
                                    Text(selectedEventId?.let { id -> allEvents.find { it.id == id }?.title ?: "Evento" } ?: "Todos los eventos")
                                }
                                DropdownMenu(expanded = eventMenu, onDismissRequest = { eventMenu = false }) {
                                    DropdownMenuItem(text = { Text("Todos") }, onClick = { selectedEventId = null; eventMenu = false })
                                    allEvents.forEach { ev -> DropdownMenuItem(text = { Text(ev.title) }, onClick = { selectedEventId = ev.id; eventMenu = false }) }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay solicitudes para los filtros seleccionados.") }
                        } else {
                            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(filtered) { (res, evt) ->
                                    val statusColor = when (res.status) {
                                        "APPROVED" -> Color(0x332E7D32)
                                        "REJECTED" -> Color(0x33C62828)
                                        "CANCELLED" -> Color(0x33546E7A)
                                        else -> Color(0x33EF6C00)
                                    }
                                    val eventMillis = evt?.date?.let { dateToMillis(it) }
                                    val canModify = eventMillis == null || eventMillis >= startOfTodayMillis()
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(4.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color.Gray)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(listOf(evt.region, evt.comuna).filter { it.isNotBlank() }.joinToString(" · "))
                                                }
                                                if (evt.hasCapacity) {
                                                    Spacer(Modifier.height(6.dp))
                                                    val total = evt.capacityTotal ?: 0
                                                    val avail = evt.capacityAvailable ?: 0
                                                    val progress = if (total > 0) avail.toFloat() / total.toFloat() else 0f
                                                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                                    Text("Cupos: $avail/$total", color = Color.Gray)
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text("Solicitante: ${res.userName} (${res.userEmail})")
                                            if (res.reason != null && res.status == "REJECTED") {
                                                Spacer(Modifier.height(6.dp))
                                                Text("Motivo: ${res.reason}", color = Color(0xFFB00020))
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            if (res.status == "PENDING") {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    val canApprove = (evt?.hasCapacity == true) && ((evt.capacityAvailable ?: 0) > 0) && canModify
                                                    Button(onClick = {
                                                        scope.launch {
                                                            val userId = SessionHelper.getUserId(context)
                                                            if (userId == null) {
                                                                Toast.makeText(context, "Error: No se encontró sesión de usuario", Toast.LENGTH_LONG).show()
                                                                return@launch
                                                            }
                                                            
                                                            val current = withContext(Dispatchers.IO) { reservationDao.getById(res.id) }
                                                            val event = evt
                                                            if (current != null && event != null && event.hasCapacity && (event.capacityAvailable ?: 0) > 0) {
                                                                try {
                                                                    // Llamar primero a la API
                                                                    val reservationService = RetrofitProvider.retrofit(context).create(ReservationService::class.java)
                                                                    val response = withContext(Dispatchers.IO) {
                                                                        reservationService.approve(current.id.toLong(), userId)
                                                                    }
                                                                    
                                                                    // Si tiene éxito, actualizar local
                                                                    withContext(Dispatchers.IO) {
                                                                        val updatedReservation = response.toReservation()
                                                                        reservationDao.update(updatedReservation)
                                                                        eventDao.updateEvent(event.copy(capacityAvailable = (event.capacityAvailable ?: 0) - 1))
                                                                    }
                                                                    
                                                                    Log.d("ReservationsScreen", "Reserva ${current.id} aprobada en API")
                                                                    refresh()
                                                                } catch (e: HttpException) {
                                                                    val errorMessage = when (e.code()) {
                                                                        400 -> "Error: Datos inválidos."
                                                                        401 -> "Error: No autorizado."
                                                                        403 -> "Error: No tienes permiso."
                                                                        404 -> "Error: Reserva no encontrada."
                                                                        500 -> "Error del servidor."
                                                                        else -> "Error HTTP ${e.code()}"
                                                                    }
                                                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                                                    Log.e("ReservationsScreen", "Error HTTP ${e.code()}: ${e.message}", e)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Error al aprobar reserva: ${e.message}", Toast.LENGTH_LONG).show()
                                                                    Log.e("ReservationsScreen", "Error al aprobar reserva: ${e.message}", e)
                                                                }
                                                            }
                                                        }
                                                    }, enabled = canApprove) { Text("Aprobar") }
                                                    OutlinedButton(onClick = {
                                                        selectedReservationId = res.id
                                                        rejectPreset = presets.first()
                                                        rejectReason = ""
                                                        showRejectDialog = true
                                                    }, enabled = canModify) { Text("Rechazar") }
                                                    OutlinedButton(onClick = {
                                                        profileUserEmail = res.userEmail
                                                        showProfile = true
                                                        scope.launch(Dispatchers.IO) { loadProfile(res.userEmail) }
                                                    }) { Text("Ver perfil") }
                                                }
                                            } else {
                                                // Acciones de reversión hasta la fecha del evento
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    when (res.status) {
                                                        "APPROVED" -> {
                                                            // Revertir a REJECTED (motivo) o CANCELLED, liberando cupo
                                                            OutlinedButton(onClick = {
                                                                selectedReservationId = res.id
                                                                rejectPreset = presets.first()
                                                                rejectReason = ""
                                                                showRejectDialog = true
                                                            }, enabled = canModify) { Text("Rechazar") }
                                                            OutlinedButton(onClick = {
                                                                if (!canModify) return@OutlinedButton
                                                                scope.launch {
                                                                    val userId = SessionHelper.getUserId(context)
                                                                    if (userId == null) {
                                                                        Toast.makeText(context, "Error: No se encontró sesión de usuario", Toast.LENGTH_LONG).show()
                                                                        return@launch
                                                                    }
                                                                    
                                                                    val current = withContext(Dispatchers.IO) { reservationDao.getById(res.id) }
                                                                    val event = evt
                                                                    if (current != null && event != null) {
                                                                        // Nota: La API no tiene endpoint para CANCELLED, pero podemos intentar rechazar
                                                                        try {
                                                                            val reservationService = RetrofitProvider.retrofit(context).create(ReservationService::class.java)
                                                                            // Intentar rechazar con motivo de cancelación
                                                                            val response = withContext(Dispatchers.IO) {
                                                                                reservationService.reject(current.id.toLong(), userId, RejectReservationRequest("Cancelada por el organizador"))
                                                                            }
                                                                            
                                                                            // Actualizar local como CANCELLED
                                                                            withContext(Dispatchers.IO) {
                                                                                if (event.hasCapacity) {
                                                                                    eventDao.updateEvent(event.copy(capacityAvailable = (event.capacityAvailable ?: 0) + 1))
                                                                                }
                                                                                reservationDao.update(current.copy(status = "CANCELLED"))
                                                                            }
                                                                            
                                                                            Log.d("ReservationsScreen", "Reserva ${current.id} cancelada")
                                                                            refresh()
                                                                        } catch (e: Exception) {
                                                                            // Si falla, solo actualizar local
                                                                            Log.e("ReservationsScreen", "Error al cancelar reserva: ${e.message}", e)
                                                                            withContext(Dispatchers.IO) {
                                                                                if (event.hasCapacity) {
                                                                                    eventDao.updateEvent(event.copy(capacityAvailable = (event.capacityAvailable ?: 0) + 1))
                                                                                }
                                                                                reservationDao.update(current.copy(status = "CANCELLED"))
                                                                                db.pendingOperationDao().insert(PendingOperation(type = "RESERVATION_CANCEL", payload = "{\"reservationId\":${current.id}}"))
                                                                            }
                                                                            refresh()
                                                                        }
                                                                    }
                                                                }
                                                            }, enabled = canModify) { Text("Cancelar") }
                                                        }
                                                        "REJECTED", "CANCELLED" -> {
                                                            val canApproveBack = (evt?.hasCapacity == true) && ((evt.capacityAvailable ?: 0) > 0) && canModify
                                                            Button(onClick = {
                                                                scope.launch {
                                                                    val userId = SessionHelper.getUserId(context)
                                                                    if (userId == null) {
                                                                        Toast.makeText(context, "Error: No se encontró sesión de usuario", Toast.LENGTH_LONG).show()
                                                                        return@launch
                                                                    }
                                                                    
                                                                    val current = withContext(Dispatchers.IO) { reservationDao.getById(res.id) }
                                                                    val event = evt
                                                                    if (current != null && event != null && event.hasCapacity && (event.capacityAvailable ?: 0) > 0) {
                                                                        try {
                                                                            // Llamar primero a la API
                                                                            val reservationService = RetrofitProvider.retrofit(context).create(ReservationService::class.java)
                                                                            val response = withContext(Dispatchers.IO) {
                                                                                reservationService.approve(current.id.toLong(), userId)
                                                                            }
                                                                            
                                                                            // Si tiene éxito, actualizar local
                                                                            withContext(Dispatchers.IO) {
                                                                                val updatedReservation = response.toReservation()
                                                                                reservationDao.update(updatedReservation)
                                                                                eventDao.updateEvent(event.copy(capacityAvailable = (event.capacityAvailable ?: 0) - 1))
                                                                            }
                                                                            
                                                                            Log.d("ReservationsScreen", "Reserva ${current.id} aprobada en API")
                                                                            refresh()
                                                                        } catch (e: HttpException) {
                                                                            val errorMessage = when (e.code()) {
                                                                                400 -> "Error: Datos inválidos."
                                                                                401 -> "Error: No autorizado."
                                                                                403 -> "Error: No tienes permiso."
                                                                                404 -> "Error: Reserva no encontrada."
                                                                                500 -> "Error del servidor."
                                                                                else -> "Error HTTP ${e.code()}"
                                                                            }
                                                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                                                            Log.e("ReservationsScreen", "Error HTTP ${e.code()}: ${e.message}", e)
                                                                        } catch (e: Exception) {
                                                                            Toast.makeText(context, "Error al aprobar reserva: ${e.message}", Toast.LENGTH_LONG).show()
                                                                            Log.e("ReservationsScreen", "Error al aprobar reserva: ${e.message}", e)
                                                                        }
                                                                    }
                                                                }
                                                            }, enabled = canApproveBack) { Text("Aprobar") }
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Box(Modifier.fillMaxWidth().height(4.dp).background(statusColor))
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
                            val userId = SessionHelper.getUserId(context)
                            if (userId == null) {
                                Toast.makeText(context, "Error: No se encontró sesión de usuario", Toast.LENGTH_LONG).show()
                                showRejectDialog = false
                                return@launch
                            }
                            
                            val current = withContext(Dispatchers.IO) { reservationDao.getById(id) }
                            if (current != null) {
                                val reason = if (rejectPreset == "Mensaje personalizado") rejectReason else rejectPreset
                                
                                try {
                                    // Llamar primero a la API
                                    val reservationService = RetrofitProvider.retrofit(context).create(ReservationService::class.java)
                                    val response = withContext(Dispatchers.IO) {
                                        reservationService.reject(current.id.toLong(), userId, RejectReservationRequest(reason))
                                    }
                                    
                                    // Si tiene éxito, actualizar local
                                    withContext(Dispatchers.IO) {
                                        val updatedReservation = response.toReservation()
                                        reservationDao.update(updatedReservation)
                                    }
                                    
                                    Log.d("ReservationsScreen", "Reserva ${current.id} rechazada en API")
                                    loading = true
                                    reservations = withContext(Dispatchers.IO) { loadData() }
                                    loading = false
                                } catch (e: HttpException) {
                                    val errorMessage = when (e.code()) {
                                        400 -> "Error: Datos inválidos."
                                        401 -> "Error: No autorizado."
                                        403 -> "Error: No tienes permiso."
                                        404 -> "Error: Reserva no encontrada."
                                        500 -> "Error del servidor."
                                        else -> "Error HTTP ${e.code()}"
                                    }
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    Log.e("ReservationsScreen", "Error HTTP ${e.code()}: ${e.message}", e)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al rechazar reserva: ${e.message}", Toast.LENGTH_LONG).show()
                                    Log.e("ReservationsScreen", "Error al rechazar reserva: ${e.message}", e)
                                }
                            }
                            showRejectDialog = false
                        }
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Eliminar bloque duplicado anterior: dejamos sólo este AlertDialog
    if (showProfile) {
        val emailValue = profileUserEmail ?: ""
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text("Perfil del usuario") },
            text = {
                val emailValue = profileUserEmail ?: ""
                if (emailValue.isBlank()) {
                    Text("Cargando…")
                } else {
                    val data = profileData
                    // Cargar notas y favorito una sola vez por email
                    LaunchedEffect(emailValue) {
                        profileNotes = notesPrefs.getString("notes_$emailValue", "") ?: ""
                        favorite = notesPrefs.getBoolean("fav_$emailValue", false)
                        withContext(Dispatchers.IO) {
                            val allUserRes = reservationDao.getForUser(emailValue)
                            val approved = allUserRes.count { it.status == "APPROVED" }
                            val rejected = allUserRes.count { it.status == "REJECTED" }
                            val pending = allUserRes.count { it.status == "PENDING" }
                            val cancelled = allUserRes.count { it.status == "CANCELLED" }
                            val total = allUserRes.size
                            userStats = mapOf(
                                "total" to total,
                                "approved" to approved,
                                "rejected" to rejected,
                                "pending" to pending,
                                "cancelled" to cancelled,
                            )
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { Text(emailValue.firstOrNull()?.uppercase() ?: "?") }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(emailValue, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val avg = data?.first ?: 0.0
                                val count = data?.second ?: 0
                                Text("Calificación: ${String.format(Locale.getDefault(),"%.1f", avg)} ⭐ ($count)")
                            }
                            IconButton(onClick = {
                                favorite = !favorite
                                notesPrefs.edit { putBoolean("fav_$emailValue", favorite) }
                            }) { Icon(if (favorite) Icons.Filled.Place else Icons.Filled.Store, contentDescription = null) }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Badges
                        val badges = buildList {
                            val approved = userStats["approved"] ?: 0
                            val total = userStats["total"] ?: 0
                            if (approved >= 1) add("Primer evento")
                            if (approved >= 5) add("Asistente Frecuente")
                            if (approved >= 15) add("Veterano")
                            val rate = if (total > 0) approved * 100 / total else 0
                            if (rate >= 70) add("Alta Aprobación")
                            if ((data?.first ?: 0.0) >= 4.5 && (data?.second ?: 0) >= 3) add("Excelente")
                        }
                        if (badges.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                badges.forEach { b -> AssistChip(onClick = {}, label = { Text(b) }) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        // Estadísticas
                        val total = userStats["total"] ?: 0
                        Text("Solicitudes totales: $total")
                        Text("Aprobadas: ${userStats["approved"] ?: 0}")
                        Text("Pendientes: ${userStats["pending"] ?: 0}")
                        Text("Rechazadas: ${userStats["rejected"] ?: 0}")
                        Text("Canceladas: ${userStats["cancelled"] ?: 0}")
                        val rate = (userStats["approved"] ?: 0).let { a -> if (total>0) String.format(Locale.getDefault(),"%.0f%%", a*100.0/total) else "-" }
                        Text("Tasa de aprobación: $rate")
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        // Nueva reseña
                        var myRating by remember { mutableStateOf(5) }
                        var myComment by remember { mutableStateOf("") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tu calificación:")
                            Spacer(Modifier.width(8.dp))
                            var open by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { open = true }) { Text("$myRating ⭐") }
                                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                    (1..5).forEach { r -> DropdownMenuItem(text = { Text("$r ⭐") }, onClick = { myRating = r; open = false }) }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = myComment, onValueChange = { myComment = it }, label = { Text("Comentario (opcional)") })
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                reviewDao.insert(com.example.eventconnect.data.UserReview(
                                    reviewedUserEmail = emailValue,
                                    reviewerStore = storeName,
                                    rating = myRating,
                                    comment = myComment.ifBlank { null }
                                ))
                                AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                    PendingOperation(
                                        type = "USER_REVIEW_CREATE",
                                        payload = "{\"email\":\"$emailValue\",\"reviewerStore\":\"$storeName\",\"rating\":$myRating,\"comment\":${if (myComment.isBlank()) "null" else "" + myComment.replace("\"", "\\\"") + ""}}"
                                    )
                                )
                                loadProfile(emailValue)
                                remoteReviewSync.submit(UserReviewDto(reviewedUserEmail=emailValue, reviewerStore=storeName, rating=myRating, comment=myComment.ifBlank{null}))
                            }
                        }) { Text("Guardar reseña") }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Notas internas", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(value = profileNotes, onValueChange = { profileNotes = it; notesPrefs.edit { putString("notes_$emailValue", it) } }, label = { Text("Notas / observaciones") })
                        Spacer(Modifier.height(12.dp))
                        Text("Reseñas recientes", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        val list = data?.third.orEmpty()
                        if (list.isEmpty()) {
                            Text("Aún no hay reseñas para este usuario.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                list.take(6).forEach { review ->
                                    Column(Modifier.fillMaxWidth()) {
                                        Text("${review.reviewerStore} · ${review.rating} ⭐", fontWeight = FontWeight.SemiBold)
                                        review.comment?.let { c -> Text(c) }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProfile = false }) { Text("Cerrar") } }
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

    val remoteReservationSync = remember { RemoteReservationSync(context) }

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
                        val counts = mapOf<String?, Int>(
                            null to reservations.size,
                            "PENDING" to reservations.count { it.status == "PENDING" },
                            "APPROVED" to reservations.count { it.status == "APPROVED" },
                            "REJECTED" to reservations.count { it.status == "REJECTED" },
                            "CANCELLED" to reservations.count { it.status == "CANCELLED" },
                        )
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
                            LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(list) { r ->
                                    val evt = eventsById[r.eventId]
                                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
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
                                                                val payload = "{" + "\"reservationId\":${'$'}{current.id}" + "}"
                                                                AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                                                    PendingOperation(type = "RESERVATION_CANCEL", payload = payload)
                                                                )
                                                                remoteReservationSync.changeStatus(current.id, "CANCELLED")
                                                            }
                                                            val event = withContext(Dispatchers.IO) { db.eventDao().getEventById(r.eventId) }
                                                            NotificationHelper.showCancellationNotification(context, organizerName = event?.store, eventTitle = event?.title ?: "", reserverName = r.userName)
                                                            withContext(Dispatchers.IO) { loadData() }
                                                        }
                                                    }) { Text("Cancelar reserva") }
                                                }
                                            }
                                            // Acciones útiles extra
                                            if (evt != null) {
                                                Spacer(Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    val addr = listOf(evt.addressStreet, evt.addressNumber, evt.comuna, evt.region).filter { it.isNotBlank() }.joinToString(", ")
                                                    if (addr.isNotBlank()) {
                                                        OutlinedButton(onClick = {
                                                            val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(addr))
                                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                            mapIntent.setPackage("com.google.android.apps.maps")
                                                            context.startActivity(mapIntent)
                                                        }) { Text("Abrir mapa") }
                                                    }
                                                    OutlinedButton(onClick = {
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_SUBJECT, evt.title)
                                                            val body = buildString {
                                                                appendLine(evt.title)
                                                                appendLine("Fecha: ${evt.date}")
                                                                appendLine("Organiza: ${evt.store}")
                                                                if (addr.isNotBlank()) appendLine("Dirección: $addr")
                                                            }
                                                            putExtra(Intent.EXTRA_TEXT, body)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, "Compartir evento"))
                                                    }) { Text("Compartir") }
                                                    if (r.status == "APPROVED") {
                                                        OutlinedButton(onClick = {
                                                            val ok = CalendarUtils.addEventToCalendar(context, evt)
                                                            if (!ok) android.widget.Toast.makeText(context, "No se pudo abrir el calendario", android.widget.Toast.LENGTH_LONG).show()
                                                        }) { Text("Calendario") }
                                                    }
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

// Agregar helper local (fuera de composables) para inicio de hoy
private fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
