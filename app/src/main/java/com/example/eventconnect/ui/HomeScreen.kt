package com.example.eventconnect.ui

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventconnect.R
import com.example.eventconnect.data.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EventUi(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val date: String,
    val store: String,
    val imageUri: String? = null,
    val hasCapacity: Boolean = false,
    val capacityAvailable: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    onOpenEvent: ((Int) -> Unit)? = null,
    onEditEvent: ((Int) -> Unit)? = null,
    onOpenReservations: (() -> Unit)? = null,
    onOpenRequests: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
    val userType = prefs.getString("type", "Usuario") ?: "Usuario"
    val storeName = prefs.getString("storeName", "") ?: ""

    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = remember { db.eventDao() }
    val scope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<EventUi>>(emptyList()) }

    suspend fun loadEvents() {
        val loaded = withContext(Dispatchers.IO) {
            if (userType == "Tienda" && storeName.isNotBlank()) {
                eventDao.getEventsByStore(storeName)
            } else {
                eventDao.getAllEvents()
            }
        }
        events = loaded.map { e ->
            EventUi(
                id = e.id,
                title = e.title,
                description = e.description,
                category = e.category,
                date = e.date,
                store = e.store,
                imageUri = e.imageUri,
                hasCapacity = e.hasCapacity,
                capacityAvailable = e.capacityAvailable
            )
        }
    }

    LaunchedEffect(userType, storeName) {
        isRefreshing = true
        val start = System.currentTimeMillis()
        try { loadEvents() } finally {
            val elapsed = System.currentTimeMillis() - start
            val min = 1200L
            if (elapsed < min) delay(min - elapsed)
            isRefreshing = false
        }
    }

    val frases = listOf(
        "Cada carta cuenta una historia.",
        "Nunca subestimes una buena estrategia.",
        "Recuerda hidratarte y sonreír.",
        "Hoy puede ser el día de tu mejor jugada.",
        "El mejor movimiento es disfrutar el juego.",
        "¡El azar favorece a los valientes!",
        "A veces se gana, a veces se aprende.",
        "Piensa rápido, juega mejor."
    )
    var fraseActual by remember { mutableStateOf(frases.random()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            fraseActual = frases.random()
        }
    }

    val gradientBackground = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D47A1),
                Color(0xFF1976D2),
                Color(0xFF42A5F5)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EventConnect", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0)),
                actions = {
                    if (userType == "Tienda") {
                        TextButton(onClick = { onOpenRequests?.invoke() }) { Text("Solicitudes", color = Color.White) }
                    } else {
                        TextButton(onClick = { onOpenReservations?.invoke() }) { Text("Mis reservas", color = Color.White) }
                    }
                    TextButton(onClick = {
                        scope.launch {
                            isRefreshing = true
                            val start = System.currentTimeMillis()
                            try {
                                loadEvents()
                            } finally {
                                val elapsed = System.currentTimeMillis() - start
                                val min = 1200L
                                if (elapsed < min) delay(min - elapsed)
                                isRefreshing = false
                            }
                        }
                    }) { Text("Actualizar", color = Color.White) }
                    TextButton(onClick = onLogout) { Text("Cerrar sesión", color = Color.White) }
                }
            )
        },
        floatingActionButton = {
            if (userType == "Tienda") {
                FloatingActionButton(
                    onClick = onCreateEvent,
                    containerColor = Color(0xFF0D47A1),
                    contentColor = Color.White
                ) { Text("+", fontSize = 24.sp) }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hola, $userName 👋",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Crossfade(targetState = fraseActual, label = "frase") { frase ->
                    Text(
                        text = frase,
                        color = Color(0xFFE3F2FD),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }

                Text(
                    text = if (userType == "Tienda") "Tus Eventos Publicados" else "Eventos Recomendados",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (events.isEmpty() && !isRefreshing) {
                    Text("No hay eventos por ahora", color = Color.White)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(events) { event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (userType == "Tienda") onEditEvent?.invoke(event.id) else onOpenEvent?.invoke(event.id)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(6.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    if (event.imageUri != null) {
                                        coil.compose.AsyncImage(
                                            model = event.imageUri,
                                            contentDescription = "Imagen del evento",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                                    Text("Categoría: ${event.category}", color = Color.Gray)
                                    Text("Fecha: ${event.date}", color = Color.Gray)
                                    Text("Organiza: ${event.store}", color = Color.Gray)
                                    if (event.hasCapacity) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Cupos disponibles: ${event.capacityAvailable ?: 0}", color = Color(0xFF2E7D32))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(event.description)
                                }
                            }
                        }
                    }
                }
            }

            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    SearchLoading(message = "Buscando...", drawableRes = R.drawable.search_anim)
                }
            }
        }
    }
}
