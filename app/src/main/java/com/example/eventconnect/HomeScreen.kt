package com.example.eventconnect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventconnect.data.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userType: String,
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) } // ✅ FIX
    val eventDao = db.eventDao()
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf(listOf<Event>()) }

    // Cargar eventos desde Room
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            events = eventDao.getAllEvents()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EventConnect") },
                actions = {
                    TextButton(onClick = { onLogout() }) {
                        Text("Cerrar sesión", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (userType == "Tienda/Productor") {
                FloatingActionButton(onClick = onCreateEvent) { Text("+") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = if (userType == "Tienda/Productor") "Tus Eventos Publicados" else "Eventos Recomendados",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay eventos disponibles", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(events) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Categoría: ${event.category}")
                                Text("Fecha: ${event.date}")
                                Text("Cupos: ${event.capacity}")
                                Text("Organiza: ${event.store}")
                                Spacer(Modifier.height(6.dp))
                                Text(event.description)
                            }
                        }
                    }
                }
            }
        }
    }
}
