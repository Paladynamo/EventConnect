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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventconnect.data.Event
import com.example.eventconnect.location.ChileLocations
import com.example.eventconnect.notification.NotificationHelper
import com.example.eventconnect.ui.vm.EditEventViewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import java.util.Collections
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun EditEventScreen(
    eventId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: EditEventViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val categorias = listOf("TCG", "Juegos de Mesa", "Eventos")

    var event by remember { mutableStateOf<Event?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }

    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    // Control de menú de edición
    var editingMenuFor by remember { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) imageUris = imageUris + uris.map { it.toString() }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) saveBitmapToInternal(context, bmp)?.let { imageUris = imageUris + it }
    }
    // Launcher para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null) else Toast.makeText(context, "Permiso de cámara requerido para tomar fotos", Toast.LENGTH_SHORT).show()
    }

    var hasCapacity by remember { mutableStateOf(false) }
    var capacityText by remember { mutableStateOf("") }

    // Ubicación/dirección
    var region by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    var addressStreet by remember { mutableStateOf("") }
    var addressNumber by remember { mutableStateOf("") }

    // Cargar datos del evento al abrir la pantalla
    LaunchedEffect(eventId) {
        val loaded = vm.getEvent(eventId)
        loaded?.let {
            event = it
            title = it.title
            description = it.description
            category = it.category
            date = it.date
            store = it.store
            imageUris = it.imageUris ?: emptyList()
            hasCapacity = it.hasCapacity
            capacityText = it.capacityTotal?.toString() ?: ""
            // loc/dirección
            region = it.region
            comuna = it.comuna
            addressStreet = it.addressStreet
            addressNumber = it.addressNumber
        }
    }

    val scrollState = rememberScrollState()

    // Config drag & drop
    val density = LocalDensity.current
    val itemSizeDp = 120.dp
    val spacingDp = 8.dp
    val itemWidthPx = with(density) { itemSizeDp.roundToPx() }
    val spacingPx = with(density) { spacingDp.roundToPx() }
    val offsets = remember { mutableStateMapOf<String, Float>() }

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
        imageUris = imageUris.map { if (it == oldUriStr) newUri else it }
    }

    // Selector de fecha con bloqueo de días pasados
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> date = "$day/${month + 1}/$year" },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    datePickerDialog.datePicker.minDate = startOfToday

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Editar evento",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") })
        Spacer(modifier = Modifier.height(8.dp))
        CategoryDropdown(selected = category, options = categorias, onSelected = { category = it })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = date, onValueChange = {}, enabled = false, label = { Text("Fecha") })
        Spacer(modifier = Modifier.height(6.dp))
        Button(onClick = { datePickerDialog.show() }) { Text("Seleccionar fecha") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = store, onValueChange = { store = it }, label = { Text("Tienda / Organizador") })

        Spacer(modifier = Modifier.height(12.dp))
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
        if (imageUris.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                imageUris.forEach { u ->
                    val tx = offsets[u] ?: 0f
                    Box(
                        Modifier
                            .size(itemSizeDp)
                            .padding(end = spacingDp)
                            .graphicsLayer { translationX = tx }
                            .pointerInput(u) {
                                detectDragGestures(
                                    onDragStart = { offsets[u] = 0f },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val cur = (offsets[u] ?: 0f) + drag.x
                                        offsets[u] = cur
                                        val threshold = (itemWidthPx + spacingPx) / 2f
                                        if (abs(cur) > threshold) {
                                            val from = imageUris.indexOf(u)
                                            val dir = if (cur > 0f) 1 else -1
                                            val to = (from + dir).coerceIn(0, imageUris.lastIndex)
                                            if (to != from) {
                                                val list = imageUris.toMutableList()
                                                Collections.swap(list, from, to)
                                                imageUris = list
                                            }
                                            offsets[u] = 0f
                                        }
                                    },
                                    onDragEnd = { offsets[u] = 0f },
                                    onDragCancel = { offsets[u] = 0f }
                                )
                            }
                    ) {
                        AsyncImage(model = u, contentDescription = "Imagen del evento", modifier = Modifier.matchParentSize())
                        // Botón Editar (menú con acciones)
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
                        IconButton(
                            onClick = { imageUris = imageUris.filterNot { it == u } },
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

        Spacer(modifier = Modifier.height(16.dp))

        // Ubicación/dirección
        LocationSelectors(region = region, comuna = comuna, onRegion = { region = it; comuna = "" }, onComuna = { comuna = it })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = addressStreet, onValueChange = { addressStreet = it }, label = { Text("Calle / Avenida / Pasaje") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = addressNumber, onValueChange = { addressNumber = it.filter { ch -> ch.isDigit() } }, label = { Text("Número") })

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de cupos
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
                label = { Text("Cantidad de cupos") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para guardar cambios
        Button(
            onClick = {
                coroutineScope.launch {
                    if (title.isBlank() || description.isBlank() || category.isBlank() || date.isBlank()) {
                        Toast.makeText(context, "Completa los campos obligatorios", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    if (region.isBlank() || comuna.isBlank() || addressStreet.isBlank() || addressNumber.isBlank()) {
                        Toast.makeText(context, "Dirección completa obligatoria (calle y número)", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val total = if (hasCapacity) capacityText.toIntOrNull() else null
                    if (hasCapacity && (total == null || total <= 0)) {
                        Toast.makeText(context, "Ingresa una cantidad de cupos válida (> 0)", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    // Validación de fecha no pasada
                    val selectedMillis = parseDateToMillisEdit(date)
                    val startOfToday = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (selectedMillis == null) {
                        Toast.makeText(context, "Fecha inválida. Usa un formato dd/MM/yyyy", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    if (selectedMillis < startOfToday) {
                        Toast.makeText(context, "La fecha no puede ser anterior a hoy", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    // Detectar cambios relevantes
                    val e = event
                    if (e == null) {
                        Toast.makeText(context, "Evento no encontrado", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val changes = mutableListOf<String>()
                    if (e.date != date) changes += "fecha: ${e.date} → $date"
                    if (e.region != region) changes += "región: ${e.region} → $region"
                    if (e.comuna != comuna) changes += "comuna: ${e.comuna} → $comuna"
                    val oldAddr = listOf(e.addressStreet, e.addressNumber).filter { it.isNotBlank() }.joinToString(" ")
                    val newAddr = listOf(addressStreet, addressNumber).filter { it.isNotBlank() }.joinToString(" ")
                    if (oldAddr != newAddr) changes += "dirección: $oldAddr → $newAddr"

                    val newAvailable = when {
                        !hasCapacity -> null
                        e.capacityAvailable != null && total != null -> minOf(e.capacityAvailable, total)
                        else -> total
                    }
                    val updated = e.copy(
                        title = title,
                        description = description,
                        category = category,
                        date = date,
                        store = store,
                        imageUris = imageUris,
                        hasCapacity = hasCapacity,
                        capacityTotal = total,
                        capacityAvailable = newAvailable,
                        region = region,
                        comuna = comuna,
                        addressStreet = addressStreet,
                        addressNumber = addressNumber
                    )
                    vm.update(updated)
                    vm.queueUpdatePayload(e.id, title, category)
                    if (changes.isNotEmpty()) {
                        val noticeText = changes.joinToString("; ")
                        vm.notifyInterested(e.id, title, store, noticeText)
                        NotificationHelper.createNotificationChannel(context)
                        NotificationHelper.showEventUpdatedNotification(context, eventTitle = title, store = store)
                    }
                    Toast.makeText(context, "Evento actualizado correctamente", Toast.LENGTH_LONG).show()
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar cambios")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón para eliminar evento
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    event?.let { e ->
                        vm.delete(e)
                        vm.queueDelete(e.id)
                        Toast.makeText(context, "Evento eliminado", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Eliminar evento")
        }
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
            ChileLocations.regiones.forEach { r ->
                DropdownMenuItem(text = { Text(r) }, onClick = { onRegion(r); regionExpanded = false })
            }
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

// Helper de fecha (edición)
private fun parseDateToMillisEdit(dateStr: String): Long? {
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
