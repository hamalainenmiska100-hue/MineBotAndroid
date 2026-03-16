package com.minebot.app

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF09111F),
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFF05070B),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF191C22),
    onSurfaceVariant = Color(0xFFC4C8D0),
    error = Color(0xFFFF6B6B),
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0B1736),
    tertiary = Color(0xFF3B82F6),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0C1117),
    surface = Color.White,
    onSurface = Color(0xFF0C1117),
    surfaceVariant = Color(0xFFF0F3F7),
    onSurfaceVariant = Color(0xFF4B5563),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MineBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MineBotTypography,
        shapes = MineBotShapes,
        content = content
    )
}
