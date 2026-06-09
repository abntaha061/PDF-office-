package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF004987),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFC4C6CF),
    onSecondary = Color(0xFF1E232F),
    secondaryContainer = Color(0xFF1E232F),
    onSecondaryContainer = Color(0xFFE2E2E6),
    background = Color(0xFF11141A),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1E232F),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF282F3E),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF282F3E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1A73E8), // Beautiful Google/Minimal Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD), // Clean blue tint container
    onPrimaryContainer = Color(0xFF041E49), // Deep dark navy
    secondary = Color(0xFF44474E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EEF6), // Light blue-grey background
    onSecondaryContainer = Color(0xFF1B1B1F),
    background = Color(0xFFF7F9FC), // Clean minimal background
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF), // White containers
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE9EEF6),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFDDE3EA) // Light divider/borders
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
