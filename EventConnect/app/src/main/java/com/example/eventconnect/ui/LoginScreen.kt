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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eventconnect.R
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.dto.LoginRequest
import com.example.eventconnect.remote.service.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import android.util.Patterns

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val userDao = db.userDao()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val isEmailValid by remember(email) { mutableStateOf(Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) }

    val scrollState = rememberScrollState()

    val authService = remember { RetrofitProvider.retrofit(context).create(AuthService::class.java) }

    AppBackground {
        Box(modifier = Modifier
            .fillMaxSize()
            .imePadding()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_login_eventconnect),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Iniciar Sesión", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.replace("\n", "") },
                    label = { Text("Correo") },
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val emailNorm = email.trim().lowercase()
                        if (emailNorm.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Ingresa correo y contraseña", Toast.LENGTH_LONG).show()
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
                        scope.launch {
                            isLoading = true
                            try {
                                val authResponse = withContext(Dispatchers.IO) {
                                    try { authService.login(LoginRequest(emailNorm, password)) } catch (ex: Exception) { null }
                                }
                                if (authResponse != null) {
                                    val user = authResponse.user
                                    val sharedPref = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                                    sharedPref.edit {
                                        putString("email", user.email)
                                        putString("name", user.name)
                                        putString("type", user.userType)
                                        putString("storeName", user.storeName ?: "")
                                        putString("token", authResponse.token)
                                        putLong("userId", user.id) // Guardar userId para usar en otros endpoints
                                    }
                                    Toast.makeText(context, "¡Bienvenido, ${user.name}!", Toast.LENGTH_LONG).show()
                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                } else {
                                    // Fallback local
                                    val user = userDao.getUserByEmailAndPassword(emailNorm, password)
                                    delay(200)
                                    if (user != null) {
                                        val sharedPref = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                                        sharedPref.edit {
                                            putString("email", user.email)
                                            putString("name", user.name)
                                            putString("type", user.userType)
                                            putString("storeName", user.storeName ?: "")
                                        }
                                        Toast.makeText(context, "¡Bienvenido, ${user.name}! (offline)", Toast.LENGTH_LONG).show()
                                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                    } else {
                                        Toast.makeText(context, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEmailValid && password.isNotBlank()
                ) {
                    Text("Ingresar")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { navController.navigate("register") }) {
                    Text("¿No tienes cuenta? Regístrate")
                }
                TextButton(onClick = { navController.navigate("forgotPassword") }) {
                    Text("¿Olvidaste tu contraseña?")
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}
