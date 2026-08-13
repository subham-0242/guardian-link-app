package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TacticalCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF164E63),
    onPrimaryContainer = TacticalCyanLight,
    secondary = CrisisRed,
    onSecondary = Color.White,
    secondaryContainer = CrisisRedDark,
    onSecondaryContainer = Color(0xFFFECACA),
    tertiary = WarningAmber,
    onTertiary = Color.Black,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun GuardianLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
