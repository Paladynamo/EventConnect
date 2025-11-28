package com.example.eventconnect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eventconnect.R

@Composable
fun SearchLoading(
    message: String = "Buscando...",
    drawableRes: Int = R.drawable.search_anim,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val ctx = LocalContext.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.wrapContentSize().padding(16.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.Transparent)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(drawableRes).build(),
                    contentDescription = "search_gif",
                    modifier = Modifier.size(140.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
