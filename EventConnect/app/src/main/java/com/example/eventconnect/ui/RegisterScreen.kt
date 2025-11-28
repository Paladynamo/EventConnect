package com.example.eventconnect.ui

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.User
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.dto.RegisterRequest
import com.example.eventconnect.remote.service.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.util.Patterns
import androidx.compose.ui.text.input.KeyboardCapitalization

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
    val authService = remember { RetrofitProvider.retrofit(context).create(AuthService::class.java) }

    var step by remember { mutableStateOf(RegisterStep.ChooseType) }
    var userType by remember { mutableStateOf("Usuario") } // "Usuario" o "Tienda"

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showEmailExistsDialog by remember { mutableStateOf(false) }

    // Validación de email
    val isEmailValid by remember(email) {
        mutableStateOf(Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
    }

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

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.replace("\n", "") },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Words
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.replace("\n", "") },
                            label = { Text("Correo electrónico") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = email.isNotBlank() && !isEmailValid,
                            supportingText = {
                                if (email.isNotBlank() && !isEmailValid) {
                                    Text("Ingresa un correo válido")
                                }
                            }
                        )
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
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    capitalization = KeyboardCapitalization.Words
                                )
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
                                if (!isEmailValid) {
                                    Toast.makeText(context, "Ingresa un correo válido", Toast.LENGTH_LONG).show()
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
                                        val emailNorm = email.trim().lowercase()
                                        val userTypeApi = if (userType == "Tienda") "TIENDA" else "USUARIO"
                                        
                                        // Intentar registrar en la API
                                        val authResponse = withContext(Dispatchers.IO) {
                                            try {
                                                authService.register(
                                                    RegisterRequest(
                                                        name = name,
                                                        email = emailNorm,
                                                        password = password,
                                                        userType = userTypeApi,
                                                        storeName = if (userType == "Tienda") storeName else null
                                                    )
                                                )
                                            } catch (ex: Exception) {
                                                // Log del error completo para debugging
                                                android.util.Log.e("RegisterScreen", "Error al registrar en API", ex)
                                                
                                                // Mostrar mensaje específico según el tipo de error
                                                withContext(Dispatchers.Main) {
                                                    val errorMsg = when {
                                                        ex.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                                                            "No se puede conectar al servidor. Verifica que la API esté corriendo."
                                                        ex.message?.contains("timeout", ignoreCase = true) == true -> 
                                                            "Timeout: El servidor no responde."
                                                        ex.message?.contains("404", ignoreCase = true) == true -> 
                                                            "Endpoint no encontrado. Verifica la URL."
                                                        ex.message?.contains("400", ignoreCase = true) == true -> 
                                                            "Datos inválidos. Verifica los campos ingresados."
                                                        ex.message?.contains("409", ignoreCase = true) == true -> 
                                                            "El correo ya está registrado."
                                                        ex.message?.contains("500", ignoreCase = true) == true -> 
                                                            "Error del servidor."
                                                        else -> "Error: ${ex.javaClass.simpleName}"
                                                    }
                                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                                }
                                                null
                                            }
                                        }
                                        
                                        if (authResponse != null) {
                                            // Registro exitoso en API
                                            val user = authResponse.user
                                            // Guardar también localmente como backup
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    userDao.insertUser(
                                                        User(
                                                            name = user.name,
                                                            email = user.email,
                                                            password = password, // Guardar localmente para fallback
                                                            userType = user.userType,
                                                            storeName = user.storeName ?: ""
                                                        )
                                                    )
                                                } catch (_: Exception) {
                                                    // Ignorar error de inserción local
                                                }
                                            }
                                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_LONG).show()
                                            onBackToLogin()
                                        } else {
                                            // Fallback: verificar si existe localmente
                                            val exists = withContext(Dispatchers.IO) { userDao.getUserByEmail(emailNorm) }
                                            if (exists != null) {
                                                showEmailExistsDialog = true
                                            } else {
                                                // Registrar solo localmente como fallback
                                                withContext(Dispatchers.IO) {
                                                    userDao.insertUser(
                                                        User(
                                                            name = name,
                                                            email = emailNorm,
                                                            password = password,
                                                            userType = userType,
                                                            storeName = if (userType == "Tienda") storeName else ""
                                                        )
                                                    )
                                                }
                                                Toast.makeText(context, "Registro exitoso (modo offline)", Toast.LENGTH_LONG).show()
                                                onBackToLogin()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al registrar: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = !isSubmitting && name.isNotBlank() && email.isNotBlank() && isEmailValid && password.isNotBlank() && (userType != "Tienda" || storeName.isNotBlank()),
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
