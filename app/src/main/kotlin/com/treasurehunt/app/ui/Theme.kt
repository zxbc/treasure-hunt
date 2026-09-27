package com.treasurehunt.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2A33C),
    onPrimary = Color(0xFF221602),
    primaryContainer = Color(0xFF4A3313),
    onPrimaryContainer = Color(0xFFFFDDB1),
    secondary = Color(0xFF4FB39B),
    onSecondary = Color(0xFF02211B),
    background = Color(0xFF0E1218),
    surface = Color(0xFF141A22),
    surfaceVariant = Color(0xFF1D2530),
    onBackground = Color(0xFFE9E6DF),
    onSurface = Color(0xFFE9E6DF),
    onSurfaceVariant = Color(0xFFB7C0CC),
    outline = Color(0xFF3A4553),
    error = Color(0xFFE5484D),
)

/** Urgency colors, index 0 (cold) to 4 (burning hot). */
val UrgencyPalette = listOf(
    Color(0xFF4FA3E3),
    Color(0xFF57C785),
    Color(0xFFE8C547),
    Color(0xFFF08C3A),
    Color(0xFFE5484D),
)

@Composable
fun TreasureHuntTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
