package com.wam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun YemenServicesTheme(
    primaryColor: Color,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Check luminance of dynamic primary color to ensure readable foreground texts
    val isColorBright = (primaryColor.red * 0.299f + primaryColor.green * 0.587f + primaryColor.blue * 0.114f) > 0.5f
    val onPrimaryColor = if (isColorBright) Color.Black else Color.White

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = primaryColor.copy(alpha = 0.8f),
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color.LightGray,
            outlineVariant = Color(0xFF3A3A42)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = primaryColor.copy(alpha = 0.8f),
            background = LightBackground,
            surface = LightSurface,
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color.DarkGray,
            outlineVariant = Color(0xFFD1D1D6)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
