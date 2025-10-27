package com.example.eventconnect.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }   // ✅ método corregido
    val userDao = db.userDao()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("Usuario") }   // "Usuario" o "Tienda"
    var storeName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                .padding(24.dp)
        ) {
            Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") })

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Nombre de tienda (si aplica)") })

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tipo de cuenta:")
                Spacer(modifier = Modifier.width(8.dp))
                DropdownMenuButton(
                    text = userType,
                    onOptionSelected = { selected -> userType = selected }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch(Dispatchers.IO) {
                            val newUser = User(
                                name = name,
                                email = email,
                                password = password,
                                userType = userType,
                                storeName = storeName
                            )
                            userDao.insertUser(newUser)
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                onBackToLogin()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarse")
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBackToLogin) {
                Text("Volver al inicio de sesión")
            }
        }
    }
}

// 🔧 Componente auxiliar para el selector de tipo de usuario
@Composable
fun DropdownMenuButton(text: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(text)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Usuario") },
                onClick = {
                    onOptionSelected("Usuario")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Tienda") },
                onClick = {
                    onOptionSelected("Tienda")
                    expanded = false
                }
            )
        }
    }
}
