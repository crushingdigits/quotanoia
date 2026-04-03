package dev.claudewidget.wear

import android.graphics.Color
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.*
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UsageTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        // Kick off background fetch, tile renders from cache
        scope.launch { fetchAll() }

        val store = WearApp.instance.store
        val accounts = store.loadAccounts()

        val root = if (accounts.isEmpty()) emptyLayout() else accountsLayout(accounts, store)

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(5 * 60 * 1000L)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                Layout.Builder().setRoot(root).build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> {
        return Futures.immediateFuture(
            Resources.Builder().setVersion("1").build()
        )
    }

    // ── Layouts ────────────────────────────────────────────

    private fun emptyLayout(): LayoutElement =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                Text.Builder()
                    .setText("Open app\nto add accounts")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(12f))
                            .setColor(argb(Color.parseColor("#AAAAAA")))
                            .build()
                    )
                    .build()
            )
            .build()

    private fun accountsLayout(
        accounts: List<WearAccountStore.WearAccount>,
        store: WearAccountStore,
    ): LayoutElement {
        val column = Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        // Title
        column.addContent(
            Text.Builder()
                .setText("Claude Usage")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(sp(12f))
                        .setColor(argb(Color.parseColor("#7B9CFF")))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        )
        column.addContent(spacer(4f))

        // Show max 3 accounts (watch screen is tiny)
        for (account in accounts.take(3)) {
            val usage = store.getCachedUsage(account.id)
            column.addContent(accountRow(account.name, usage))
            column.addContent(spacer(4f))
        }

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(column.build())
            .build()
    }

    private fun accountRow(
        name: String,
        usage: WearAccountStore.WearUsage,
    ): LayoutElement {
        val hasError = usage.hasError
        val nameColor = if (hasError) Color.parseColor("#E53935") else Color.parseColor("#E0E0E0")

        val col = Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        col.addContent(
            Text.Builder()
                .setText(name)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(sp(11f))
                        .setColor(argb(nameColor))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build()
                )
                .setMaxLines(1)
                .build()
        )

        if (usage.fiveHourPct >= 0) {
            // Compact text display: "5h: 42% | 7d: 71%"
            val fiveColor = pctToArgb(usage.fiveHourPct)
            val sevenColor = pctToArgb(usage.sevenDayPct)

            val row = Row.Builder()
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)

            row.addContent(coloredLabel("5h:${usage.fiveHourPct.toInt()}%", fiveColor, 10f))
            row.addContent(
                Text.Builder()
                    .setText("  ")
                    .setFontStyle(FontStyle.Builder().setSize(sp(10f)).build())
                    .build()
            )
            row.addContent(coloredLabel("7d:${usage.sevenDayPct.toInt()}%", sevenColor, 10f))

            col.addContent(row.build())
        } else if (hasError) {
            col.addContent(
                Text.Builder()
                    .setText(usage.errorMsg ?: "Error")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(9f))
                            .setColor(argb(Color.parseColor("#E53935")))
                            .build()
                    )
                    .setMaxLines(1)
                    .build()
            )
        }

        return col.build()
    }

    private fun coloredLabel(text: String, color: Int, sizeSp: Float): LayoutElement =
        Text.Builder()
            .setText(text)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(sizeSp))
                    .setColor(argb(color))
                    .setWeight(FONT_WEIGHT_BOLD)
                    .build()
            )
            .build()

    private fun spacer(heightDp: Float): LayoutElement =
        Spacer.Builder().setHeight(dp(heightDp)).build()

    private fun pctToArgb(pct: Float): Int {
        val clamped = pct.coerceIn(0f, 100f)
        return when {
            clamped <= 40f -> Color.parseColor("#4CAF50")
            clamped <= 70f -> Color.parseColor("#FFCA28")
            clamped <= 85f -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#E53935")
        }
    }

    // ── Data fetching ───────────────────────────────────────

    private suspend fun fetchAll() {
        val store = WearApp.instance.store
        for (account in store.loadAccounts()) {
            store.cacheUsage(account.id, fetchUsage(account.token))
        }
    }

    private fun fetchUsage(token: String): WearAccountStore.WearUsage {
        return try {
            val request = Request.Builder()
                .url("https://api.anthropic.com/api/oauth/usage")
                .header("Authorization", "Bearer $token")
                .header("anthropic-beta", "oauth-2025-04-20")
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                when (resp.code) {
                    200 -> {
                        val json = JSONObject(resp.body?.string() ?: "{}")
                        WearAccountStore.WearUsage(
                            fiveHourPct = json.getJSONObject("five_hour")
                                .getDouble("utilization").toFloat(),
                            sevenDayPct = json.getJSONObject("seven_day")
                                .getDouble("utilization").toFloat(),
                        )
                    }
                    401 -> WearAccountStore.WearUsage(-1f, -1f, true, "Token expired")
                    429 -> WearAccountStore.WearUsage(-1f, -1f, true, "Rate limited")
                    else -> WearAccountStore.WearUsage(-1f, -1f, true, "HTTP ${resp.code}")
                }
            }
        } catch (e: Exception) {
            WearAccountStore.WearUsage(-1f, -1f, true, e.message ?: "Network error")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
