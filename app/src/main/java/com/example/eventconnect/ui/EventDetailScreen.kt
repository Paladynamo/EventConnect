package com.example.eventconnect.ui

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlin.math.absoluteValue
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.eventconnect.reminder.EventReminderWorker
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventconnect.ui.vm.EventDetailViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    eventId: Int,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // ViewModel
    val vm: EventDetailViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
    val userEmail = prefs.getString("email", "") ?: ""

    LaunchedEffect(eventId, userEmail) {
        vm.load(eventId, userEmail)
    }

    val event by vm.event.collectAsState()
    val myLastReservation by vm.myReservation.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    AppBackground {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                event?.let { e ->
                    val scrollState = rememberScrollState()
                    val available = e.capacityAvailable ?: 0
                    val hasPending = myLastReservation?.status == "PENDING"
                    val isApproved = myLastReservation?.status == "APPROVED"

                    // Estados y datos usados por el carrusel y el diálogo de zoom
                    val images = e.imageUris ?: emptyList()
                    var showZoom by rememberSaveable { mutableStateOf(false) }
                    var zoomImage by remember { mutableStateOf<String?>(null) }
                    var zoomScale by remember { mutableStateOf(1f) }
                    var zoomOffsetX by remember { mutableStateOf(0f) }
                    var zoomOffsetY by remember { mutableStateOf(0f) }
                    val transformState = remember {
                        TransformableState { zoomChange, panChange, _ ->
                            zoomScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
                            zoomOffsetX += panChange.x
                            zoomOffsetY += panChange.y
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
                                    .height(260.dp)
                            ) { page ->
                                val uri = images[page]
                                val pageOffset = (
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                ).absoluteValue
                                val pageScale = 0.85f + (1 - pageOffset) * 0.15f
                                val pageAlpha = 0.7f + (1 - pageOffset) * 0.3f
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Imagen del evento",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            this.scaleX = pageScale
                                            this.scaleY = pageScale
                                            this.alpha = pageAlpha
                                        }
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            zoomImage = uri
                                            showZoom = true
                                            zoomScale = 1f
                                            zoomOffsetX = 0f
                                            zoomOffsetY = 0f
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
                                if ((event?.capacityAvailable ?: 0) <= 0) {
                                    Toast.makeText(context, "No quedan cupos disponibles", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                vm.requestSeat(e, userEmail, userName) {
                                    Toast.makeText(context, "Solicitud enviada a la tienda", Toast.LENGTH_LONG).show()
                                }
                            }, enabled = canRequest) { Text(buttonLabel) }
                        } else {
                            // Evento sin cupo: recordar
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilledTonalButton(onClick = {
                                    val ok = CalendarUtils.addEventToCalendar(context, e)
                                    vm.likeEvent(e.id, userEmail)
                                    scope.launch { snackbarHostState.showSnackbar("Abriendo calendario…") }
                                    if (!ok) Toast.makeText(context, "No se pudo abrir el calendario", Toast.LENGTH_LONG).show()
                                }) {
                                    Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Calendario")
                                }
                                // Recordatorio interno a las 8:00 AM del día del evento (WorkManager)
                                val lastWorkId = remember { mutableStateOf<java.util.UUID?>(null) }
                                FilledTonalButton(onClick = {
                                    val millisAt8 = parseDateToMorningMillis(e.date) ?: run {
                                        Toast.makeText(context, "Fecha inválida del evento", Toast.LENGTH_LONG).show(); return@FilledTonalButton
                                    }
                                    val delayMs = millisAt8 - System.currentTimeMillis()
                                    if (delayMs <= 0) { Toast.makeText(context, "El evento ya pasó o es hoy después de las 8:00", Toast.LENGTH_LONG).show(); return@FilledTonalButton }
                                    val data = Data.Builder()
                                        .putString(EventReminderWorker.KEY_TITLE, e.title)
                                        .putString(EventReminderWorker.KEY_MESSAGE, "Recordatorio: hoy es \"${e.title}\"")
                                        .build()
                                    val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                                        .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                                        .setInputData(data)
                                        .build()
                                    WorkManager.getInstance(context).enqueue(request)
                                    lastWorkId.value = request.id
                                    vm.likeEvent(e.id, userEmail)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Recordatorio 8:00 programado",
                                            actionLabel = "Deshacer",
                                            withDismissAction = true
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            WorkManager.getInstance(context).cancelWorkById(request.id)
                                            snackbarHostState.showSnackbar("Recordatorio cancelado")
                                        }
                                    }
                                }) {
                                    Icon(imageVector = Icons.Filled.Alarm, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Recordatorio 8:00")
                                }
                            }
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
                                            scaleX = zoomScale,
                                            scaleY = zoomScale,
                                            translationX = zoomOffsetX,
                                            translationY = zoomOffsetY
                                        )
                                        .transformable(transformState)
                                )
                            }
                        }
                    }
                } ?: run {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

private fun parseDateToMorningMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm",
        "dd-MM-yyyy HH:mm",
        "dd/MM/yyyy HH:mm",
        "yyyy/MM/dd HH:mm",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
    )
    var parsed: java.util.Date? = null
    var hasTime = false
    for (p in patterns) {
        val sdf = java.text.SimpleDateFormat(p, java.util.Locale.getDefault())
        sdf.isLenient = false
        try {
            parsed = sdf.parse(dateStr)
            hasTime = p.contains("H")
            if (parsed != null) break
        } catch (_: java.text.ParseException) { }
    }
    if (parsed == null) return null
    val cal = java.util.Calendar.getInstance().apply { time = parsed }
    if (!hasTime) {
        cal.set(java.util.Calendar.HOUR_OF_DAY, 8)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
