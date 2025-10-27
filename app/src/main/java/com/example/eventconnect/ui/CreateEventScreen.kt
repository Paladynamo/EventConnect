package com.example.eventconnect.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    storeName: String,
    onEventCreated: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }   // ✅ usa el método correcto
    val eventDao = db.eventDao()
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> date = "$day/${month + 1}/$year" },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
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
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = date, onValueChange = {}, label = { Text("Fecha") }, enabled = false, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { datePickerDialog.show() }) { Text("Seleccionar fecha") }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Cupos disponibles (opcional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (title.isBlank() || description.isBlank() || category.isBlank() || date.isBlank()) {
                    Toast.makeText(context, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch(Dispatchers.IO) {
                        val event = Event(
                            title = title,
                            description = description,
                            category = category,
                            date = date,
                            store = storeName
                        )
                        eventDao.insertEvent(event)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Evento publicado correctamente", Toast.LENGTH_SHORT).show()
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
