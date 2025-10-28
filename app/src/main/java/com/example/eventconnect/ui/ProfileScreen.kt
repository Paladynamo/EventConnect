package com.example.eventconnect.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventconnect.data.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val name = prefs.getString("name", "Usuario") ?: "Usuario"
    val email = prefs.getString("email", "") ?: ""

    var avatarUri by remember { mutableStateOf(prefs.getString("avatarUri", null)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
            prefs.edit().putString("avatarUri", avatarUri).apply()
        }
    }

    AppBackground {
        Scaffold(topBar = {
            TopAppBar(title = { Text("Perfil") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Volver") }
            })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (avatarUri != null) {
                    AsyncImage(model = avatarUri, contentDescription = "Avatar", modifier = Modifier.size(120.dp))
                } else {
                    Surface(modifier = Modifier.size(120.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(text = name.take(1)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(email)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Cambiar foto") }
            }
        }
    }
}
