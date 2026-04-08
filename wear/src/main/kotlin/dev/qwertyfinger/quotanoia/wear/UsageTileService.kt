package dev.qwertyfinger.quotanoia.wear

import android.graphics.Color
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Watch tile that displays Claude usage data synced from the phone.
 * No direct API calls — all data comes via Wearable Data Layer.
 */
class UsageTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val store = WearApp.instance.store
        val entries = store.getAll()

        val root = if (entries.isEmpty()) emptyLayout() else accountsLayout(entries)

        val tile = TileBuilders.Tile
            .Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTileTimeline(
                Timeline
                    .Builder()
                    .addTimelineEntry(
                        TimelineEntry
                            .Builder()
                            .setLayout(Layout.Builder().setRoot(root).build())
                            .build(),
                    ).build(),
            ).build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<Resources> =
        Futures
            .immediateFuture(
                Resources.Builder().setVersion("1").build(),
            )

    // ── Layouts ────────────────────────────────────────────

    private fun emptyLayout(): LayoutElement = Box
        .Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
        .addContent(
            Text
                .Builder()
                .setText("Open phone app\nto add accounts")
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_SMALL))
                        .setColor(argb(Color.parseColor("#AAAAAA")))
                        .build(),
                ).build(),
        ).build()

    private fun accountsLayout(entries: List<WearAccountStore.AccountWithUsage>): LayoutElement {
        val column = Column
            .Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        column.addContent(
            Text
                .Builder()
                .setText("quotanoia")
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_SMALL))
                        .setColor(argb(Color.parseColor("#7B9CFF")))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build(),
                ).build(),
        )
        column.addContent(spacer(SPACER_SMALL_DP))

        for (entry in entries.take(MAX_ACCOUNTS_SHOWN)) {
            column.addContent(accountRow(entry.account.name, entry.usage))
            column.addContent(spacer(SPACER_SMALL_DP))
        }

        return Box
            .Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(column.build())
            .build()
    }

    @Suppress("LongMethod")
    private fun accountRow(
        name: String,
        usage: WearAccountStore.WearUsage,
    ): LayoutElement {
        val nameColor = if (usage.hasError) Color.parseColor("#E53935") else Color.parseColor("#E0E0E0")

        val col = Column
            .Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        col.addContent(
            Text
                .Builder()
                .setText(name)
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_ACCOUNT_NAME))
                        .setColor(argb(nameColor))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build(),
                ).setMaxLines(1)
                .build(),
        )

        if (usage.fiveHourPct >= 0) {
            val row = Row
                .Builder()
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)

            val fiveHourLabel = coloredLabel(
                "5h:${usage.fiveHourPct.toInt()}%",
                pctToArgb(usage.fiveHourPct),
                FONT_SIZE_USAGE_LABEL,
            )
            row.addContent(fiveHourLabel)
            row.addContent(
                Text
                    .Builder()
                    .setText("  ")
                    .setFontStyle(FontStyle.Builder().setSize(sp(FONT_SIZE_USAGE_LABEL)).build())
                    .build(),
            )
            val sevenDayLabel = coloredLabel(
                "7d:${usage.sevenDayPct.toInt()}%",
                pctToArgb(usage.sevenDayPct),
                FONT_SIZE_USAGE_LABEL,
            )
            row.addContent(sevenDayLabel)

            col.addContent(row.build())
        } else if (usage.hasError) {
            col.addContent(
                Text
                    .Builder()
                    .setText(usage.errorMsg ?: "Error")
                    .setFontStyle(
                        FontStyle
                            .Builder()
                            .setSize(sp(FONT_SIZE_ERROR))
                            .setColor(argb(Color.parseColor("#E53935")))
                            .build(),
                    ).setMaxLines(1)
                    .build(),
            )
        }

        return col.build()
    }

    private fun coloredLabel(
        text: String,
        color: Int,
        sizeSp: Float,
    ): LayoutElement = Text
        .Builder()
        .setText(text)
        .setFontStyle(
            FontStyle
                .Builder()
                .setSize(sp(sizeSp))
                .setColor(argb(color))
                .setWeight(FONT_WEIGHT_BOLD)
                .build(),
        ).build()

    private fun spacer(heightDp: Float): LayoutElement = Spacer.Builder().setHeight(dp(heightDp)).build()

    private fun pctToArgb(pct: Float): Int {
        val clamped = pct.coerceIn(0f, PCT_MAX)
        return when {
            clamped <= PCT_LOW -> Color.parseColor("#4CAF50")
            clamped <= PCT_MEDIUM -> Color.parseColor("#FFCA28")
            clamped <= PCT_HIGH -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#E53935")
        }
    }

    companion object {
        private const val FRESHNESS_INTERVAL_MS = 5 * 60 * 1000L
        private const val MAX_ACCOUNTS_SHOWN = 3
        private const val SPACER_SMALL_DP = 4f
        private const val FONT_SIZE_SMALL = 12f
        private const val FONT_SIZE_ACCOUNT_NAME = 11f
        private const val FONT_SIZE_USAGE_LABEL = 10f
        private const val FONT_SIZE_ERROR = 9f
        private const val PCT_MAX = 100f
        private const val PCT_LOW = 40f
        private const val PCT_MEDIUM = 70f
        private const val PCT_HIGH = 85f
    }
}
