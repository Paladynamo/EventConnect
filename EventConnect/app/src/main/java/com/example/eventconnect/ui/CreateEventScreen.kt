package com.example.eventconnect.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.location.ChileLocations
import com.example.eventconnect.location.PlacesUtils
import com.example.eventconnect.location.AddressValidator
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.dto.CreateEventRequest
import com.example.eventconnect.remote.dto.toEvent
import com.example.eventconnect.util.SessionHelper
import retrofit2.HttpException
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.os.Build
import android.location.Geocoder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.Locale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    storeName: String,
    onEventCreated: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = db.eventDao()
    val scope = rememberCoroutineScope()

    // Categorías exactas como en el backend
    val categorias = listOf("TCG", "JUEGOS_DE_MESA", "EVENTOS")

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    // Múltiples imágenes
    var pickedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    // Control de menú de edición por imagen
    var editingMenuFor by remember { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) pickedImageUris = pickedImageUris + uris.map { it.toString() }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) saveBitmapToInternal(context, bmp)?.let { pickedImageUris = pickedImageUris + it }
    }
    // Launcher para pedir permiso de cámara y, si se concede, lanzar la cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara requerido para tomar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    var hasCapacity by remember { mutableStateOf(false) }
    var capacityText by remember { mutableStateOf("") }

    // Ubicación
    var region by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    var addressStreet by remember { mutableStateOf("") }
    var addressNumber by remember { mutableStateOf("") }

    var addressStatus by remember { mutableStateOf<AddressValidator.Result?>(null) }
    var validating by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> date = "$day/${month + 1}/$year" },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    // Deshabilitar días anteriores a hoy (inicio del día local)
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    datePickerDialog.datePicker.minDate = startOfToday

    val scrollState = rememberScrollState()

    // Helper de conectividad local
    fun isOnline(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) { false }
    }

    // Validación online de dirección
    var showAddrWarn by remember { mutableStateOf(false) }
    var pendingEvent by remember { mutableStateOf<Event?>(null) }

    suspend fun validateAddressOnline(context: Context, fullAddress: String): Boolean? {
        // Primero intenta Google Places por calidad; si no está inicializado o falla, cae al Geocoder previo
        val places = PlacesUtils.validateAddress(context, fullAddress)
        if (places != null) return places
        // fallback Geocoder si hay conectividad
        if (!isOnline(context)) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    try {
                        val geocoder = Geocoder(context, Locale("es", "CL"))
                        geocoder.getFromLocationName(fullAddress, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) { cont.resume(addresses.isNotEmpty()) }
                            override fun onError(errorMessage: String?) { cont.resume(false) }
                        })
                    } catch (_: Exception) { cont.resume(false) }
                }
            } else {
                @Suppress("DEPRECATION")
                val list = withContext(Dispatchers.IO) { Geocoder(context, Locale("es", "CL")).getFromLocationName(fullAddress, 1) }
                list != null && list.isNotEmpty()
            }
        } catch (_: Exception) { null }
    }

    // Helper para transformar y reemplazar una URI en la lista
    fun transformAndReplace(oldUriStr: String, op: (Bitmap) -> Bitmap?) {
        val bmp = loadBitmapFromUri(context, oldUriStr)
        if (bmp == null) {
            Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            return
        }
        val out = op(bmp)
        if (out == null) {
            Toast.makeText(context, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show()
            return
        }
        val newUri = saveBitmapToInternal(context, out)
        if (newUri == null) {
            Toast.makeText(context, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
            return
        }
        pickedImageUris = pickedImageUris.map { if (it == oldUriStr) newUri else it }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Publicar nuevo evento", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.replace("\n", "") },
                label = { Text("Título del evento") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            Spacer(Modifier.height(12.dp))
            // Selector de categoría obligatorio
            CategoryDropdown(selected = category, options = categorias, onSelected = { category = it })

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Fecha") }, enabled = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = { datePickerDialog.show() }) { Text("Seleccionar fecha") }

            Spacer(Modifier.height(16.dp))
            // Región / Comuna y dirección
            LocationSelectors(region = region, comuna = comuna, onRegion = { region = it; comuna = "" }, onComuna = { comuna = it })
            Spacer(Modifier.height(8.dp))
            val addressEnabled = region.isNotBlank() && comuna.isNotBlank()
            OutlinedTextField(
                value = addressStreet,
                onValueChange = { addressStreet = it },
                enabled = addressEnabled,
                label = { Text("Calle / Avenida / Pasaje") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = addressNumber, onValueChange = { addressNumber = it.filter { ch -> ch.isDigit() } }, enabled = addressEnabled, label = { Text("Número") }, modifier = Modifier.fillMaxWidth())

            if (addressEnabled) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (streetNumberReady(addressStreet, addressNumber)) {
                            scope.launch {
                                validating = true
                                addressStatus = AddressValidator.validate(context, addressStreet, addressNumber, comuna, region)
                                validating = false
                            }
                        } else {
                            Toast.makeText(context, "Ingresa calle y número válidos", Toast.LENGTH_LONG).show()
                        }
                    }, enabled = !validating) { Text(if (validating) "Verificando…" else "Verificar dirección") }
                    addressStatus?.let { result ->
                        AssistChip(onClick = {}, label = { Text(result.message) }, enabled = false)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            // Imágenes: agregar desde galería o cámara y previsualizar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { galleryPicker.launch("image/*") }) { Text("Agregar desde galería") }
                OutlinedButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Tomar foto") }
            }
            Spacer(Modifier.height(8.dp))
            if (pickedImageUris.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    pickedImageUris.forEach { u ->
                        Box(
                            modifier = Modifier.size(120.dp).padding(end = 8.dp)
                        ) {
                            AsyncImage(model = u, contentDescription = "Imagen del evento", modifier = Modifier.matchParentSize())
                            // Botón Editar (menú con acciones de transformación)
                            Box(modifier = Modifier.align(Alignment.TopStart)) {
                                IconButton(
                                    onClick = { editingMenuFor = u },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = editingMenuFor == u,
                                    onDismissRequest = { editingMenuFor = null }
                                ) {
                                    DropdownMenuItem(text = { Text("Rotar 90°") }, onClick = {
                                        editingMenuFor = null
                                        transformAndReplace(u) { rotateBitmap(it, 90f) }
                                    })
                                    DropdownMenuItem(text = { Text("Rotar -90°") }, onClick = {
                                        editingMenuFor = null
                                        transformAndReplace(u) { rotateBitmap(it, -90f) }
                                    })
                                    DropdownMenuItem(text = { Text("Voltear H") }, onClick = {
                                        editingMenuFor = null
                                        transformAndReplace(u) { flipBitmap(it, horizontal = true) }
                                    })
                                    DropdownMenuItem(text = { Text("Voltear V") }, onClick = {
                                        editingMenuFor = null
                                        transformAndReplace(u) { flipBitmap(it, horizontal = false) }
                                    })
                                    DropdownMenuItem(text = { Text("Recorte 1:1 (centro)") }, onClick = {
                                        editingMenuFor = null
                                        transformAndReplace(u) { centerCropSquare(it) }
                                    })
                                }
                            }
                            // Botón para eliminar imagen
                            IconButton(
                                onClick = { pickedImageUris = pickedImageUris.filterNot { it == u } },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Selector de cupos
            Spacer(Modifier.height(12.dp))
            Text("Disponibilidad de cupos")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !hasCapacity, onClick = { hasCapacity = false }, label = { Text("Cupos libres") })
                FilterChip(selected = hasCapacity, onClick = { hasCapacity = true }, label = { Text("Con cupos") })
            }
            if (hasCapacity) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Cantidad de cupos") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() || category.isBlank() || date.isBlank()) {
                        Toast.makeText(context, "Completa todos los campos obligatorios", Toast.LENGTH_LONG).show()
                    } else if (region.isBlank() || comuna.isBlank() || addressStreet.isBlank() || addressNumber.isBlank()) {
                        Toast.makeText(context, "Completa la ubicación y dirección del evento", Toast.LENGTH_LONG).show()
                    } else if (hasCapacity && (capacityText.isBlank() || capacityText.toIntOrNull() == null || capacityText.toInt() <= 0)) {
                        Toast.makeText(context, "Ingresa una cantidad de cupos válida (> 0)", Toast.LENGTH_LONG).show()
                    } else {
                        val selectedMillis = parseDateToMillis(date)
                        val todayStart = startOfToday
                        if (selectedMillis == null) {
                            Toast.makeText(context, "Fecha inválida. Usa el selector de fecha.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (selectedMillis < todayStart) {
                            Toast.makeText(context, "La fecha no puede ser anterior a hoy", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val total = if (hasCapacity) capacityText.toInt() else null
                        val event = Event(
                            title = title,
                            description = description,
                            category = category,
                            date = date,
                            store = storeName,
                            region = region,
                            comuna = comuna,
                            addressStreet = addressStreet,
                            addressNumber = addressNumber,
                            imageUris = pickedImageUris,
                            hasCapacity = hasCapacity,
                            capacityTotal = total,
                            capacityAvailable = total
                        )
                        // Validación online de dirección (no bloquea la creación)
                        val addressLine = listOf(addressStreet, addressNumber, comuna, region).filter { it.isNotBlank() }.joinToString(", ")
                        scope.launch {
                            // Validar dirección en background (solo advertencia, no bloquea)
                            val valid = validateAddressOnline(context, addressLine)
                            if (valid == false) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Advertencia: La dirección no pudo ser validada", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            // SIEMPRE intentar crear en la API
                            val userId = SessionHelper.getUserId(context)
                            if (userId == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: No se encontró sesión de usuario. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
                                }
                                return@launch
                            }
                            
                            try {
                                // Convertir fecha a ISO 8601
                                val dateMillis = parseDateToMillis(date)
                                if (dateMillis == null) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error: Fecha inválida", Toast.LENGTH_LONG).show()
                                    }
                                    return@launch
                                }
                                
                                val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                // Formato ISO 8601: YYYY-MM-DDTHH:mm:ss (CORREGIDO)
                                val isoDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02dT%02d:%02d:%02d",
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH) + 1,
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                    9,  // Hora por defecto: 9:00 AM
                                    0,  // Minutos
                                    0   // Segundos
                                )
                                
                                android.util.Log.d("CreateEvent", "Creando evento en API con userId=$userId, fecha=$isoDate, categoria=$category")
                                
                                val eventService = RetrofitProvider.retrofit(context).create(EventService::class.java)
                                val createRequest = CreateEventRequest(
                                    title = title,
                                    description = description,
                                    category = category,
                                    date = isoDate,
                                    region = region,
                                    comuna = comuna,
                                    addressStreet = addressStreet,
                                    addressNumber = addressNumber,
                                    imageUrls = emptyList(), // Por ahora sin imágenes en la API
                                    hasCapacity = hasCapacity,
                                    capacityTotal = total
                                )
                                
                                android.util.Log.d("CreateEvent", "Request: title=${createRequest.title}, category=${createRequest.category}, date=${createRequest.date}")
                                
                                val response = withContext(Dispatchers.IO) {
                                    eventService.create(userId, createRequest)
                                }
                                
                                android.util.Log.d("CreateEvent", "Respuesta de API: id=${response.id}, title=${response.title}")
                                
                                // Guardar el evento retornado por la API en la base de datos local
                                withContext(Dispatchers.IO) {
                                    val eventFromApi = response.toEvent()
                                    eventDao.insertEvent(eventFromApi)
                                }
                                
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Evento publicado correctamente", Toast.LENGTH_LONG).show()
                                    onEventCreated()
                                }
                            } catch (e: HttpException) {
                                // Manejo específico de errores HTTP
                                val errorMessage = when (e.code()) {
                                    400 -> "Error: Datos inválidos. Verifica que todos los campos estén correctos."
                                    401 -> "Error: No autorizado. Por favor, inicia sesión nuevamente."
                                    403 -> "Error: No tienes permiso para crear eventos."
                                    404 -> "Error: Recurso no encontrado."
                                    500 -> "Error del servidor. Por favor, intenta más tarde."
                                    else -> "Error HTTP ${e.code()}: ${e.message()}"
                                }
                                android.util.Log.e("CreateEvent", "Error HTTP ${e.code()}: ${e.message()}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: java.net.UnknownHostException) {
                                android.util.Log.e("CreateEvent", "Error de conexión: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: No se pudo conectar al servidor. Verifica tu conexión a internet.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: java.net.SocketTimeoutException) {
                                android.util.Log.e("CreateEvent", "Timeout: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: Tiempo de espera agotado. Por favor, intenta nuevamente.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CreateEvent", "Error inesperado: ${e.message}", e)
                                android.util.Log.e("CreateEvent", "Stack trace:", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error al crear evento: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Publicar evento") }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }

    if (showAddrWarn) {
        AlertDialog(
            onDismissRequest = { showAddrWarn = false },
            title = { Text("No se pudo validar la dirección") },
            text = { Text("No pudimos confirmar esta dirección en línea. ¿Deseas publicar de todos modos?") },
            confirmButton = {
                TextButton(onClick = {
                    val ev = pendingEvent
                    if (ev != null) {
                        scope.launch {
                            val userId = SessionHelper.getUserId(context)
                            if (userId == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: No se encontró sesión de usuario. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
                                    showAddrWarn = false
                                    pendingEvent = null
                                }
                                return@launch
                            }
                            
                            try {
                                // Convertir fecha a ISO 8601
                                val dateMillis = parseDateToMillis(ev.date)
                                if (dateMillis == null) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error: Fecha inválida", Toast.LENGTH_LONG).show()
                                        showAddrWarn = false
                                        pendingEvent = null
                                    }
                                    return@launch
                                }
                                
                                val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                // Formato ISO 8601: YYYY-MM-DDTHH:mm:ss (CORREGIDO)
                                val isoDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02dT%02d:%02d:%02d",
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH) + 1,
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                    9,  // Hora por defecto: 9:00 AM
                                    0,  // Minutos
                                    0   // Segundos
                                )
                                
                                android.util.Log.d("CreateEvent", "Creando evento en API (desde diálogo) con userId=$userId, fecha=$isoDate")
                                
                                val eventService = RetrofitProvider.retrofit(context).create(EventService::class.java)
                                val createRequest = CreateEventRequest(
                                    title = ev.title,
                                    description = ev.description,
                                    category = ev.category,
                                    date = isoDate,
                                    region = ev.region,
                                    comuna = ev.comuna,
                                    addressStreet = ev.addressStreet,
                                    addressNumber = ev.addressNumber,
                                    imageUrls = emptyList(),
                                    hasCapacity = ev.hasCapacity,
                                    capacityTotal = ev.capacityTotal
                                )
                                
                                val response = withContext(Dispatchers.IO) {
                                    eventService.create(userId, createRequest)
                                }
                                
                                android.util.Log.d("CreateEvent", "Respuesta de API (desde diálogo): id=${response.id}")
                                
                                withContext(Dispatchers.IO) {
                                    val eventFromApi = response.toEvent()
                                    eventDao.insertEvent(eventFromApi)
                                }
                                
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Evento publicado correctamente", Toast.LENGTH_LONG).show()
                                    showAddrWarn = false
                                    pendingEvent = null
                                    onEventCreated()
                                }
                            } catch (e: HttpException) {
                                // Manejo específico de errores HTTP
                                val errorMessage = when (e.code()) {
                                    400 -> "Error: Datos inválidos. Verifica que todos los campos estén correctos."
                                    401 -> "Error: No autorizado. Por favor, inicia sesión nuevamente."
                                    403 -> "Error: No tienes permiso para crear eventos."
                                    404 -> "Error: Recurso no encontrado."
                                    500 -> "Error del servidor. Por favor, intenta más tarde."
                                    else -> "Error HTTP ${e.code()}: ${e.message()}"
                                }
                                android.util.Log.e("CreateEvent", "Error HTTP ${e.code()}: ${e.message()}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    showAddrWarn = false
                                    pendingEvent = null
                                }
                            } catch (e: java.net.UnknownHostException) {
                                android.util.Log.e("CreateEvent", "Error de conexión: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: No se pudo conectar al servidor. Verifica tu conexión a internet.", Toast.LENGTH_LONG).show()
                                    showAddrWarn = false
                                    pendingEvent = null
                                }
                            } catch (e: java.net.SocketTimeoutException) {
                                android.util.Log.e("CreateEvent", "Timeout: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: Tiempo de espera agotado. Por favor, intenta nuevamente.", Toast.LENGTH_LONG).show()
                                    showAddrWarn = false
                                    pendingEvent = null
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CreateEvent", "Error inesperado: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error al crear evento: ${e.message}", Toast.LENGTH_LONG).show()
                                    showAddrWarn = false
                                    pendingEvent = null
                                }
                            }
                        }
                    } else { showAddrWarn = false }
                }) { Text("Publicar igual") }
            },
            dismissButton = {
                TextButton(onClick = { showAddrWarn = false }) { Text("Corregir dirección") }
            }
        )
    }
}

@Composable
private fun CategoryDropdown(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            enabled = false,
            label = { Text("Categoría") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = { expanded = true }) { Text(if (selected.isBlank()) "Elegir categoría" else "Cambiar categoría") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelected(opt); expanded = false })
            }
        }
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
            ChileLocations.regions().forEach { r ->
                DropdownMenuItem(text = { Text(r) }, onClick = { onRegion(r); regionExpanded = false })
            }
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

private fun loadBitmapFromUri(context: Context, uriStr: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriStr)
        val stream: InputStream? = context.contentResolver.openInputStream(uri)
        stream.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}

private fun rotateBitmap(bmp: Bitmap, degrees: Float): Bitmap? {
    return try {
        val m = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    } catch (_: Exception) { null }
}

private fun flipBitmap(bmp: Bitmap, horizontal: Boolean): Bitmap? {
    return try {
        val sx = if (horizontal) -1f else 1f
        val sy = if (horizontal) 1f else -1f
        val m = Matrix().apply { postScale(sx, sy, bmp.width / 2f, bmp.height / 2f) }
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    } catch (_: Exception) { null }
}

private fun centerCropSquare(bmp: Bitmap): Bitmap? {
    val size = minOf(bmp.width, bmp.height)
    val x = (bmp.width - size) / 2
    val y = (bmp.height - size) / 2
    return try {
        Bitmap.createBitmap(bmp, x, y, size, size)
    } catch (_: Exception) { null }
}

private fun saveBitmapToInternal(context: Context, bmp: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "event_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file).toString()
    } catch (_: Exception) {
        null
    }
}

// Helpers de fecha para validación
private fun parseDateToMillis(dateStr: String): Long? {
    val patterns = listOf(
        "d/M/yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd"
    )
    for (p in patterns) {
        val sdf = SimpleDateFormat(p, Locale.getDefault())
        sdf.isLenient = false
        try {
            val d = sdf.parse(dateStr) ?: continue
            return d.time
        } catch (_: ParseException) { }
    }
    return null
}

private fun streetNumberReady(street: String, number: String) = street.length >= 4 && number.isNotBlank()
