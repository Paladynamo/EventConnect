package com.example.eventconnect.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditEventScreen(
    eventId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = db.eventDao()
    val coroutineScope = rememberCoroutineScope()

    val categorias = listOf("TCG", "Juegos de Mesa", "Eventos")

    var event by remember { mutableStateOf<Event?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasCapacity by remember { mutableStateOf(false) }
    var capacityText by remember { mutableStateOf("") }

    // Cargar datos del evento al abrir la pantalla
    LaunchedEffect(eventId) {
        val loadedEvent = withContext(Dispatchers.IO) { eventDao.getEventById(eventId) }
        loadedEvent?.let {
            event = it
            title = it.title
            description = it.description
            category = it.category
            date = it.date
            store = it.store
            imageUri = it.imageUri
            hasCapacity = it.hasCapacity
            capacityText = it.capacityTotal?.toString() ?: ""
        }
    }

    val scrollState = rememberScrollState()

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
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Fecha") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = store, onValueChange = { store = it }, label = { Text("Tienda / Organizador") })

        Spacer(modifier = Modifier.height(12.dp))
        ImagePicker(
            onImagePicked = { uri -> pickedUri = uri; pickedBitmap = null },
            onBitmapPicked = { bmp -> pickedBitmap = bmp; pickedUri = null }
        )
        Spacer(Modifier.height(8.dp))
        val previewModel: Any? = pickedUri ?: pickedBitmap ?: imageUri
        if (previewModel != null) {
            AsyncImage(model = previewModel, contentDescription = "Imagen del evento", modifier = Modifier.size(160.dp))
        }

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
                    val total = if (hasCapacity) capacityText.toIntOrNull() else null
                    if (hasCapacity && (total == null || total <= 0)) {
                        Toast.makeText(context, "Ingresa una cantidad de cupos válida (> 0)", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val finalUri = withContext(Dispatchers.IO) {
                        when {
                            pickedUri != null -> pickedUri.toString()
                            pickedBitmap != null -> saveBitmapToInternal(context, pickedBitmap!!)
                            else -> imageUri
                        }
                    }
                    withContext(Dispatchers.IO) {
                        event?.let {
                            val newAvailable = when {
                                !hasCapacity -> null
                                it.capacityAvailable != null && total != null -> minOf(it.capacityAvailable, total)
                                else -> total
                            }
                            val updated = it.copy(
                                title = title,
                                description = description,
                                category = category,
                                date = date,
                                store = store,
                                imageUri = finalUri,
                                hasCapacity = hasCapacity,
                                capacityTotal = total,
                                capacityAvailable = newAvailable
                            )
                            eventDao.updateEvent(updated)
                        }
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
                    withContext(Dispatchers.IO) {
                        event?.let { eventDao.deleteEvent(it) }
                    }
                    Toast.makeText(context, "Evento eliminado", Toast.LENGTH_LONG).show()
                    onBack()
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
