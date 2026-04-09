package dev.crushingdigits.quotanoia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7B9CFF),
    onPrimary = Color.White,
    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF252542),
    onSurfaceVariant = Color(0xFFB0B0B0),
    background = Color(0xFF0F0F1A),
    onBackground = Color(0xFFE0E0E0),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun QuotanoiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
