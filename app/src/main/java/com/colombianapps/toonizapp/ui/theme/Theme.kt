package com.colombianapps.toonizapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClipStudioDarkColorScheme = darkColorScheme(
    primary = ClipBrightCyan,
    onPrimary = ClipDarkSlate,
    primaryContainer = ClipVibrantTeal,
    onPrimaryContainer = Color.White,
    secondary = ClipDeepTeal,
    onSecondary = Color.White,
    surface = ClipDarkSlate,
    onSurface = Color.White,
    surfaceVariant = ClipDeepTeal,
    onSurfaceVariant = Color.LightGray
)

@Composable
fun ToonizAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = ClipStudioDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
