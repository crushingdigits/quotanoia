package dev.qwertyfinger.quotanoia.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.qwertyfinger.quotanoia.MainActivity
import dev.qwertyfinger.quotanoia.QuotanoiaApp
import dev.qwertyfinger.quotanoia.data.model.AccountState
import dev.qwertyfinger.quotanoia.ui.components.utilizationColor

class UsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
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

private const val WIDGET_CORNER_RADIUS = 16
private const val WIDGET_PADDING = 10
private const val WIDGET_EMPTY_FONT_SIZE = 14
private const val WIDGET_ACCOUNT_SPACING = 6
private const val WIDGET_USAGE_SPACING = 4
private const val WIDGET_USAGE_COL_SPACING = 12
private const val WIDGET_BAR_SIZE = 44
private const val WIDGET_ACCOUNT_NAME_FONT_SIZE = 13
private const val WIDGET_ERROR_FONT_SIZE = 10
private const val BITMAP_SIZE_PX = 80
private const val BITMAP_STROKE_RATIO = 0.12f
private const val BITMAP_PADDING_EXTRA = 2f
private const val BITMAP_TEXT_SIZE_RATIO = 0.24f
private const val BITMAP_TEXT_Y_RATIO = 0.35f
private const val BITMAP_LABEL_SIZE_RATIO = 0.17f
private const val BITMAP_LABEL_PADDING_OFFSET = 1f
private val MUTED_TEXT_COLOR = android.graphics.Color.parseColor("#B0B0B0")

@Suppress("UnstableCollections")
@Composable
private fun WidgetContent(states: List<AccountState>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BG_COLOR)
            .cornerRadius(WIDGET_CORNER_RADIUS.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(WIDGET_PADDING.dp),
    ) {
        if (states.isEmpty()) {
            Text(
                text = "Tap to add accounts",
                style = TextStyle(color = ColorProvider(MUTED_COLOR), fontSize = WIDGET_EMPTY_FONT_SIZE.sp),
            )
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) {
                states.forEach { state ->
                    AccountRow(state)
                    if (state != states.last()) {
                        Spacer(GlanceModifier.height(WIDGET_ACCOUNT_SPACING.dp))
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
                fontSize = WIDGET_ACCOUNT_NAME_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        val usage = state.usage
        if (usage != null) {
            Spacer(GlanceModifier.height(WIDGET_USAGE_SPACING.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UsageBarWidget(usage.fiveHour.utilization, "5h")
                Spacer(GlanceModifier.width(WIDGET_USAGE_COL_SPACING.dp))
                UsageBarWidget(usage.sevenDay.utilization, "7d")
            }
        } else if (hasError) {
            Text(
                text = state.error?.displayMessage.orEmpty(),
                style = TextStyle(color = ColorProvider(ERROR_COLOR), fontSize = WIDGET_ERROR_FONT_SIZE.sp),
            )
        }
    }
}

@Composable
private fun UsageBarWidget(
    utilization: Float,
    label: String,
) {
    val bitmap = renderCircularBarBitmap(utilization, label, BITMAP_SIZE_PX)
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "$label: ${utilization.toInt()}%",
        modifier = GlanceModifier.size(WIDGET_BAR_SIZE.dp),
    )
}

@Suppress("MagicNumber")
private fun renderCircularBarBitmap(
    utilization: Float,
    label: String,
    sizePx: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = sizePx * BITMAP_STROKE_RATIO
    val padding = strokeWidth / 2f + BITMAP_PADDING_EXTRA
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
    val colorMax = 255
    val arcColor = android.graphics.Color.argb(
        (composeColor.alpha * colorMax).toInt(),
        (composeColor.red * colorMax).toInt(),
        (composeColor.green * colorMax).toInt(),
        (composeColor.blue * colorMax).toInt(),
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
        textSize = sizePx * BITMAP_TEXT_SIZE_RATIO
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textX = sizePx / 2f
    val textY = sizePx / 2f + textPaint.textSize * BITMAP_TEXT_Y_RATIO
    canvas.drawText("${utilization.toInt()}%", textX, textY, textPaint)

    // Label
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MUTED_TEXT_COLOR
        textSize = sizePx * BITMAP_LABEL_SIZE_RATIO
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(label, sizePx / 2f, sizePx - padding - BITMAP_LABEL_PADDING_OFFSET, labelPaint)

    return bitmap
}
