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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.location.ChileLocations
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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

    val categorias = listOf("TCG", "Juegos de Mesa", "Eventos")

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
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título del evento") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())

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
            OutlinedTextField(value = addressStreet, onValueChange = { addressStreet = it }, label = { Text("Calle / Avenida / Pasaje") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = addressNumber, onValueChange = { addressNumber = it.filter { ch -> ch.isDigit() } }, label = { Text("Número") }, modifier = Modifier.fillMaxWidth())

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
                        scope.launch(Dispatchers.IO) {
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
                            eventDao.insertEvent(event)
                            // Encolar operación para backend futuro
                            val payload = "{" +
                                    "\"title\":\"$title\"," +
                                    "\"category\":\"$category\"," +
                                    "\"date\":\"$date\"," +
                                    "\"store\":\"$storeName\"" +
                                    "}"
                            AppDatabase.getDatabase(context).pendingOperationDao().insert(
                                PendingOperation(type = "EVENT_CREATE", payload = payload)
                            )
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Evento publicado correctamente", Toast.LENGTH_LONG).show()
                                onEventCreated()
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
