package com.example.rateme.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFFB74D),
    secondary = androidx.compose.ui.graphics.Color(0xFFFFCC80),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFF8A65),
    background = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
    surface = androidx.compose.ui.graphics.Color(0xFF16213E),
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF6F00),
    secondary = androidx.compose.ui.graphics.Color(0xFFFFB74D),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFF8A65),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBF0),
    surface = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun RateMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}