package com.example.eventconnect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WarmBrown80,
    secondary = WarmAmber80,
    tertiary = WarmOrange80
)

private val LightColorScheme = lightColorScheme(
    primary = WarmBrown40,
    secondary = WarmAmber40,
    tertiary = WarmOrange40,
    background = WarmBackground,
    surface = WarmSurface
)

@Composable
fun EventConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}