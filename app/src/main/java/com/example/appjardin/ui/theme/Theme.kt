package com.example.appjardin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ColorVerdeAlegre,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = ColorVerdeAlegre,
    onSecondary = Color.White,
    surface = Color.White,
    onSurface = DarkText,
    surfaceVariant = Color(0xFFF0F4F1),
    onSurfaceVariant = DarkText,
    background = CreamBackground,
    onBackground = DarkText
)

private val DarkColorScheme = lightColorScheme( // Keep light palette for consistency with design spec
    primary = ColorVerdeAlegre,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = ColorVerdeAlegre,
    onSecondary = Color.White,
    surface = Color.White,
    onSurface = DarkText,
    surfaceVariant = Color(0xFFF0F4F1),
    onSurfaceVariant = DarkText,
    background = CreamBackground,
    onBackground = DarkText
)

@Composable
fun AppJardinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
