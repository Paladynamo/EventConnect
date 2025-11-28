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
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onAccountDeleted: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    var name by remember { mutableStateOf(prefs.getString("name", "Usuario") ?: "Usuario") }
    val email = prefs.getString("email", "") ?: ""
    val userType = prefs.getString("type", "Usuario") ?: "Usuario"
    val storeName = prefs.getString("storeName", "") ?: ""

    var avatarUri by remember { mutableStateOf(prefs.getString("avatarUri", null)) }

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

    AppBackground {
        Scaffold(topBar = {
            TopAppBar(title = { Text("Perfil") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Volver") }
            })
        }) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                // Botones cambiar foto
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { galleryPicker.launch("image/*") }) { Text("Galería") }
                    OutlinedButton(onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text("Cámara") }
                    if (avatarUri != null) {
                        OutlinedButton(onClick = { showEditPhoto = true }) { Text("Editar foto") }
                    }
                }

                Spacer(Modifier.height(16.dp))
                // Nombre (ver/editar)
                if (!editNameMode) {
                    if (userType == "Tienda" && storeName.isNotBlank()) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text("Dueño de ${storeName}", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(email)
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
                                    // encola op
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

                Spacer(Modifier.height(24.dp))
                // Cambiar contraseña (desde perfil)
                if (!showChangePass) {
                    OutlinedButton(onClick = { showChangePass = true }) { Text("Cambiar contraseña") }
                } else {
                    Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
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
                                    // encola op
                                    withContext(Dispatchers.IO) {
                                        db.pendingOperationDao().insert(PendingOperation(type = "USER_UPDATE_PASSWORD", payload = "{\"email\":\"$email\"}"))
                                    }
                                    Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_LONG).show()
                                    showChangePass = false
                                    currentPass = ""; newPass = ""
                                } else {
                                    Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text("Guardar contraseña") }
                        OutlinedButton(onClick = { showChangePass = false; currentPass = ""; newPass = "" }) { Text("Cancelar") }
                    }
                }

                Spacer(Modifier.height(24.dp))
                // Eliminar cuenta
                OutlinedButton(
                    onClick = {
                        // reset campos del diálogo
                        deletePass = ""; deletePassVisible = false; deleteConfirmText = ""
                        showDeleteDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar cuenta") }

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

                Spacer(Modifier.height(8.dp))
                Text("Tipo de usuario: $userType")
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
        var width = options.outWidth
        var height = options.outHeight
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
