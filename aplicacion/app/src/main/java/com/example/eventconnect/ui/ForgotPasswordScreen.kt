package com.example.eventconnect.ui

import android.util.Patterns
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
    var showNewPass by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AppBackground {
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
                    onValueChange = { email = it.trim().replace("\n", "") },
                    label = { Text("Correo registrado") },
                    isError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                    supportingText = {
                        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Text("Correo inválido")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it.replace("\n", "") },
                    label = { Text("Nueva contraseña") },
                    trailingIcon = {
                        IconButton(onClick = { showNewPass = !showNewPass }) {
                            val icon = if (showNewPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    visualTransformation = if (showNewPass) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    supportingText = {
                        if (newPassword.isNotEmpty() && newPassword.length < 6) Text("Mínimo 6 caracteres")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it.replace("\n", "") },
                    label = { Text("Confirmar contraseña") },
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
                            val icon = if (showConfirmPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    visualTransformation = if (showConfirmPass) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) Text("No coincide con la nueva contraseña")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_LONG).show(); return@Button
                        }
                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(context, "Correo inválido", Toast.LENGTH_LONG).show(); return@Button
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
}
