package com.ntvelop.mobileparastatiko.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    primaryContainer = DarkSurface,
    onPrimaryContainer = NeonGreen,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C2C),
    onSecondaryContainer = Color.White,
    tertiary = Pink80,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFE0E0E0),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF555555)
)

@Composable
fun MobileParastatikoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}