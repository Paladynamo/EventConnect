package com.example.eventconnect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun LoadingScreen(
    message: String = "Cargando...",
    gifUrlOrRes: Any = "https://example.com/tu_animacion.gif",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val ctx = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(gifUrlOrRes).build(),
            contentDescription = "loading_gif",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
