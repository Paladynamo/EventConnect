package com.example.eventconnect.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class EventUi(
    val title: String,
    val description: String,
    val category: String,
    val date: String,
    val store: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    // 🛈 Tomamos nombre y tipo desde SharedPreferences (guardados en Login)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
    val userType = prefs.getString("type", "Usuario") ?: "Usuario"

    val events = listOf(
        EventUi("Torneo Pokémon TCG", "Competencia regional de cartas Pokémon", "TCG", "10 Nov 2025", "GeekStore"),
        EventUi("Lanzamiento Catan Expansión", "Nueva edición de Catan Exploradores", "Juegos de Mesa", "22 Oct 2025", "MeepleLand"),
        EventUi("Expo Anime Santiago", "Feria de cultura japonesa y cosplay", "Eventos Anime", "5 Dic 2025", "OtakuPro")
    )

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
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            visible = false
            delay(250)
            fraseActual = frases.random()
            visible = true
            delay(6000)
        }
    }

    val gradientBackground = Brush.linearGradient(
        listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5)),
        start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EventConnect", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0)),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Cerrar sesión", color = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (userType == "Tienda") {
                FloatingActionButton(
                    onClick = { /* TODO: abrir crear evento */ },
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
                AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = fraseActual,
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

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(events) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                                Text("Categoría: ${event.category}", color = Color.Gray)
                                Text("Fecha: ${event.date}", color = Color.Gray)
                                Text("Organiza: ${event.store}", color = Color.Gray)
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
