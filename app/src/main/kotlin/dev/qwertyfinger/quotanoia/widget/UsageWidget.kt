package dev.qwertyfinger.quotanoia.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.qwertyfinger.quotanoia.QuotanoiaApp
import dev.qwertyfinger.quotanoia.MainActivity
import dev.qwertyfinger.quotanoia.data.AccountState
import dev.qwertyfinger.quotanoia.ui.components.utilizationColor

class UsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = QuotanoiaApp.instance
        app.repository.initialize()
        val states = app.repository.states.value

        provideContent {
            WidgetContent(states)
        }
    }
}

private val BG_COLOR = android.graphics.Color.parseColor("#1A1A2E")
private val TEXT_COLOR = android.graphics.Color.parseColor("#E0E0E0")
private val MUTED_COLOR = android.graphics.Color.parseColor("#AAAAAA")
private val ERROR_COLOR = android.graphics.Color.parseColor("#E53935")

@Composable
private fun WidgetContent(states: List<AccountState>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BG_COLOR)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(10.dp),
    ) {
        if (states.isEmpty()) {
            Text(
                text = "Tap to add accounts",
                style = TextStyle(color = ColorProvider(MUTED_COLOR), fontSize = 14.sp),
            )
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) {
                states.forEach { state ->
                    AccountRow(state)
                    if (state != states.last()) {
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(state: AccountState) {
    val hasError = state.hasError

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = state.account.name,
            style = TextStyle(
                color = ColorProvider(if (hasError) ERROR_COLOR else TEXT_COLOR),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        if (state.usage != null) {
            Spacer(GlanceModifier.height(4.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UsageBarWidget(state.usage.fiveHour.utilization, "5h")
                Spacer(GlanceModifier.width(12.dp))
                UsageBarWidget(state.usage.sevenDay.utilization, "7d")
            }
        } else if (hasError) {
            Text(
                text = state.error!!.displayMessage,
                style = TextStyle(color = ColorProvider(ERROR_COLOR), fontSize = 10.sp),
            )
        }
    }
}

@Composable
private fun UsageBarWidget(utilization: Float, label: String) {
    val bitmap = renderCircularBarBitmap(utilization, label, 80)
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "$label: ${utilization.toInt()}%",
        modifier = GlanceModifier.size(44.dp),
    )
}

private fun renderCircularBarBitmap(utilization: Float, label: String, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = sizePx * 0.12f
    val padding = strokeWidth / 2f + 2f
    val rect = RectF(padding, padding, sizePx - padding, sizePx - padding)

    // Track
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2A2A40")
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(rect, -90f, 360f, false, trackPaint)

    // Progress arc
    val composeColor = utilizationColor(utilization)
    val arcColor = android.graphics.Color.argb(
        (composeColor.alpha * 255).toInt(),
        (composeColor.red * 255).toInt(),
        (composeColor.green * 255).toInt(),
        (composeColor.blue * 255).toInt(),
    )
    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = arcColor
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    val sweep = (utilization / 100f).coerceIn(0f, 1f) * 360f
    if (sweep > 0f) {
        canvas.drawArc(rect, -90f, sweep, false, progressPaint)
    }

    // Percentage text
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = arcColor
        textSize = sizePx * 0.24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText("${utilization.toInt()}%", sizePx / 2f, sizePx / 2f + textPaint.textSize * 0.35f, textPaint)

    // Label
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#B0B0B0")
        textSize = sizePx * 0.17f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(label, sizePx / 2f, sizePx - padding - 1f, labelPaint)

    return bitmap
}
