package com.example.eventconnect.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.window.Dialog

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
    var myLastReservation by remember { mutableStateOf<Reservation?>(null) }

    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
    val userEmail = prefs.getString("email", "") ?: ""

    LaunchedEffect(eventId, userEmail) {
        event = withContext(Dispatchers.IO) { eventDao.getEventById(eventId) }
        myLastReservation = withContext(Dispatchers.IO) { reservationDao.getLatestForEventAndUser(eventId, userEmail) }
    }

    AppBackground {
        event?.let { e ->
            val scrollState = rememberScrollState()
            val available = e.capacityAvailable ?: 0
            val hasPending = myLastReservation?.status == "PENDING"
            val isApproved = myLastReservation?.status == "APPROVED"

            // Estados y datos usados por el carrusel y el diálogo de zoom
            val images = e.imageUris ?: emptyList()
            var showZoom by rememberSaveable { mutableStateOf(false) }
            var zoomImage by remember { mutableStateOf<String?>(null) }
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }
            val transformState = remember {
                TransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 4f)
                    offsetX += panChange.x
                    offsetY += panChange.y
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                if (images.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { images.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) { page ->
                        val uri = images[page]
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen del evento",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    zoomImage = uri
                                    showZoom = true
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                        )
                    }
                    if (images.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(images.size) { idx ->
                                val active = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (active) Modifier.background(MaterialTheme.colorScheme.primary)
                                            else Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                                        )
                                )
                                if (idx != images.lastIndex) Spacer(Modifier.width(6.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(e.title, style = MaterialTheme.typography.headlineSmall)
                Text("Categoría: ${e.category}")
                Text("Fecha: ${e.date}")
                Text("Organiza: ${e.store}")
                if (e.region.isNotBlank()) Text("Región: ${e.region}")
                if (e.comuna.isNotBlank()) Text("Comuna: ${e.comuna}")
                val address = listOf(e.addressStreet, e.addressNumber).filter { it.isNotBlank() }.joinToString(" ")
                if (address.isNotBlank()) Text("Dirección: $address")
                Spacer(Modifier.height(12.dp))
                Text(e.description)
                Spacer(Modifier.height(16.dp))

                if (e.hasCapacity) {
                    Text("Cupos disponibles: $available")
                    Spacer(Modifier.height(8.dp))
                    val canRequest = available > 0 && !hasPending && !isApproved
                    val buttonLabel = when {
                        isApproved -> "Reserva aprobada"
                        hasPending -> "Solicitud en revisión"
                        available <= 0 -> "Sin cupos"
                        else -> "Solicitar cupo"
                    }
                    Button(onClick = {
                        val nowAvailable = (event?.capacityAvailable ?: 0)
                        if (nowAvailable <= 0) {
                            Toast.makeText(context, "No quedan cupos disponibles", Toast.LENGTH_LONG).show()
                            return@Button
                        }
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
                                // Encola op de crear reserva
                                val payload = "{" +
                                    "\"eventId\":${e.id}," +
                                    "\"userEmail\":\"$userEmail\"" +
                                    "}"
                                AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                    PendingOperation(type = "RESERVATION_CREATE", payload = payload)
                                )
                            }
                            NotificationHelper.showReservationNotification(
                                context = context,
                                organizerName = e.store,
                                eventTitle = e.title,
                                reserverName = userName
                            )
                            Toast.makeText(context, "Solicitud enviada a la tienda", Toast.LENGTH_LONG).show()
                            myLastReservation = Reservation(eventId = e.id, userEmail = userEmail, userName = userName, status = "PENDING")
                        }
                    }, enabled = canRequest) { Text(buttonLabel) }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("Volver") }
            }

            if (showZoom && zoomImage != null) {
                Dialog(onDismissRequest = { showZoom = false }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = zoomImage,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .transformable(transformState)
                        )
                    }
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
