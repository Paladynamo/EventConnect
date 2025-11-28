package com.example.eventconnect.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.eventconnect.R
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

private enum class RegisterStep { ChooseType, Form }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val userDao = db.userDao()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(RegisterStep.ChooseType) }
    var userType by remember { mutableStateOf("Usuario") } // "Usuario" o "Tienda"

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showEmailExistsDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            when (step) {
                RegisterStep.ChooseType -> {
                    Card(modifier = Modifier.padding(24.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp).verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("¿Qué tipo de cuenta deseas crear?", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    userType = "Usuario"
                                    step = RegisterStep.Form
                                }) { Text("Usuario") }
                                Button(onClick = {
                                    userType = "Tienda"
                                    step = RegisterStep.Form
                                }) { Text("Tienda / Productora") }
                            }
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = onBackToLogin) { Text("Volver al inicio de sesión") }
                        }
                    }
                }
                RegisterStep.Form -> {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                            .padding(24.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            if (userType == "Tienda") "Crear cuenta de tienda/productora" else "Crear cuenta",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(value = name, onValueChange = { name = it.replace("\n", "") }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it.replace("\n", "") }, label = { Text("Correo electrónico") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.replace("\n", "") },
                            label = { Text("Contraseña") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        if (userType == "Tienda") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it.replace("\n", "") },
                                label = { Text("Nombre de tienda o productora") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Validaciones
                                if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                if (password.length < 6) {
                                    Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                if (userType == "Tienda" && storeName.isBlank()) {
                                    Toast.makeText(context, "Ingresa el nombre de la tienda/productora", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                scope.launch {
                                    isSubmitting = true
                                    try {
                                        val exists = withContext(Dispatchers.IO) { userDao.getUserByEmail(email) }
                                        if (exists != null) {
                                            showEmailExistsDialog = true
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                userDao.insertUser(
                                                    User(
                                                        name = name,
                                                        email = email.trim().lowercase(),
                                                        password = password,
                                                        userType = userType,
                                                        storeName = if (userType == "Tienda") storeName else ""
                                                    )
                                                )
                                            }
                                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_LONG).show()
                                            onBackToLogin()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al registrar", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isSubmitting) "Registrando..." else "Registrarse")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { step = RegisterStep.ChooseType }, enabled = !isSubmitting) {
                                Text("Cambiar tipo")
                            }
                            TextButton(onClick = onBackToLogin, enabled = !isSubmitting) {
                                Text("Volver al inicio de sesión")
                            }
                        }
                    }
                }
            }
        }

        if (showEmailExistsDialog) {
            AlertDialog(
                onDismissRequest = { showEmailExistsDialog = false },
                confirmButton = {
                    TextButton(onClick = { showEmailExistsDialog = false }) { Text("Entendido") }
                },
                title = { Text("Correo ya registrado") },
                text = { Text("El correo ingresado ya existe. Intenta iniciar sesión o usa otro correo.") }
            )
        }
    }
}
