package com.example.eventconnect.ui

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.example.eventconnect.R
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.EventUpdateNotice
import com.example.eventconnect.location.ChileLocations
import com.example.eventconnect.location.LocationHelper
import com.example.eventconnect.location.fetchRemoteLocations
import com.example.eventconnect.remote.HolidayProvider
import com.example.eventconnect.ui.vm.HomeViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import kotlin.coroutines.resume
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.eventconnect.data.AppDatabase

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
    val application = context.applicationContext as Application
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userName = prefs.getString("name", "Usuario") ?: "Usuario"
    val userType = prefs.getString("type", "Usuario") ?: "Usuario"
    val storeName = prefs.getString("storeName", "") ?: ""
    val email = prefs.getString("email", "") ?: ""
    val userId = prefs.getLong("userId", -1L)

    val vm: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(application) as T
            }
        }
    )

    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var selRegion by rememberSaveable { mutableStateOf(appPrefs.getString("region", "") ?: "") }
    var selComuna by rememberSaveable { mutableStateOf(appPrefs.getString("comuna", "") ?: "") }
    var selCategory by rememberSaveable { mutableStateOf("") }

    // Estados para confirmación de ubicación detectada y selección manual
    var showDetectedDialog by remember { mutableStateOf(false) }
    var detectedRegion by remember { mutableStateOf<String?>(null) }
    var detectedComuna by remember { mutableStateOf<String?>(null) }
    var manualSelectMode by remember { mutableStateOf(false) }
    var manualRegion by remember { mutableStateOf("") }
    var manualComuna by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }

    // Utilidad de validación/canonización de comuna acorde a región
    fun normalize(s: String?): String = Normalizer.normalize(s ?: "", Normalizer.Form.NFD)
        .replace("\\p{Mn}".toRegex(), "").lowercase(Locale.getDefault()).trim()

    fun canonicalComunaForRegion(region: String, comuna: String): String? {
        if (region.isBlank() || comuna.isBlank()) return null
        val list = ChileLocations.comunasFor(region)
        return list.firstOrNull { it.equals(comuna, ignoreCase = true) }
    }

    // Al iniciar la pantalla, si la comuna guardada no corresponde a la región, limpiarla/canonizarla
    LaunchedEffect(Unit) {
        if (selRegion.isNotBlank() && selComuna.isNotBlank()) {
            val canon = canonicalComunaForRegion(selRegion, selComuna)
            if (canon == null) { selComuna = ""; appPrefs.edit { putString("comuna", "") } }
            else if (canon != selComuna) { selComuna = canon; appPrefs.edit { putString("comuna", canon) } }
        }
    }

    // Cada vez que cambia la región, asegurar que la comuna siga siendo válida
    LaunchedEffect(selRegion) {
        if (selRegion.isNotBlank() && selComuna.isNotBlank()) {
            val canon = canonicalComunaForRegion(selRegion, selComuna)
            if (canon == null) { selComuna = ""; appPrefs.edit { putString("comuna", "") } }
            else if (canon != selComuna) { selComuna = canon; appPrefs.edit { putString("comuna", canon) } }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { }
    )
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Recoger eventos y mapear a UI
    val eventsState = vm.events.collectAsState(initial = emptyList())
    val eventsRaw: List<Event> = eventsState.value
    val events: List<EventUi> = remember(eventsRaw) {
        eventsRaw.map { e ->
            EventUi(
                id = e.id,
                title = e.title,
                description = e.description,
                category = e.category,
                date = e.date,
                store = e.store,
                imageUri = e.imageUris?.firstOrNull(),
                hasCapacity = e.hasCapacity,
                capacityAvailable = e.capacityAvailable,
                region = e.region,
                comuna = e.comuna,
                address = listOf(e.addressStreet, e.addressNumber).filter { it.isNotBlank() }.joinToString(" ")
            )
        }
    }

    // Cargar avisos
    val noticesState = vm.notices.collectAsState(initial = emptyList())
    val notices: List<EventUpdateNotice> = noticesState.value
    var showNotices by remember { mutableStateOf(false) }
    LaunchedEffect(email) { vm.loadNotices(email) }
    LaunchedEffect(notices) { if (notices.isNotEmpty()) showNotices = true }

    // Cargar eventos segun filtros
    LaunchedEffect(userType, storeName, selRegion, selComuna, selCategory) {
        isRefreshing = true
        val start = System.currentTimeMillis()
        try {
            when {
                userType.uppercase() == "TIENDA" && userId != -1L -> vm.loadByStore(userId)
                selRegion.isNotBlank() && selComuna.isNotBlank() && selCategory.isNotBlank() -> vm.loadByRegionComunaCategory(selRegion, selComuna, selCategory)
                selRegion.isNotBlank() && selComuna.isNotBlank() -> vm.loadByRegionComuna(selRegion, selComuna)
                selRegion.isNotBlank() && selCategory.isNotBlank() -> vm.loadByRegionCategory(selRegion, selCategory)
                selRegion.isNotBlank() -> vm.loadByRegion(selRegion)
                else -> vm.loadAll()
            }
        } finally {
            val elapsed = System.currentTimeMillis() - start
            val min = 800L
            if (elapsed < min) delay(min - elapsed)
            isRefreshing = false
        }
    }

    // Frases
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

    // Diálogo inicial para elegir ubicación para usuario común
    var showLocationDialog by remember { mutableStateOf(false) }
    LaunchedEffect(userType) {
        if (userType != "Tienda") {
            // Mostrar solo si nunca se configuró una región+comuna válidas y el usuario no lo descartó
            val dismissed = appPrefs.getBoolean("location_prompt_dismissed", false)
            showLocationDialog = (selRegion.isBlank() || selComuna.isBlank()) && !dismissed
        }
    }

    suspend fun reverseGeocodeRegionComuna(lat: Double, lon: Double): Pair<String?, String?> {
        fun matchRegionAndComuna(admin: String?, locality: String?): Pair<String?, String?> {
            val normAdmin = normalize(admin)
            val regionMatch = ChileLocations.regions().firstOrNull {
                val n = normalize(it)
                n.contains(normAdmin) || normAdmin.contains(n)
            }
            val candidateComunas = if (regionMatch != null) ChileLocations.comunasFor(regionMatch) else emptyList()
            val normLoc = normalize(locality)
            val comunaMatch = candidateComunas.firstOrNull {
                val n = normalize(it)
                n.contains(normLoc) || normLoc.contains(n)
            }
            return regionMatch to comunaMatch
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                try {
                    val geocoder = Geocoder(context, Locale("es", "CL"))
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val addr = addresses.firstOrNull()
                            val res = matchRegionAndComuna(addr?.adminArea ?: addr?.subAdminArea, addr?.locality ?: addr?.subLocality)
                            cont.resume(res)
                        }
                        override fun onError(errorMessage: String?) { cont.resume(null to null) }
                    })
                } catch (_: Exception) { cont.resume(null to null) }
            }
        } else {
            try {
                val geocoder = Geocoder(context, Locale("es", "CL"))
                @Suppress("DEPRECATION")
                val results = withContext(Dispatchers.IO) { geocoder.getFromLocation(lat, lon, 1) }
                if (!results.isNullOrEmpty()) {
                    val addr = results[0]
                    matchRegionAndComuna(addr.adminArea ?: addr.subAdminArea, addr.locality ?: addr.subLocality)
                } else null to null
            } catch (_: Exception) { null to null }
        }
    }

    fun tryUseLocation() {
        permissionLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        scope.launch {
            isLocating = true
            val helper = LocationHelper(context)
            val enabled = helper.isLocationEnabled(context)
            if (!enabled) {
                isLocating = false
                Toast.makeText(context, "Activa la ubicación del dispositivo", Toast.LENGTH_LONG).show()
                return@launch
            }
            val loc = withContext(Dispatchers.IO) { helper.getCurrentLocationOrLast() }
            if (loc != null) {
                val (reg, com) = reverseGeocodeRegionComuna(loc.latitude, loc.longitude)
                if (!reg.isNullOrBlank()) {
                    detectedRegion = reg
                    // Canonizar comuna si existe
                    if (!com.isNullOrBlank()) {
                        val canon = canonicalComunaForRegion(reg, com)
                        detectedComuna = canon ?: ""
                    } else detectedComuna = null
                    manualRegion = detectedRegion ?: ""
                    manualComuna = detectedComuna ?: ""
                    showDetectedDialog = true
                    manualSelectMode = false
                } else {
                    Toast.makeText(context, "No se pudo derivar región desde tu ubicación", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "No se pudo obtener tu ubicación", Toast.LENGTH_LONG).show()
            }
            isLocating = false
        }
    }

    // Sugerencia basada en GPS con cooldown de 24h para no molestar
    var suggestRegion by remember { mutableStateOf<String?>(null) }
    var suggestComuna by remember { mutableStateOf<String?>(null) }

    // Al iniciar, intento cargar regiones/comunas del backend y aplicar override dinámico si hay datos
    LaunchedEffect(Unit) {
        val remote = fetchRemoteLocations()
        if (remote != null) {
            val (regions, byRegion) = remote
            if (regions.isNotEmpty()) {
                ChileLocations.setDynamic(regions, byRegion)
                if (selRegion.isNotBlank() && !ChileLocations.regions().contains(selRegion)) {
                    selRegion = ""
                    appPrefs.edit { putString("region", "") }
                    selComuna = ""
                    appPrefs.edit { putString("comuna", "") }
                } else if (selRegion.isNotBlank() && selComuna.isNotBlank()) {
                    val canon = canonicalComunaForRegion(selRegion, selComuna)
                    if (canon == null) { selComuna = ""; appPrefs.edit { putString("comuna", "") } }
                    else if (canon != selComuna) { selComuna = canon; appPrefs.edit { putString("comuna", canon) } }
                }
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    // DAOs y badge de solicitudes pendientes para tienda
    val db = remember { AppDatabase.getDatabase(context) }
    val reservationDao = remember { db.reservationDao() }
    val eventDao = remember { db.eventDao() }
    var pendingRequests by remember { mutableStateOf(0) }

    LaunchedEffect(userType, storeName, isRefreshing) {
        if (userType == "Tienda" && storeName.isNotBlank()) {
            val count = withContext(Dispatchers.IO) {
                val evs = eventDao.getEventsByStore(storeName)
                val ids = evs.map { it.id }
                if (ids.isEmpty()) 0 else reservationDao.getForEvents(ids).count { it.status == "PENDING" }
            }
            pendingRequests = count
        } else pendingRequests = 0
    }

    // Estado para próximos feriados
    var upcomingHolidays by remember { mutableStateOf<List<com.example.eventconnect.remote.service.HolidayDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        val list = try { HolidayProvider.upcomingChile(context) } catch (_: Exception) { emptyList() }
        upcomingHolidays = list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row { Text("EventConnect", color = colors.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.primary),
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = colors.onPrimary)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (userType.uppercase() == "TIENDA") {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Solicitudes")
                                        if (pendingRequests > 0) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
                                        }
                                    }
                                },
                                onClick = { menuExpanded = false; onOpenRequests?.invoke() }
                            )
                        } else {
                            val hasUpdates = notices.isNotEmpty()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Mis reservas")
                                        if (hasUpdates) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFA000)))
                                        }
                                    }
                                },
                                onClick = { menuExpanded = false; onOpenReservations?.invoke() }
                            )
                        }
                        DropdownMenuItem(text = { Text("Perfil") }, onClick = { menuExpanded = false; onOpenProfile?.invoke() })
                        DropdownMenuItem(text = { Text("Actualizar") }, onClick = {
                            menuExpanded = false
                            scope.launch {
                                isRefreshing = true
                                val start = System.currentTimeMillis()
                                try { vm.loadAll() } finally {
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
            if (userType.uppercase() == "TIENDA") {
                FloatingActionButton(
                    onClick = onCreateEvent,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ) { Text("+", fontSize = 24.sp) }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Fondo con imagen suave
            AppBackground { }

            // Capa de superficie tenue para mejorar legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surface.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = "Hola, $userName 👋",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }
                    if (userType == "Tienda" && storeName.isNotBlank()) {
                        Text(
                            text = "Dueño de ${storeName}",
                            color = colors.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )
                    }
                    Crossfade(targetState = fraseActual, label = "frase") { frase ->
                        Text(
                            text = frase,
                            color = colors.onSurface.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                    }

                    if (upcomingHolidays.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Próximos feriados", style = MaterialTheme.typography.titleSmall)
                                upcomingHolidays.forEach { h -> Text("• ${h.localName} (${h.date})", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (userType != "Tienda") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            var regionExpanded by remember { mutableStateOf(false) }
                            var comunaExpanded by remember { mutableStateOf(false) }
                            var categoryExpanded by remember { mutableStateOf(false) }

                            FilterChip(selected = selRegion.isNotBlank(), onClick = { regionExpanded = true }, label = { Text(if (selRegion.isBlank()) "Región" else selRegion) })
                            DropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                                ChileLocations.regions().forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = {
                                    val regionChanged = !r.equals(selRegion, ignoreCase = true)
                                    selRegion = r
                                    appPrefs.edit { putString("region", r) }
                                    if (regionChanged) { selComuna = ""; appPrefs.edit { putString("comuna", "") } }
                                    regionExpanded = false
                                }) }
                            }

                            FilterChip(selected = selComuna.isNotBlank(), onClick = { if (selRegion.isNotBlank()) comunaExpanded = true }, enabled = selRegion.isNotBlank(), label = { Text(if (selComuna.isBlank()) "Comuna" else selComuna) })
                            DropdownMenu(expanded = comunaExpanded, onDismissRequest = { comunaExpanded = false }) {
                                (ChileLocations.comunasFor(selRegion)).forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = {
                                    selComuna = c
                                    appPrefs.edit { putString("comuna", c) }
                                    comunaExpanded = false
                                }) }
                            }

                            FilterChip(selected = selCategory.isNotBlank(), onClick = { categoryExpanded = true }, label = { Text(if (selCategory.isBlank()) "Categoría" else selCategory) })
                            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                listOf("", "TCG", "JUEGOS_DE_MESA", "EVENTOS").forEach { c ->
                                    val label = if (c.isBlank()) "Todas" else c
                                    DropdownMenuItem(text = { Text(label) }, onClick = { selCategory = c; categoryExpanded = false })
                                }
                            }

                            AssistChip(onClick = { tryUseLocation() }, enabled = !isLocating, label = { Text(if (isLocating) "Buscando…" else "Mi ubicación") })
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = if (userType == "Tienda") "Tus Eventos Publicados" else when {
                            selComuna.isNotBlank() -> "Eventos en $selComuna"
                            selRegion.isNotBlank() -> "Eventos en región $selRegion"
                            else -> "Eventos Recomendados"
                        },
                        fontSize = 20.sp,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (events.isEmpty() && !isRefreshing) {
                        Text("No hay eventos por ahora", color = colors.onSurface)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(events, key = { it.id }) { ev ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenEvent?.invoke(ev.id) },
                                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                                    elevation = CardDefaults.cardElevation(6.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        if (ev.imageUri != null) {
                                            AsyncImage(
                                                model = ev.imageUri,
                                                contentDescription = "Imagen del evento",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                        }
                                        Text(ev.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                                        Text("Categoría: ${ev.category}", color = colors.onSurface.copy(alpha = 0.7f))
                                        Text("Fecha: ${ev.date}", color = colors.onSurface.copy(alpha = 0.7f))
                                        Text("Organiza: ${ev.store}", color = colors.onSurface.copy(alpha = 0.7f))
                                        if (ev.region.isNotBlank()) Text("Región: ${ev.region}", color = colors.onSurface.copy(alpha = 0.7f))
                                        if (ev.comuna.isNotBlank()) Text("Comuna: ${ev.comuna}", color = colors.onSurface.copy(alpha = 0.7f))
                                        if (ev.address.isNotBlank()) Text("Dirección: ${ev.address}", color = colors.onSurface.copy(alpha = 0.7f))
                                        if (ev.hasCapacity) {
                                            Spacer(Modifier.height(4.dp))
                                            Text("Cupos disponibles: ${ev.capacityAvailable ?: 0}", color = Color(0xFF2E7D32))
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(ev.description, color = colors.onSurface)
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
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            GifImage(resId = R.drawable.gatito_girando, modifier = Modifier.size(112.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Actualizando…", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false; appPrefs.edit { putBoolean("location_prompt_dismissed", true) } },
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
                    appPrefs.edit { putString("region", selRegion) }
                    appPrefs.edit { putString("comuna", selComuna) }
                    appPrefs.edit { putBoolean("location_prompt_dismissed", true) }
                    showLocationDialog = false
                }, enabled = selRegion.isNotBlank() && selComuna.isNotBlank()) { Text("Guardar") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { tryUseLocation() }) { Text("Usar mi ubicación") }
                    TextButton(onClick = { showLocationDialog = false; appPrefs.edit { putBoolean("location_prompt_dismissed", true) } }) { Text("Más tarde") }
                }
            }
        )
    }

    if (suggestRegion != null && suggestComuna != null) {
        AlertDialog(
            onDismissRequest = { suggestRegion = null; suggestComuna = null },
            title = { Text("Ubicación detectada") },
            text = { Text("Parece que estás cerca de ${suggestComuna}, ${suggestRegion}. ¿Quieres actualizar tu ubicación guardada?") },
            confirmButton = {
                TextButton(onClick = {
                    val r = suggestRegion!!; val c = suggestComuna!!
                    selRegion = r; selComuna = c
                    appPrefs.edit { putString("region", r) }
                    appPrefs.edit { putString("comuna", c) }
                    suggestRegion = null; suggestComuna = null
                }) { Text("Actualizar") }
            },
            dismissButton = {
                TextButton(onClick = { suggestRegion = null; suggestComuna = null }) { Text("Mantener actual") }
            }
        )
    }

    if (showNotices && notices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showNotices = false },
            title = { Text("Actualizaciones de tus eventos") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    notices.take(5).forEach { n ->
                        Text("• ${n.eventTitle} (${n.store}): ${n.changes}")
                        Spacer(Modifier.height(6.dp))
                    }
                    if (notices.size > 5) Text("… y ${notices.size - 5} más")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = notices.map { it.id }
                    scope.launch { vm.clearNotices(ids) }
                    showNotices = false
                }) { Text("Entendido") }
            }
        )
    }

    if (showDetectedDialog) {
        AlertDialog(
            onDismissRequest = {
                showDetectedDialog = false
                detectedRegion = null
                detectedComuna = null
                manualSelectMode = false
            },
            title = { Text(if (!manualSelectMode) "Ubicación detectada" else "Selecciona ubicación") },
            text = {
                if (!manualSelectMode) {
                    Column {
                        val r = detectedRegion ?: "(sin región)"
                        val c = detectedComuna?.takeIf { it.isNotBlank() } ?: "(sin comuna)"
                        Text("Se ha detectado que estás cerca de $c, $r.")
                        Spacer(Modifier.height(8.dp))
                        Text("Confirma si es correcto, o cambia manualmente si no coincide.")
                    }
                } else {
                    Column {
                        LocationSelectors(
                            region = manualRegion,
                            comuna = manualComuna,
                            onRegion = { manualRegion = it; manualComuna = "" },
                            onComuna = { manualComuna = it }
                        )
                    }
                }
            },
            confirmButton = {
                if (!manualSelectMode) {
                    TextButton(onClick = {
                        val r = detectedRegion
                        if (r != null) {
                            selRegion = r
                            appPrefs.edit { putString("region", r) }
                            val c = detectedComuna?.takeIf { it!!.isNotBlank() }
                            if (c != null) {
                                selComuna = c
                                appPrefs.edit { putString("comuna", c) }
                            } else {
                                selComuna = ""
                                appPrefs.edit { putString("comuna", "") }
                            }
                            appPrefs.edit { putBoolean("location_prompt_dismissed", true) }
                        }
                        showDetectedDialog = false
                        manualSelectMode = false
                    }) { Text("Confirmar") }
                } else {
                    TextButton(onClick = {
                        if (manualRegion.isNotBlank() && manualComuna.isNotBlank()) {
                            selRegion = manualRegion
                            selComuna = manualComuna
                            appPrefs.edit { putString("region", manualRegion) }
                            appPrefs.edit { putString("comuna", manualComuna) }
                            appPrefs.edit { putBoolean("location_prompt_dismissed", true) }
                            showDetectedDialog = false
                            manualSelectMode = false
                        }
                    }, enabled = manualRegion.isNotBlank() && manualComuna.isNotBlank()) { Text("Guardar") }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!manualSelectMode) {
                        TextButton(onClick = { manualSelectMode = true }) { Text("Cambiar") }
                        TextButton(onClick = {
                            showDetectedDialog = false
                            manualSelectMode = false
                        }) { Text("Cancelar") }
                    } else {
                        TextButton(onClick = { manualSelectMode = false }) { Text("Volver") }
                        TextButton(onClick = {
                            showDetectedDialog = false
                            manualSelectMode = false
                        }) { Text("Cancelar") }
                    }
                }
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
            ChileLocations.regions().forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { onRegion(r); regionExpanded = false }) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = comuna, onValueChange = {}, enabled = false, label = { Text("Comuna") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        val comunas = ChileLocations.comunasFor(region)
        Button(onClick = { comunaExpanded = true }, enabled = region.isNotBlank()) { Text(if (comuna.isBlank()) "Elegir comuna" else "Cambiar comuna") }
        DropdownMenu(expanded = comunaExpanded, onDismissRequest = { comunaExpanded = false }) {
            comunas.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onComuna(c); comunaExpanded = false }) }
        }
    }
}
