package com.example.eventconnect.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.eventconnect.R
import com.example.eventconnect.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val userDao = db.userDao()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(Modifier
        .fillMaxSize()
        .imePadding()
    ) {
        // Fondo suave
        Image(
            painter = painterResource(id = R.drawable.fondos_screens),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.15f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Correo registrado") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Nueva contraseña") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar contraseña") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_LONG).show(); return@Button
                    }
                    if (newPassword.length < 6) {
                        Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_LONG).show(); return@Button
                    }
                    if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show(); return@Button
                    }
                    scope.launch {
                        isSubmitting = true
                        val exists = withContext(Dispatchers.IO) { userDao.getUserByEmail(email) }
                        if (exists == null) {
                            Toast.makeText(context, "No existe una cuenta con ese correo", Toast.LENGTH_LONG).show()
                        } else {
                            val updated = withContext(Dispatchers.IO) { userDao.updatePassword(email, newPassword) }
                            if (updated > 0) {
                                Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "No se pudo actualizar", Toast.LENGTH_LONG).show()
                            }
                        }
                        isSubmitting = false
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isSubmitting) "Actualizando..." else "Actualizar contraseña") }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack, enabled = !isSubmitting) { Text("Volver al inicio de sesión") }
        }
    }
}
