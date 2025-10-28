package com.example.eventconnect.ui

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import com.example.eventconnect.location.ChileLocations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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

    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }

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

    val scrollState = rememberScrollState()

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
            // Imagen: galería o cámara y preview
            ImagePicker(
                onImagePicked = { uri -> pickedImageUri = uri; pickedBitmap = null },
                onBitmapPicked = { bmp -> pickedBitmap = bmp; pickedImageUri = null }
            )
            Spacer(Modifier.height(8.dp))
            if (pickedImageUri != null || pickedBitmap != null) {
                AsyncImage(
                    model = pickedImageUri ?: pickedBitmap,
                    contentDescription = "Imagen del evento",
                    modifier = Modifier.size(160.dp)
                )
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
                        scope.launch(Dispatchers.IO) {
                            val uriToSave = when {
                                pickedImageUri != null -> pickedImageUri.toString()
                                pickedBitmap != null -> saveBitmapToInternal(context, pickedBitmap!!)
                                else -> null
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
                                imageUri = uriToSave,
                                hasCapacity = hasCapacity,
                                capacityTotal = total,
                                capacityAvailable = total
                            )
                            eventDao.insertEvent(event)
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
