package dev.crushingdigits.quotanoia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.crushingdigits.quotanoia.ui.theme.QuotanoiaTheme

private const val DEFAULT_SIZE = 80
private const val DEFAULT_STROKE_WIDTH = 8
private const val TRACK_COLOR = 0xFF2A2A40L
private const val LARGE_SIZE_THRESHOLD = 60
private const val FONT_SIZE_LARGE = 16
private const val FONT_SIZE_SMALL = 12
private const val LABEL_FONT_SIZE_LARGE = 10
private const val LABEL_FONT_SIZE_SMALL = 8
private const val PCT_DIVISOR = 100f
private const val FULL_CIRCLE_DEGREES = 360f

@Composable
fun CircularUsageBar(
    utilization: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_SIZE.dp,
    strokeWidth: Dp = DEFAULT_STROKE_WIDTH.dp,
    trackColor: Color = Color(TRACK_COLOR),
) {
    val progressColor = utilizationColor(utilization)
    val sweepAngle = (utilization / PCT_DIVISOR).coerceIn(0f, 1f) * FULL_CIRCLE_DEGREES

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val padding = strokeWidth.toPx() / 2
            val arcSize = Size(
                this.size.width - strokeWidth.toPx(),
                this.size.height - strokeWidth.toPx(),
            )
            val topLeft = Offset(padding, padding)

            // Track (background)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            // Progress
            if (sweepAngle > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }

        // Center text: percentage + label stacked
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${utilization.toInt()}%",
                color = progressColor,
                fontSize = if (size > LARGE_SIZE_THRESHOLD.dp) FONT_SIZE_LARGE.sp else FONT_SIZE_SMALL.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (size > LARGE_SIZE_THRESHOLD.dp) LABEL_FONT_SIZE_LARGE.sp else LABEL_FONT_SIZE_SMALL.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F1A)
@Composable
private fun CircularUsageBarLowPreview() {
    QuotanoiaTheme {
        CircularUsageBar(utilization = 15f, label = "5h", size = 90.dp, strokeWidth = 10.dp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F1A)
@Composable
private fun CircularUsageBarMidPreview() {
    QuotanoiaTheme {
        CircularUsageBar(utilization = 55f, label = "5h", size = 90.dp, strokeWidth = 10.dp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F1A)
@Composable
private fun CircularUsageBarHighPreview() {
    QuotanoiaTheme {
        CircularUsageBar(utilization = 92f, label = "7d", size = 90.dp, strokeWidth = 10.dp)
    }
}
