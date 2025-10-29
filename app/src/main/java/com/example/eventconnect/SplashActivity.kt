package com.example.eventconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.eventconnect.ui.AppBackground
import com.example.eventconnect.ui.theme.EventConnectTheme
import kotlinx.coroutines.delay
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.compose.AsyncImage

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventConnectTheme {
                var progress by remember { mutableStateOf(0f) }
                val durationMs = 1500L // más ágil

                // ImageLoader para GIFs
                val gifLoader = remember {
                    ImageLoader.Builder(this)
                        .components {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory())
                            else add(GifDecoder.Factory())
                        }
                        .build()
                }

                // Medidas para sincronizar el GIF con la barra
                var barWidthPx by remember { mutableStateOf(0) }
                val gifSizeDp = 56.dp
                val density = LocalDensity.current
                val gifSizePx = with(density) { gifSizeDp.roundToPx() }

                LaunchedEffect(Unit) {
                    val steps = 100
                    val stepDelay = durationMs / steps
                    repeat(steps) { i ->
                        progress = (i + 1).toFloat() / steps
                        delay(stepDelay)
                    }
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }

                AppBackground {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Logo centrado arriba
                        Image(
                            painter = painterResource(id = R.drawable.logo_eventconnect),
                            contentDescription = "Logo EventConnect",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp)
                                .size(200.dp)
                        )

                        // Barra + GIF + porcentaje centrados abajo
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Contenedor de barra con medición de ancho
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .wrapContentHeight()
                                    .onGloballyPositioned { coords -> barWidthPx = coords.size.width }
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .height(6.dp)
                                )
                                // Pikachu animado encima de la barra
                                val xPx = ((barWidthPx - gifSizePx).coerceAtLeast(0) * progress).toInt()
                                val xDp = with(density) { xPx.toDp() }
                                AsyncImage(
                                    model = R.drawable.pikachu_carga_screen,
                                    contentDescription = "Carga",
                                    imageLoader = gifLoader,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .offset(x = xDp, y = (-24).dp)
                                        .size(gifSizeDp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
