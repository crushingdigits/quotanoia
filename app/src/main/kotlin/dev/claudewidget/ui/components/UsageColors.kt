package dev.claudewidget.ui.components

import androidx.compose.ui.graphics.Color

/** Maps utilization 0..100 to a blue→green→yellow→orange→red gradient. */
fun utilizationColor(pct: Float): Color {
    val clamped = pct.coerceIn(0f, 100f)
    return when {
        clamped <= 25f -> {
            // Blue → Teal
            val t = clamped / 25f
            lerp(Color(0xFF4A90D9), Color(0xFF2EC4B6), t)
        }
        clamped <= 50f -> {
            // Teal → Green
            val t = (clamped - 25f) / 25f
            lerp(Color(0xFF2EC4B6), Color(0xFF4CAF50), t)
        }
        clamped <= 70f -> {
            // Green → Yellow
            val t = (clamped - 50f) / 20f
            lerp(Color(0xFF4CAF50), Color(0xFFFFCA28), t)
        }
        clamped <= 85f -> {
            // Yellow → Orange
            val t = (clamped - 70f) / 15f
            lerp(Color(0xFFFFCA28), Color(0xFFFF9800), t)
        }
        else -> {
            // Orange → Red
            val t = (clamped - 85f) / 15f
            lerp(Color(0xFFFF9800), Color(0xFFE53935), t)
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
