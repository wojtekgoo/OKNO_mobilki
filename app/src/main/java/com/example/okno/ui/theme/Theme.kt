package com.example.okno.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// --- Define your colors ---
private val DarkGray = Color(0xFF121212)
private val LighterGray = Color(0xFF1E1E1E)
private val LightBlue = Color(0xFF81D4FA)
private val BrightBlue = Color(0xFF29B6F6)
private val DeepBlue = Color(0xFF0D47A1)
private val RedError = Color(0xFFE57373)
private val White = Color(0xFFFFFFFF)

// --- Define your dark and light color schemes ---
private val DarkColorScheme = darkColorScheme(
    primary = BrightBlue,              // main accent for buttons
    onPrimary = Color.Black,           // text on buttons
    secondary = LightBlue,             // secondary accents
    background = DarkGray,             // app background
    surface = LighterGray,             // cards & top bar background
    onBackground = LightBlue,          // text color
    onSurface = LightBlue,             // text on surfaces
    error = RedError,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = White,
    secondary = BrightBlue,
    background = White,
    surface = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = RedError,
    onError = White
)

@Composable
fun OKNOTheme(
    // If null → follow system; otherwise force the provided value.
    forceDark: Boolean? = null,
    content: @Composable () -> Unit
) {
    val dark = forceDark ?: isSystemInDarkTheme()
    val colors = if (dark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(),
        content = content
    )
}