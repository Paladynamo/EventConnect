package com.example.eventconnect.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EditEventScreen(
    eventId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val eventDao = db.eventDao()
    val coroutineScope = rememberCoroutineScope()

    var event by remember { mutableStateOf<Event?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }

    // Cargar datos del evento al abrir la pantalla
    LaunchedEffect(eventId) {
        coroutineScope.launch(Dispatchers.IO) {
            val loadedEvent = eventDao.getEventById(eventId)
            loadedEvent?.let {
                event = it
                title = it.title
                description = it.description
                category = it.category
                date = it.date
                store = it.store
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Fecha") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = store, onValueChange = { store = it }, label = { Text("Tienda / Organizador") })

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para guardar cambios
        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    event?.let {
                        val updated = it.copy(
                            title = title,
                            description = description,
                            category = category,
                            date = date,
                            store = store
                        )
                        eventDao.updateEvent(updated)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Evento actualizado correctamente", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
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
                coroutineScope.launch(Dispatchers.IO) {
                    event?.let {
                        eventDao.deleteEvent(it)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Evento eliminado", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
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
