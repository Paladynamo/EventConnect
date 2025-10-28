package com.example.eventconnect.ui

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventconnect.R
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.location.ChileLocations
import com.example.eventconnect.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import kotlin.coroutines.resume
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

// Modelo UI enriquecido

data class EventUi(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val date: String,
    val store: String,
    val imageUri: String? = null,
    val hasCapacity: Boolean = false,
    val capacityAvailable: Int? = null,
    val region: String = "",
    val comuna: String = "",
    val address: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    onOpenEvent: ((Int) -> Unit)? = null,
    onEditEvent: ((Int) -> Unit)? = null,
    onOpenReservations: (() -> Unit)? = null,
    onOpenRequests: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
    val userType = prefs.getString("type", "Usuario") ?: "Usuario"
    val storeName = prefs.getString("storeName", "") ?: ""

    // Preferencias de ubicación del usuario común
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var selRegion by remember { mutableStateOf(appPrefs.getString("region", "") ?: "") }
    var selComuna by remember { mutableStateOf(appPrefs.getString("comuna", "") ?: "") }
    var selCategory by remember { mutableStateOf("") }

    // Permisos de ubicación y helper
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grants ->
            // Se maneja dentro de la función tryUseLocation via estado
        }
    )

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
                if (selRegion.isNotBlank() && selComuna.isNotBlank()) {
                    if (selCategory.isNotBlank()) eventDao.getEventsByComunaAndCategory(selRegion, selComuna, selCategory)
                    else eventDao.getEventsByComuna(selRegion, selComuna)
                } else {
                    eventDao.getAllEvents()
                }
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
                capacityAvailable = e.capacityAvailable,
                region = e.region,
                comuna = e.comuna,
                address = listOf(e.addressStreet, e.addressNumber).filter { it.isNotBlank() }.joinToString(" ")
            )
        }
    }

    LaunchedEffect(userType, storeName, selRegion, selComuna, selCategory) {
        isRefreshing = true
        val start = System.currentTimeMillis()
        try { loadEvents() } finally {
            val elapsed = System.currentTimeMillis() - start
            val min = 800L
            if (elapsed < min) delay(min - elapsed)
            isRefreshing = false
        }
    }

    // Frases y estilo
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
                Color(0xFF0D47A1).copy(alpha = 0.92f),
                Color(0xFF1976D2).copy(alpha = 0.92f),
                Color(0xFF42A5F5).copy(alpha = 0.92f)
            )
        )
    }

    // Diálogo inicial para elegir ubicación para usuario común
    var showLocationDialog by remember { mutableStateOf(userType != "Tienda" && (selRegion.isBlank() || selComuna.isBlank())) }

    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.getDefault())

    suspend fun reverseGeocodeRegionComuna(lat: Double, lon: Double): Pair<String?, String?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: usar listener async
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                try {
                    val geocoder = Geocoder(context, Locale("es", "CL"))
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val addr = addresses.firstOrNull()
                            val admin = addr?.adminArea ?: addr?.subAdminArea
                            val locality = addr?.locality ?: addr?.subLocality
                            val normAdmin = admin?.let { normalize(it) }
                            val regionMatch = ChileLocations.regiones.firstOrNull { normalize(it).contains(normAdmin ?: "") || (normAdmin?.contains(normalize(it)) == true) }
                            val candidateComunas = ChileLocations.comunasPorRegion[regionMatch] ?: emptyList()
                            val normLoc = locality?.let { normalize(it) }
                            val comunaMatch = candidateComunas.firstOrNull { normalize(it).contains(normLoc ?: "") || (normLoc?.contains(normalize(it)) == true) }
                            cont.resume(regionMatch to comunaMatch)
                        }
                        override fun onError(errorMessage: String?) {
                            cont.resume(null to null)
                        }
                    })
                } catch (_: Exception) {
                    cont.resume(null to null)
                }
            }
        } else {
            try {
                val geocoder = Geocoder(context, Locale("es", "CL"))
                @Suppress("DEPRECATION")
                val results = withContext(Dispatchers.IO) { geocoder.getFromLocation(lat, lon, 1) }
                if (!results.isNullOrEmpty()) {
                    val addr = results[0]
                    val admin = addr.adminArea ?: addr.subAdminArea
                    val locality = addr.locality ?: addr.subLocality
                    val normAdmin = admin?.let { normalize(it) }
                    val regionMatch = ChileLocations.regiones.firstOrNull { normalize(it).contains(normAdmin ?: "") || (normAdmin?.contains(normalize(it)) == true) }
                    val candidateComunas = ChileLocations.comunasPorRegion[regionMatch] ?: emptyList()
                    val normLoc = locality?.let { normalize(it) }
                    val comunaMatch = candidateComunas.firstOrNull { normalize(it).contains(normLoc ?: "") || (normLoc?.contains(normalize(it)) == true) }
                    regionMatch to comunaMatch
                } else null to null
            } catch (_: Exception) {
                null to null
            }
        }
    }

    fun tryUseLocation() {
        permissionLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        scope.launch {
            val helper = LocationHelper(context)
            val loc = withContext(Dispatchers.IO) { helper.getLastLocation() }
            if (loc != null) {
                val (reg, com) = reverseGeocodeRegionComuna(loc.latitude, loc.longitude)
                if (!reg.isNullOrBlank()) { selRegion = reg; appPrefs.edit().putString("region", reg).apply() }
                if (!com.isNullOrBlank()) { selComuna = com; appPrefs.edit().putString("comuna", com).apply() }
            }
        }
    }

    // Sugerencia de ubicación si difiere de guardada (sin pedir permiso si no lo tiene)
    var suggestRegion by remember { mutableStateOf<String?>(null) }
    var suggestComuna by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(userType) {
        if (userType != "Tienda") {
            val fine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine == android.content.pm.PackageManager.PERMISSION_GRANTED || coarse == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val helper = LocationHelper(context)
                val loc = withContext(Dispatchers.IO) { helper.getLastLocation() }
                if (loc != null) {
                    val (reg, com) = reverseGeocodeRegionComuna(loc.latitude, loc.longitude)
                    if (!reg.isNullOrBlank() && !com.isNullOrBlank()) {
                        val diff = normalize(reg) != normalize(selRegion) || normalize(com) != normalize(selComuna)
                        if (diff) { suggestRegion = reg; suggestComuna = com }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            "EventConnect",
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0)),
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = Color.White) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (userType == "Tienda") {
                            DropdownMenuItem(text = { Text("Solicitudes") }, onClick = { menuExpanded = false; onOpenRequests?.invoke() })
                        } else {
                            DropdownMenuItem(text = { Text("Mis reservas") }, onClick = { menuExpanded = false; onOpenReservations?.invoke() })
                        }
                        DropdownMenuItem(text = { Text("Perfil") }, onClick = { menuExpanded = false; onOpenProfile?.invoke() })
                        DropdownMenuItem(text = { Text("Actualizar") }, onClick = {
                            menuExpanded = false
                            scope.launch {
                                isRefreshing = true
                                val start = System.currentTimeMillis()
                                try { loadEvents() } finally {
                                    val elapsed = System.currentTimeMillis() - start
                                    val min = 800L
                                    if (elapsed < min) delay(min - elapsed)
                                    isRefreshing = false
                                }
                            }
                        })
                        DropdownMenuItem(text = { Text("Cerrar sesión") }, onClick = { menuExpanded = false; onLogout() })
                    }
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
                .padding(padding)
        ) {
            // Fondo global
            AppBackground { }

            // Aplicar fondo a la pantalla principal
            Box(modifier = Modifier.fillMaxSize().background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color.Blue)
                )
            )) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBackground)
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

                    if (userType != "Tienda") {
                        // Nuevo layout de filtros con chips y FlowRow para evitar desbordes
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            var regionExpanded by remember { mutableStateOf(false) }
                            var comunaExpanded by remember { mutableStateOf(false) }
                            var categoryExpanded by remember { mutableStateOf(false) }

                            FilterChip(selected = selRegion.isNotBlank(), onClick = { regionExpanded = true }, label = { Text(if (selRegion.isBlank()) "Región" else selRegion) })
                            DropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                                ChileLocations.regiones.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = {
                                    selRegion = r
                                    selComuna = ""
                                    appPrefs.edit().putString("region", r).apply()
                                    regionExpanded = false
                                }) }
                            }

                            FilterChip(selected = selComuna.isNotBlank(), onClick = { if (selRegion.isNotBlank()) comunaExpanded = true }, enabled = selRegion.isNotBlank(), label = { Text(if (selComuna.isBlank()) "Comuna" else selComuna) })
                            DropdownMenu(expanded = comunaExpanded, onDismissRequest = { comunaExpanded = false }) {
                                (ChileLocations.comunasPorRegion[selRegion] ?: emptyList()).forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = {
                                    selComuna = c
                                    appPrefs.edit().putString("comuna", c).apply()
                                    comunaExpanded = false
                                }) }
                            }

                            FilterChip(selected = selCategory.isNotBlank(), onClick = { categoryExpanded = true }, label = { Text(if (selCategory.isBlank()) "Categoría" else selCategory) })
                            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                listOf("", "TCG", "Juegos de Mesa", "Eventos").forEach { c ->
                                    val label = if (c.isBlank()) "Todas" else c
                                    DropdownMenuItem(text = { Text(label) }, onClick = { selCategory = c; categoryExpanded = false })
                                }
                            }

                            AssistChip(onClick = { tryUseLocation() }, label = { Text("Mi ubicación") })
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = if (userType == "Tienda") "Tus Eventos Publicados" else if (selComuna.isNotBlank()) "Eventos en $selComuna" else "Eventos Recomendados",
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
                                        if (event.region.isNotBlank()) Text("Región: ${event.region}", color = Color.Gray)
                                        if (event.comuna.isNotBlank()) Text("Comuna: ${event.comuna}", color = Color.Gray)
                                        if (event.address.isNotBlank()) Text("Dirección: ${event.address}", color = Color.Gray)
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

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("¿Dónde estás?") },
            text = {
                Column {
                    Text("Selecciona tu región y comuna para ver eventos cercanos.")
                    Spacer(Modifier.height(8.dp))
                    LocationSelectors(
                        region = selRegion,
                        comuna = selComuna,
                        onRegion = { selRegion = it; selComuna = "" },
                        onComuna = { selComuna = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    appPrefs.edit().putString("region", selRegion).apply()
                    appPrefs.edit().putString("comuna", selComuna).apply()
                    showLocationDialog = false
                }, enabled = selRegion.isNotBlank() && selComuna.isNotBlank()) { Text("Guardar") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { tryUseLocation() }) { Text("Usar mi ubicación") }
                    TextButton(onClick = { showLocationDialog = false }) { Text("Más tarde") }
                }
            }
        )
    }

    // Diálogo de sugerencia para actualizar ubicación si difiere de la detectada
    if (suggestRegion != null && suggestComuna != null) {
        AlertDialog(
            onDismissRequest = { suggestRegion = null; suggestComuna = null },
            title = { Text("Ubicación detectada") },
            text = { Text("Parece que estás cerca de ${suggestComuna}, ${suggestRegion}. ¿Quieres actualizar tu ubicación guardada?") },
            confirmButton = {
                TextButton(onClick = {
                    val r = suggestRegion!!; val c = suggestComuna!!
                    selRegion = r; selComuna = c
                    appPrefs.edit().putString("region", r).apply()
                    appPrefs.edit().putString("comuna", c).apply()
                    suggestRegion = null; suggestComuna = null
                }) { Text("Actualizar") }
            },
            dismissButton = {
                TextButton(onClick = { suggestRegion = null; suggestComuna = null }) { Text("Mantener actual") }
            }
        )
    }
}

@Composable
private fun LocationSelectors(
    region: String,
    comuna: String,
    onRegion: (String) -> Unit,
    onComuna: (String) -> Unit
) {
    var regionExpanded by remember { mutableStateOf(false) }
    var comunaExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = region, onValueChange = {}, enabled = false, label = { Text("Región") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Button(onClick = { regionExpanded = true }) { Text(if (region.isBlank()) "Elegir región" else "Cambiar región") }
        DropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
            ChileLocations.regiones.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { onRegion(r); regionExpanded = false }) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = comuna, onValueChange = {}, enabled = false, label = { Text("Comuna") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        val comunas = ChileLocations.comunasPorRegion[region] ?: emptyList()
        Button(onClick = { comunaExpanded = true }, enabled = region.isNotBlank()) { Text(if (comuna.isBlank()) "Elegir comuna" else "Cambiar comuna") }
        DropdownMenu(expanded = comunaExpanded, onDismissRequest = { comunaExpanded = false }) {
            comunas.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onComuna(c); comunaExpanded = false }) }
        }
    }
}
