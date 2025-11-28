package com.example.eventconnect.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.content.edit
import android.widget.Toast
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.PendingOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.util.Locale
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.service.ReservationService
import com.example.eventconnect.util.SessionHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onAccountDeleted: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    var name by remember { mutableStateOf(prefs.getString("name", "Usuario") ?: "Usuario") }
    val email = prefs.getString("email", "") ?: ""
    val userType = prefs.getString("type", "Usuario") ?: "Usuario"
    val storeName = prefs.getString("storeName", "") ?: ""

    var avatarUri by remember { mutableStateOf(prefs.getString("avatarUri", null)) }
    // Nuevos campos de perfil enriquecido
    var bio by remember { mutableStateOf(prefs.getString("bio", "") ?: "") }
    var interests by remember { mutableStateOf(prefs.getString("interests", "") ?: "") }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    // Preferencias adicionales (sin tema)
    var hidePublicProfile by remember { mutableStateOf(prefs.getBoolean("hide_public_profile", false)) }
    var showEmailPublic by remember { mutableStateOf(prefs.getBoolean("show_email_public", true)) }
    var eventRemindersEnabled by remember { mutableStateOf(prefs.getBoolean("event_reminders_enabled", true)) }

    // Estados UI
    var editNameMode by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(name) }

    var showChangePass by remember { mutableStateOf(false) }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    // Estados para eliminar cuenta
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePass by remember { mutableStateOf("") }
    var deletePassVisible by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    // Estado editor de foto
    var showEditPhoto by remember { mutableStateOf(false) }

    // Estados extra perfil público / configuración
    val availableInterests = remember { listOf("Música", "Cocinar", "Dibujar", "Videojuegos", "Lectura", "Coleccionismo", "TCG", "Juegos de Mesa", "Fotografía", "Deportes") }
    var showPublicEditDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editBioTemp by remember { mutableStateOf(bio) }
    var editInterestsChecked by remember { mutableStateOf<Map<String, Boolean>>(availableInterests.associateWith { interests.split(',').map { it.trim() }.contains(it) }) }

    // Launchers: galería y cámara
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
            prefs.edit { putString("avatarUri", avatarUri) }
            Toast.makeText(context, "Foto actualizada", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            val saved = saveBitmapToInternal(context, bmp)
            if (saved != null) {
                avatarUri = saved
                prefs.edit { putString("avatarUri", saved) }
                Toast.makeText(context, "Foto tomada y guardada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No se pudo guardar la foto", Toast.LENGTH_LONG).show()
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null) else Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_LONG).show()
    }

    val db = remember { AppDatabase.getDatabase(context) }
    val userDao = remember { db.userDao() }
    val scope = rememberCoroutineScope()
    val reservationDao = remember { db.reservationDao() }
    val eventDao = remember { db.eventDao() }

    // Estadísticas (se cargarán en LaunchedEffect)
    var statsEventsCreated by remember { mutableStateOf(0) }
    var statsReservationsMade by remember { mutableStateOf(0) }
    var statsApproved by remember { mutableStateOf(0) }
    var statsPending by remember { mutableStateOf(0) }

    LaunchedEffect(email, userType, storeName) {
        scope.launch(Dispatchers.IO) {
            val userId = SessionHelper.getUserId(context)
            if (userId != null) {
                try {
                    val eventService = RetrofitProvider.retrofit(context).create(EventService::class.java)
                    val reservationService = RetrofitProvider.retrofit(context).create(ReservationService::class.java)
                    
                    if (userType.uppercase() == "TIENDA" && storeName.isNotBlank()) {
                        // Para TIENDA: obtener eventos creados y reservas recibidas
                        val events = eventService.getByStore(userId)
                        val evCreated = events.size
                        
                        // Obtener todas las reservas de todos los eventos de la tienda
                        val allReservations = mutableListOf<com.example.eventconnect.remote.dto.ReservationResponse>()
                        events.forEach { event ->
                            try {
                                val eventReservations = reservationService.getEventReservations(event.id, userId)
                                allReservations.addAll(eventReservations)
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileScreen", "Error al obtener reservas del evento ${event.id}: ${e.message}")
                            }
                        }
                        
                        val received = allReservations.size
                        val approved = allReservations.count { it.status == "APPROVED" }
                        val pending = allReservations.count { it.status == "PENDING" }
                        
                        withContext(Dispatchers.Main) {
                            statsEventsCreated = evCreated
                            statsReservationsMade = received
                            statsApproved = approved
                            statsPending = pending
                        }
                    } else {
                        // Para USUARIO: obtener reservas hechas
                        val reservations = reservationService.getUserReservations(userId)
                        val made = reservations.size
                        val approved = reservations.count { it.status == "APPROVED" }
                        val pending = reservations.count { it.status == "PENDING" }
                        
                        withContext(Dispatchers.Main) {
                            statsEventsCreated = 0
                            statsReservationsMade = made
                            statsApproved = approved
                            statsPending = pending
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "Error al cargar estadísticas desde API: ${e.message}", e)
                    // Fallback a base de datos local si falla la API
                    val evCreated = if (userType.uppercase() == "TIENDA" && storeName.isNotBlank()) 
                        eventDao.getEventsByStore(storeName).size else 0
                    val reservations = reservationDao.getForUser(email)
                    val made = reservations.size
                    val approved = reservations.count { it.status == "APPROVED" }
                    val pending = reservations.count { it.status == "PENDING" }
                    withContext(Dispatchers.Main) {
                        statsEventsCreated = evCreated
                        statsReservationsMade = made
                        statsApproved = approved
                        statsPending = pending
                    }
                }
            } else {
                // Si no hay userId, usar base de datos local
                val evCreated = if (userType.uppercase() == "TIENDA" && storeName.isNotBlank()) 
                    eventDao.getEventsByStore(storeName).size else 0
                val reservations = reservationDao.getForUser(email)
                val made = reservations.size
                val approved = reservations.count { it.status == "APPROVED" }
                val pending = reservations.count { it.status == "PENDING" }
                withContext(Dispatchers.Main) {
                    statsEventsCreated = evCreated
                    statsReservationsMade = made
                    statsApproved = approved
                    statsPending = pending
                }
            }
        }
    }

    AppBackground {
        Scaffold(topBar = {
            TopAppBar(title = { Text("Perfil") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Volver") }
            })
        }) { padding ->
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sección avatar + nombre
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (avatarUri != null) {
                            AsyncImage(model = avatarUri, contentDescription = "Avatar", modifier = Modifier.size(120.dp))
                        } else {
                            Surface(
                                modifier = Modifier.size(120.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) { Box(contentAlignment = Alignment.Center) { Text(text = name.take(1)) } }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { galleryPicker.launch("image/*") }) { Text("Galería") }
                            OutlinedButton(onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }) { Text("Cámara") }
                            if (avatarUri != null) { OutlinedButton(onClick = { showEditPhoto = true }) { Text("Editar foto") } }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Tipo: $userType", style = MaterialTheme.typography.bodySmall)
                        if (userType == "Tienda" && storeName.isNotBlank()) {
                            Text("Tienda: ${storeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Datos personales y edición de nombre
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Identidad", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (!editNameMode) {
                            Text(name, style = MaterialTheme.typography.titleLarge)
                            if (showEmailPublic) Text(email, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { editNameMode = true; newName = name }) { Text("Editar nombre") }
                        } else {
                            OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nuevo nombre") })
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val trimmed = newName.trim()
                                    if (trimmed.length < 3) { Toast.makeText(context, "Mínimo 3 caracteres", Toast.LENGTH_LONG).show(); return@Button }
                                    scope.launch {
                                        val updated = withContext(Dispatchers.IO) { userDao.updateName(email, trimmed) }
                                        if (updated > 0) {
                                            name = trimmed
                                            prefs.edit { putString("name", trimmed) }
                                            withContext(Dispatchers.IO) {
                                                db.pendingOperationDao().insert(PendingOperation(type = "USER_UPDATE_NAME", payload = "{\"email\":\"$email\",\"name\":\"$trimmed\"}"))
                                            }
                                            Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_LONG).show()
                                            editNameMode = false
                                        } else {
                                            Toast.makeText(context, "No se pudo actualizar", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }) { Text("Guardar") }
                                OutlinedButton(onClick = { editNameMode = false; newName = name }) { Text("Cancelar") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Eliminamos edición inline de bio/intereses, mostramos sólo si existen y botón para editar
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Perfil público", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        val showBio = bio.isNotBlank() && !hidePublicProfile
                        val showInterests = interests.isNotBlank() && !hidePublicProfile
                        if (showBio) {
                            Text(bio, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                        }
                        if (showInterests) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                interests.split(',').map { it.trim() }.filter { it.isNotBlank() }.take(12).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        TextButton(onClick = {
                            editBioTemp = bio
                            val currentSelected = interests.split(',').map { it.trim() }
                            editInterestsChecked = availableInterests.associateWith { ai -> currentSelected.contains(ai) }
                            showPublicEditDialog = true
                        }) { Text(if (!showBio && !showInterests) "Agregar perfil público" else "Editar perfil público") }
                        if (hidePublicProfile) {
                            Spacer(Modifier.height(4.dp))
                            Text("(Perfil público oculto)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Estadísticas diferenciadas
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Estadísticas", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (userType.uppercase() == "TIENDA" && storeName.isNotBlank()) {
                            val level = (statsApproved * 2 + statsEventsCreated * 3 + statsReservationsMade).coerceAtLeast(0)
                            Text("Eventos creados: $statsEventsCreated")
                            Text("Reservas recibidas: ${statsReservationsMade}")
                            Text("Reservas aprobadas: $statsApproved")
                            Text("Reservas pendientes: $statsPending")
                            Spacer(Modifier.height(8.dp))
                            Text("Nivel de actividad: $level")
                            val badges = buildList {
                                if (statsEventsCreated >= 1) add("Organizador Novato")
                                if (statsEventsCreated >= 5) add("Organizador Activo")
                                if (statsReservationsMade >= 10) add("Popular")
                                if (statsApproved >= 20) add("Experto")
                            }
                            if (badges.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { badges.forEach { b -> SuggestionChip(onClick = {}, label = { Text(b) }) } }
                            }
                        } else {
                            // Para usuario común: rating promedio y eventos asistidos (aprobados + finalizados hipotéticos)
                            // Eventos asistidos: contamos aprobados por ahora como proxy
                            val attended = statsApproved
                            val userReviewDao = remember { db.userReviewDao() }
                            var avgRating by remember { mutableStateOf<Double?>(null) }
                            var reviewsCount by remember { mutableStateOf(0) }
                            LaunchedEffect(email) {
                                withContext(Dispatchers.IO) {
                                    avgRating = userReviewDao.getAverageRating(email)
                                    reviewsCount = userReviewDao.getReviewsCount(email)
                                }
                            }
                            Text("Eventos asistidos: $attended")
                            val ratingDisplay = avgRating?.takeIf { reviewsCount > 0 }?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "Sin valoraciones"
                            Text("Valoración: $ratingDisplay" + if (reviewsCount > 0) " (${reviewsCount})" else "")
                            if (reviewsCount > 0) {
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(progress = { ((avgRating ?: 0.0) / 5.0).toFloat() })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Botón Configuraciones al final
                OutlinedButton(onClick = { showSettingsDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Configuraciones") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showEditPhoto && avatarUri != null) {
        EditAvatarDialog(
            initialUri = avatarUri!!,
            onDismiss = { showEditPhoto = false },
            onSave = { editedBitmap ->
                val saved = saveBitmapToInternal(context, editedBitmap)
                if (saved != null) {
                    avatarUri = saved
                    prefs.edit { putString("avatarUri", saved) }
                    Toast.makeText(context, "Avatar actualizado", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "No se pudo guardar la edición", Toast.LENGTH_LONG).show()
                }
                showEditPhoto = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar cuenta") },
            text = {
                Column {
                    Text("Esta acción es permanente. Perderás todos tus datos locales.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deletePass,
                        onValueChange = { deletePass = it.replace("\n","") },
                        label = { Text("Contraseña actual") },
                        visualTransformation = if (deletePassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { deletePassVisible = !deletePassVisible }) {
                                Icon(imageVector = if (deletePassVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                            }
                        },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it.replace("\n","") },
                        label = { Text("Escribe: eliminar") },
                        supportingText = { Text("Confirma escribiendo la palabra eliminar") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val okWord = deleteConfirmText.trim().lowercase() == "eliminar"
                    if (!okWord) {
                        Toast.makeText(context, "Debes escribir 'eliminar' para confirmar", Toast.LENGTH_LONG).show()
                        return@TextButton
                    }
                    if (deletePass.isBlank()) {
                        Toast.makeText(context, "Ingresa tu contraseña", Toast.LENGTH_LONG).show()
                        return@TextButton
                    }
                    scope.launch {
                        val user = withContext(Dispatchers.IO) { userDao.getUserByEmailAndPassword(email, deletePass) }
                        if (user == null) {
                            Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        val deleted = withContext(Dispatchers.IO) { userDao.deleteByEmail(email) }
                        if (deleted > 0) {
                            withContext(Dispatchers.IO) {
                                db.pendingOperationDao().insert(PendingOperation(type = "USER_DELETE", payload = "{\"email\":\"$email\"}"))
                            }
                            prefs.edit { clear() }
                            Toast.makeText(context, "Cuenta eliminada", Toast.LENGTH_LONG).show()
                            showDeleteDialog = false
                            onAccountDeleted()
                        } else {
                            Toast.makeText(context, "No se pudo eliminar", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    // Dialogo edición perfil público
    if (showPublicEditDialog) {
        AlertDialog(
            onDismissRequest = { showPublicEditDialog = false },
            title = { Text("Editar perfil público") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = editBioTemp, onValueChange = { editBioTemp = it }, label = { Text("Biografía") }, supportingText = { Text("Describe brevemente quién eres.") })
                    Spacer(Modifier.height(12.dp))
                    Text("Intereses", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    availableInterests.chunked(2).forEach { row: List<String> ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { interest: String ->
                                val checked = editInterestsChecked[interest] ?: false
                                FilterChip(selected = checked, onClick = {
                                    val current = editInterestsChecked.toMutableMap(); current[interest] = !checked; editInterestsChecked = current
                                }, label = { Text(interest) })
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    bio = editBioTemp.trim()
                    val selected = editInterestsChecked.filter { it.value }.keys.toList()
                    interests = selected.joinToString(", ")
                    prefs.edit { putString("bio", bio); putString("interests", interests) }
                    showPublicEditDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showPublicEditDialog = false }) { Text("Cancelar") } }
        )
    }
    // Dialogo de Configuraciones (preferencias + seguridad)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Configuraciones") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("Preferencias", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notificaciones locales", modifier = Modifier.weight(1f))
                        Switch(checked = notificationsEnabled, onCheckedChange = {
                            notificationsEnabled = it
                            prefs.edit { putBoolean("notifications_enabled", it) }
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recordatorios de eventos", modifier = Modifier.weight(1f))
                        Switch(checked = eventRemindersEnabled, onCheckedChange = {
                            eventRemindersEnabled = it
                            prefs.edit { putBoolean("event_reminders_enabled", it) }
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mostrar correo en perfil", modifier = Modifier.weight(1f))
                        Switch(checked = showEmailPublic, onCheckedChange = {
                            showEmailPublic = it
                            prefs.edit { putBoolean("show_email_public", it) }
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ocultar perfil público", modifier = Modifier.weight(1f))
                        Switch(checked = hidePublicProfile, onCheckedChange = {
                            hidePublicProfile = it
                            prefs.edit { putBoolean("hide_public_profile", it) }
                        })
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Acciones", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            // Borrar avatar
                            avatarUri = null
                            prefs.edit { remove("avatarUri") }
                            Toast.makeText(context, "Avatar eliminado", Toast.LENGTH_LONG).show()
                        }) { Text("Borrar avatar") }
                        OutlinedButton(onClick = {
                            // Limpiar bio e intereses
                            bio = ""; interests = ""; hidePublicProfile = false
                            prefs.edit { putString("bio", ""); putString("interests", ""); putBoolean("hide_public_profile", false) }
                            Toast.makeText(context, "Perfil público limpiado", Toast.LENGTH_LONG).show()
                        }) { Text("Limpiar perfil") }
                    }
                    Spacer(Modifier.height(8.dp))
                    val pendingDao = remember { db.pendingOperationDao() }
                    var pendingCount by remember { mutableStateOf(0) }
                    LaunchedEffect(showSettingsDialog) {
                        if (showSettingsDialog) {
                            pendingCount = withContext(Dispatchers.IO) { pendingDao.getAll().size }
                        }
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val all = withContext(Dispatchers.IO) { pendingDao.getAll() }
                            // Aquí podrías implementar sincronización real; por ahora sólo simula procesado
                            if (all.isNotEmpty()) {
                                withContext(Dispatchers.IO) { all.forEach { pendingDao.delete(it) } }
                                Toast.makeText(context, "${all.size} operaciones pendientes procesadas", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No hay operaciones pendientes", Toast.LENGTH_LONG).show()
                            }
                            pendingCount = withContext(Dispatchers.IO) { pendingDao.getAll().size }
                        }
                    }) { Text("Sincronizar pendientes" + if (pendingCount > 0) " ($pendingCount)" else "") }
                    Spacer(Modifier.height(16.dp))
                    Text("Seguridad", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (!showChangePass) {
                        OutlinedButton(onClick = { showChangePass = true }) { Text("Cambiar contraseña") }
                    } else {
                        OutlinedTextField(value = currentPass, onValueChange = { currentPass = it }, label = { Text("Contraseña actual") })
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("Nueva contraseña") })
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val cur = currentPass
                                val newp = newPass
                                if (cur.isBlank() || newp.length < 6) { Toast.makeText(context, "Revisa las contraseñas (mín. 6)", Toast.LENGTH_LONG).show(); return@Button }
                                scope.launch {
                                    val updated = withContext(Dispatchers.IO) { userDao.updatePasswordChecked(email, cur, newp) }
                                    if (updated > 0) {
                                        withContext(Dispatchers.IO) { db.pendingOperationDao().insert(PendingOperation(type = "USER_UPDATE_PASSWORD", payload = "{\"email\":\"$email\"}")) }
                                        Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_LONG).show()
                                        showChangePass = false
                                        currentPass = ""; newPass = ""
                                    } else {
                                        Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) { Text("Guardar") }
                            OutlinedButton(onClick = { showChangePass = false; currentPass = ""; newPass = "" }) { Text("Cancelar") }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        deletePass = ""; deletePassVisible = false; deleteConfirmText = ""; showDeleteDialog = true
                    }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar cuenta") }
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun EditAvatarDialog(
    initialUri: String,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableStateOf(0) } // múltiplos de 90
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Cargar bitmap una vez
    LaunchedEffect(initialUri) {
        withContext(Dispatchers.IO) {
            original = loadBitmapFromUri(context, Uri.parse(initialUri))
            preview = original
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar foto de perfil") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (preview != null) {
                    AsyncImage(
                        model = preview,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Controles
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy, onClick = {
                        rotation = (rotation + 90) % 360
                        applyAsync(original, rotation, flipH, flipV) { bmp -> preview = bmp }
                    }) { Text("Rotar 90°") }
                    OutlinedButton(enabled = !busy, onClick = {
                        flipH = !flipH
                        applyAsync(original, rotation, flipH, flipV) { bmp -> preview = bmp }
                    }) { Text("Espejo H") }
                    OutlinedButton(enabled = !busy, onClick = {
                        flipV = !flipV
                        applyAsync(original, rotation, flipH, flipV) { bmp -> preview = bmp }
                    }) { Text("Espejo V") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy, onClick = {
                        val src = preview ?: original
                        if (src != null) {
                            busy = true
                            // Recorte centrado 1:1
                            scope.launch(Dispatchers.Default) {
                                val cropped = cropCenterSquare(src)
                                withContext(Dispatchers.Main) {
                                    preview = cropped
                                    busy = false
                                }
                            }
                        }
                    }) { Text("Recortar 1:1") }
                    OutlinedButton(enabled = !busy, onClick = {
                        // Restablecer al original
                        rotation = 0
                        flipH = false
                        flipV = false
                        preview = original
                    }) { Text("Restablecer") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null && !busy, onClick = {
                preview?.let { onSave(it) }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun applyAsync(
    original: Bitmap?,
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    onReady: (Bitmap?) -> Unit
) {
    if (original == null) { onReady(null); return }
    // Lanzar en background sin GlobalScope
    kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
        val transformed = transformBitmap(original, rotation, flipH, flipV)
        withContext(Dispatchers.Main) { onReady(transformed) }
    }
}

private fun transformBitmap(src: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean): Bitmap {
    val m = Matrix()
    if (flipH || flipV) {
        val sx = if (flipH) -1f else 1f
        val sy = if (flipV) -1f else 1f
        m.postScale(sx, sy)
    }
    if (rotation % 360 != 0) m.postRotate(rotation.toFloat())

    val transformed = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    return transformed
}

private fun cropCenterSquare(src: Bitmap): Bitmap {
    val size = minOf(src.width, src.height)
    val x = (src.width - size) / 2
    val y = (src.height - size) / 2
    return Bitmap.createBitmap(src, x, y, size, size)
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        // Primer pase: obtener dimensiones
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        val maxDim = 2048 // límite razonable para edición de avatar
        var inSample = 1
        val width = options.outWidth
        val height = options.outHeight
        while (width / inSample > maxDim || height / inSample > maxDim) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    } catch (_: Exception) { null }
}

private fun saveBitmapToInternal(context: Context, bmp: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        Uri.fromFile(file).toString()
    } catch (_: Exception) { null }
}


