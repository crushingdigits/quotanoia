package dev.qwertyfinger.quotanoia.ui.components

import androidx.compose.ui.graphics.Color

private const val PCT_MAX = 100f
private const val PCT_TEAL_THRESHOLD = 25f
private const val PCT_GREEN_THRESHOLD = 50f
private const val PCT_YELLOW_THRESHOLD = 70f
private const val PCT_ORANGE_THRESHOLD = 85f

private val COLOR_BLUE = Color(0xFF4A90D9)
private val COLOR_TEAL = Color(0xFF2EC4B6)
private val COLOR_GREEN = Color(0xFF4CAF50)
private val COLOR_YELLOW = Color(0xFFFFCA28)
private val COLOR_ORANGE = Color(0xFFFF9800)
private val COLOR_RED = Color(0xFFE53935)

/** Maps utilization 0..100 to a blue→green→yellow→orange→red gradient. */
@Suppress("MagicNumber")
fun utilizationColor(pct: Float): Color {
    val clamped = pct.coerceIn(0f, PCT_MAX)
    return when {
        clamped <= PCT_TEAL_THRESHOLD -> {
            // Blue → Teal
            val t = clamped / PCT_TEAL_THRESHOLD
            lerp(COLOR_BLUE, COLOR_TEAL, t)
        }

        clamped <= PCT_GREEN_THRESHOLD -> {
            // Teal → Green
            val t = (clamped - PCT_TEAL_THRESHOLD) / PCT_TEAL_THRESHOLD
            lerp(COLOR_TEAL, COLOR_GREEN, t)
        }

        clamped <= PCT_YELLOW_THRESHOLD -> {
            // Green → Yellow
            val t = (clamped - PCT_GREEN_THRESHOLD) / (PCT_YELLOW_THRESHOLD - PCT_GREEN_THRESHOLD)
            lerp(COLOR_GREEN, COLOR_YELLOW, t)
        }

        clamped <= PCT_ORANGE_THRESHOLD -> {
            // Yellow → Orange
            val t = (clamped - PCT_YELLOW_THRESHOLD) / (PCT_ORANGE_THRESHOLD - PCT_YELLOW_THRESHOLD)
            lerp(COLOR_YELLOW, COLOR_ORANGE, t)
        }

        else -> {
            // Orange → Red
            val t = (clamped - PCT_ORANGE_THRESHOLD) / (PCT_MAX - PCT_ORANGE_THRESHOLD)
            lerp(COLOR_ORANGE, COLOR_RED, t)
        }
    }
}

private fun lerp(
    a: Color,
    b: Color,
    t: Float,
): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
